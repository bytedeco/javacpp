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
 * The peer class to native pointers and arrays of {@code size_t}.
 * All operations take into account the position and limit, when appropriate.
 * <p>
 * We need this class because the size differs between 32-bit and 64-bit platforms.
 *
 * @author Samuel Audet
 */
@Name("size_t")
public class SizeTPointer extends Pointer {
    /**
     * Allocates a native {@code size_t} array of the given size.
     *
     * @param size the number of {@code size_t} elements to allocate
     */
    public SizeTPointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    /** @see Pointer#Pointer() */
    public SizeTPointer() { }
    /** @see Pointer#Pointer(Pointer) */
    public SizeTPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    /** @see Pointer#position(int) */
    @Override public SizeTPointer position(int position) {
        return super.position(position);
    }
    /** @see Pointer#limit(int) */
    @Override public SizeTPointer limit(int limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(int) */
    @Override public SizeTPointer capacity(int capacity) {
        return super.capacity(capacity);
    }

    /** @return {@code get(0)} */
    public long get() { return get(0); }
    /** @return the i-th {@code size_t} value of a native array */
    @Cast("size_t") public native long get(int i);
    /** @return {@code put(0, s)} */
    public SizeTPointer put(long s) { return put(0, s); }
    /**
     * Copies the {@code size_t} value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param s the {@code size_t} value to copy
     * @return this
     */
    public native SizeTPointer put(int i, long s);
}
