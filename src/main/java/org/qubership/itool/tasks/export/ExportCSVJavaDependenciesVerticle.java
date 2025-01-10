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

package org.qubership.itool.tasks.export;

import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.tuple.Pair;
import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.utils.FSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.qubership.itool.modules.gremlin2.P.eq;
import static org.qubership.itool.modules.gremlin2.P.neq;
import static org.qubership.itool.modules.gremlin2.graph.__.outE;
import static org.qubership.itool.modules.gremlin2.graph.__.select;

public class ExportCSVJavaDependenciesVerticle extends AbstractExportVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ExportCSVJavaDependenciesVerticle.class);

    @Override
    protected String[] features() {
        return new String[] { "csvJavaDependenciesExport" };
    }

    @Override
    protected String getExportPath() {
        return "output/export/java_dependencies.csv";
    }

    @Override
    protected void build(String finalExportPath) throws IOException {
        List<Pair<JsonObject, List<JsonObject>>> componentDirectDep     = new ArrayList<>();
        List<Pair<JsonObject, List<JsonObject>>> componentTransitiveDep = new ArrayList<>();

        List<JsonObject> components = V().hasType("domain").out().toList();
        for (JsonObject component : components) {
            // directDependencies ================================
            GraphTraversal<JsonObject, JsonObject> directTraversal =
                V(component.getString("id")).as("C").out("module")
                    .outE("dependency")
                    .has("scope", neq("test"))
                    .has("component", eq(select("C").id()))
                    .inV().dedup();
            componentDirectDep.add(Pair.of(component, directTraversal.clone().toList()));

            // transitiveDependencies ============================
            List<JsonObject> transitiveDependencies =
                directTraversal.clone()
                    .outE("dependency").has("scope", neq("test")).inV().dedup()
                    .repeat(
                        outE("dependency")
                            .has("scope", neq("test"))
                            .has("component", eq(select("C").id()))
                            .inV().dedup()
                    ).emit().dedup().toList();
            componentTransitiveDep.add(Pair.of(component, transitiveDependencies));
        }

        Map<String, JavaDependency> map = new HashMap<>();

        for (Pair<JsonObject, List<JsonObject>> pair : componentDirectDep) {
            List<JsonObject> directDep = pair.getRight();
            for (JsonObject dep : directDep) {
                JavaDependency javaDependency = map.computeIfAbsent(dep.getString("id"),
                    key -> new JavaDependency(
                        dep.getString("groupId"),
                        dep.getString("artifactId"),
                        dep.getString("version")));
                javaDependency.getDirectComponents().add(pair.getLeft());
            }
        }

        for (Pair<JsonObject, List<JsonObject>> pair : componentTransitiveDep) {
            List<JsonObject> transitiveDep = pair.getRight();
            for (JsonObject dep : transitiveDep) {
                JavaDependency javaDependency = map.computeIfAbsent(dep.getString("id"),
                    key -> new JavaDependency(
                        dep.getString("groupId"),
                        dep.getString("artifactId"),
                        dep.getString("version")));
                javaDependency.getTransitiveComponents().add(pair.getLeft());
            }
        }

        List<JavaDependency> collection = new ArrayList<>(map.values());
        Collections.sort(collection);

        StringBuilder builder = new StringBuilder();
        builder.append("groupId,artifactId,version,direct,transitive\n");
        for (JavaDependency dep : collection) {
            builder.append(dep.groupId).append(",");
            builder.append(dep.artifactId).append(",");
            builder.append(dep.version).append(",");
            separatedList(builder, dep.getDirectComponents());
            builder.append(",");
            separatedList(builder, dep.getTransitiveComponents());
            builder.append("\n");
        }

        FSUtils.createFile(finalExportPath, builder.toString());
    }

    private void separatedList(StringBuilder builder, List<JsonObject> components) {
        Collections.sort(components, Comparator.comparing(o -> o.getString("id")));
        builder.append("\"");
        for (int i=0; i<components.size() ; i++) {
            builder.append(components.get(i).getString("id"));
            if (i != components.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append("\"");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private static class JavaDependency implements Comparable<JavaDependency> {
        private String groupId;
        private String artifactId;
        private String version;

        private List<JsonObject> directComponents;
        private List<JsonObject> transitiveComponents;

        public JavaDependency(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.directComponents = new ArrayList<>();
            this.transitiveComponents = new ArrayList<>();
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public List<JsonObject> getDirectComponents() {
            return directComponents;
        }

        public List<JsonObject> getTransitiveComponents() {
            return transitiveComponents;
        }

        @Override
        public int compareTo(JavaDependency o1) {
            if (   groupId.equals(o1.groupId)
                && artifactId.equals(o1.artifactId)
                && version.equals(o1.version))
            {
                return 0;
            }

            if (!groupId.equals(o1.groupId)) {
                return groupId.compareTo(o1.groupId);
            }

            if (!artifactId.equals(o1.artifactId)) {
                return  artifactId.compareTo(o1.artifactId);
            }

            if (version != null && o1.version == null) {
                return 1;
            }

            if (version == null && o1.version != null) {
                return -1;
            }

            return P.lteVersion(o1.version).test(version) ? -1 : 1;
        }
    }
}
