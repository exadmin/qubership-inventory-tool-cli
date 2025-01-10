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

package org.qubership.itool.tasks;


import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.qubership.itool.modules.gremlin2.graph.__.hasType;


@SuppressWarnings("rawtypes")
public abstract class AbstractAggregationTaskVerticle extends FlowTask {
    protected Logger LOG = LoggerFactory.getLogger(AbstractAggregationTaskVerticle.class);

    protected List<Future> processGraph(
            Function<JsonObject, List<Future>> domainProcessor, Function<JsonObject, List<Future>> componentProcessor,
            BiFunction<Graph, JsonObject, List<JsonObject>> componentExtractor) {
        Graph graph = this.graph;
        List<JsonObject> domains = V(graph).hasType("domain").toList();
        List<Future> futures = new ArrayList<>();
        for (JsonObject domain : domains) {
            futures.addAll(domainProcessor.apply(domain));
            List<JsonObject> components = componentExtractor.apply(graph, domain);
            for (JsonObject component : components) {
                futures.addAll(componentProcessor.apply(component));
            }
        }
        return futures;
    }

    public static List<JsonObject> getMavenDependencyComponents(Graph graph, JsonObject domain) {
        return graph.traversal().V(domain.getString(Graph.F_ID)).out().not(hasType("ui", "ui cdn", "ui app bundle")).hasKey("repository", "details").toList();
    }

    protected void completeCompositeTask(List<Future> futureList, Promise<?> taskPromise) {
        if (futureList == null) {
            report.internalError("List of futures should not be null");
            taskCompleted(taskPromise);
            return;
        }

        joinFuturesAndHandleResult(futureList)
                .onComplete(ar -> taskCompleted(taskPromise));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
