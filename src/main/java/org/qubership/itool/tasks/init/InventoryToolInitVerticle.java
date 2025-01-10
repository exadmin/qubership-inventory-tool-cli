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

package org.qubership.itool.tasks.init;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.qubership.itool.cli.config.ConfigProvider.HIDDEN_PROPERTIES;


public class InventoryToolInitVerticle extends FlowTask {
    protected Logger LOGGER = LoggerFactory.getLogger(InventoryToolInitVerticle.class);

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        getLogger().debug("Initializing the tool for a flow. Graph instance is: Graph @{}", System.identityHashCode(graph));
        ConfigUtils.getFilesFromJsonConfig(vertx, config(), JsonPointer.from("/files"), "config", "cleanupConfig.json")
                // Clean up of working directory
                .compose(files -> {
                    @SuppressWarnings("rawtypes")
                    List<Future> futures = files.stream()
                            .map(f -> vertx.fileSystem().deleteRecursive(f.toString(), true)
                                    .recover(e -> reportDeleteFailure(e, f)))
                            .collect(Collectors.toList());
                    return CompositeFuture.join(futures);
                })
                // Save effective config
                .compose(r -> saveConfigFuture())
                .onComplete(r -> taskCompleted(taskPromise));

    }

    private Future<Void> reportDeleteFailure(Throwable e, Path f) {
        if (   e instanceof io.vertx.core.file.FileSystemException
            && e.getCause() instanceof java.nio.file.NoSuchFileException)
        {
            getLogger().info("Failed to delete {} : file not found", f);
        } else {
            report.internalError("Failed to delete " + f + " : "
                + ExceptionUtils.getStackTrace(e));
        }
        return Future.succeededFuture();
    }

    private Future<Object> saveConfigFuture() {
        return Future.future(promise -> {
            JsonObject effectiveConfig = new JsonObject(new HashMap<>(config().getMap()));  // Shallow clone
            HIDDEN_PROPERTIES.forEach(
                    pr -> effectiveConfig.getMap().replace(pr, "*****"));
            getLogger().info("Effective config: {}", effectiveConfig);

            try {
                Path eConfigPath = Path.of("output", "effectiveConfig.json");
                if (!eConfigPath.getParent().toFile().exists()) {
                    eConfigPath.getParent().toFile().mkdirs();
                }
                JsonUtils.saveJson(eConfigPath, effectiveConfig, false);
            } catch (IOException e) {
                promise.fail("Couldn't save effective config: " + ExceptionUtils.getStackTrace(e));
            }
            promise.complete();
        }).onFailure(e -> report.internalError(e.getMessage()));
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
