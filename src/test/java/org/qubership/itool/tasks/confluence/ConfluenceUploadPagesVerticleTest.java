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

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.qubership.itool.tasks.confluence.ConfluenceUploadPagesVerticle.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.qubership.itool.utils.ConfigProperties.UPLOAD_KEY_ALL;
import static org.qubership.itool.utils.ConfigProperties.UPLOAD_KEY_NONE;

public class ConfluenceUploadPagesVerticleTest {

    private ConfluenceUploadPagesVerticle uploadPagesVerticle = new ConfluenceUploadPagesVerticle();

    @Test
    void considerUploadingTitle() {
        JsonObject generatedPage = new JsonObject()
                .put("title", "Tech of domain1")
                .put("type", "component");
        JsonObject action = new JsonObject()
                .put(ACTION_KEY_ACTION, ACTION_CREATE)
                .put(ACTION_KEY_GENERATED_PAGE, generatedPage);
        assertTrue(uploadPagesVerticle.considerToPerformAction("Tech of domain1", action));
        assertTrue(uploadPagesVerticle.considerToPerformAction("Tech of domain2, Tech of domain1", action));
        assertTrue(uploadPagesVerticle.considerToPerformAction("Tech of domain1, Tech of domain3", action));
        assertFalse(uploadPagesVerticle.considerToPerformAction("Tech of domain1.component 1 name", action));
    }

    @Test
    void considerUploadingType() {
        JsonObject generatedPage = new JsonObject()
                .put("title", "Some report")
                .put("type", "report");
        JsonObject action = new JsonObject()
                .put(ACTION_KEY_ACTION, ACTION_CREATE)
                .put(ACTION_KEY_GENERATED_PAGE, generatedPage);
        assertTrue(uploadPagesVerticle.considerToPerformAction("type:report", action));
        assertTrue(uploadPagesVerticle.considerToPerformAction("type:report, Tech of domain1", action));
        assertTrue(uploadPagesVerticle.considerToPerformAction("Tech of domain1, type:report", action));
        assertFalse(uploadPagesVerticle.considerToPerformAction("type:component, Tech of domain1", action));
    }

    @Test
    void considerUploadingKeys() {
        JsonObject generatedPage = new JsonObject()
                .put("title", "Tech of domain1")
                .put("type", "component");
        JsonObject action = new JsonObject()
                .put(ACTION_KEY_ACTION, ACTION_CREATE)
                .put(ACTION_KEY_GENERATED_PAGE, generatedPage);
        assertTrue(uploadPagesVerticle.considerToPerformAction(UPLOAD_KEY_ALL, action));
        assertFalse(uploadPagesVerticle.considerToPerformAction(UPLOAD_KEY_NONE, action));
    }
}