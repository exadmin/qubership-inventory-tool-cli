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

package org.qubership.itool.tasks.parsing.other;

import org.qubership.itool.tasks.parsing.AbstractParseFileTask;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.qubership.itool.utils.FSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class ParseSearchPasswordInYamlVerticle extends AbstractParseFileTask {
    protected Logger LOGGER = LoggerFactory.getLogger(ParseSearchPasswordInYamlVerticle.class);

    private static Pattern pattern = Pattern.compile("^\\s*(?![#\\s])-?.*?(password(?!\\w).*?[=:]" +
            "\\s*((\\$\\{\\S{3,}\\s*:\\s*\\S{3,}\\})|((?!\\$\\{.*\\})(?!.*\\{\\{.*\\}\\})\\S{3,})|(\\S\\{\\{(?!\\s*\\.Values\\.\\w+).*\\}\\})).*?)$",  CASE_INSENSITIVE);

    @Override
    protected String[] getFilePatterns() {
        return new String[] {"*.yml", "*.yaml"};
    }

    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName) throws IOException {
        JsonObject details = component.getJsonObject("details");
        if (details == null) {
            return;
        }

        String yamlSource = FSUtils.readFileSafe(fileName);
        if (yamlSource == null) {
            return;
        }

        JsonObject fileEntry = new JsonObject();
        JsonArray passwordsList = new JsonArray();
        if (fileName.toLowerCase(Locale.ROOT).contains("dev")) {
            fileEntry.put("profile", "dev");
        }
        fileEntry.put("passwords", passwordsList);
        for (String row : yamlSource.split("\n\r|\r\n|\r|\n")) {
            if (checkPattern(row)) {
                passwordsList.add(row);
            }
        }

        if (!passwordsList.isEmpty()) {
            LOGGER.info("Plain password in YAML found for " + component.getString("id"));
            JsonObject passwordsPerFile = details.getJsonObject("passwords");
            if (null == passwordsPerFile) {
                passwordsPerFile = new JsonObject();
                details.put("passwords", passwordsPerFile);
            }
            passwordsPerFile.put(FSUtils.relativePath(component, fileName), fileEntry);
        }
    }

    public boolean checkPattern(String row){
        Matcher matcher = pattern.matcher(row);
        return  matcher.matches();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
