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

package org.qubership.itool.tasks.repository;

import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.git.GitAdapter;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.List;

import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;

public class RepositoriesSetRevisionVerticle extends AbstractAggregationTaskVerticle {
    protected Logger LOGGER = LoggerFactory.getLogger(RepositoriesSetRevisionVerticle.class);

    @Resource
    @Nullable
    private GitAdapter gitAdapter;

    @Override
    protected String[] features() {
        return new String[]{"repositoryUpdate"};
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        if (gitAdapter == null) {
            getLogger().warn("Offline mode. Exitting.");
            taskCompleted(taskPromise);
            return;
        }

        List<JsonObject> components =
                V().hasType("domain")
                        .out().hasKeys("directoryPath").as("component")
                        .<JsonObject>select("component").toList();
        gitAdapter.openSuperrepository()
                .compose(sr -> gitAdapter.submodulesCheckout(sr,
                        ConfigUtils.getConfigValue(RELEASE_POINTER, config()), components))
                .onFailure(f -> report.internalError("Failed to update repository (" + ExceptionUtils.getStackTrace(f) + ")"))
                .onComplete(f -> taskCompleted(taskPromise));
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
