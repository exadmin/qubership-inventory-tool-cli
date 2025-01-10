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

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.utils.ConfigProperties.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * java -jar exec -l your_login -p -sp true -df confluenceGenerate,confluenceUpload,excelExport,mavenDependency,repositoryUpdate
 * java -jar exec -l your_login -p -sp true -df confluenceUpload,mavenDependency,repositoryUpdate \
 *     -ss excelExport -ls releaseDiff
 * java -jar exec -l your_login -p -sp confluenceGenerateComponentPages,parseComponentConfFiles -df .....
 */
@Name("exec")
@Summary("Execute inventory-tool")
public class ExecCommand extends AbstractCommand {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ExecCommand.class);

    public static final String DEFAULT_INPUT_DIRECTORY_DOCKER = "/var/input";
    public static final String DEFAULT_OUTPUT_DIRECTORY_DOCKER = "/var/output";


    protected Logger getLogger() {
        return LOGGER;
    }


    @Override
    public void run() throws CLIException {
        getLogger().info("Inventory tool main flow execution");
        runFlow(new ExecVerticle(), null);
    }

    //------------------------------------------------------
    // Processing for default directories

    // When flow runs, "ciInputDirectory" and "ciOutputDirectory" may be taken from "ci.properties" file.
    // This method helps the command to fall back to default directories before the flow starts.
    protected void logAndFillDirs() {
        boolean isDocker = Boolean.parseBoolean(properties.getOrDefault(DOCKER_MODE, "false"));
        getLogger().info("dockerMode: {}", isDocker);

        String defInputDirectory = getDefaultInputDir(isDocker);
        String defOutputDirectory = getDefaultOutputDir(isDocker);

        getLogger().info("explicit inputDirectory: {}", properties.get(P_INPUT_DIRECTORY));
        getLogger().info("default inputDirectory: {}", defInputDirectory);
        getLogger().info("explicit outputDirectory: {}", properties.get(P_OUTPUT_DIRECTORY));
        getLogger().info("default outputDirectory: {}", defOutputDirectory);

        if (! properties.containsKey(P_INPUT_DIRECTORY)) {
            if (defInputDirectory != null) {
                properties.put(P_INPUT_DIRECTORY, DEFAULT_INPUT_DIRECTORY_DOCKER);
            } else {
                LOGGER.error("Either --docker=true or --inputDirectory must be specified! EXITTING!");
                System.exit(1);
            }
        }
        if (! properties.containsKey(P_OUTPUT_DIRECTORY)) {
            if (defOutputDirectory != null) {
                properties.put(P_OUTPUT_DIRECTORY, defOutputDirectory);
            } else {
                // Do nothing
            }
        }
    }

    protected String getDefaultInputDir(boolean isDocker) {
        return isDocker ? DEFAULT_INPUT_DIRECTORY_DOCKER : null;
    }

    protected String getDefaultOutputDir(boolean isDocker) {
        return properties.get(P_DEFAULT_OUTPUT_DIRECTORY);
    }

    //------------------------------------------------------
    // Command-line args

    @Option(longName = "login", argName = "login", shortName = "l", required = false)
    @Description("Login to access services requiring authentication")
    public void setLogin(String login) {
        properties.put("login", login);
    }

    @Option(longName = "excelExport", argName = "excelExport", shortName = "e", required = false)
    @Description("Path pattern for excel report export")
    public void setExcelExport(String excelExport) {
        properties.put("excelExport", excelExport);
    }

    @Option(longName = "uploadConfluencePages", argName = "upload", shortName = "u", required = false)
    @Description("List of the page titles to be uploaded to Confluence. Delimiter is ','. " +
            "Examples: all; none; type:report; \"Tech of DOMAIN1, Cloud Libraries list\"")
    public void setUploadConfluencePages(String uploadConfluencePages) {
        properties.put(UPLOAD_CONFLUENCE_PAGES_POINTER, uploadConfluencePages);
    }

    @Option(longName = SAVE_PROGRESS, argName = SAVE_PROGRESS, shortName = "sp", required = false)
    @Description("Save execution progress. That allow restart progress from the specified step.")
    public void setSaveProgress(String saveProgress) {
        properties.put(SAVE_PROGRESS, saveProgress);
    }

    @Option(longName = "startStep", argName = "startStep", shortName = "ss", required = false)
    @Description("Start execution from the specified step if progress was saved before. See 'saveProgress' property.")
    public void setStartStep(String startStep) {
        properties.put("startStep", startStep);
    }

    @Option(longName = "lastStep", argName = "lastStep", shortName = "ls", required = false)
    @Description("Last execution step (if progress was saved before). See 'saveProgress' property.")
    public void setLastStep(String lastStep) {
        properties.put("lastStep", lastStep);
    }

    @Option(longName = "includeDomains", argName = "includeDomains", shortName = "id", required = false)
    @Description("List of Domains that must be processed. Delimiter is ','")
    public void setIncludeDomains(String includeDomains) {
        properties.put("includeDomains", includeDomains);
    }

    @Option(longName = "disabledFeatures", argName = "disabledFeatures", shortName = "df", required = false)
    @Description("List of the disabled features. Delimiter is ','. Possible: confluenceGenerate,confluenceUpload,excelExport,mavenDependency,repositoryUpdate")
    public void setDisabledFeatures(String disabledFeatures) {
        properties.put("disabledFeatures", disabledFeatures);
    }

    @Option(longName = "release", argName = "release", shortName = "r", required = false)
    @Description("Release version to be used as a suffix during the export to Confluence")
    public void setRelease(String release) {
        properties.put(RELEASE_POINTER, release);
    }

    @Option(longName = "releaseBranch", argName = "releaseBranch", shortName = "rb", required = false)
    @Description("Release version to be used as a suffix during the export to Confluence")
    public void setReleaseBranch(String releaseBranch) {
        properties.put(RELEASE_BRANCH_POINTER, releaseBranch);
    }

    @Option(longName = "priorRelease", argName = "priorRelease", shortName = "pr", required = false)
    @Description("Release version prior to selected release to compare to")
    public void setPriorRelease(String priorRelease) {
        properties.put(PRIOR_RELEASE_POINTER, priorRelease);
    }

    @Option(longName = "passwordSource", argName = "passwordSource", shortName = "pws", required = false)
    @Description("Password source, e.g.: \"file:password.txt\"")
    public void setPasswordSource(String passwordSource) {
        properties.put(PASSWORD_SOURCE_PROPERTY, passwordSource);
    }

    @Option(longName = "offline", argName = "offline", required = false)
    @Description("Offline mode: true/false")
    public void setOfflineMode(String offlineMode) {
        properties.put(OFFLINE_MODE, offlineMode);
    }

    @Option(longName = "dockerMode", shortName = "docker", argName = "dockerMode", required = false)
    @Description("Docker mode: true/false")
    public void setDockerMode(boolean dockerMode) {
        properties.put(DOCKER_MODE, Boolean.toString(dockerMode));
        if (dockerMode) {
            properties.put(SAVE_PROGRESS, "false");
        }
    }

}
