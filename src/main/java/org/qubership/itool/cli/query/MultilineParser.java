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

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;
import org.jline.reader.impl.DefaultParser;

public class MultilineParser extends DefaultParser {

    @Override
    public ParsedLine parse(String line, int cursor, ParseContext context) throws SyntaxError {
        if ((Parser.ParseContext.UNSPECIFIED.equals(context)
            || Parser.ParseContext.ACCEPT_LINE.equals(context))
            && !line.trim().endsWith(";")) {
            throw new EOFError(-1, cursor, "Missing semicolon (;)");
        }

        return super.parse(line, cursor, context);
    }

}
