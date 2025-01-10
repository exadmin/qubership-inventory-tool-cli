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

package org.qubership.itool.tasks.ci;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.io.File;

import org.apache.commons.codec.digest.DigestUtils;
import org.qubership.itool.modules.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.tasks.FlowTask;

import static org.qubership.itool.cli.ci.CiConstants.*;


public class SaveSingleResultVerticle extends FlowTask {
    protected Logger LOGGER = LoggerFactory.getLogger(SaveSingleResultVerticle.class);

    @Override
    protected String[] features() {
        return new String[]{"unskippable"};
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        JsonObject config = config();

        String dumpFile = config.getString(P_OUTPUT_FILE);
        String dumpDir = config.getString(P_OUTPUT_DIRECTORY);
        File dumpDirAsFile;

        if (dumpFile == null) { // Generate output file name automatically
            String repository = config.getString(P_REPOSITORY);
            String dumpBy = config.getString(P_DUMP_BY, DUMP_BY_HASH);
            String name;
            switch (dumpBy) {
//            case DUMP_BY_HASH:
            default:
                name = DigestUtils.md5Hex(repository.getBytes());
                break;
            case DUMP_BY_ID:
                JsonObject targetComp = V().out().has("repository", repository).next();
                if (targetComp != null) {
                    name = targetComp.getString(Graph.F_ID);
                    break;
                }
                // Component not found by repository. Fall back to repo name
            case DUMP_BY_REPO:
                name = config.getString(P_RUN_NAME);
            }

            dumpFile = "result." + name + ".json" /*+ ".gz"*/; // We may do it!

            if (dumpDir == null) {  // Should not happen, must be already filled from defaults
                dumpDir = config.getString(P_DEFAULT_OUTPUT_DIRECTORY);
            }
            dumpDirAsFile = new File(dumpDir);

        } else if (dumpDir == null) {   // Should not happen, must be already filled from defaults
            if (dumpFile.contains("/") || dumpFile.contains("\\")) {    // Absolute or relative path provided
                File dump = new File(dumpFile).getAbsoluteFile();
                dumpDirAsFile = dump.getParentFile();
                dumpFile = dump.getName();
            } else {    // Only file name: use default output dir
                dumpDir = config.getString(P_DEFAULT_OUTPUT_DIRECTORY);
                dumpDirAsFile = new File(dumpDir);
            }

        } else {
            dumpDirAsFile = new File(dumpDir);
        }

        flowContext.dumpDataToFile(dumpDirAsFile, dumpFile);

        taskCompleted(taskPromise);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
