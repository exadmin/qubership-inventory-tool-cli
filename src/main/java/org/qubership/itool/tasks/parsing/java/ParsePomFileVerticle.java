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

import io.vertx.core.json.JsonObject;

import org.qubership.itool.utils.LanguageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  See <a href="https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-source-and-target.html">this article</a>
 *  for more details on setting java versions for maven compiler plugin
 */
public class ParsePomFileVerticle extends AbstractParseFileTask {
    protected Logger LOGGER = LoggerFactory.getLogger(ParsePomFileVerticle.class);

    @Override
    protected String[] getFilePatterns() {
        return new String[] {
            "pom.xml"
        };
    }

    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName) {
        LanguageUtils.updateDetailsLanguagesUsingPomFile(graph, component);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
