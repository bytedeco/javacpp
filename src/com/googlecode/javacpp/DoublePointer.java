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

import java.nio.DoubleBuffer;

/**
 *
 * @author Samuel Audet
 */
public class DoublePointer extends Pointer {
    public DoublePointer(double ... array) {
        this(array.length);
        asBuffer().put(array);
    }
    public DoublePointer(DoubleBuffer buffer) {
        super(buffer);
        if (buffer.hasArray()) {
            double[] array = buffer.array();
            allocateArray(array.length);
            asBuffer().put(array);
            position(buffer.position());
        }
    }
    public DoublePointer(int size) { allocateArray(size); }
    public DoublePointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public DoublePointer position(int position) {
        return (DoublePointer)super.position(position);
    }
    @Override public DoublePointer capacity(int capacity) {
        return (DoublePointer)super.capacity(capacity);
    }

    public double get() { return get(0); }
    public native double get(int i);
    public DoublePointer put(double d) { return put(0, d); }
    public native DoublePointer put(int i, double d);

    @Override public final DoubleBuffer asBuffer() {
        return asByteBuffer().asDoubleBuffer();
    }
}
