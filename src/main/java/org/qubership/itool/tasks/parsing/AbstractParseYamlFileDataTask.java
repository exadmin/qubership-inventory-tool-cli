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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public abstract class AbstractParseYamlFileDataTask extends AbstractParseFileDataTask {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractParseYamlFileDataTask.class);


    @Override
    protected void processFile(JsonObject domain, JsonObject component, JsonObject file) {
        JsonArray sections = file.getJsonArray("structured");
        String filePath = file.getString("path");
        if (sections == null) {
            report.internalError("File " + filePath + " (" + file.getString("id") + ") contains no data or could not be parsed");
            return;
        }
        int index = 0;
        for (Object section: sections) {
            processYamlSection(domain, component, file, index++, section);
        }
    }

    /* Process a single section of YAML document already stored in the graph. Assume that file data
     * are always JSON models rather than raw Maps and arrays/Lists.
     * */
    protected void processYamlSection(JsonObject domain, JsonObject component, JsonObject file, int index, Object data) {
        if (data instanceof JsonObject) {
            processYamlSection(domain, component, file, index, (JsonObject)data);
        } else if (data instanceof JsonArray) {
            processYamlSection(domain, component, file, index, (JsonArray)data);
        } else if (data == null) {
            // An empty section. Just skip it.
        } else {
            String fileLink = file.getString("fileLink");
            report.internalError("File " + fileLink + " (" + file.getString("id")
                + ") contains unrecognized section at index " + index);
        }
    }

    /* Process a single Object section of YAML document */
    protected void processYamlSection(JsonObject domain, JsonObject component, JsonObject file, int index, JsonObject data) {
        String fileLink = file.getString("fileLink");
        report.internalError("File " + fileLink + " (" + file.getString("id") + ") contains Object section at index " + index
                + ", but task " + getClass().getName() + " does not support it");
    }

    /* Process a single List section of YAML document */
    protected void processYamlSection(JsonObject domain, JsonObject component, JsonObject file, int index, JsonArray data) {
        String fileLink = file.getString("fileLink");
        report.internalError("File " + fileLink + " (" + file.getString("id") + ") contains Array section at index " + index
                + ", but task " + getClass().getName() + " does not support it");
    }

}
