/*
 * Copyright (C) 2011,2012,2013 Samuel Audet
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

package org.bytedeco.javacpp;

import org.bytedeco.javacpp.annotation.ByPtr;
import org.bytedeco.javacpp.annotation.ByRef;
import org.bytedeco.javacpp.annotation.ByVal;
import org.bytedeco.javacpp.tools.Generator;

/**
 * All peer classes to function pointers must derive from FunctionPointer.
 * Defining a subclass lets {@link Generator} create a native function type.
 * A C++ function object gets instantiated for each call to {@code allocate()}
 * as well. That function object can be accessed by annotating any method
 * parameter with {@link ByVal} or {@link ByRef}. By default, an actual
 * function pointer gets passed {@link ByPtr}.
 * <p>
 * To use FunctionPointer, subclass and add a native method named {@code call()}
 * or {@code apply()}, along with its return type and parameters, as well as the
 * usual {@code native void allocate()} method to support explicit allocation,
 * which is typically a requirement for callback functions.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
public abstract class FunctionPointer extends Pointer {
    protected FunctionPointer() { }
    protected FunctionPointer(Pointer p) { super(p); }
}
