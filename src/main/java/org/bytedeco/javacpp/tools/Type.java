/*
 * Copyright (C) 2014-2015 Samuel Audet
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

/**
 *
 * @author Samuel Audet
 */
class Type {
    Type() { }
    Type(String name) { cppName = javaName = name; }

    int indirections = 0;
    boolean anonymous = false, constExpr = false, constPointer = false, constValue = false, constructor = false,
            destructor = false, operator = false, simple = false, staticMember = false, using = false,
            reference = false, rvalue = false, value = false, friend = false, typedef = false, virtual = false;
    String annotations = "", cppName = "", javaName = "", javaNames[] = null;
    Type[] arguments = null;
    Attribute[] attributes = null;

    @Override public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj.getClass() == getClass()) {
            Type other = (Type)obj;
            return cppName.equals(other.cppName) && javaName.equals(other.javaName);
        } else {
            return false;
        }
    }

    @Override public int hashCode() {
        return cppName.hashCode() ^ javaName.hashCode();
    }

    String signature() {
        String sig = "";
        for (char c : javaName.substring(javaName.lastIndexOf(' ') + 1).toCharArray()) {
            sig += Character.isJavaIdentifierPart(c) ? c : '_';
        }
        return sig;
    }
}
