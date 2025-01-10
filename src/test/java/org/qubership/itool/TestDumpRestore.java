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

package org.qubership.itool;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.modules.graph.GraphImpl;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestDumpRestore {

    private Graph graph;
    private GraphReport report;

    @BeforeAll
    public void setup() {
        this.graph = new GraphImpl();
        this.report = new GraphReportImpl();
    }

    @BeforeEach
    public void cleanup() {
        this.graph.clear();
        this.report.clear();
    }

    @Test
    public void testReportDump() {
        this.report.addRecord(new JsonObject().put("id", 1).put("name", "record 1"));
        this.report.addRecord(new JsonObject().put("id", 2).put("name", "record 2"));

        JsonArray dump = this.report.dumpRecords(false);
        Assertions.assertEquals(2, dump.size());

        JsonObject record1 = dump.getJsonObject(0);
        Assertions.assertEquals(2, record1.getMap().size());
        Assertions.assertEquals(1, record1.getInteger("id"));
        Assertions.assertEquals("record 1", record1.getString("name"));

        JsonObject record2 = dump.getJsonObject(1);
        Assertions.assertEquals(2, record2.getMap().size());
        Assertions.assertEquals(2, record2.getInteger("id"));
        Assertions.assertEquals("record 2", record2.getString("name"));
    }

    @Test void testReportRestore() {
        this.report.addRecord(new JsonObject().put("id", 1).put("name", "record 1"));
        this.report.addRecord(new JsonObject().put("id", 2).put("name", "record 2"));
        JsonArray dump = this.report.dumpRecords(false);

        this.report.addRecord(new JsonObject().put("id", 3).put("name", "record 3"));
        Assertions.assertEquals(2, dump.size());

        this.report.restoreRecords(dump);

        dump = this.report.dumpRecords(false);

        Assertions.assertEquals(2, dump.size());

        JsonObject record1 = dump.getJsonObject(0);
        Assertions.assertEquals(2, record1.getMap().size());
        Assertions.assertEquals(1, record1.getInteger("id"));
        Assertions.assertEquals("record 1", record1.getString("name"));

        JsonObject record2 = dump.getJsonObject(1);
        Assertions.assertEquals(2, record2.getMap().size());
        Assertions.assertEquals(2, record2.getInteger("id"));
        Assertions.assertEquals("record 2", record2.getString("name"));
    }

    @Test
    public void testGraphDump() {
        JsonObject vertex1 = new JsonObject().put("id", "1");
        JsonObject vertex1_1 = new JsonObject().put("id", "1_1");
        JsonObject vertex2 = new JsonObject().put("id", "2");

        this.graph.addVertexUnderRoot(vertex1);
        this.graph.addVertexUnderRoot(vertex2);
        this.graph.addVertex(vertex1, vertex1_1);
        this.graph.addEdge(vertex1, vertex2, new JsonObject().put("type", "edge"));

        JsonObject dump = this.graph.dumpGraphData(false);
       // Assertions.assertEquals(4, dump.getInteger("edgeSupplierCounter"));
        Assertions.assertEquals(3, dump.getJsonArray("vertexList").size());
        AssertVertex(dump.getJsonArray("vertexList"), "1");
        AssertVertex(dump.getJsonArray("vertexList"), "1_1");
        AssertVertex(dump.getJsonArray("vertexList"), "2");

        Assertions.assertEquals(4, dump.getJsonArray("edgeList").size());
        AssertEdge(dump.getJsonArray("edgeList"), "root", "1");
        AssertEdge(dump.getJsonArray("edgeList"), "root", "2");
        AssertEdge(dump.getJsonArray("edgeList"), "1", "1_1");
        AssertEdge(dump.getJsonArray("edgeList"), "1", "2", "type", "edge");
    }

    @Test
    public void testGraphRestore() {
        JsonObject vertex1 = new JsonObject().put("id", "1");
        JsonObject vertex1_1 = new JsonObject().put("id", "1_1");
        JsonObject vertex2 = new JsonObject().put("id", "2");

        this.graph.addVertexUnderRoot(vertex1);
        this.graph.addVertexUnderRoot(vertex2);
        this.graph.addVertex(vertex1, vertex1_1);
        this.graph.addEdge(vertex1, vertex2, new JsonObject().put("type", "edge"));

        JsonObject dump = this.graph.dumpGraphData(false);
        this.graph.clear();
//        Assertions.assertEquals(4, dump.getInteger("edgeSupplierCounter"));
        Assertions.assertEquals(3, dump.getJsonArray("vertexList").size());
        Assertions.assertEquals(4, dump.getJsonArray("edgeList").size());

        this.graph.restoreGraphData(dump);
        dump = this.graph.dumpGraphData(false);

        //Assertions.assertEquals(4, dump.getInteger("edgeSupplierCounter"));
        Assertions.assertEquals(3, dump.getJsonArray("vertexList").size());
        AssertVertex(dump.getJsonArray("vertexList"), "1");
        AssertVertex(dump.getJsonArray("vertexList"), "1_1");
        AssertVertex(dump.getJsonArray("vertexList"), "2");

        Assertions.assertEquals(4, dump.getJsonArray("edgeList").size());
        AssertEdge(dump.getJsonArray("edgeList"), "root", "1");
        AssertEdge(dump.getJsonArray("edgeList"), "root", "2");
        AssertEdge(dump.getJsonArray("edgeList"), "1", "1_1");
        AssertEdge(dump.getJsonArray("edgeList"), "1", "2", "type", "edge");
    }

    @Test
    @Disabled
    public void test_large() throws IOException {
        String source;
        try (InputStream inputStream = FSUtils.openUrlStream(getClass(), "task.result.json")) {
            InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
            source = IOUtils.toString(reader);
        }
        System.out.println("Source length: " + source.length() + " chars");

        long startTime = System.currentTimeMillis();
        JsonObject dump = new JsonObject(source);
        long endTime = System.currentTimeMillis();
        System.out.println("Parse string to Json: " + (endTime - startTime) + " ms");

        startTime = System.currentTimeMillis();
        this.report.restoreRecords(dump.getJsonArray("report"));
        endTime = System.currentTimeMillis();
        System.out.println("Restoring report: " + (endTime - startTime) + " ms");

        startTime = System.currentTimeMillis();
        this.graph.restoreGraphData(dump.getJsonObject("graph"));
        endTime = System.currentTimeMillis();
        System.out.println("Restoring graph: " + (endTime - startTime) + " ms");

        startTime = System.currentTimeMillis();
        JsonObject result = GraphDumpSupport.dumpToJson(this.graph, false);
        endTime = System.currentTimeMillis();
        System.out.println("Create dump: " + (endTime - startTime) + " ms");

        startTime = System.currentTimeMillis();
        File file = new File("./tmp.json");
        JsonUtils.saveJson(file.toPath(), result, false);
        System.out.println("File length: " + file.length() + " bytes");
        file.delete();
        endTime = System.currentTimeMillis();
        System.out.println("Dump to file: " + (endTime - startTime) + " ms");
    }

    private void AssertEdge(JsonArray edgeList, String source, String target, String field, String value) {
        boolean result = false;
        for (Object obj : edgeList) {
            JsonObject edge = (JsonObject)obj;
            if(edge.getString("source").equals(source) && edge.getString("target").equals(target)
            && edge.getJsonObject("edge") != null && edge.getJsonObject("edge").getString(field).equals(value)) {
                result = true;
                break;
            }
        }
        Assertions.assertTrue(result);
    }

    private void AssertEdge(JsonArray edgeList, String source, String target) {
        boolean result = false;
        for (Object obj : edgeList) {
            JsonObject edge = (JsonObject)obj;
            if(edge.getString("source").equals(source) && edge.getString("target").equals(target)) {
                result = true;
                break;
            }
        }
        Assertions.assertTrue(result);
    }

    private void AssertVertex(JsonArray vertexList, String s) {
        boolean result = false;
        for (Object obj : vertexList) {
            JsonObject vertex = (JsonObject)obj;
            if (vertex.getString("id").equals(s)) {
                result = true;
                break;
            }
        }
        Assertions.assertTrue(result);
    }

}
