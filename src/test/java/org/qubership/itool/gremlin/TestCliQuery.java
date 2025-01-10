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

package org.qubership.itool.gremlin;

import org.qubership.itool.cli.query.CliQuery;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;
import org.qubership.itool.modules.graph.GraphImpl;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCliQuery extends AbstractGremlinTest {

    private CliQuery cli;

    @BeforeAll
    public void setup() {
        this.graph = new GraphImpl();
    }

    @BeforeEach
    public void cleanup() {
        this.graph.clear();
        createSimpleGraph();
        this.cli = new CliQuery(this.graph);
    }

    @Test
    void testV() {
        String query = ".V(\"v1\").next();";
        JsonObject result = (JsonObject)this.cli.executeGremlinQuery(query);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("v1", result.getString("id"));
    }

    @Test
    void testJsonObject() {
        String query = ".V().group().<String, JsonObject>by(\"type\").<String, String>by(\"name\").next();";
        Object result = this.cli.executeGremlinQuery(query);
        Assertions.assertNotNull(result);
    }

    @Test
    void testPredicates() {
        String query = ".V().has(\"name\", within(\"josh\", \"ripple\"));";
        Object result = this.cli.executeGremlinQuery(query);
        Assertions.assertNotNull(result);
    }

    @Test
    void testGroupCount() {
        String query = ".V().group().by(\"/details/document\").by(__.count()).toList();";
        Object result = this.cli.executeGremlinQuery(query);
        Assertions.assertNotNull(result);
    }

}
