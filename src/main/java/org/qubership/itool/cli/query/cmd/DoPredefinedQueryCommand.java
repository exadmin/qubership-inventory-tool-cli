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
import org.qubership.itool.cli.query.QueryItem;
import org.qubership.itool.modules.gremlin2.GremlinException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DoPredefinedQueryCommand extends AbstractCliCommand {

    public DoPredefinedQueryCommand(CliContext context) {
        super(context);
    }

    @Override
    public String name() {
        return "do XXX;";
    }

    @Override
    public String description() {
        return "Execute predefined Gremlin queries. Example: do (1); do someQuery();";
    }

    @Override
    public boolean acceptCommand(String command) {
        return command.startsWith("do ");
    }

    @Override
    public Object doCommand(String command) {
        QueryItem queryItem = null;
        List<QueryItem> queryItems = getQueryItems();

        String method = command.trim().substring("do ".length());
        method = method.substring(0, method.length()-1);
        Pattern pattern = Pattern.compile("^\\((\\d+)\\)$");
        Matcher matcher = pattern.matcher(method);
        if (matcher.matches()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index >= 1 && index <= queryItems.size()) {
                queryItem = queryItems.get(index - 1);
            }

        }
        if (queryItem == null) {
            boolean success = false;
            for (QueryItem tmp : queryItems) {
                if (method.equals(tmp.getMethod())) {
                    success = true;
                    queryItem = tmp;
                    break;
                }

                if (!success) {
                    System.out.println("Predefined query not found: " + method);
                    return null;
                }
            }
        }
        System.out.println(queryItem.getDescription());
        System.out.println(queryItem.getQuery());
        executeGremlinQuery(queryItem.getQuery());
        return null;
    }

    private Object executeGremlinQuery(String query) throws GremlinException {
        Object result = null;
        for (CliCommand command : this.context.getCommands()) {
            if (command.acceptCommand(query)) {
                command.doCommand(query);
                break;
            }
        }
        return result;
    }

    private List<QueryItem> getQueryItems() {
        return (List<QueryItem>)this.context.getValue(ListPredefinedQueriesCommand.QUERY_ITEMS);
    }

}
