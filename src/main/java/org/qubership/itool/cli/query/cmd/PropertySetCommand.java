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

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.qubership.itool.cli.query.CliContext.PROPERTIES;

public class PropertySetCommand extends AbstractCliCommand {

    Properties props;

    public PropertySetCommand(CliContext context) {
        super(context);
        this.props = (Properties)context.getValue(PROPERTIES);
    }

    @Override
    public String name() {
        return "set <property> = <name>;";
    }

    @Override
    public String description() {
        return "Set value for the specified property";
    }

    @Override
    public boolean acceptCommand(String command) {
        return command.startsWith("set ");
    }

    @Override
    public Object doCommand(String command) {
        Pattern pattern = Pattern.compile("^set\\s+([a-zA-Z\\.]+)\\s*=\\s*(.+)\\s*;\\s*$");
        Matcher matcher = pattern.matcher(command);
        if (!matcher.matches()) {
            System.out.println("Wrong set command: " + command);
            return null;
        }
        String property = matcher.group(1);
        String value = matcher.group(2);
        if (!this.props.containsKey(property)) {
            System.out.println("Wrong property name: " + property);
            System.out.print("Possible name: ");
            StringBuilder builder = new StringBuilder();
            for (String key : this.props.stringPropertyNames()) {
                builder.append(key).append(", ");
            }
            String propertyNames = builder.toString();
            System.out.println(propertyNames.substring(0, propertyNames.length()-2));
            return null;
        }

        if (property.equals("result.limit")) {
            this.props.put(property, Integer.valueOf(value));
        } else {
            this.props.put(property, value);
        }

        return null;
    }

}
