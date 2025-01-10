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

package org.qubership.itool.tasks.parsing.configuration;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.tasks.parsing.AbstractParseFileTask;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

public class ProcessGitInfoFilesTask extends AbstractParseFileTask {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ProcessGitInfoFilesTask.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String[] getFilePatterns() {
        return new String[] { "git.info" };
    }

    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName)
            throws IOException
    {
        JsonObject gitInfo = JsonUtils.readJsonFile(fileName);
        String branch = gitInfo.getString("branch");
        if (StringUtils.isNotEmpty(branch)) {
            getLogger().info("Setting branch {} for {} from git.info", branch, component.getString(Graph.F_ID));
            JsonPointer releaseBranchPtr = JsonPointer.from("/details/releaseBranch");
            releaseBranchPtr.writeJson(component, branch, true);
            component.put("releaseBranch", branch);
        }
    }

}
