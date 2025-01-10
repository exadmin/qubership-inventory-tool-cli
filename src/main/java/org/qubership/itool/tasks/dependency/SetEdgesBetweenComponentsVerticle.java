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

package org.qubership.itool.tasks.dependency;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.gremlin2.graph.__;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.LanguageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.qubership.itool.modules.graph.Graph.F_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.F_DNS_NAMES;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_MOCKED_FOR;
import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAMES;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.modules.graph.Graph.V_UNKNOWN;
import static org.qubership.itool.modules.graph.GraphDataConstants.COMP_DEPENDENCY_TYPES;
import static org.qubership.itool.modules.graph.GraphDataConstants.NOS_TO_RECOGNIZE;
import static org.qubership.itool.modules.gremlin2.P.eq;
import static org.qubership.itool.modules.gremlin2.P.neq;
import static org.qubership.itool.modules.gremlin2.graph.__.*;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SetEdgesBetweenComponentsVerticle extends FlowTask {
    protected Logger LOGGER = LoggerFactory.getLogger(SetEdgesBetweenComponentsVerticle.class);

    public static final String INFRA_VERTEX_ID = "Infra";

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        List<JsonObject> components = new ArrayList<>();
        List<JsonObject> domains = V(V_ROOT).out().hasType(V_DOMAIN).toList();

        for (JsonObject domain : domains) {
            components.addAll(getComponents(graph, domain));
        }

        Map<Object, Object> componentFrameworks = V().hasType("domain").out().as("C")
            .out("module").outE("dependency")
            .has("scope", neq("test"))
            .has("component", eq(select("C").id()))
            .inV()
            .or(
                has("groupId", eq("org.springframework.boot"))
                , has("groupId", eq("io.quarkus"))
            ).as("F")
            .select("C", "F")
            .<String>values("/C/id", "/F/groupId", "/F/version").dedup()
            .group().by(F_ID).next();

        vertx.executeBlocking(promise -> {
            for (JsonObject component : components) {
                String componentId = component.getString(F_ID);
                getLogger().debug("{}: Set outgoing Edges", componentId);
                try {
                    setEdgesToInfraVertexes(component);
                    processDatabases(component);
                    processMessagesQueues(component);
                    processDependencies(component);
                    LanguageUtils.buildLanguageVerticesWithEdges(graph, component);

                    if (componentFrameworks.containsKey(componentId)) {
                        List<Map> frameworks = (List<Map>) componentFrameworks.get(componentId);
                        processDetailsProperty(component, frameworks, "framework", "info");
                    } else {
                        processDetailsProperty(component, "framework", "info");
                    }

                    processDetailsProperty(component, "tmfSpec", "implemented");
                    processGateways(component, "info");

                } catch (Exception ex) {
                    this.report.exceptionThrown(component, ex);
                }
            }
            // format: key=artifactId, value = array[libraryId]
            Map<String, List<String>> librariesArtifacts =
                    V(V_ROOT).out().hasType(V_DOMAIN)
                            .out().id().as("L")
                            .out("module").<String>value("artifactId").as("A")
                            .<String>select("L", "A")
                            .<String, List<String>>group().by("A").by("L").next();

            for (JsonObject component : components) {
                try {
                    processLanguageArtifactDependencies(component, librariesArtifacts);
                } catch (Exception ex) {
                    this.report.exceptionThrown(component, ex);
                }
            }
            promise.complete();
        }, res -> {
            taskCompleted(taskPromise);
        });
    }

    private void processLanguageArtifactDependencies(JsonObject component, Map<String, List<String>> librariesArtifacts) {
        if (LanguageUtils.hasLanguage(graph, component, "GoLang")){
            processGoDependencies(component, librariesArtifacts);
        } else {
            processMavenDependencies(component, librariesArtifacts);
        }
    }

    private void setEdgesToInfraVertexes(JsonObject component) {
        String componentId = component.getString(F_ID);
        List<JsonObject> dependencies = new ArrayList<>();
        Graph graph = this.graph;

        // directDependencies ================================
        GraphTraversal<JsonObject, JsonObject> directTraversal =
            V(graph, componentId).as("C").out("module")
                .outE("dependency")
                .has("scope", neq("test"))
                .has("component", eq(select("C").id()))
                .inV().dedup();
        dependencies.addAll(directTraversal.clone().toList());

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
        dependencies.addAll(transitiveDependencies);

        List<JsonObject> infraVertexes = V(INFRA_VERTEX_ID).out().toList();

        for (JsonObject vertex : infraVertexes) {
            JsonArray drivers = vertex.getJsonArray("drivers");
            if (drivers == null || drivers.isEmpty()) {
                continue;
            }
            for (Object tmp : drivers) {
                JsonObject driver = (JsonObject) tmp;
                String driverGroupId = driver.getString("groupId");
                String driverArtifactId = driver.getString("artifactId");

                for (JsonObject dependency : dependencies) {
                    String dependencyGroupId = dependency.getString("groupId");
                    String dependencyArtifactId = dependency.getString("artifactId");
                    if (driverGroupId != null && driverGroupId.equals(dependencyGroupId)
                     && driverArtifactId != null && driverArtifactId.equals(dependencyArtifactId))
                    {
                        graph.addEdge(component, vertex,
                            new JsonObject().put("type", "fromDependency")
                                .put("thirdParty", vertex.getString(F_ID))
                                .put("component", JsonPointer.from("/details/abbreviation").queryJson(component)));
                    }
                }
            }
        }
    }

    private void processMavenDependencies(JsonObject component, Map<String, List<String>> librariesArtifacts) {
        String componentId = component.getString(F_ID);
        Graph graph = this.graph;

        // directDependencies ================================
        GraphTraversal<JsonObject, JsonObject> directTraversal =
            V(graph, componentId).as("C").out("module")
                .outE("dependency")
                .has("scope", neq("test"))
                .has("component", eq(select("C").id()))
                .inV().dedup();

        List<String> directDependencies =
            directTraversal.clone().<String>value("artifactId").toList();

        // transitiveDependencies ============================
        List<String> transitiveDependencies =
            directTraversal.clone()
                .outE("dependency").has("scope", neq("test")).inV().dedup()
                .repeat(
                    outE("dependency")
                        .has("scope", neq("test"))
                        .has("component", eq(select("C").id()))
                        .inV().dedup()
                ).emit().dedup()
                .<String>value("artifactId").toList();

        Set<String> refSet = new HashSet<>();
        getLogger().debug("{}: Processing direct maven dependencies", componentId);
        extractDependency(librariesArtifacts, directDependencies, refSet);
        getLogger().debug("{}: Processing transitive maven dependencies", componentId);
        extractDependency(librariesArtifacts, transitiveDependencies, refSet);

        for (String libraryId : refSet) {
            if (componentId.equals(libraryId)) {
                continue;
            }
            graph.addEdge(
                component,
                graph.getVertex(libraryId),
                new JsonObject().put("type", "library")
            );
        }
    }

    private  void processGoDependencies(JsonObject component, Map<String, List<String>> librariesArtifacts){
        String componentId = component.getString(F_ID);
        List<String> dependencies = V(componentId).as("C").out("module")
                .outE("dependency")
                .has("component", eq(select("C").id()))
                .inV().dedup().<String>value("artifactId").toList();
        Set<String> refSet = new HashSet<>();
        extractDependency(librariesArtifacts, dependencies, refSet);

        for (String libraryId : refSet) {
            if (componentId.equals(libraryId)) {
                continue;
            }
            graph.addEdge(
                    component,
                    graph.getVertex(libraryId),
                    new JsonObject().put("type", "library")
            );
        }

    }

    private void extractDependency(Map<String, List<String>> librariesArtifacts, List<String> dependencies, Set<String> refSet) {
        for (String artifactId : dependencies) {
            List<String> libraryIds = librariesArtifacts.get(artifactId);
            if (libraryIds != null && !libraryIds.isEmpty()) {
                refSet.add(libraryIds.get(0));
            }
        }
    }

    private void processDetailsProperty(JsonObject component, List<Map> frameworks, String detailsType, String edgeType) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (Map framework : frameworks) {
            String name = "Unknown";
            if ("org.springframework.boot".equals(framework.get("groupId"))) {
                name = "SpringBoot";
            }
            if ("io.quarkus".equals(framework.get("groupId"))) {
                name = "Quarkus";
            }
            String version = (String) framework.get("version");
            if (!isFirst) {
                builder.append(", ");
            }
            builder.append(name).append(" ").append(version);
            isFirst = false;
        }
        component.getJsonObject("details").put(detailsType, builder.toString());
        processDetailsProperty(component, detailsType, edgeType);
    }

    private void processDetailsProperty(JsonObject component, String detailsType, String edgeType) {
        Object detailsProperty = JsonPointer.from("/details").append(detailsType).queryJson(component);
        if (detailsProperty == null) {
            return;
        }
        List<String> detailsProperties;
        if (detailsProperty instanceof String) {
            detailsProperties = Arrays.asList(((String) detailsProperty).split("\\s*,\\s*"));
        } else if (detailsProperty instanceof JsonArray) {
            detailsProperties = ((JsonArray) detailsProperty).getList();
        } else {
            // Should not happen
            this.report.conventionNotMatched(component, "/details/" + detailsType + ": must be String or JsonArray",
                    detailsProperty.getClass().getName());
            return;
        }

        Graph graph = this.graph;
        detailsProperties.stream()
        .filter(p -> StringUtils.isNotBlank(p) && ! NOS_TO_RECOGNIZE.contains(p))
        .forEach(p -> {
            JsonObject propertyVertex = graph.getVertex(p);
            if (propertyVertex == null) {
                propertyVertex = new JsonObject()
                    .put(F_ID, p)
                    .put(F_NAME, p)
                    .put(F_TYPE, detailsType);
                graph.addVertex("Info", propertyVertex);
            } else if (! propertyVertex.getString(F_TYPE).equals(detailsType)) {
                this.report.addMessage(GraphReport.CONF_ERROR, propertyVertex,
                    "Reference duplicated. Types: [" + detailsType + ", " + propertyVertex.getString(F_TYPE) + "]"
                );
            }
            graph.addEdge(component, propertyVertex, new JsonObject().put("type", edgeType));
        });
    }

    private void processGateways(JsonObject component, String edgeType) {

        Set<String> gateways = new HashSet<>();
        Graph graph = this.graph;

        JsonObject deployOptions = (JsonObject) JsonPointer.from("/details/deploymentConfiguration/deployOptions").queryJson(component);
        if (deployOptions != null) {
            if (!deployOptions.getString("generateFacadeGateway", "false").equals("false")){
                gateways.add("FACADE");
            }
            if (deployOptions.containsKey("generateNamedGateway")) {
                gateways.add("COMPOSITE");
            }
        }

        JsonArray gatewaysList = (JsonArray) JsonPointer.from("/details/gateways").queryJson(component);
        if (gatewaysList != null && !gatewaysList.isEmpty()) {
            gateways.addAll(gatewaysList.getList());
        }

        if (gateways.isEmpty()) {
            return;
        }

        String type = "gateway";
        gateways.stream()
            .filter(g -> StringUtils.isNotBlank(g) && ! NOS_TO_RECOGNIZE.contains(g))
            .map(g -> g.toUpperCase())
            .forEach(g -> {
                JsonObject propertyVertex = graph.getVertex(g);
                if (propertyVertex == null) {
                    propertyVertex = new JsonObject()
                        .put(F_ID, g)
                        .put(F_NAME, g)
                        .put(F_TYPE, type);
                    graph.addVertex("Info", propertyVertex);
                }
                graph.addEdge(component, propertyVertex, new JsonObject().put("type", edgeType));
            });
    }

    /* Create links to Infra vertices based on "database" section of inventory.md
     * **if** they have not been created by setEdgesToInfraVertexes() from maven dependencies
     */
    private void processDatabases(JsonObject component) {
        JsonArray databaseList = JsonUtils.getOrCreateJsonArray(component, JsonPointer.from("/details/database/database"));
        if (databaseList.isEmpty()) {
            return;
        }

        for (Object tmp : databaseList) {
            createDatabaseEdge(component, (JsonObject) tmp, "database", "mandatory");
        }

        JsonObject externalIndices = (JsonObject) JsonPointer.from("/details/database/externalIndices").queryJson(component);
        createDatabaseEdge(component, externalIndices, "indexation", "mandatory");

        JsonObject externalCache = (JsonObject) JsonPointer.from("/details/database/externalCache").queryJson(component);
        createDatabaseEdge(component, externalCache, "caching", "optional");
    }

    private String normalizeThirdpartyName(String value, String vertexType) {
        String name = value.split("\\s+")[0];
        List<JsonObject> dbList = V(INFRA_VERTEX_ID).out().hasType(vertexType).toList();
        for (JsonObject item : dbList) {
            String thirdpartyName = item.getString("name");
            if (name.equalsIgnoreCase(thirdpartyName)) {
                return thirdpartyName;
            }
        }
        return value;
    }

    private void createDatabaseEdge(JsonObject sourceComponent, JsonObject detailsJson, String vertexType, String edgeType) {
        if (detailsJson == null) {
            return;
        }
        String databaseName = detailsJson.getString("item");
        if (   StringUtils.isEmpty(databaseName)
            || NOS_TO_RECOGNIZE.contains(databaseName.toLowerCase())) {
            // Skip missed items
            return;
        }

        databaseName = normalizeThirdpartyName(databaseName, vertexType);
        Graph graph = this.graph;
        JsonObject dbVertex = graph.getVertex(databaseName);
        if (dbVertex == null) {
            dbVertex = new JsonObject();
            dbVertex.put(F_ID, databaseName);
            dbVertex.put(F_NAME, databaseName);
            dbVertex.put(F_TYPE, vertexType);
            dbVertex.put("fromInventory", true);
            graph.addVertex(INFRA_VERTEX_ID, dbVertex);
        }

        graph.addEdge(sourceComponent, dbVertex, new JsonObject().put("type", edgeType));
    }

    private void processMessagesQueues(JsonObject component) {
        JsonObject messageQueues = (JsonObject) JsonPointer.from("/details/messageQueues").queryJson(component);
        if (messageQueues == null) {
            return;
        }

        JsonArray rabbitMQProducer = (JsonArray) JsonPointer.from("/details/messageQueues/rabbitMQ/producer").queryJson(component);
        JsonArray rabbitMQConsumer = (JsonArray) JsonPointer.from("/details/messageQueues/rabbitMQ/consumer").queryJson(component);

        Graph graph = this.graph;
        JsonObject rabbitMQComponent = graph.getVertex("RabbitMQ");
        createMessageQueueEdge(graph, component, rabbitMQComponent, rabbitMQProducer, "producer");
        createMessageQueueEdge(graph, rabbitMQComponent, component, rabbitMQConsumer, "consumer");

        JsonArray kafkaProducer = (JsonArray) JsonPointer.from("/details/messageQueues/kafka/producer").queryJson(component);
        JsonArray kafkaConsumer = (JsonArray) JsonPointer.from("/details/messageQueues/kafka/consumer").queryJson(component);

        JsonObject kafkaComponent = graph.getVertex("Kafka");
        createMessageQueueEdge(graph, component, kafkaComponent, kafkaProducer, "producer");
        createMessageQueueEdge(graph, kafkaComponent, component, kafkaConsumer, "consumer");
    }

    private void createMessageQueueEdge(Graph graph, JsonObject sourceComponent, JsonObject mqComponent, JsonArray list, String type) {
        if (mqComponent == null || list == null) {
            return;
        }

        for (Object exchangeName : list) {
            if (NOS_TO_RECOGNIZE.contains(exchangeName.toString().toLowerCase())) {
                // Skipping creation of missing and not required edges
                continue;
            }
            graph.addEdge(sourceComponent, mqComponent, new JsonObject()
                .put("type", type)
                .put("name", exchangeName)
            );
        }
    }

    private void processDependencies(JsonObject component) {
        getLogger().debug("{}: Processing http dependencies", component.getString(F_ID));
        JsonObject dependencies = (JsonObject) JsonPointer.from("/details/dependencies").queryJson(component);
        if (dependencies == null) {
            getLogger().debug( "Dependencies are null for {}", component.getString("id"));
            return;
        }
        COMP_DEPENDENCY_TYPES.forEach((key, value) ->
            createDependencyEdge(component, (JsonArray) JsonPointer.from(value).queryJson(component), key));
    }

    private void createDependencyEdge(JsonObject sourceComponent, JsonArray dependencies, String type) {
        if (dependencies == null) {
            return;
        }

        for (Object tmp : dependencies) {
            String dependency = (String) tmp;
            if (NOS_TO_RECOGNIZE.contains(dependency.toLowerCase())) {
                continue;
            }

            List<JsonObject> destinationComponents = V(V_ROOT).out().hasType(V_DOMAIN).out()
                    .or(
                            __.<JsonObject>has(F_MOCK_FLAG, P.neq(true)).has(P_DETAILS_DNS_NAMES, P.containing(dependency))
                            , __.<JsonObject>has(F_MOCK_FLAG, P.eq(true)).has(P_DETAILS_DNS_NAMES, P.eq(dependency)))
                    .toList();
            JsonObject destinationComponent;
            String edgeType = null;
            if (destinationComponents.isEmpty()) {
                destinationComponent = createMockByDnsName(dependency);
                graph.addVertex(destinationComponent);
                edgeType = getEdgeType(type, sourceComponent, destinationComponent);
                this.report.referenceNotFound(sourceComponent, edgeType + " http dependency " + dependency);
            } else if (destinationComponents.size() == 1) {
                destinationComponent = destinationComponents.get(0);
            } else {
                destinationComponent = destinationComponents.get(0);
                JsonObject another = destinationComponents.get(1);
                // TODO: Perform such check NOT only when dnsName is referenced. See also: RecreateHttpDependenciesTask
                this.report.addMessage(GraphReport.CONF_ERROR, destinationComponent,
                        "Vertices '" + destinationComponent.getString(F_ID) + "' and '" + another.getString(F_ID)
                        + "' share the same dnsName '" + dependency + "'");
            }

            if (edgeType == null) {
                edgeType = getEdgeType(type, sourceComponent, destinationComponent);
            }
            graph.addEdge(sourceComponent, destinationComponent,
                new JsonObject().put(F_TYPE, edgeType).put("protocol", "http")
            );
        }
    }

    private JsonObject createMockByDnsName(String dnsName) {
        String mockId = "mock:dnsName:" + dnsName;
        getLogger().info("Mock vertex {} created to substitute dnsName {}", mockId, dnsName);
        return new JsonObject()
            .put(F_ID, mockId)
            .put(F_TYPE, V_UNKNOWN)
            .put(F_NAME, dnsName)
            .put(F_MOCK_FLAG, true)
            .put(F_MOCKED_FOR, new JsonArray().add(P_DETAILS_DNS_NAMES))
            .put("details", new JsonObject()
                    .put(F_DNS_NAME, dnsName)
                    .put(F_DNS_NAMES, dnsName)
            );
    }

    // XXX Specific support for "graphql" edge type. It should be reviewed.
    private static final JsonPointer DNS_NAME_PTR = JsonPointer.from(P_DETAILS_DNS_NAME);

    private static String getEdgeType(String type, JsonObject sourceComponent, JsonObject destinationComponent) {
        if (   isGqls(destinationComponent)
            || "optional".equals(type) && isGqls(sourceComponent))
        {
            return "graphql";
        } else {
            return type;
        }
    }

    private static boolean isGqls(JsonObject comp) {
        return "GQLS".equals(comp.getString(F_ID))
            || "cloud-graphql".equals(DNS_NAME_PTR.queryJson(comp)); // Result of createMockByDnsName() counts, too
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
