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

package org.qubership.itool.cli.query;

import org.qubership.itool.cli.query.cmd.CliCommand;
import org.qubership.itool.modules.graph.Graph;

import java.util.*;

public class CliContext {

    public static final String PROPERTIES = "properties";

    private Graph graph;
    private List<CliCommand> commands = new ArrayList<>();
    private Map<String, Object> contextMap = new HashMap<>();

    public CliContext(Graph graph) {
        this.graph = graph;
        Properties props = new Properties();
        this.contextMap.put(PROPERTIES, props);
        // Set default value
        props.put("view.json", "compact");
        props.put("view.map", "compact");
        props.put("result.limit", -1);
    }

    public Object getValue(String key) {
        return this.contextMap.get(key);
    }

    public void setValue (String key, Object value) {
        this.contextMap.put(key, value);
    }

    public List<CliCommand> getCommands() {
        return this.commands;
    }

    public Graph getGraph() {
        return this.graph;
    }

}
