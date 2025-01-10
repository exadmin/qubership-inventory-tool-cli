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

package org.qubership.itool.tasks.extract;

import org.qubership.itool.tasks.FlowTask;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.graphExtractor.GraphDataExtractor;
import org.qubership.itool.modules.graphExtractor.impl.LanguageAndFrameworkExtractor;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.qubership.itool.cli.ci.CiConstants.P_OUTPUT_FILE;


/** Extract frameworks and languages and save the results to output file */
public class ExtractFrameworksAndLanguagesVerticle extends FlowTask {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ExtractFrameworksAndLanguagesVerticle.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        GraphDataExtractor extractor = new LanguageAndFrameworkExtractor();
        var result = extractor.getDataFromGraph(this.graph);

        JsonObject config = config();
        String resultFile = config.getString(P_OUTPUT_FILE);
        JsonUtils.saveJson(Path.of(resultFile), result, true);

        taskCompleted(taskPromise);
    }

}
