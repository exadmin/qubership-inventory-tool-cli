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

package org.qubership.itool.tasks.init;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_MICROSERVICE_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.V_UNKNOWN;
import static org.qubership.itool.utils.GraphHelper.isComponentAMicroservice;

public class FillMandatoryValuesVerticle extends FlowTask {
    protected Logger LOGGER = LoggerFactory.getLogger(FillMandatoryValuesVerticle.class);

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        List<JsonObject> components =
                V().hasType("domain").out().hasKeys(F_REPOSITORY).toList();
        for (JsonObject component : components) {
            tryToFillMandatoryComponentFields(component);
        }

        List<JsonObject> domains = V().hasType("domain").toList();
        for (JsonObject domain : domains) {
            tryToFillMandatoryDomainFields(domain);
        }

        taskCompleted(taskPromise);
    }

    private void tryToFillMandatoryComponentFields(JsonObject component) {
        // Component name
        String name = component.getString(F_NAME);
        if (null == name) {
            name = (String) JsonPointer.from("/details/name").queryJson(component);
        }
        if (null == name) {
            name = (String) JsonPointer.from(P_DETAILS_DNS_NAME).queryJson(component);
        }
        if (null == name) {
            name = component.getString(F_ID);
        }
        getLogger().warn("{} : Component name was set to {}", component.getString(F_ID), name);
        component.put(F_NAME, name);

        // Component type
        String type = component.getString(F_TYPE, V_UNKNOWN);
        component.put(F_TYPE, type);

        //Component abbreviation
        JsonObject details = component.getJsonObject("details");

        if(details != null){
            if(details.getString("abbreviation") == null){
                if(component.getString("abbreviation") == null){
                    component.put("abbreviation", component.getString(F_ID));
                }
                details.put("abbreviation", component.getString("abbreviation"));
            }
        }

        if(component.getString("abbreviation") == null){
            component.put("abbreviation", component.getString(F_ID));
        }

        if (component.getBoolean(F_MICROSERVICE_FLAG) == null) {
            component.put(F_MICROSERVICE_FLAG, isComponentAMicroservice(graph, component));
        }
    }

    private void tryToFillMandatoryDomainFields(JsonObject domain) {
        String domainId = domain.getString("id");
        domain.put("abbreviation", ConfigUtils.stripDomainId(domainId));
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
