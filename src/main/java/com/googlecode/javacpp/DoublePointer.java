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

import java.nio.DoubleBuffer;

/**
 *
 * @author Samuel Audet
 */
public class DoublePointer extends Pointer {
    public DoublePointer(double ... array) {
        this(array.length);
        put(array);
    }
    public DoublePointer(DoubleBuffer buffer) {
        super(buffer);
        if (buffer != null && buffer.hasArray()) {
            double[] array = buffer.array();
            allocateArray(array.length);
            put(array);
            position(buffer.position());
        }
    }
    public DoublePointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    public DoublePointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public DoublePointer position(int position) {
        return super.position(position);
    }
    @Override public DoublePointer limit(int limit) {
        return super.limit(limit);
    }
    @Override public DoublePointer capacity(int capacity) {
        return super.capacity(capacity);
    }

    public double get() { return get(0); }
    public native double get(int i);
    public DoublePointer put(double d) { return put(0, d); }
    public native DoublePointer put(int i, double d);

    public DoublePointer get(double[] array) { return get(array, 0, array.length); }
    public DoublePointer put(double[] array) { return put(array, 0, array.length); }
    public native DoublePointer get(double[] array, int offset, int length);
    public native DoublePointer put(double[] array, int offset, int length);

    @Override public final DoubleBuffer asBuffer() {
        return asByteBuffer().asDoubleBuffer();
    }
}
