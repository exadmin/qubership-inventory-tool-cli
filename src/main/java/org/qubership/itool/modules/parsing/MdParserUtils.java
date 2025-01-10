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

package org.qubership.itool.modules.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.commonmark.ext.gfm.tables.*;
import org.commonmark.node.*;

/**
 * Contains useful common methods for MD parsers
 */
public class MdParserUtils {

    //--- Finders for headings

    public static final Function<Node, String> SIMPLE_EXTRACTOR =
        textNode -> {
            if (textNode instanceof Text) {
                return ((Text) textNode).getLiteral();
            } else if (textNode instanceof Link) {
                return ((Link)textNode).getDestination();
            } else {
                return null;
            }
        };

    public static final Function<Node, String> LOWERCASE_EXTRACTOR =
        textNode -> {
            if (textNode instanceof Text) {
                return ((Text) textNode).getLiteral().toLowerCase();
            } else if (textNode instanceof Link) {
                return ((Link)textNode).getDestination().toLowerCase();
            } else {
                return null;
            }
        };

    public static Node findHeading(Node node, int level, String titleToFind) {
        return findHeading(node, level, titleToFind, SIMPLE_EXTRACTOR);
    }

    public static Node findHeading(Node node, int level, String titleToFind, Function<Node, String> titleExtractor) {
        while (node != null) {
            if (node instanceof Heading) {
                Heading heading = (Heading) node;
                if (heading.getLevel() == level) {
                    Node textNode = heading.getFirstChild();
                    if (titleToFind.equals(titleExtractor.apply(textNode))) {
                        return node;
                    }
                }
            }
            node = node.getNext();
        }

        return null;
    }


    //--- Collectors

    public static <T> List<T> collectSiblings(Node firstSibling, Class<T> clazz) {
        List<T> siblings = new ArrayList<>();
        Node siblingPointer = firstSibling;
        while (clazz.isInstance(siblingPointer)) {
            siblings.add(clazz.cast(siblingPointer));
            siblingPointer = siblingPointer.getNext();
        }
        return siblings;
    }

    public static List<Node> collectSiblings(Node firstSibling) {
        return collectSiblings(firstSibling, Node.class);
    }

    public static List<Paragraph> collectParagraphs(Node firstParagraph) {
        return collectSiblings(firstParagraph, Paragraph.class);
    }

    public static List<TableCell> collectCells(Node firstCell) {
        return collectSiblings(firstCell, TableCell.class);
    }

    public static List<TableRow> collectRows(Node firstRow) {
        return collectSiblings(firstRow, TableRow.class);
    }


    //--- Table processors

    public static TableHead findTableHead(Node previous) {
        Node node = previous.getNext();
        while (node != null) {
            if (node instanceof TableBlock) {
                TableBlock tableBlock = (TableBlock) node;
                return (TableHead) tableBlock.getFirstChild();
            }
            node = node.getNext();
        }
        return null;
    }

    public static TableBody findTableBody(Node previous) {
        Node node = previous.getNext();
        while (node != null) {
            if (node instanceof TableBody) {
                return (TableBody) node;
            }
            if (node instanceof TableBlock) {
                TableBlock tableBlock = (TableBlock) node;
                return (TableBody) tableBlock.getLastChild();
            }
            node = node.getNext();
        }
        return null;
    }

    public static Integer findColumnIdxByText(List<TableCell> rowCells, String... anyOf) {
        Set<String> find = Set.of(anyOf);
        for (int idx = 0; idx < rowCells.size(); idx++) {
            TableCell cell = rowCells.get(idx);
            Node firstChild = cell.getFirstChild();
            if (firstChild instanceof Text && find.contains(((Text)firstChild).getLiteral()))
                return idx;
        }
        return null;
    }

}
