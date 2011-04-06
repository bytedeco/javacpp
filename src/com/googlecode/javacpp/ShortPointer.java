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

import java.nio.ShortBuffer;

/**
 *
 * @author Samuel Audet
 */
public class ShortPointer extends Pointer {
    public ShortPointer(short ... array) {
        this(array.length);
        asBuffer(array.length).put(array);
    }
    public ShortPointer(int size) { allocateArray(size); }
    public ShortPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public ShortPointer position(int position) {
        return (ShortPointer)super.position(position);
    }

    public native short get();
    public native ShortPointer put(short s);

    @Override public final ShortBuffer asBuffer(int capacity) {
        return asByteBuffer(capacity).asShortBuffer();
    }
}
