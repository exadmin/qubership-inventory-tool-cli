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
import org.qubership.itool.cli.query.QueryFileParser;
import org.qubership.itool.cli.query.QueryItem;

import java.util.ArrayList;
import java.util.List;

public class ListPredefinedQueriesCommand extends AbstractCliCommand {
    public static final String QUERY_ITEMS = "queryItems";
    public static final String QUERY_DEFAULT_TXT = "inventory-tool/default/config/cli/query_default.txt";
    public static final String QUERY_CUSTOM_TXT = "inventory-tool/default/config/cli/query_custom.txt";

    private List<QueryItem> queryItems = new ArrayList<>();

    public ListPredefinedQueriesCommand(CliContext context) {
        super(context);
        QueryFileParser parser = new QueryFileParser();
        parser.parse(this.queryItems, QUERY_DEFAULT_TXT);
        parser.parse(this.queryItems, QUERY_CUSTOM_TXT);
        context.setValue(QUERY_ITEMS, this.queryItems);
    }

    @Override
    public String name() {
        return "list;";
    }

    @Override
    public String description() {
        return "List predefined Gremlin queries";
    }

    @Override
    public boolean acceptCommand(String command) {
        return "list;".equals(command);
    }

    @Override
    public Object doCommand(String command) {
        commandList(command);
        return null;
    }

    private void commandList(String command) {
        System.out.println("Predefined query list:");
        for (int i=0 ; i<this.queryItems.size() ; i++) {
            System.out.println(
                "(" + (i + 1) + ")\t"
                    + this.queryItems.get(i).getMethod()
                    + " --- " + this.queryItems.get(i).getDescription()
            );
        }
    }

}
