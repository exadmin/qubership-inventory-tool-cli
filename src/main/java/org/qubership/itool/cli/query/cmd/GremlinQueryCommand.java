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

package org.qubership.itool.cli.query.cmd;

import org.qubership.itool.cli.query.CliContext;
import org.qubership.itool.modules.gremlin2.GremlinException;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.query.QueryExecutor;
import org.qubership.itool.modules.query.converter.ResultConverter;
import org.qubership.itool.modules.query.converter.ToTextConverter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GremlinQueryCommand extends AbstractCliCommand {
    public static final String LAST_EXECUTED_QUERY = "lastExecutedQuery";
    public static final String LAST_EXECUTED_RESULT = "lastExecutedResult";

    public GremlinQueryCommand(CliContext context) {
        super(context);
    }

    @Override
    public String name() {
        return ".XXX;";
    }

    @Override
    public String description() {
        return "Execute Gremlin query. Where XXX can be V() for Verticles or .E() for Edges";
    }

    @Override
    public boolean acceptCommand(String command) {
        return command.startsWith(".V(") || command.startsWith(".E(");
    }

    @Override
    public Object doCommand(String command) {
        try {
            long startTime = System.currentTimeMillis();
            QueryExecutor queryConverter = new QueryExecutor(context().getGraph());
            Object result = queryConverter.executeGremlinQuery(command);
            printGremlinResult(startTime, result);
            context().setValue(LAST_EXECUTED_QUERY, command);
            context().setValue(LAST_EXECUTED_RESULT, result);
            return result;

        } catch (GremlinException ge) {
            System.out.println(ge.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        }
        return null;
    }


    @SuppressWarnings({ "unused", "rawtypes" })
    private void printGremlinResult(long startTime, Object result) {
        if (true) {
            ResultConverter<String> converter = new ToTextConverter();
            converter.setProperties((Properties) this.context().getValue(CliContext.PROPERTIES));
            String output = converter.convert(result);
            long endTime = System.currentTimeMillis();
            System.out.print(output);
            System.out.println("Execution time (ms): " + (endTime - startTime));    // Including result conversion
            return;
        }

        List list = null;
        Map map = null;
        if (result instanceof List) {
            list = (List) result;

        } else if (result instanceof GraphTraversal) {
            list = ((GraphTraversal)result).toList();

        } else if (result instanceof Map) {
            map = (Map)result;
        }

        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof Traverser) {
                    System.out.println(((Traverser<?>) obj).get());

                } else if(obj instanceof Map) {
                    printMap((Map)obj);

                } else {
                    System.out.println(obj);
                }
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Total: " + list.size() + " // execution time (ms): " + (endTime - startTime));

        } else if (map != null) {
            printMap(map);
            long endTime = System.currentTimeMillis();
            System.out.println("Total: " + map.size() + " // execution time (ms): " + (endTime - startTime));

        } else {
            System.out.println(result);
        }
    }

    @SuppressWarnings("rawtypes")
    private void printMap(Map map) {
        // Map<JsonObject, List<JsonObject>>
        // Map<String, List<JsonObject>>
        // Map<String, List<Object>>

        if (map == null) {
            System.out.println("map is null");
            return;
        }
//        int index = 0;
//        System.out.println("{");
//        for (Object key : map.keySet()) {
//            System.out.print((index == 0) ? "  " : ", ");
//            System.out.println(key + " = " + map.get(key));
//            index += 1;
//        }
//        System.out.println("}");

        System.out.println(map);
    }

}
