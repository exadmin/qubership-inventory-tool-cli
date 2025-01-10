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
import io.vertx.core.Vertx;

public class ExitCommand extends AbstractCliCommand {

    public ExitCommand(CliContext context) {
        super(context);
    }

    @Override
    public String name() {
        return "exit;";
    }

    @Override
    public String description() {
        return "Close Gremlin CLI";
    }

    @Override
    public boolean acceptCommand(String command) {
        return "exit;".equals(command.toLowerCase());
    }

    @Override
    public Object doCommand(String command) {
        Vertx.currentContext().owner().close();
        System.exit(0);
        return null;
    }

}
