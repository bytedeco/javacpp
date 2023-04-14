/*
 * Copyright (C) 2023 Hervé Guillemet
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Templates {

    static final Pattern templatePattern = Pattern.compile("<[^<>=]*>");

    /** Remove template arguments from s, taking care of nested templates, default arguments (xxx<>), operator <=>, ->, etc... */
    static String strip(String s) {
        Matcher m;
        do {
            m = templatePattern.matcher(s);
            s = m.replaceFirst("");
        } while (!m.hitEnd());
        return s;
    }

    /** Returns {@code strip(s).length() == s.length()}. */
    static boolean notExists(String s) {
        return strip(s).length() == s.length();
    }

    /** Split s at ::, but taking care of qualified template arguments */
    static List<String> splitNamespace(String s) {
        String sTemplatesMasked = s;
        for (;;) {
            Matcher m = templatePattern.matcher(sTemplatesMasked);
            if (m.find()) {
                char[] c = new char[m.end() - m.start()];
                Arrays.fill(c, '.');
                sTemplatesMasked = sTemplatesMasked.substring(0, m.start()) + new String(c) + sTemplatesMasked.substring(m.end());
            } else {
                break;
            }
        }
        ArrayList<String> comps = new ArrayList<>();
        int start = 0;
        for (;;) {
            int i = sTemplatesMasked.indexOf("::", start);
            if (i >= 0) {
                comps.add(s.substring(start, i));
                start = i + 2;
            } else {
                break;
            }
        }
        comps.add(s.substring(start));
        return comps;
    }
}
