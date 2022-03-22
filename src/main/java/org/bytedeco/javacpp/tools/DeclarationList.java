/*
 * Copyright (C) 2014-2018 Samuel Audet
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
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

/**
 *
 * @author Samuel Audet
 */
class DeclarationList extends ArrayList<Declaration> {
    InfoMap infoMap = null;
    Context context = null;
    TemplateMap templateMap = null;
    ListIterator<Info> infoIterator = null;
    String spacing = null;
    DeclarationList inherited = null;

    DeclarationList() { }
    DeclarationList(DeclarationList inherited) {
        this.inherited = inherited;
    }

    String rescan(String lines) {
        if (spacing == null) {
            return lines;
        }
        String text = "";
        try (Scanner scanner = new Scanner(lines)) {
            while (scanner.hasNextLine()) {
                text += spacing + scanner.nextLine();
                int newline = spacing.lastIndexOf('\n');
                spacing = newline >= 0 ? spacing.substring(newline) : "\n";
            }
        }
        return text;
    }

    @Override public boolean add(Declaration decl) {
        return add(decl, null);
    }
    public boolean add(Declaration decl, String fullName) {
        boolean add = true;
        if (templateMap != null && templateMap.empty() && !decl.custom && (decl.type != null || decl.declarator != null)) {
            // method templates cannot be declared in Java, but make sure to make their
            // info available on request (when Info.javaNames is set) to be able to create instances
            if (infoIterator == null) {
                Type type = templateMap.type = decl.type;
                Declarator dcl = templateMap.declarator = decl.declarator;
                for (String name : new String[] {fullName, dcl != null ? dcl.cppName : type.cppName}) {
                    if (name == null) {
                        continue;
                    }
                    List<Info> infoList = infoMap.get(name);
                    boolean hasJavaName = false;
                    for (Info info : infoList) {
                        hasJavaName |= info.javaNames != null && info.javaNames.length > 0;
                    }
                    if (!decl.function || hasJavaName) {
                        infoIterator = infoList.size() > 0 ? infoList.listIterator() : null;
                        break;
                    }
                }
            }
            add = false;
        } else if (infoMap != null && !decl.incomplete && decl.type != null && decl.type.cppName != null) {
            // check if the user gave us different names for the same type with and without const
            if (infoIterator == null) {
                String constName = null, name = null;
                List<Info> infoList = infoMap.get(decl.type.cppName);
                List<Info> constInfoList = infoMap.get("const " + decl.type.cppName);
                if (infoList != null && constInfoList != null && !infoList.equals(constInfoList)) {
                    for (Info info : infoList) {
                        if (info.pointerTypes != null && info.pointerTypes.length > 0) {
                            name = info.pointerTypes[0].substring(info.pointerTypes[0].lastIndexOf(" ") + 1);
                            break;
                        }
                    }
                    for (Info info : constInfoList) {
                        if (info.pointerTypes != null && info.pointerTypes.length > 0) {
                            constName = info.pointerTypes[0].substring(info.pointerTypes[0].lastIndexOf(" ") + 1);
                            break;
                        }
                    }
                }
                if (constName != null && name != null && !constName.equals(name)) {
                    // if so, let's reparse this twice to create two Java peer classes
                    infoList.addAll(constInfoList);
                    infoIterator = infoList.size() > 0 ? infoList.listIterator() : null;
                    add = false;
                }
            }
        }
        if (infoMap != null && decl.declarator != null && decl.declarator.type != null) {
            // honor to skip over declarations when tagged as such
            Info info = infoMap.getFirst(decl.declarator.type.cppName);
            if (info != null && info.skip && info.valueTypes == null && info.pointerTypes == null) {
                add = false;
            } else if (decl.declarator.parameters != null) {
                for (Declarator d : decl.declarator.parameters.declarators) {
                    if (d != null && d.type != null) {
                        info = infoMap.getFirst(d.type.cppName);
                        if (info != null && info.skip && info.valueTypes == null && info.pointerTypes == null) {
                            add = false;
                            break;
                        }
                    }
                }
            }
        }
        if (decl.type != null && decl.type.javaName.equals("Pointer")) {
            add = false;
        }
        if (!add) {
            return false;
        }

        // place all definitions prior to the declaractions that need them
        List<Declaration> stack = new ArrayList<Declaration>();
        ListIterator<Declaration> it = stack.listIterator();
        it.add(decl); it.previous();
        while (it.hasNext()) {
            decl = it.next();
            Declarator dcl = decl.declarator;
            if (dcl != null && dcl.definition != null) {
                it.add(dcl.definition); it.previous();
            }
            if (dcl != null && dcl.parameters != null && dcl.parameters.declarators != null) {
                for (Declarator d : dcl.parameters.declarators) {
                    if (d != null && d.definition != null) {
                        it.add(d.definition); it.previous();
                    }
                }
            }
        }

        add = false;
        while (!stack.isEmpty()) {
            decl = stack.remove(stack.size() - 1);
            if (context != null) {
                decl.inaccessible = context.inaccessible
                        && !(context.virtualize && decl.declarator != null && decl.declarator.type != null && decl.declarator.type.virtual);
            }
            if (decl.text.length() == 0) {
                decl.inaccessible = true;
            }
            it = listIterator();
            boolean found = false;
            while (it.hasNext()) {
                Declaration d = it.next();
                if (d != null && d.signature != null && d.signature.length() > 0 && d.signature.equals(decl.signature)) {
                    if ((d.constMember && !decl.constMember) || (d.inaccessible && !decl.inaccessible) || (d.incomplete && !decl.incomplete)) {
                        // add preferably non-const accessible complete versions of functions and types
                        it.remove();
                    } else {
                        found = true;
                    }
                }
            }
            if (inherited != null) {
                it = inherited.listIterator();
                while (it.hasNext()) {
                    Declaration d = it.next();
                    if (d.signature.length() > 0 && d.signature.equals(decl.signature) && !d.incomplete && decl.incomplete) {
                        // suppress forward declaration if they are found complete in inherited declarations
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                decl.text = rescan(decl.text);
                super.add(decl);
                add = true;
            }
        }
        return add;
    }
}
