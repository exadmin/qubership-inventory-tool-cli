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

package org.qubership.itool.tasks.html;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.diagram.UMLDiagramEncoder;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.modules.template.TemplateService;
import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;
import org.qubership.itool.tasks.confluence.AbstractGenerationPageVerticle;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.JsonUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.qubership.itool.utils.ConfigProperties.PLANTUML_URL_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;

public abstract class AbstractHtmlGenerationPageVerticle extends AbstractGenerationPageVerticle {

    @Resource
    private TemplateService templateService;

    public static final String OUTPUT_HTML = "output/html/";

    @Override
    protected String[] features() {
        return new String[] { "html2Generate" };
    }

    @Override
    protected String getOutputPath() {
        return OUTPUT_HTML;
    }

    @Override
    protected void setupPageProperties(ConfluencePage page) {
        page.addDataModel("release", ConfigUtils.getConfigValue(RELEASE_POINTER, config()));
    }

    @Override
    protected String getPageExtension() {
        return ".html";
    }

    @Override
    protected String getPagesMetaLocation() {
        return "htmlPages";
    }

    protected String getPublicDomainId(String domainId) {
        return ConfigUtils.stripDomainId(domainId);
    }

}
