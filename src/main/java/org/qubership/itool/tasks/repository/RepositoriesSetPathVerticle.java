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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.utils.ConfigProperties.SUPER_REPOSITORY_DIR_POINTER;
import static org.qubership.itool.utils.ConfigProperties.SUPER_REPOSITORY_MODULES_DIR_POINTER;


public class RepositoriesSetPathVerticle extends AbstractAggregationTaskVerticle {
    protected Logger LOGGER = LoggerFactory.getLogger(RepositoriesSetPathVerticle.class);

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        List<Map<String, JsonObject>> jsonObjectList =
            V().hasType("domain").as("domain")
            .out().hasKeys(F_REPOSITORY).as("component")
            .<JsonObject>select("domain", "component").toList();

        for (Map<String, JsonObject> json : jsonObjectList) {
            JsonObject component = json.get("component");
            JsonObject domain = json.get("domain");

            String directoryPath = buildDirectoryPath(
                      ConfigUtils.getConfigValue(SUPER_REPOSITORY_DIR_POINTER, config())
                    , ConfigUtils.getConfigValue(SUPER_REPOSITORY_MODULES_DIR_POINTER, config())
                    , component, domain)
                    .getPath();
            component.put("directoryPath", directoryPath);
        }

        taskCompleted(taskPromise);
    }

    private File buildDirectoryPath(String cloneRootFolderName, String submodulesDir, JsonObject component, JsonObject domain) {
        String domainId = domain.getString(F_ID);
        String compId = component.getString(F_ID);

        // Try: new path in updated superrepo
        File dir = Path.of(cloneRootFolderName, submodulesDir, domainId, compId).toFile();
        if (dir.isDirectory())
            return dir;

        // Otherwise, use old path in old superrepo. No other alternatives known, no reason to check existence.
        dir = Path.of(cloneRootFolderName, submodulesDir, domainId.replaceFirst("^D_", ""), compId).toFile();
        return dir;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
