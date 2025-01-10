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

package org.qubership.itool;

import org.qubership.itool.modules.parsing.InventoryJsonParser;

import io.vertx.core.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestParseInventoryFile {

    @Disabled
    @Test
    public void testParseJson() throws IOException {
        JsonObject domain = new JsonObject().put("id", "domain_id");
        JsonObject component = new JsonObject();
        InventoryJsonParser parser = new InventoryJsonParser();

        String inventorySource = null;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("inventory/t2_inventory.json");
        try(InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8")) {
            inventorySource = IOUtils.toString(reader);
        }

        parser.parse(domain, component, inventorySource);

        // TODO: some real tests, then remove @Disabled
    }

}
