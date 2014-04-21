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

import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Name;

/**
 * The peer class to native pointers and arrays of {@code bool}.
 * All operations take into account the position and limit, when appropriate.
 * <p>
 * We need this class because C++ does not define the size of the {@code bool} type.
 *
 * @author Samuel Audet
 */
@Name("bool")
public class BoolPointer extends Pointer {
    /**
     * Allocates a native {@code bool} array of the given size.
     *
     * @param size the number of {@code bool} elements to allocate
     */
    public BoolPointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    /** @see Pointer#Pointer() */
    public BoolPointer() { }
    /** @see Pointer#Pointer(Pointer) */
    public BoolPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    /** @see Pointer#position(int) */
    @Override public BoolPointer position(int position) {
        return super.position(position);
    }
    /** @see Pointer#limit(int) */
    @Override public BoolPointer limit(int limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(int) */
    @Override public BoolPointer capacity(int capacity) {
        return super.capacity(capacity);
    }

    /** @return {@code get(0)} */
    public boolean get() { return get(0); }
    /** @return the i-th {@code bool} value of a native array */
    @Cast("bool") public native boolean get(int i);
    /** @return {@code put(0, b)} */
    public BoolPointer put(boolean b) { return put(0, b); }
    /**
     * Copies the {@code bool} value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param b the {@code bool} value to copy
     * @return this
     */
    public native BoolPointer put(int i, boolean b);
}
