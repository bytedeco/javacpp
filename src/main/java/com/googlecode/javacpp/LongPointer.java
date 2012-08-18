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

import java.nio.LongBuffer;

/**
 *
 * @author Samuel Audet
 */
public class LongPointer extends Pointer {
    public LongPointer(long ... array) {
        this(array.length);
        put(array);
    }
    public LongPointer(LongBuffer buffer) {
        super(buffer);
        if (buffer != null && buffer.hasArray()) {
            long[] array = buffer.array();
            allocateArray(array.length);
            put(array);
            position(buffer.position());
        }
    }
    public LongPointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    public LongPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public LongPointer position(int position) {
        return (LongPointer)super.position(position);
    }
    @Override public LongPointer limit(int limit) {
        return (LongPointer)super.limit(limit);
    }
    @Override public LongPointer capacity(int capacity) {
        return (LongPointer)super.capacity(capacity);
    }

    public long get() { return get(0); }
    public native long get(int i);
    public LongPointer put(long l) { return put(0, l); }
    public native LongPointer put(int i, long l);

    public LongPointer get(long[] array) { return get(array, 0, array.length); }
    public LongPointer put(long[] array) { return put(array, 0, array.length); }
    public native LongPointer get(long[] array, int offset, int length);
    public native LongPointer put(long[] array, int offset, int length);

    @Override public final LongBuffer asBuffer() {
        return asByteBuffer().asLongBuffer();
    }
}
