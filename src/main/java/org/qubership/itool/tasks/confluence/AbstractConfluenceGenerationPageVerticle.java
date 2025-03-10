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

import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.utils.ConfigUtils;

import java.util.List;

import static org.qubership.itool.utils.ConfigProperties.CONFLUENCE_SPACE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;

public abstract class AbstractConfluenceGenerationPageVerticle extends AbstractGenerationPageVerticle {

    public static final String OUTPUT_CONFLUENCE = "output/confluence/";

    @Override
    protected String[] features() {
        return new String[] { "confluence2Generate" };
    }

    @Override
    protected String getOutputPath() {
        return OUTPUT_CONFLUENCE;
    }

    @Override
    protected void setupPageProperties(ConfluencePage page) {
        page.addDataModel("release", ConfigUtils.getConfigValue(RELEASE_POINTER, config()));
        page.setSpace(ConfigUtils.getConfigValue(CONFLUENCE_SPACE_POINTER, config()));
    }

    @Override
    protected String getPageExtension() {
        return ".confluence";
    }

    @Override
    protected String getPagesMetaLocation() {
        return "confluencePages";
    }

    protected String getPublicDomainId(String domainId) {
        return ConfigUtils.stripDomainId(domainId);
    }

    protected abstract List<ConfluencePage> prepareConfluencePageList(String department);

    @Override
    protected List<ConfluencePage> preparePageList() {
        return prepareConfluencePageList();
    }

    @Override
    protected List<ConfluencePage> preparePageList(String department) {
        return prepareConfluencePageList(department);
    }

    protected List<ConfluencePage> prepareConfluencePageList() {
        return super.preparePageList();
    };
}
