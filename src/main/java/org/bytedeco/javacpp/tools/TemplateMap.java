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

import java.util.LinkedHashMap;

/**
 *
 * @author Samuel Audet
 */
class TemplateMap extends LinkedHashMap<String,String> {
    TemplateMap(TemplateMap parent) {
        this.parent = parent;
    }
    Type type = null;
    Declarator declarator = null;
    TemplateMap parent = null;

    String getName() {
        return type != null ? type.cppName : declarator != null ? declarator.cppName : null;
    }

    boolean full() {
        for (String s : values()) {
            if (s == null) {
                return false;
            }
        }
        return true;
    }

    String get(String key) {
        String value = super.get(key);
        if (value == null && parent != null) {
            return parent.get(key);
        } else {
            return value;
        }
    }
}
