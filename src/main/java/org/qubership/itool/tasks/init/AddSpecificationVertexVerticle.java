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

package org.qubership.itool.tasks.init;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AddSpecificationVertexVerticle extends FlowTask {
    protected Logger LOGGER = LoggerFactory.getLogger(AddSpecificationVertexVerticle.class);

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        JsonObject vertex = graph.getVertex("Spec");
        JsonObject tmfSpecMapping = vertex.getJsonObject("tmf");

        tmfSpecMapping = (tmfSpecMapping == null) ? new JsonObject() : tmfSpecMapping;

        processDetailsProperty(vertex, tmfSpecMapping, "tmfSpec", "tmf");

        taskCompleted(taskPromise);
    }

    @SuppressWarnings("rawtypes")
    protected void processDetailsProperty(JsonObject vertex, JsonObject tmfSpecMapping, String detailsType, String vertexType) {
        List propertyList = V().hasType("domain").out().value("/details/" + detailsType).dedup().toList();
        List<String > property = new ArrayList<>();
        for (Object prop : propertyList) {
            if (prop instanceof String) {
                property.add((String)prop);
            } else if (prop instanceof JsonArray) {
                List tmpList = ((JsonArray)prop).getList();
                for (Object tmp : tmpList) {
                    if (tmp instanceof String) {
                        property.add((String) tmp);
                    } else {
                        this.report.conventionNotMatched(vertex, "String", tmp.getClass().getName());
                    }
                }
            } else {
                this.report.conventionNotMatched(vertex, "String, JsonArray", prop.getClass().getName());
            }
        }

        property.stream()
            .filter(p -> ! GraphDataConstants.NOS_TO_RECOGNIZE.contains(p))
            .flatMap(p -> Arrays.stream(p.split("\\s*,\\s*")))
            .forEach(p -> createIfRequired(vertex, tmfSpecMapping, p, vertexType, p));
    }

    protected void createIfRequired(JsonObject vertex, JsonObject tmfSpecMapping, String id, String type, String name) {
        Graph graph = this.graph;
        String vId = id.replaceAll("(\\d+)\\s*v?\\S*", "$1");
        JsonObject component = graph.getVertex(vId);
        if (component != null) {
            return;
        }
        String specName = (String) JsonPointer.from("/" + id + "/name").queryJson(tmfSpecMapping);
        String specUrl = (String)JsonPointer.from("/" + id + "/url").queryJson(tmfSpecMapping);
        String specVersion = (String)JsonPointer.from("/" + id + "/version").queryJson(tmfSpecMapping);
        Integer specCode = (Integer)JsonPointer.from("/" + id + "/code").queryJson(tmfSpecMapping);

        component = new JsonObject()
            .put("id", vId)
            .put("type", type)
            .put("name", specName != null ? specName : name)
            .put("url", specUrl == null ? generateUrlFromId(id) : specUrl)
            .put("version", specVersion)
            .put("code", specCode != null ? specCode : vId);

        graph.addVertex(vertex, component);
        getLogger().info("Specification component added: " + component);
    }

    private String generateUrlFromId(String id) {
        return "https://www.tmforum.org/resources/?yith_wcan=1&s="+ id + "&post_type=product&filter_document-type=specifications";
    }


    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
