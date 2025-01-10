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

package org.qubership.itool.tasks.confluence;

import org.qubership.itool.modules.diagram.UMLDiagramEncoder;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.modules.template.TemplateService;
import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.JsonUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.qubership.itool.utils.ConfigProperties.CONFLUENCE_SPACE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.PLANTUML_URL_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;

public abstract class AbstractConfluenceGenerationPageVerticle extends AbstractAggregationTaskVerticle {

    @Resource
    private TemplateService templateService;

    public static final String OUTPUT_CONFLUENCE = "output/confluence/";

    @Override
    protected String[] features() {
        return new String[] { "confluence2Generate" };
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        final JsonArray rootConfluencePages = JsonUtils.getOrCreateJsonArray(graph.getVertex(Graph.V_ROOT), "confluencePages");

        vertx.executeBlocking(promise -> {
            List<ConfluencePage> pageList = prepareConfluencePageList();
            pageList.forEach(page -> {
                page.addDataModel("release", ConfigUtils.getConfigValue(RELEASE_POINTER, config()));
                page.setSpace(ConfigUtils.getConfigValue(CONFLUENCE_SPACE_POINTER, config()));
            });
            promise.complete(pageList);
        }, res -> {
            if (res.failed()) {
                report.internalError(ExceptionUtils.getStackTrace(res.cause()));
                taskCompleted(taskPromise);
                return;
            }

            @SuppressWarnings("unchecked")
            List<ConfluencePage> confluencePageList = (List<ConfluencePage>)res.result();
            if (confluencePageList == null) {
                taskCompleted(taskPromise);
                return;
            }

            @SuppressWarnings("rawtypes")
            List<Future> futureList = new ArrayList<>();
            for (ConfluencePage confluencePage : confluencePageList) {
                getLogger().info("Generate Confluence page: " + confluencePage.getDirectoryPath() + "/" + confluencePage.getFileName());

                Future<Void> future = Future.future(promise -> {
                    try {
                        String result =  templateService.processTemplate(confluencePage);
                        if (result != null) {
                            File dir = new File(OUTPUT_CONFLUENCE + confluencePage.getDirectoryPath());
                            if (!dir.exists()) {
                                dir.mkdirs();
                            }
                            String path = dir.getPath() + "/" + confluencePage.getFileName() + ".confluence";
                            vertx.fileSystem().writeFileBlocking(path, Buffer.buffer(result));

                            JsonObject page = new JsonObject();
                            page.put("title", confluencePage.getTitle());
                            page.put("parentTitle", confluencePage.getParentTitle());
                            page.put("type", confluencePage.getType());
                            page.put("onDiskPath", path);
                            page.put("space", confluencePage.getSpace());
                            rootConfluencePages.add(page);
                            getLogger().debug("Page '" + page.getString("title")
                                    + "' generation complete (" + page.getString("onDiskPath") + ")");
                        }
                        promise.complete();

                    } catch (Exception ex) {
                        promise.fail(ex);
                        JsonObject component = confluencePage.getElement();
                        this.report.exceptionThrown(component, ex);
                    }
                });
                futureList.add(future);
            }

            completeCompositeTask(futureList, taskPromise);
        });
    }

    protected List<ConfluencePage> prepareConfluencePageList() {
        List<ConfluencePage> confluencePageList = new ArrayList<>();

        List<String> departments = V().hasType("domain").<String>value("department").dedup().toList();
        for (String department : departments) {
            List<ConfluencePage> confluencePages = prepareConfluencePageList(department);
            for (ConfluencePage page : confluencePages) {
                page.addDataModel("department", department);
            }
            confluencePageList.addAll(confluencePages);
        }

        return confluencePageList;
    }

    protected String getPublicDomainId(String domainId) {
        return ConfigUtils.stripDomainId(domainId);
    }

    protected abstract List<ConfluencePage> prepareConfluencePageList(String department);

    protected String buildDiagramImageURL(TemplateMethodModelEx diagramMethod, List<Object> arguments) {
        String generatedText = null;
        try {
            generatedText = (String) diagramMethod.exec(arguments);
        } catch (TemplateModelException e) {
            getLogger().error("Could not generate plantuml diagram. StackTrace : "+e.getMessage());
        }

        if (StringUtils.isNotEmpty(generatedText)) {
            String encodedText = UMLDiagramEncoder.encodeDiagram(generatedText);
            JsonObject config = config();
            String plantumlURL = ConfigUtils.getConfigValue(PLANTUML_URL_POINTER, config);
            encodedText = plantumlURL + encodedText;
            return encodedText;
        }
        return null;
    }
}
