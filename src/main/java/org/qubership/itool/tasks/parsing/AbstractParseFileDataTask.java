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

package org.qubership.itool.tasks.parsing;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * Common subclass for data parsers reading from "file" and "directory" elements of the graph
 */
public abstract class AbstractParseFileDataTask extends FlowTask {

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {

        List<Map<String, JsonObject>> tuples = getTuples();

        for (Map<String, JsonObject> tuple: tuples) {
            JsonObject domain = tuple.get("D");
            JsonObject component = tuple.get("C");
            JsonObject file = tuple.get("F");
            try {
                processFile(domain, component, file);
            } catch (Exception e) {
                report.addMessage(
                    "configuration", component,
                    e.getMessage() + " // File " + file.getString("path") + " [" + component.getString("id") + "]");
            }
        }

        taskCompleted(taskPromise);
    }

    /**
     * Get data tuples tuples of the following structure: "D" - domain, "C" - component, "F" - file
     *
     * @return Triplets in form (domain, component, config file entry)
     */
    protected abstract List<Map<String, JsonObject>> getTuples();

    protected abstract void processFile(JsonObject domain, JsonObject component, JsonObject file);

}
