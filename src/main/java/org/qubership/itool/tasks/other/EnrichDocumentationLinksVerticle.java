/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.tasks.other;

import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.GitUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.qubership.itool.modules.graph.Graph.F_DIRECTORY;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;

public class EnrichDocumentationLinksVerticle extends AbstractAggregationTaskVerticle {
    protected Logger LOGGER = LoggerFactory.getLogger(EnrichDocumentationLinksVerticle.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    private static String[] patterns = new String[] {
            "ReadMe.md",
            "docs",
            "documents",
            "documentation"
    };
    private JsonPointer docsPtr = JsonPointer.from("/details/documentationLink");

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        Integer coresCount = CpuCoreSensor.availableProcessors();
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("enrichment-worker-pool"
                , coresCount
                , 10
                , TimeUnit.MINUTES);

        List<JsonObject> components = V().hasType(V_DOMAIN)
                .out().hasKeys(F_DIRECTORY)
                .toList();

        List<Future> blockingFutures = new ArrayList<>();
        for (JsonObject component : components) {
            blockingFutures.add(executor.executeBlocking(promise -> enrichComponentWithDocs(component, promise)));
        }
        completeCompositeTask(blockingFutures, taskPromise);
    }

    private void enrichComponentWithDocs(JsonObject component, Promise<Object> promise) {
        JsonArray additionalDocLinks = new JsonArray();
        String directoryPath = FSUtils.getComponentDirPath(component);
        for (String pattern : patterns) {
            File file = new File(directoryPath, pattern);
            if (file.exists()) {
                try {
                    additionalDocLinks.add(GitUtils.buildRepositoryLink(component, file.getCanonicalPath(), config()));
                } catch (IOException e) {
                    report.exceptionThrown(component, e);
                }
            }
        }
        JsonArray documentation = JsonUtils.getOrCreateJsonArray(component, docsPtr);
        documentation.addAll(additionalDocLinks);
        if (!documentation.isEmpty()) {
            docsPtr.writeJson(component, documentation, true);
        }
        promise.complete();
    }

}
