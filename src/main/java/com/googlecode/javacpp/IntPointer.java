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

import java.nio.IntBuffer;

/**
 *
 * @author Samuel Audet
 */
public class IntPointer extends Pointer {
    public IntPointer(String s) {
        this(s.length()+1);
        putString(s);
    }
    public IntPointer(int ... array) {
        this(array.length);
        put(array);
    }
    public IntPointer(IntBuffer buffer) {
        super(buffer);
        if (buffer != null && buffer.hasArray()) {
            int[] array = buffer.array();
            allocateArray(array.length);
            put(array);
            position(buffer.position());
        }
    }
    public IntPointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    public IntPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public IntPointer position(int position) {
        return (IntPointer)super.position(position);
    }
    @Override public IntPointer capacity(int capacity) {
        return (IntPointer)super.capacity(capacity);
    }

    public int[] getStringCodePoints() {
        // This may be kind of slow, and should be moved to a JNI function.
        int[] buffer = new int[16];
        int i = 0, j = position();
        while ((buffer[i] = position(j).get()) != 0) {
            i++; j++;
            if (i >= buffer.length) {
                int[] newbuffer = new int[2*buffer.length];
                System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
                buffer = newbuffer;
            }
        }
        int[] newbuffer = new int[i];
        System.arraycopy(buffer, 0, newbuffer, 0, i);
        return newbuffer;
    }
    public String getString() {
        int[] codePoints = getStringCodePoints();
        return new String(codePoints, 0, codePoints.length);
    }
    public IntPointer putString(String s) {
        int[] codePoints = new int[s.length()];
        for (int i = 0; i < codePoints.length; i++) {
            codePoints[i] = s.codePointAt(i);
        }
        return put(codePoints).put(codePoints.length, (int)0);
    }

    public int get() { return get(0); }
    public native int get(int i);
    public IntPointer put(int j) { return put(0, j); }
    public native IntPointer put(int i, int j);

    public IntPointer get(int[] array) { return get(array, 0, array.length); }
    public IntPointer put(int[] array) { return put(array, 0, array.length); }
    public native IntPointer get(int[] array, int offset, int length);
    public native IntPointer put(int[] array, int offset, int length);

    @Override public final IntBuffer asBuffer() {
        return asByteBuffer().asIntBuffer();
    }
}
