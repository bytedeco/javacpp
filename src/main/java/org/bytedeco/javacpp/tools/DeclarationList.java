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

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;

/**
 *
 * @author Samuel Audet
 */
class DeclarationList extends LinkedList<Declaration> {
    InfoMap infoMap = null;
    Context context = null;
    TemplateMap templateMap = null;
    ListIterator<Info> infoIterator = null;
    String spacing = null;

    String rescan(String lines) {
        if (spacing == null) {
            return lines;
        }
        String text = "";
        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            text += spacing + scanner.nextLine();
            int newline = spacing.lastIndexOf('\n');
            spacing = newline >= 0 ? spacing.substring(newline) : "\n";
        }
        return text;
    }

    @Override public boolean add(Declaration decl) {
        boolean add = true;
        if (templateMap != null && !templateMap.full() && (decl.type != null || decl.declarator != null)) {
            if (infoIterator == null) {
                Type type = templateMap.type = decl.type;
                Declarator dcl = templateMap.declarator = decl.declarator;
                LinkedList<Info> infoList = infoMap.get(dcl != null ? dcl.cppName : type.cppName);
                infoIterator = infoList.size() > 0 ? infoList.listIterator() : null;
            }
            add = false;
        } else if (decl.declarator != null && decl.declarator.type != null) {
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
        if (!add) {
            return false;
        }

        LinkedList<Declaration> stack = new LinkedList<Declaration>();
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

        while (!stack.isEmpty()) {
            decl = stack.removeLast();
            if (context != null) {
                decl.inaccessible = context.inaccessible;
            }
            if (decl.text.length() == 0) {
                decl.inaccessible = true;
            }
            it = listIterator();
            boolean found = false;
            while (it.hasNext()) {
                Declaration d = it.next();
                if (d.signature.length() > 0 && d.signature.equals(decl.signature)) {
                    if ((d.constMember && !decl.constMember) || (d.inaccessible && !decl.inaccessible) || (d.incomplete && !decl.incomplete)) {
                        // add preferably non-const accessible complete versions of functions and types
                        it.remove();
                    } else {
                        found = true;
                    }
                }
            }
            if (!found) {
                decl.text = rescan(decl.text);
                super.add(decl);
            }
        }
        return true;
    }
}
