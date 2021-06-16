/*
 * Copyright (C) 2014-2019 Samuel Audet
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Samuel Audet
 */
class Context {
    Context() {
        usingList = new ArrayList<String>();
        namespaceMap = new HashMap<String,String>();
    }
    Context(Context c) {
        namespace = c.namespace;
        baseType = c.baseType;
        cppName = c.cppName;
        javaName = c.javaName;
        constName = c.constName;
        constBaseName = c.constBaseName;
        immutable = c.immutable;
        inaccessible = c.inaccessible;
        beanify = c.beanify;
        objectify = c.objectify;
        virtualize = c.virtualize;
        variable = c.variable;
        infoMap = c.infoMap;
        templateMap = c.templateMap;
        usingList = c.usingList;
        namespaceMap = c.namespaceMap;
    }

    String namespace = null;
    String baseType = null;
    String cppName = null;
    String javaName = null;
    String constName = null;
    String constBaseName = null;
    boolean immutable = false;
    boolean inaccessible = false;
    boolean beanify = false;
    boolean objectify = false;
    boolean virtualize = false;
    Declarator variable = null;
    InfoMap infoMap = null;
    TemplateMap templateMap = null;
    List<String> usingList = null;
    Map<String,String> namespaceMap = null;

    /** Return all likely combinations of namespaces and template arguments for this C++ type */
    String[] qualify(String cppName) {
        return qualify(cppName, null);
    }
    /** or function, if parameters != null */
    String[] qualify(String cppName, String parameters) {
        if (cppName == null || cppName.length() == 0) {
            return new String[0];
        }
        if (cppName.startsWith("::")) {
            // already in global namespace, so strip leading operator
            return new String[] { cppName.substring(2) };
        }
        for (Map.Entry<String, String> e : namespaceMap.entrySet()) {
            cppName = cppName.replaceAll(e.getKey() + "::", e.getValue() + "::");
        }
        List<String> names = new ArrayList<String>();
        String ns = namespace != null ? namespace : "";
        while (ns != null) {
            String name = ns.length() > 0 ? ns + "::" + cppName : cppName;
            if (parameters != null && name.endsWith(parameters)) {
                name = name.substring(0, name.length() - parameters.length());
            }
            TemplateMap map = templateMap;
            while (map != null) {
                String name2 = map.getName();
                if (parameters != null && name2 != null && name2.endsWith(parameters)) {
                    name2 = name2.substring(0, name2.length() - parameters.length());
                }
                if (name.equals(name2)) {
                    String args = "<", separator = "";
                    for (Type t : map.values()) {
                        // assume that missing arguments have default values
                        if (t != null) {
                            args += separator + t.cppName;
                            separator = ",";
                        }
                    }
                    names.add(name + args + (args.endsWith(">") ? " >" : ">") + (parameters != null ? parameters : ""));
                    break;
                }
                map = map.parent;
            }
            names.add(name);

            for (String s : usingList) {
                String prefix = infoMap.normalize(cppName, false, true);
                int i = s.lastIndexOf("::") + 2;
                String ns2 = ns.length() > 0 ? ns + "::" + s.substring(0, i) : s.substring(0, i);
                String suffix = s.substring(i);
                if (suffix.length() == 0 || prefix.equals(suffix)) {
                    names.add(ns2 + cppName);
                }
            }
            ns = infoMap.normalize(ns, false, true);
            int i = ns.lastIndexOf("::");
            ns = i >= 0 ? ns.substring(0, i) : ns.length() > 0 ? "" : null;
        }
        return names.toArray(new String[names.size()]);
    }

    /** Shorten a qualified Java name, given the Context */
    String shorten(String javaName) {
        if (this.javaName != null) {
            int lastDot = 0;
            String s1 = javaName, s2 = this.javaName + '.';
            for (int i = 0; i < s1.length() && i < s2.length(); i++) {
                if (s1.charAt(i) != s2.charAt(i)) {
                    break;
                } else if (s1.charAt(i) == '.') {
                    lastDot = i;
                }
            }
            if (lastDot > 0) {
                javaName = javaName.substring(lastDot + 1);
            }
        }
        return javaName;
    }
}
