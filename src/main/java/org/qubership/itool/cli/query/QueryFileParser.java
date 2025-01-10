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

package org.qubership.itool.cli.query;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.utils.FSUtils;

import java.io.IOException;
import java.util.List;

public class QueryFileParser {
    private enum STATE { init, method, description, query };

    public void parse(List<QueryItem> queryItems, String fileName) {
        String source;
        try {
            source = FSUtils.readFileSafe(fileName);
        } catch (IOException e) {
            System.out.println("Failed to read predefined queries");
            return;
        }

        String method = null;
        String description = null;
        String query = "";
        STATE state = STATE.init;

        for (String row : source.split("\n\r|\r\n|\r|\n")) {
            if (row.startsWith("#")) {
                // comment
                continue;
            }

            if (StringUtils.isBlank(row) && state.equals(STATE.init)) {
                continue;
            }

            if (StringUtils.isBlank(row) && state.equals(STATE.query)) {
                QueryItem queryItem = new QueryItem(method, description, query);
                queryItems.add(queryItem);
                method = null;
                description = null;
                query = "";
                state = STATE.method;
                continue;
            }

            switch (state) {
                case init:
                case method: method = row; state = STATE.description; break;
                case description: description = row; state = STATE.query; break;
                case query: query = query + row + "\n";
            }
        }
    }
}
