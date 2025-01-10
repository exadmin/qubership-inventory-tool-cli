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

package org.qubership.itool.tasks.parsing.go;

import org.qubership.itool.tasks.parsing.AbstractParseFileTask;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.TechNormalizationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_VERSION;
import static org.qubership.itool.utils.LanguageUtils.LANGUAGE_PATH_POINTER;

public class ParseGoModFileVerticle extends AbstractParseFileTask {

    private static final Object GO_LANGUAGE_NAME = "GoLang";
    protected Logger LOGGER = LoggerFactory.getLogger(ParseGoModFileVerticle.class);

    protected final Pattern MODULE_NAME_PATTERN = Pattern.compile("^module(\\s+).*$", CASE_INSENSITIVE);
    protected final Pattern GO_VERSION_PATTERN = Pattern.compile("^go(\\s+).*$", CASE_INSENSITIVE);
    protected final Pattern REQUIRE_PATTERN = Pattern.compile("^require(\\s+).*$", CASE_INSENSITIVE);

    @Override
    protected String[] getFilePatterns() {
        return new String[]{"**/go.mod"};
    }

    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName) throws IOException {
        String goModSource = FSUtils.readFileSafe(fileName);
        if (goModSource == null) {
            getLogger().warn("{}: File '{}' is missing contents", component.getString("id"), fileName);
            return;
        }
        getLogger().debug("{}: Found file '{}', parsing it", component.getString("id"), fileName);
        JsonObject module = null;
        Iterator<String> linesIterator = Arrays.stream(goModSource.split("[\\n\\r]+")).iterator();
        List<String> linesBeforeModule = new ArrayList<>();
        boolean moduleDetected = false;
        String goVersion;
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            if (!moduleDetected  && !MODULE_NAME_PATTERN.matcher(line).matches() && !line.matches("require\\s*\\(") &&
                    !GO_VERSION_PATTERN.matcher(line).matches())
            {
                linesBeforeModule.add(line);
                continue;
            }
            if (MODULE_NAME_PATTERN.matcher(line).matches()) {
                String moduleName = line.replaceAll("module\\s+", "");
                module = addModule(moduleName, component);
                moduleDetected = true;
            } else if (moduleDetected && REQUIRE_PATTERN.matcher(line).matches()) {
                getLogger().debug("{}: parsing dependencies", component.getString("id"));
                parseDependencies(linesBeforeModule.iterator(), component, module);
                linesBeforeModule.clear();
                parseDependencies(linesIterator, component, module);
            } else if (GO_VERSION_PATTERN.matcher(line).matches()) {
                JsonObject detectedVersion = TechNormalizationHelper.normalizeTechAsJson(line);
                if (detectedVersion != null) {
                    Object versionsObj = LANGUAGE_PATH_POINTER.queryJson(component);
                    List<JsonObject> versions = JsonUtils.asList(versionsObj);
                    if (versions == null) {
                        versions = new ArrayList<>();
                    }

                    AtomicBoolean versionFound = new AtomicBoolean(false);
                    List<JsonObject> languageVersions = versions.stream()
                            .map(version -> updateGoVersions(version, detectedVersion.getString(F_VERSION), versionFound))
                            .collect(Collectors.toList());
                    if (!versionFound.get()) {
                        getLogger().debug("{}: New language version was added: {}", component.getString(F_ID), detectedVersion.encode());
                        languageVersions.add(detectedVersion);
                    }

                    LANGUAGE_PATH_POINTER.writeJson(component, new JsonArray(languageVersions), true);
                }
            }
        }
    }

    private JsonObject updateGoVersions(JsonObject language, String detectedVersion, AtomicBoolean versionFound) {
        if (GO_LANGUAGE_NAME.equals(language.getString(F_NAME))) {
            String languageVersion = language.getString(F_VERSION);
            if (languageVersion == null && detectedVersion != null) {
                language.put(F_VERSION, detectedVersion);
                versionFound.set(true);
                getLogger().debug("GoLang version is populated from the go.mod file: {}", detectedVersion);
            } else if (languageVersion != null && languageVersion.equals(detectedVersion)) {
                versionFound.set(true);
            }
        }
        return language;
    }

    private JsonObject addModule(String name, JsonObject component) {
        JsonObject module;
        String[] artifact = name.split("/");
        String[] groupId = artifact[0].replaceAll("\\s","").split("\\.");
        String reversedResult = generateGroupId(groupId);
        String artifactId = name.replaceAll("^.+?/", "");
        module = new JsonObject()
                .put("artifactId", artifactId)
                .put("groupId", reversedResult)
                .put("package", "golang")
                .put("type", "library")
                .put("version", "unknown");
        generateId(module);
        graph.addVertex(module, component);
        JsonObject dependencyEdge = new JsonObject()
                .put("type", "module")
                .put("component", component.getString("id"));
        graph.addEdge(component, module, dependencyEdge);
        getLogger().debug("{}: Added module with name {} based on value in go.mod", component.getString("id"), name);
        return module;
    }

    private void parseDependencies(Iterator<String> linesIterator, JsonObject component, JsonObject module) {

        Set<String> goDependenciesSet = new HashSet<>();
        while (linesIterator.hasNext()) {
            String nextLine = linesIterator.next();
            if (StringUtils.isBlank(nextLine))
                continue;
            if (nextLine.contains(")")) {
                break;
            }

            if (StringUtils.isNotBlank(nextLine)) {
                goDependenciesSet.add(nextLine.trim());
            }
        }
        List<JsonObject> directDependencies = new ArrayList<>();
        List<JsonObject> indirectDependencies = new ArrayList<>();
        List<String> indirectList = new ArrayList<>();
        for (String goDependency : goDependenciesSet) {
            if (goDependency.contains("indirect")) {
                indirectList.add(goDependency);
                continue;
            } else if (goDependency.equals("require (")) {
                continue;
            }
            else if (goDependency.matches("^require\\s+.+")){
                directDependencies.add(processDependency(goDependency.replaceAll("^require\\s+","")));
            }
            try {
                directDependencies.add(processDependency(goDependency));
            } catch (Exception e) {
                getLogger().error(e.getMessage() + goDependency);
            }
        }
        for (String indrectDependency : indirectList) {
            try {
                indrectDependency = indrectDependency.replaceAll("\\s/+\\sindirect$", "");
                indirectDependencies.add(processDependency(indrectDependency));
            } catch (Exception e) {
                getLogger().error(e.getMessage(), indrectDependency);
            }

        }
        for (JsonObject destination : directDependencies) {
            addDependencyEdge(component.getString(F_ID), destination, module, false);
        }
        for (JsonObject destination : indirectDependencies) {
            addDependencyEdge(component.getString(F_ID), destination, module, true);
        }
    }

    private JsonObject processDependency(String dependency) {
        try {
            String[] artifact;
            String artifactId;
            String groupId;
            if (dependency.contains("/")) {
                artifact = dependency.split("/");
                artifactId =  dependency.replaceAll("^.+?/(.+)\\s+.+", "$1");
            } else {
                artifact = dependency.split(" ");
                artifactId = dependency.replaceAll("\\s(.+)\\s.+", "$1");
            }
            String[] url = artifact[0].replaceAll("\\s","").split("\\.");
            groupId = generateGroupId(url);
            String version = dependency.replaceAll(".+\\s(.+)", "$1").replaceAll("v","");
            JsonObject dependencyNode =  new JsonObject()
                    .put("artifactId", artifactId)
                    .put("groupId", groupId)
                    .put("package", "golang" )
                    .put("version", version)
                    .put("type", "library");
            generateId(dependencyNode);
            return dependencyNode;
        } catch (Exception e) {
            getLogger().error(e.getMessage(), dependency);
            return null;
        }
    }

    private void addDependencyEdge(String componentId, JsonObject destination, JsonObject module, Boolean transitive) {
        JsonObject dependencyEdge = new JsonObject()
                .put("type", "dependency")
                .put("scope", "compile")
                .put("transitive", transitive)
                .put("component", componentId);
        graph.addEdge(module, destination, dependencyEdge);
    }


    private String generateGroupId(String [] url){
        StringBuilder reversedString = new StringBuilder();

        for (int i = url.length - 1; i >= 0; i--) {
            reversedString.append(url[i]);

            if (i != 0) {
                reversedString.append(".");
            }
        }
        return reversedString.toString();
    }

    private void generateId(JsonObject destination) {
        destination.put(F_ID, new StringBuffer()
                .append(destination.getString("groupId")).append(":")
                .append(destination.getString("artifactId")).append(":")
                .append(destination.getString("package")).append(":")
                .append(destination.getString("version"))
                .toString()
        );
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
