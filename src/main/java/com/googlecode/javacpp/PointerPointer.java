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

/**
 *
 * @author Samuel Audet
 */
public class PointerPointer extends Pointer {
    public PointerPointer(Pointer  ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(byte[]   ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(short[]  ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(int[]    ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(long[]   ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(float[]  ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(double[] ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(char[]   ... array) { this(array.length); put(array); position(0); }
    public PointerPointer(int size) { allocateArray(size); }
    public PointerPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    // this is just to keep references to Pointer objects we create
    private Pointer[] pointerArray;

    @Override public PointerPointer position(int position) {
        return (PointerPointer)super.position(position);
    }

    public PointerPointer put(Pointer ... array) {
        for (int i = 0; i < array.length; i++) {
            position(i).put(array[i]);
        }
        return this;
    }
    public PointerPointer put(byte[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new BytePointer(array[i]);
        }
        return put(pointerArray);
    }
    public PointerPointer put(short[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new ShortPointer(array[i]);
        }
        return put(pointerArray);
    }
    public PointerPointer put(int[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new IntPointer(array[i]);
        }
        return put(pointerArray);
    }
    public PointerPointer put(long[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new LongPointer(array[i]);
        }
        return put(pointerArray);
    }
    public PointerPointer put(float[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new FloatPointer(array[i]);
        }
        return put(pointerArray);
    }
    public PointerPointer put(double[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new DoublePointer(array[i]);
        }
        return put(pointerArray);
    }
    public PointerPointer put(char[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new CharPointer(array[i]);
        }
        return put(pointerArray);
    }

    public Pointer get() { return get(0); }
    public native Pointer get(int i);
    public PointerPointer put(Pointer p) { return put(0, p); }
    public native PointerPointer put(int i, Pointer p);
}
