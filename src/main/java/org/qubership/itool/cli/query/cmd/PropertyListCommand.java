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

import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.qubership.itool.cli.query.CliContext.PROPERTIES;

public class PropertyListCommand extends AbstractCliCommand {
    private static final Pattern PATTERN = Pattern.compile("^\\s*show\\s+properties\\s*;\\s*$");

    Properties props;

    public PropertyListCommand(CliContext context) {
        super(context);
        this.props = (Properties)context.getValue(PROPERTIES);
    }

    @Override
    public String name() {
        return "show properties;";
    }

    @Override
    public String description() {
        return "Show properties name and current values";
    }

    @Override
    public boolean acceptCommand(String command) {
        return PATTERN.matcher(command).matches();
    }

    @Override
    public Object doCommand(String command) {
        Enumeration<Object> enumeration = this.props.keys();
        System.out.println("Current settings:");
        for (String key : this.props.stringPropertyNames()) {
            System.out.println(key + " = " + this.props.getProperty(key));
        }
        return null;
    }

}
