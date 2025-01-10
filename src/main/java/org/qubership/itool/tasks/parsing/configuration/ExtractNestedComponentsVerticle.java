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

import org.qubership.itool.tasks.parsing.AbstractParseFileTask;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.F_DIRECTORY;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_UNKNOWN;

/**
 * Extract nested components from top-level components.
 *
 * <p>This task bootstraps with pre-existing set of components (top ones), creates additional components
 * (not processed by itself), and alters some attributes of top components that are accounted by other
 * subclasses of {@link AbstractParseFileTask} later.
 */
public class ExtractNestedComponentsVerticle extends AbstractParseFileTask {

    protected static final Logger LOG = LoggerFactory.getLogger(ExtractNestedComponentsVerticle.class);

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected String[] getFilePatterns() {
        return new String[] { "inventory-components.json" };
    }

    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject topComponent, String fileName) throws IOException {

        JsonObject inventory = JsonUtils.readJsonFile(fileName);

        JsonArray componentDescriptions = inventory.getJsonArray("components");
        if (componentDescriptions == null) {
            // Leave the top component as is, though it is not listed
            return;
        }

        String releaseBranch = (String) JsonPointer.from("/details/releaseBranch").queryJson(topComponent);
        String domainFromInventory = inventory.getString("domain");
        domainFromInventory = ConfigUtils.fillDomainId(domainFromInventory);

        Graph graph = this.graph;

        //Handle the top component descriptor first
        JsonObject topComponentDescriptions = getTopComponentDescriptions(componentDescriptions);
        if (topComponentDescriptions != null) {
            // Remove the descriptor from further processing
            componentDescriptions.remove(topComponentDescriptions);

            // Relocate initial top component if provided ID in inventory file doesn't match the initial ID
            if (!topComponentDescriptions.getString(F_ID).equals(topComponent.getString(F_ID))) {
                LOG.info("Initial top component ID ({}) didn't match the ID of the top component in inventory-componentns " +
                        "file, relocating it to new id {}", topComponent.getString(F_ID), topComponentDescriptions.getString(F_ID));
                graph.relocateVertex(topComponent, topComponentDescriptions.getString(F_ID));
            }

            processTopComponent(topComponent, topComponentDescriptions, domainFromInventory);
            // Handle excluded dirs for top component
            JsonArray excludedDirs = getDirsToExclude(componentDescriptions);
            topComponent.put("excludeDirs", excludedDirs);
            getLogger().info("Following subdirectories are excluded from top component: {}", excludedDirs);
        } else {
            getLogger().info("Root component {} not present in directory '{}' and will be removed from the graph",
                    topComponent.getString(F_ID), topComponent.getString(F_DIRECTORY));
            graph.removeVertex(topComponent);
        }

        // Go through remaining components
        for (Object descriptionObject: componentDescriptions) {
            JsonObject componentDescription = (JsonObject) descriptionObject;
            String id = componentDescription.getString("id");

            String directory = getDirectory(componentDescription);

            JsonObject detailsJson = new JsonObject()
                    .put("domain", domainFromInventory)
                    .put("domainFromInventory", domainFromInventory)
                    .put("releaseBranch", releaseBranch)
                    .put("abbreviation", id);
            JsonObject newComponent = new JsonObject()
                    .put(F_ID, id)
                    .put(F_TYPE, V_UNKNOWN)
                    .put(F_DIRECTORY, Path.of(topComponent.getString(F_DIRECTORY), directory).toString())
                    .put(F_REPOSITORY, topComponent.getString(F_REPOSITORY))
                    .put("repositorySubDir", directory)
                    .put("details", detailsJson);

            // Get or create the domain in the graph and put the nested component into it.
            // The top component may be relocated into appropriate domain by RelocateComponentsVerticle later.
            JsonObject newDomain = V(domainFromInventory).next();
            if (newDomain == null) {
                newDomain = new JsonObject()
                        .put(F_ID, domainFromInventory)
                        .put(F_TYPE, "domain")
                        .put("department", domain.getString("department", V_UNKNOWN))
                        .put(F_MOCK_FLAG, true);
                graph.addVertexUnderRoot(newDomain);
            }
            processNewComponent(topComponent, graph, newComponent, newDomain);

        }
    }

    private JsonArray getDirsToExclude(JsonArray componentDescriptions) {
        return new JsonArray(componentDescriptions.stream().map(o -> (JsonObject) o)
                .map(ExtractNestedComponentsVerticle::getDirectory)
                .collect(Collectors.toList()));

    }

    private static String getDirectory(JsonObject componentDescription) {
        return componentDescription.getString("directory", "")
                .replaceFirst("^\\.?/*", "")  // Remove starting ".", "./", "/" -- FIXME: but not ".dir/"
                .replaceFirst("/+$", ""); // Remove trailing slashes
    }

    private JsonObject getTopComponentDescriptions(JsonArray componentDescriptions) {
        for (Object o : componentDescriptions) {
            JsonObject componentDescriptor = (JsonObject) o;
            String directory = getDirectory(componentDescriptor);
            if (directory.isEmpty()) {
                return componentDescriptor;
            }
        }
        return null;
    }

    private void processNewComponent(JsonObject topComponent, Graph graph, JsonObject newComponent, JsonObject newDomain) {
        String id = newComponent.getString(F_ID);
        if (graph.getVertex(id) == null) {
            getLogger().info("Adding new component placeholder ({}) to graph", newComponent.getString(F_ID));
            boolean isAdded = graph.addVertex(newDomain, newComponent);
            if (!isAdded) {
                report.addMessage(GraphReport.ERROR, topComponent, "Failed to add component '" + newComponent + "' from multi-component repository");
            }
        } else {
            report.addMessage(GraphReport.CONF_ERROR, newComponent, "The component with same ID is already " +
                        "present in multi-component repository");
        }
    }

    private void processTopComponent(JsonObject topComponent, JsonObject componentDescription, String domainFromInventory) {
        getLogger().info("Processing top component '{}'", topComponent.getString(F_ID));
        JsonObject topDetails = JsonUtils.getOrCreateJsonObject(topComponent, "details");
        topDetails.put("abbreviation", componentDescription.getString("id"));
        topDetails.put("domainFromInventory", domainFromInventory);
    }

}
