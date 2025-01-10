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

package org.qubership.itool.cli;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qubership.itool.utils.ConfigProperties.*;


@Name("query")
@Summary("Execute Gremlin query")
public class QueryCommand extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryCommand.class);

    protected Logger getLogger() {
        return LOGGER;
    }

    public QueryCommand() {
        super();
        properties.put(OFFLINE_MODE, "true");
    }

    @Override
    public void run() throws CLIException {
        runFlow(new QueryVerticle(), null);
    }

    @Option(longName = "file", argName = "file", shortName = "f", required = false)
    @Description("JSON file that CLI should load instead of Graph dump")
    public void setFile(String file) {
        properties.put(QUERY_FILE_POINTER, file);
    }

    @Option(longName = "step", argName = "step", shortName = "s", required = false)
    @Description("Execution step for query (default is 'result' step)")
    public void setStep(String step) {
        properties.put(QUERY_STEP_POINTER, step);
    }

    @Option(longName = "login", argName = "login", shortName = "l", required = false)
    @Description("Login to access services requiring authentication")
    public void setLogin(String login) {
        properties.put("login", login);
    }

    @Option(longName = "passwordSource", argName = "passwordSource", shortName = "pws", required = false)
    @Description("Password source, default: \"file:password.txt\"")
    public void setPasswordSource(String passwordSource) {
        properties.put(PASSWORD_SOURCE_PROPERTY, passwordSource);
    }

    @Option(longName = "progressPath", argName = "progressPath", shortName = "pp", required = false)
    @Description("Path to progress folder (default is 'progress')")
    public void setProgressPath(String progressPath) {
        this.properties.put(QUERY_PROGRESS_PATH_POINTER, progressPath);
    }

}
