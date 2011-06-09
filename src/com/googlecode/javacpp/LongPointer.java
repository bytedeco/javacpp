/*
 * Copyright (C) 2011 Samuel Audet
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
        asBuffer().put(array);
    }
    public LongPointer(int size) { allocateArray(size); }
    public LongPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public LongPointer position(int position) {
        return (LongPointer)super.position(position);
    }
    @Override public LongPointer capacity(int capacity) {
        return (LongPointer)super.capacity(capacity);
    }

    public long get() { return get(0); }
    public native long get(int i);
    public LongPointer put(long l) { return put(0, l); }
    public native LongPointer put(int i, long l);

    @Override public final LongBuffer asBuffer() {
        return asByteBuffer().asLongBuffer();
    }
}
