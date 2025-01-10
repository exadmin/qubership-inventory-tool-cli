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
import org.qubership.itool.utils.FSUtils;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.qubership.itool.cli.query.cmd.ListPredefinedQueriesCommand.QUERY_CUSTOM_TXT;

public class SavePredefinedQueryCommand extends AbstractCliCommand {
    private static final Pattern COMMAND_PATTERN = Pattern.compile("save query\\(\"(\\w+)\"(?:,\\s\"(.*)\")\\);");

    public SavePredefinedQueryCommand(CliContext context) {
        super(context);
    }

    @Override
    public String name() {
        return "save query(name, description);";
    }

    @Override
    public String description() {
        return "Save last query (predefined list). Example: save query(\"superQuery\", \"some description\")";
    }

    @Override
    public boolean acceptCommand(String command) {
        return command.startsWith("save query(");
    }

    @Override
    public Object doCommand(String command) {
        Matcher matcher = COMMAND_PATTERN.matcher(command);
        if (!matcher.matches()) {
            System.out.println("Wrong command. Example: save query(\"name\", \"description\");");
            return null;
        }

        String lastExecutedQuery = (String)context().getValue(GremlinQueryCommand.LAST_EXECUTED_QUERY);
        @SuppressWarnings("unchecked")
        List<QueryItem> queryItemList = (List<QueryItem>) context().getValue(ListPredefinedQueriesCommand.QUERY_ITEMS);

        String method = matcher.group(1) + "()";
        String description = matcher.group(2);
        QueryItem item = new QueryItem(method, description, lastExecutedQuery);
        StringBuilder text = new StringBuilder();
        text.append("\n");
        text.append(item.getMethod()).append("\n");
        text.append(item.getDescription()).append("\n");
        text.append(item.getQuery()).append("\n\n");

        try {
            FSUtils.appendFile(QUERY_CUSTOM_TXT, text.toString());
        } catch (IOException e) {
            System.out.println("Can't add custom query. Reason: " + e.getMessage());
        }

        queryItemList.add(item);
        System.out.println("Query saved with name: " + item.getMethod());
        return null;
    }

}
