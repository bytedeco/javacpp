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

/**
 *
 * @author Samuel Audet
 */
class Type {
    Type() { }
    Type(String name) { cppName = javaName = name; }

    boolean anonymous = false, constPointer = false, constValue = false, constructor = false,
            destructor = false, operator = false, simple = false, staticMember = false,
            pointer = false, reference = false;
    String annotations = "", cppName = "", javaName = "";
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
}
