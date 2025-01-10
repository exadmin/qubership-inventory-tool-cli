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

import org.jline.reader.*;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.widget.AutopairWidgets;
import org.qubership.itool.cli.query.cmd.CliCommand;
import org.qubership.itool.cli.query.cmd.DoPredefinedQueryCommand;
import org.qubership.itool.cli.query.cmd.ExitCommand;
import org.qubership.itool.cli.query.cmd.GremlinQueryCommand;
import org.qubership.itool.cli.query.cmd.HelpCommand;
import org.qubership.itool.cli.query.cmd.ListPredefinedQueriesCommand;
import org.qubership.itool.cli.query.cmd.NothingCommand;
import org.qubership.itool.cli.query.cmd.PropertyListCommand;
import org.qubership.itool.cli.query.cmd.PropertySetCommand;
import org.qubership.itool.cli.query.cmd.SavePredefinedQueryCommand;
import org.qubership.itool.cli.query.cmd.SaveResultCommand;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.gremlin2.GremlinException;

import java.lang.reflect.Constructor;

public class CliQuery {

    private CliContext context;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public CliQuery(Graph graph) {
        this.context = new CliContext(graph);

        Class[] commands = new Class[] {
            DoPredefinedQueryCommand.class
            , ExitCommand.class
            , GremlinQueryCommand.class
            , HelpCommand.class
            , ListPredefinedQueriesCommand.class
            , NothingCommand.class
            , PropertyListCommand.class
            , PropertySetCommand.class
            , SavePredefinedQueryCommand.class
            , SaveResultCommand.class
        };

        for (Class clazz : commands) {
            try {
                Constructor constructor = clazz.getDeclaredConstructor(CliContext.class);
                CliCommand command = (CliCommand)constructor.newInstance(this.context);
                this.context.getCommands().add(command);

            } catch (Exception e) {
                System.err.println("CliCommand registration failed. Reason: " + e);
            }
        }
    }

    public void run() {
        LineReader reader = LineReaderBuilder.builder()
            .completer(
                new AggregateCompleter(
                    new ArgumentCompleter(new StringsCompleter("show"), new NullCompleter()),
                    new ArgumentCompleter(new StringsCompleter("show"), new StringsCompleter("aaa", "access-expression", "access-lists", "accounting", "adjancey"), new NullCompleter()),
                    new ArgumentCompleter(new StringsCompleter("show"), new StringsCompleter("ip"), new StringsCompleter("access-lists", "accounting", "admission", "aliases", "arp"), new NullCompleter()),
                    new ArgumentCompleter(new StringsCompleter("show"), new StringsCompleter("ip"), new StringsCompleter("interface"), new StringsCompleter("ATM", "Async", "BVI"), new NullCompleter())
                )
            )
            .parser(new MultilineParser())
            .build();

        // Create autopair widgets
        AutopairWidgets autopairWidgets = new AutopairWidgets(reader);
        // Enable autopair
        autopairWidgets.enable();

        while (true) {
            try {
                String userInput = reader.readLine(getPrompt()).trim();

                try {
                    executeGremlinQuery(userInput);
                } catch (GremlinException ge) {
                    System.out.println("Unknown userInput:");
                    System.out.println(userInput);
                }

            } catch (UserInterruptException uie) {
                // ignore it
            } catch (EndOfFileException eof) {
                // ignore it
            }
        }
    }

    public Object executeGremlinQuery(String query) throws GremlinException {
        Object result = null;
        boolean accepted = false;

        for (CliCommand command : this.context.getCommands()) {
            if (command.acceptCommand(query)) {
                accepted = true;
                result = command.doCommand(query);
                break;
            }
        }
        if (!accepted) {
            throw new GremlinException("Can't process query. Reason: command not found");
        }
        return result;
    }

    private String getPrompt() {
        return "Gremlin> ";
    }

}
