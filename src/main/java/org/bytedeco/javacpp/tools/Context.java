/*
 * Copyright (C) 2014 Samuel Audet
 *
 * This file is part of JavaCPP.
 *
 * JavaCPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bytedeco.javacpp.tools;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author Samuel Audet
 */
class Context {
    Context() {
        usingList = new LinkedList<String>();
    }
    Context(Context c) {
        namespace = c.namespace;
        group = c.group;
        inaccessible = c.inaccessible;
        variable = c.variable;
        templateMap = c.templateMap;
        usingList = c.usingList;
    }

    String namespace = null;
    Type group = null;
    boolean inaccessible = false;
    boolean virtualize = false;
    Declarator variable = null;
    TemplateMap templateMap = null;
    LinkedList<String> usingList = null;

    /** Return all likely combinations of namespaces and template arguments for this C++ type */
    String[] qualify(String cppName) {
        if (cppName == null || cppName.length() == 0) {
            return new String[0];
        }
        ArrayList<String> names = new ArrayList<String>();
        String ns = namespace != null ? namespace : "";
        while (ns != null) {
            String name = ns.length() > 0 ? ns + "::" + cppName : cppName;
            TemplateMap map = templateMap;
            while (map != null) {
                if (name.equals(map.getName())) {
                    String args = "<", separator = "";
                    for (String s : map.values()) {
                        args += separator + s;
                        separator = ",";
                    }
                    names.add(name + args + (args.endsWith(">") ? " >" : ">"));
                    break;
                }
                map = map.parent;
            }
            names.add(name);

            ns = InfoMap.normalize(ns, false, true);
            int i = ns.lastIndexOf("::");
            ns = i >= 0 ? ns.substring(0, i) : ns.length() > 0 ? "" : null;
        }
        for (String s : usingList) {
            String prefix = InfoMap.normalize(cppName, false, true);
            int i = s.lastIndexOf("::") + 2;
            ns = s.substring(0, i);
            String suffix = s.substring(i);
            if (suffix.length() == 0 || prefix.equals(suffix)) {
                names.add(ns + cppName);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /** Shorten a qualified Java name, given the Context */
    String shorten(String javaName) {
        if (group != null) {
            int lastDot = 0;
            String s1 = javaName, s2 = group.javaName + '.';
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
