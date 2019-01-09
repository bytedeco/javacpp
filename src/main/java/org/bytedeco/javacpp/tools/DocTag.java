/*
 * Copyright (C) 2019 Samuel Audet, Hervé Guillemet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bytedeco.javacpp.tools;

import java.util.regex.Pattern;

/**
 * Documentation tags, where we keep only the ones that could be compatible between Javadoc and Doxygen.
 *
 * @author Hervé Guillemet
 */
class DocTag {
    static String[][] docTagsStr = {
        { "authors?\\b", "author" },
        { "deprecated\\b", "deprecated" },
        { "(?:exception|throws?)\\b", "throws" },
        { "param\\s*(\\[[a-z,\\s]+\\])\\s+(\\S+)", "param $2 $1 " },
        { "param\\b", "param" },
        { "(?:returns?|result)\\b", "return" },
        { "(?:see|sa)\\b", "see" },
        { "since\\b", "since" },
        { "version\\b", "version" }
        /* "code", "docRoot", "inheritDoc", "link", "linkplain", "literal", "serial", "serialData", "serialField", "value" */
    };
    static DocTag[] docTags = new DocTag[docTagsStr.length];
    static {
        for (int i=0; i<docTagsStr.length; i++)
            docTags[i] = new DocTag(docTagsStr[i][0], docTagsStr[i][1]);
    }

    Pattern pattern;
    String replacement;
    DocTag(String p, String r) {
        pattern = Pattern.compile(p);
        replacement = r;
    }
}
