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

public class HelpCommand extends AbstractCliCommand {

    public HelpCommand(CliContext context) {
        super(context);
    }

    @Override
    public boolean acceptCommand(String command) {
        return "help;".equals(command);
    }

    @Override
    public Object doCommand(String command) {
        System.out.println("Gremlin CLI commands:");
        for (CliCommand cmd : context.getCommands()) {
            if (cmd.name() != null) {
                System.out.println(cmd.name() + "\t\t- " + cmd.description());
            }
        }
        System.out.println();
        return null;
    }

}
