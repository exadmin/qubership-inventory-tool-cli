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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaveResultCommand extends AbstractCliCommand {
    private static final Pattern COMMAND_PATTERN = Pattern.compile("save result\\(\"(\\w+)\"(?:,\\s\"(.*)\")\\);");

    public SaveResultCommand(CliContext context) {
        super(context);
    }

    @Override
    public String name() {
        return "save result(format, filePath);";
    }

    @Override
    public String description() {
        return "Save query result to the file on the specified format (json, txt)";
    }

    @Override
    public boolean acceptCommand(String command) {
        return command.startsWith("save result(");
    }

    @Override
    public Object doCommand(String command) {
        Matcher matcher = COMMAND_PATTERN.matcher(command);
        if (!matcher.matches()) {
            System.out.println("Wrong command. Example: save result(\"txt\", \"all_microservices.txt\");");
            return null;
        }
        Object result = context().getValue(GremlinQueryCommand.LAST_EXECUTED_RESULT);
        if (result == null) {
            System.out.println("Gremlin query result not found");
            return null;
        }

        String fileFormat = matcher.group(1);
        String fileName = matcher.group(2);
        String fileSource = convertResult(result, fileFormat);
        if (fileSource == null) {
            System.out.println("Can't save result in the specified format");
        }
        return null;
    }

    private String convertResult(Object result, String fileFormat) {
        if ("txt".equals(fileFormat.toLowerCase())) {

        }
        return null;
    }

}
