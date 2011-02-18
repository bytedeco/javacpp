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

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 *
 * @author saudet
 */
public class IntPointer extends Pointer {
    public IntPointer(String s) {
        this(s.length()+1);
        putString(s);
    }
    public IntPointer(int[] array) {
        this(array.length);
        asBuffer(array.length).put(array);
    }
    public IntPointer(int size) { allocateArray(size); }
    public IntPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public IntPointer position(int position) {
        return (IntPointer)super.position(position);
    }

    public int[] getStringCodePoints() {
        // This may be kind of slow, and should be moved to a JNI function.
        int[] buffer = new int[16];
        int i = 0, j = position();
        while ((buffer[i] = position(j).get()) != 0) {
            i++; j++;
            if (i >= buffer.length) {
                buffer = Arrays.copyOf(buffer, 2*buffer.length);
            }
        }
        return Arrays.copyOf(buffer, i);
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
        asBuffer(codePoints.length+1).put(codePoints).put((int)0);
        return this;
    }

    public native int get();
    public native IntPointer put(int i);

    @Override public final IntBuffer asBuffer(int capacity) {
        return asByteBuffer(capacity).asIntBuffer();
    }
}
