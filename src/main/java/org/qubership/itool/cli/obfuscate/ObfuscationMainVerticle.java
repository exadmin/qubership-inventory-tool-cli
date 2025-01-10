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

package org.qubership.itool.cli.obfuscate;

import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.FlowMainVerticle;

import static org.qubership.itool.cli.ci.CiConstants.*;


public class ObfuscationMainVerticle extends FlowMainVerticle {
    protected static final Logger LOG = LoggerFactory.getLogger(ObfuscationMainVerticle.class);

    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected List<String> getFlowSequence() throws Exception {
        return loadFlowSequence("classpath:/org/qubership/itool/cli/obfuscate/ObfuscationFlow.txt");
    }

    @Override
    public void start() throws Exception {
        JsonObject config = config();
        String srcFile = config.getString(P_INPUT_FILE);
        String srcDir = config.getString(P_INPUT_DIRECTORY);
        String sourcePath;
        if (srcDir == null) {
            sourcePath = srcFile;
        } else {
            sourcePath = new File(srcDir, srcFile).getPath();
        }

        JsonObject dump = null;
        try {
            dump = JsonUtils.readJsonFile(sourcePath);
        } catch (IOException /*| DecodeException*/ e) {
            LOG.error("Can't load source graph: {}", sourcePath);
            throw e;
        }
        if (dump != null) {
            this.flowContext.restoreData(dump);
        } else {
            LOG.error("Can't load source graph: {}", sourcePath);
            throw new IllegalArgumentException("Can't load source graph");
        }

        super.start();
    }

}
