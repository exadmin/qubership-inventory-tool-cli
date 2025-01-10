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

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_UNKNOWN;

public class RelocateComponentsVerticle extends FlowTask {
    protected Logger LOGGER = LoggerFactory.getLogger(RelocateComponentsVerticle.class);

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        Graph graph = this.graph;
        String mockDomainId = config().getString(P_MOCK_DOMAIN);
        JsonObject mockDomain = V(mockDomainId).next();
        List<JsonObject> components = V(mockDomainId).out().toList();

        for (JsonObject comp: components) {
            String compId = comp.getString(F_ID);
            JsonObject details = comp.getJsonObject("details");

            String newComponentId = details.getString("abbreviation");
            boolean needRename = StringUtils.isNotBlank(newComponentId) && !newComponentId.equals(compId);
            JsonObject newComponent;
            if (needRename) {
                getLogger().info("Changing component id from '{}' to '{}'", compId, newComponentId);

                newComponent = new JsonObject();
                newComponent.put(F_ID, newComponentId);     // Make id first in LinkedHashMap
                newComponent.getMap().putAll(comp.getMap());// Shallow copy. "details" object will be shared.
                newComponent.put(F_ID, newComponentId);     // Assign proper id again
                newComponent.put(F_MOCK_FLAG, false);
                graph.addVertex(newComponent);
            } else {
                comp.put(F_MOCK_FLAG, false);   // If abbreviation is not provided, id is retained, but this is no longer a mock
                newComponentId = compId;
                newComponent = comp;
            }

            String newDomainId = details.getString("domainFromInventory");
            boolean needRelocate = StringUtils.isNotBlank(newDomainId) && !newDomainId.equals(mockDomainId);
            JsonObject newDomain;
            if (needRelocate) {
                newDomainId = ConfigUtils.fillDomainId(newDomainId);
                getLogger().info("Relocating component '{}' from domain '{}' to '{}'",
                        newComponentId, mockDomainId, newDomainId);

                // Create a new domain in the graph if absent
                newDomain = V(newDomainId).next();
                if (newDomain == null) {
                    newDomain = new JsonObject()
                            .put(F_ID, newDomainId)
                            .put(F_TYPE, "domain")
                            .put("department", V_UNKNOWN)
                            .put(F_MOCK_FLAG, true);
                    graph.addVertexUnderRoot(newDomain);
                }
            } else {
                newDomainId = mockDomainId;
                newDomain = mockDomain;
            }

            // Fill a name if it is missing in details
            String name = details.getString("name");
            if (StringUtils.isBlank(name)) {
                details.put("name", config().getValue(P_RUN_NAME));
            }

            if (needRename || needRelocate) {
                // Link the new component node to the new domain
                JsonPointer.from("/details/domain").writeJson(newComponent, newDomainId);
                graph.addEdge(newDomain, newComponent);
                if (needRelocate) {
                    graph.removeAllEdges(mockDomain, comp);
                }
                if (needRename) {
                    relocateEdges(comp, newComponent);
                    graph.removeVertex(comp);
                }
            }
        }   // end components

        // Remove the mock domain if it became empty
        List<JsonObject> remainingComponents = V(mockDomainId).out().toList();
        if (remainingComponents.isEmpty()) {
            getLogger().info("Mock domain {} is now empty, removing it", mockDomainId);
            graph.removeVertex(mockDomain);
        } else {
            for (JsonObject comp: remainingComponents) {
                report.referenceNotFound(comp, "domain");
            }
        }

        taskCompleted(taskPromise);
    }

    /** Duplicate all edges going from or coming into oldVertex, with appropriate incidence
     * point replaced to newVertex. Source edges will be dropped when the oldVertex is
     * removed from the graph. Loop edges are not supported for now. */
    private void relocateEdges(JsonObject oldVertex, JsonObject newVertex) {
        List<Map<String, JsonObject>> incoming = V(oldVertex.getString(F_ID))
                .inE().as("E")
                .outV().as("V")
                .<JsonObject>select("E", "V").toList();
        Graph graph = this.graph;
        for (Map<String, JsonObject> in: incoming) {
            JsonObject newEdge = duplicateWithoutId(in.get("E"));
            graph.addEdge(in.get("V"), newVertex, newEdge);
        }

        List<Map<String, JsonObject>> outgoing = V(oldVertex.getString(F_ID))
                .outE().as("E")
                .inV().as("V")
                .<JsonObject>select("E", "V").toList();
        for (Map<String, JsonObject> out: outgoing) {
            JsonObject newEdge = duplicateWithoutId(out.get("E"));
            graph.addEdge(newVertex, out.get("V"), newEdge);
        }

    }

    private JsonObject duplicateWithoutId(JsonObject object) {
        JsonObject newObj = new JsonObject();
        newObj.getMap().putAll(object.getMap());
        newObj.remove(F_ID);
        return newObj;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
