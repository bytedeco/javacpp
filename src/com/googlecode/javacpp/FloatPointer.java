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

import java.nio.FloatBuffer;

/**
 *
 * @author Samuel Audet
 */
public class FloatPointer extends Pointer {
    public FloatPointer(float ... array) {
        this(array.length);
        asBuffer().put(array);
    }
    public FloatPointer(int size) { allocateArray(size); }
    public FloatPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public FloatPointer position(int position) {
        return (FloatPointer)super.position(position);
    }
    @Override public FloatPointer capacity(int capacity) {
        return (FloatPointer)super.capacity(capacity);
    }

    public float get() { return get(0); }
    public native float get(int i);
    public FloatPointer put(float f) { return put(0, f); }
    public native FloatPointer put(int i, float f);

    @Override public final FloatBuffer asBuffer() {
        return asByteBuffer().asFloatBuffer();
    }
}
