/*
 * Copyright (C) 2011-2017 Samuel Audet
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

package org.bytedeco.javacpp;

import org.bytedeco.javacpp.annotation.ByPtr;
import org.bytedeco.javacpp.annotation.ByRef;
import org.bytedeco.javacpp.annotation.ByVal;
import org.bytedeco.javacpp.annotation.ValueSetter;
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
 * which is typically a requirement for callback functions. We can implement a
 * callback in Java by further subclassing and overriding {@code call/apply()}.
 * <p>
 * If you have an address to a native function, it is also possible to call it
 * by defining a {@link ValueSetter} method with a single {@link Pointer} parameter,
 * along with {@code native} declarations for {@code allocate()} and {@code call()}.
 * After allocating the object and setting the value, we can be call it from Java.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
public abstract class FunctionPointer extends Pointer {
    protected FunctionPointer() { }
    protected FunctionPointer(Pointer p) { super(p); }
}
