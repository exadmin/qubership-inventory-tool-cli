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

package org.qubership.itool.tasks.parsing;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Common subclass for data parsers reading from file system
 */
public abstract class AbstractParseFileTask extends AbstractInclusiveParseFileTask {

    @Override
    protected boolean isExcluded(Collection<Path> topDirExcludes, Path relativePath, Path fileDirectory) {
        boolean isExcluded = super.isExcluded(topDirExcludes, relativePath, fileDirectory);

        if (relativePath.toString().contains("target" + File.separator)) {
            for (int currentIndex = 0; currentIndex < relativePath.getNameCount(); currentIndex++) {
                Path currentElement = relativePath.getName(currentIndex);
                if (currentElement.toString().equals("target")) {
                    if (fileDirectory.resolve(currentElement).resolveSibling("pom.xml").toFile().exists()) {
                        getLogger().trace("Excluding directory {} with both target and pom.xml present", relativePath);
                        return true;
                    }
                }
            }
        }
        return isExcluded;
    }

}
