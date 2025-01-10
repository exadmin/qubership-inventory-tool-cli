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

package org.qubership.itool.tasks.parsing.configuration;

import org.qubership.itool.tasks.parsing.AbstractParseFileDataTask;

import io.vertx.core.json.JsonObject;

import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;

public class ParseDockerFileVerticle extends AbstractParseFileDataTask {

    protected Logger LOGGER = LoggerFactory.getLogger(ParseDockerFileVerticle.class);

    @Override
    protected List<Map<String, JsonObject>> getTuples() {
        List<Map<String, JsonObject>> tuples = V()
            .hasType("domain").as("D")
            .out().as("C")
            .out().hasType("file").has("name", "Dockerfile").as("F")
            .<JsonObject>select("D", "C", "F")
            .toList();
        return tuples;
    }

    @Override
    protected void processFile(JsonObject domain, JsonObject component, JsonObject file) {
        BufferedReader reader = new BufferedReader(new StringReader(file.getString("content", "")));

        String from = null;
        try {
            from = readFrom(reader);
        } catch (IOException neverHappens) {
            // do nothing
        }
        if (from == null) {
            return;
        }

        JsonObject details = JsonUtils.getOrCreateJsonObject(component, "details");
        details.put("dockerfile", new JsonObject().put("imageRoot", from));
    }

    private String readFrom(BufferedReader reader) throws IOException {
        for (;;) {
            String from = reader.readLine();
            if (from == null)
                return null;
            from = from.trim();
            if (from.startsWith("#")) {
                continue;
            }
            // The first non-comment line found
            if (! from.startsWith("FROM "))
                return null;
            return from.replace("FROM ", "");
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
