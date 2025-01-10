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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.FSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class RepositoriesCopyToSuperRepositoryVerticle extends AbstractAggregationTaskVerticle {
    protected Logger LOGGER = LoggerFactory.getLogger(RepositoriesCopyToSuperRepositoryVerticle.class);

    @Override
    protected String[] features() {
        return new String[]{"repositoryUpdate", "unskippable"};
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        vertx.fileSystem().readFile(FSUtils.getConfigFilePath(config(), "config", "resultsToCommit.json"))
                .map(fileContents -> new JsonObject(fileContents))
                .compose(jsonResult -> {
                    @SuppressWarnings("rawtypes")
                    List<Future> copiedResultsFutures = jsonResult.stream().map(
                            entry -> {
                                Path targetPath = ConfigUtils.getSuperRepoFilePath(config(), (String) entry.getValue());
                                String targetDir = targetPath.toString();
                                Future<Void> copiedResultFuture = vertx.fileSystem().exists(targetDir)
                                        .compose(exists -> {
                                            Future<Void> result = Future.succeededFuture();
                                            if (exists) {
                                                result = vertx.fileSystem().deleteRecursive(targetDir, true);
                                            }
                                            return result;
                                        })
                                        .compose(rr -> Files.isRegularFile(Path.of(entry.getKey()))
                                                ? vertx.fileSystem().mkdirs(targetPath.getParent().toString())
                                                : vertx.fileSystem().mkdirs(targetDir))
                                        .compose(
                                                rr -> vertx.fileSystem().copyRecursive(
                                                        Path.of(entry.getKey()).toString(),
                                                        targetDir,
                                                        true)
                                        );
                                return copiedResultFuture;
                            })
                            .collect(Collectors.toList());
                    return CompositeFuture.join(copiedResultsFutures);
                })
                .onSuccess(r -> {
                    LOG.info("{} entries copied", r.size());
                    taskCompleted(taskPromise);
                })
                .onFailure(r -> {
                    report.internalError("Failed to copy data from result (" + ExceptionUtils.getStackTrace(r) + ")");
                    taskCompleted(taskPromise);
                });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
