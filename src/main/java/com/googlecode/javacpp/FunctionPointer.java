/*
 * Copyright (C) 2011,2012 Samuel Audet
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

package com.googlecode.javacpp;

/**
 *
 * @author Samuel Audet
 *
 * To use a function pointer, subclass and add a native method named
 * call() or apply(), along with its return type and parameters, as
 * well as the usual allocate() native method to support callback.
 */
public abstract class FunctionPointer extends Pointer {
    protected FunctionPointer() { }
    protected FunctionPointer(Pointer p) { super(p); }
}
