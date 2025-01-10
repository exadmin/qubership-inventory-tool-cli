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

package org.qubership.itool.cli.ci;

public interface CiConstants {

    //--- Config parameters

    String P_INPUT_DIRECTORY = "ciInputDirectory";
    String P_INPUT_FILE = "ciInputFile";
    String P_OUTPUT_DIRECTORY = "ciOutputDirectory";
    String P_DEFAULT_OUTPUT_DIRECTORY = "ciDefaultOutputDirectory";
    String P_OUTPUT_FILE = "ciOutputFile";

    String P_MOCK_DOMAIN = "ciMockDomain";
    String P_REPOSITORY = "ciRepository";
    String P_RUN_NAME = "ciRunName";
    String P_COMP_NAME = "ciCompName";
    String P_COMP_VERSION = "ciCompVersion";

    String P_APP_NAME = "ciAppName";
    String P_APP_VERSION = "ciAppVersion";

    String P_DUMP_BY = "ciDumpBy";
    String DUMP_BY_HASH = "hash";
    String DUMP_BY_ID = "id";
    String DUMP_BY_REPO = "repo";

    String OBFUSCATION_RULES = "obfuscationRules";

    String REPO_PATH = "repoPath";


}
