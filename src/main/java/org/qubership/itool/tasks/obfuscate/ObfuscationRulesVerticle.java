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

package org.qubership.itool.tasks.obfuscate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.ci.CiConstants;
import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;

/** Initialize domains without components */
public class ObfuscationRulesVerticle extends FlowTask {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ObfuscationRulesVerticle.class);

    protected Map<String, Trie<String, Boolean>> prefixesByType = new HashMap<>();

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    //------------------------------------------------------
    // Hard-coded part of logics

    protected static final String[] processByTypes = {
            "root", "domain", "errorCode", "module", "library",
            "database", "indexation", "caching", "mq", "framework",
            "language", "tmf", "file", "directory", "gateway"
    };

    // Ensure safety of these attributes
    protected static final String[] alwaysPreserve = {
            "/id/", "/type/", "/name/", "/isMock/"
    };


    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {

        String rulesLocation = config().getString(CiConstants.OBFUSCATION_RULES);
        JsonObject rules;
        try (InputStream stream = FSUtils.openUrlStream(getClass(), rulesLocation)) {
            rules = JsonUtils.mapper().readValue(new InputStreamReader(stream, JsonUtils.UTF_8), JsonObject.class);
        }

        Set<String> processedIds = new HashSet<>();

        for (String type: processByTypes) {
            processedIds.addAll(processAllNodesByType(rules, type));
        }
        // Let them be processed after by-type
        processedIds.addAll(processComponents(rules));
        // Utilities shall be processed the last
        processedIds.addAll(processUtilities(rules));

        // Remove everything else
        Graph graph = this.graph;
        for (JsonObject vertex: graph.vertexList()) {
            String vertexId = vertex.getString(F_ID);
            if (! processedIds.contains(vertexId)) {
                getLogger().info("Unknown vertex {} dropped", vertexId);
                graph.removeVertex(vertex);
            }
        }

        taskCompleted(taskPromise);
    }

    protected Collection<String> processComponents(JsonObject allRules) {
        List<JsonObject> components = V().hasType("domain").out().toList();
        // All types of components share the same refType
        return processNodes(allRules, "*component*", components);
    }

    protected Collection<String> processUtilities(JsonObject allRules) {
        List<JsonObject> vertices = V("Info", "Infra", "Spec").toList();
        return vertices.stream()
                .flatMap(vertex -> processNodes(allRules, vertex.getString(F_TYPE), Collections.singletonList(vertex)).stream())
                .collect(Collectors.toSet());
    }

    //------------------------------------------------------
    // Helper methods and applying of externally configurable rules

    protected Collection<String> processAllNodesByType(JsonObject allRules, String type) {
        List<JsonObject> nodes = V().hasType(type).toList();
        return processNodes(allRules, type, nodes);
    }

    protected Collection<String> processNodes(JsonObject allRules, String refType, List<JsonObject> vertices) {
        JsonObject rulesForType = (JsonObject) JsonPointer.from("/vertices/byType/" + refType).queryJson(allRules);
        return processByRule(rulesForType, refType, vertices);
    }

    protected Collection<String> processByRule(JsonObject rules, String refType, List<JsonObject> vertices) {
        return vertices.stream()
                .peek(vertex -> processByRule(rules, refType, vertex))
                .map(vertex -> vertex.getString(F_ID))
                .collect(Collectors.toSet());
    }

    protected void processByRule(JsonObject rules, String refType, JsonObject vertex) {
        String vertexId = vertex.getString(F_ID);
        String vertexType = vertex.getString(F_TYPE);
        if (rules == null) {
            getLogger().debug("Vertex {} kept intact. Type: {}", vertexId, vertexType);
            return; // Default rule for known nodes: do not touch
        }

        String ruleType = rules.getString("rule", "");

        RULES:
        switch (ruleType) {
        case "drop":
            graph.removeVertex(vertex);
            getLogger().info("Vertex {} dropped. Type: {}", vertexId, vertexType);
            break RULES;
        case "truncate":
            Trie<String, Boolean> wlPrefixes = prefixesByType.computeIfAbsent(refType,
                    key -> generateTrie(rules.getJsonArray("allowList")));
            truncateMapRecursively(vertex.getMap(), wlPrefixes, "/");
            break RULES;
        case "saveByChildren":
            saveByChildren(vertex);
            break RULES;
        default:
            getLogger().error("Unknown rule type: {}", ruleType);
            // do nothing
        }
    }

    protected Trie<String, Boolean> generateTrie(JsonArray allowList) {
        Trie<String, Boolean> trie = new PatriciaTrie<>();
        for (Object prefix: allowList.getList()) {
            /* Canonical paths in the trie look like this: "/details/name/".
             * The final slash protects from false positives like matching attribute
             * "/details/name1" against prefix "/details/name".
             *
             * Arrays are not distinguished from singular elements in paths.
             */
            trie.put(("/" + prefix + "/").replaceAll("/+",  "/"), Boolean.TRUE);
        }
        for (String prefix: alwaysPreserve) {
            trie.put(prefix, Boolean.TRUE);
        }
        return trie;
    }

    protected void truncateMapRecursively(Map<String, Object> map, Trie<String, Boolean> wlPrefixes, String path) {
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> e = it.next();
            String keyPath = path + e.getKey() + "/";
            Map<String, Boolean> p = wlPrefixes.prefixMap(keyPath);
            if (p.isEmpty()) {  // No such prefix in white-list, drop the key
                getLogger().debug("Dropped path {}", keyPath);
                it.remove();
            } else if (p.containsKey(keyPath)) {
                // Exact path found. Keep that node.
            } else {    // Dive and truncate
                truncateObjectRecursively(e.getValue(), wlPrefixes, keyPath);
            }
        }
    }

    protected void truncateListRecursively(List<Object> list, Trie<String, Boolean> wlPrefixes, String path) {
        for (Object o: list) {
            truncateObjectRecursively(o, wlPrefixes, path);
        }
    }

    @SuppressWarnings("unchecked")
    protected void truncateObjectRecursively(Object value, Trie<String, Boolean> wlPrefixes, String path) {
        if (value instanceof Map) {
            truncateMapRecursively((Map<String, Object>)value, wlPrefixes, path);
        } else if (value instanceof JsonObject) {
            truncateMapRecursively(((JsonObject)value).getMap(), wlPrefixes, path);
        } else if (value instanceof List) {
            truncateListRecursively((List<Object>)value, wlPrefixes, path);
        } else if (value instanceof JsonArray) {
            truncateListRecursively(((JsonArray)value).getList(), wlPrefixes, path);
        }
        // Keep it
    }

    protected void saveByChildren(JsonObject vertex) {
        String vertexId = vertex.getString(F_ID);
        Long childrenCount = V(vertexId).out().count().next();
        if (childrenCount == 0) {
            getLogger().info("Vertex {} dropped due to lack of children. Type: {}", vertexId, vertex.getString(F_TYPE));
            graph.removeVertex(vertex);
        }
    }

}
