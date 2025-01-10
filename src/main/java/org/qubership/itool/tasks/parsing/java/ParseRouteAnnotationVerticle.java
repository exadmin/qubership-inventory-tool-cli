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

package org.qubership.itool.tasks.parsing.java;

import org.qubership.itool.tasks.parsing.AbstractParseFileTask;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.qubership.itool.utils.FSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseRouteAnnotationVerticle extends AbstractParseFileTask {

    protected Logger LOG = LoggerFactory.getLogger(ParseRouteAnnotationVerticle.class);

    final Pattern GATEWAY_TYPE_PATTERN = Pattern.compile("(PUBLIC|PRIVATE|INTERNAL|FACADE)", Pattern.CASE_INSENSITIVE);
    final String ROUTE_ANNOTATION = "@Route";

    @Override
    protected String[] getFilePatterns() {
        return new String[] { "*.java" };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName) throws IOException {
        getLogger().trace("{}: Searching for @Route annotation in file '{}'", component.getString("id"), fileName);

        JsonPointer pluginsPointer = JsonPointer.from("/details/gateways");
        Set<String> gatewaysSet = new HashSet<>();
        JsonArray gateways = (JsonArray) pluginsPointer.queryJson(component);
        if (gateways != null) {
            gatewaysSet.addAll(gateways.getList());
        }

        String fileContents = FSUtils.readFileSafe(fileName);
        String[] fileLines = fileContents.split(LINE_BREAK_REGEX);
        String componentId = component.getString("id");
        for (String fileLine : fileLines) {
            if (fileLine.startsWith(ROUTE_ANNOTATION)) {

                Matcher matcher = GATEWAY_TYPE_PATTERN.matcher(fileLine);
                if (matcher.find()) {
                    String gateway = matcher.group().toLowerCase();
                    gatewaysSet.add(gateway);
                    getLogger().debug("{}: Route configuration '{}' is found in file '{}'", componentId, gateway, fileName);
                    break;
                }
            }
        }
        if (!gatewaysSet.isEmpty()) {
            pluginsPointer.writeJson(component, new JsonArray(gatewaysSet.stream().collect(Collectors.toList())));
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
