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

package org.qubership.itool.tasks.export;

import org.qubership.itool.tasks.FlowTask;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.utils.FSUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractExportVerticle extends FlowTask {

    protected abstract String getExportPath();

    protected abstract void build(String finalExportPath) throws IOException;

    protected WorkerExecutor getWorkerExecutor() {
        return vertx.createSharedWorkerExecutor(getTaskAddress() + "-export-worker-pool"
            , 1
            , 60
            , TimeUnit.MINUTES);
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        String exportPath = getExportPath();
        String finalExportPath = exportPath;


        LOG.info("Scheduling blocking execution of " + getTaskAddress() + " process in a separate thread");
        WorkerExecutor executor = getWorkerExecutor();

        Future blockingFuture = Future.future(promise -> executor.executeBlocking(o -> {
            try {
                createFolder(finalExportPath);
                build(finalExportPath);

            } catch (IOException e) {
                promise.fail(e);
            }

            taskCompleted(taskPromise);
        }, promise));

        blockingFuture
        .onSuccess(r -> taskCompleted(taskPromise))
        .onFailure(r -> {
            report.exceptionThrown(new JsonObject().put("id", "Unexpected"), (Exception) r);
            taskCompleted(taskPromise);
        });
    }

    protected void createFolder(String finalExportPath) {
        String reportFolderName = FSUtils.getFolder(finalExportPath);
        File reportFolder = new File(reportFolderName);
        if (!reportFolder.exists()) {
            reportFolder.mkdirs();
        }
    }

}
