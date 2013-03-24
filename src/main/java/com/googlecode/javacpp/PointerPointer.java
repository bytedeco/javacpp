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

package com.googlecode.javacpp;

/**
 * The peer class to native pointers and arrays of {@code void*}.
 * All operations take into account the position and limit, when appropriate.
 * <p>
 * To support higher levels of indirection, we can create out of the Pointer
 * objects returned by {@link #get(int)} additional PointerPointer objects.
 *
 * @author Samuel Audet
 */
public class PointerPointer extends Pointer {
    /**
     * Allocates enough memory for the array and copies it.
     *
     * @param array the array to copy
     * @see #put(Pointer[])
     */
    public PointerPointer(Pointer  ... array) { this(array.length); put(array); }
    /**
     * Allocates enough memory for the array of arrays and copies it.
     *
     * @param array the array of arrays to copy
     * @see #put(byte[][])
     */
    public PointerPointer(byte[]   ... array) { this(array.length); put(array); }
    /**
     * Allocates enough memory for the array of arrays and copies it.
     *
     * @param array the array of arrays to copy
     * @see #put(short[][])
     */
    public PointerPointer(short[]  ... array) { this(array.length); put(array); }
    /**
     * Allocates enough memory for the array of arrays and copies it.
     *
     * @param array the array of arrays to copy
     * @see #put(int[][])
     */
    public PointerPointer(int[]    ... array) { this(array.length); put(array); }
    /**
     * Allocates enough memory for the array of arrays and copies it.
     *
     * @param array the array of arrays to copy
     * @see #put(long[][])
     */
    public PointerPointer(long[]   ... array) { this(array.length); put(array); }
    /**
     * Allocates enough memory for the array of arrays and copies it.
     *
     * @param array the array of arrays to copy
     * @see #put(float[][])
     */
    public PointerPointer(float[]  ... array) { this(array.length); put(array); }
    /**
     * Allocates enough memory for the array of arrays and copies it.
     *
     * @param array the array of arrays to copy
     * @see #put(double[][])
     */
    public PointerPointer(double[] ... array) { this(array.length); put(array); }
    /**
     * Allocates enough memory for the array of arrays and copies it.
     *
     * @param array the array of arrays to copy
     * @see #put(char[][])
     */
    public PointerPointer(char[]   ... array) { this(array.length); put(array); }
    /**
     * Allocates a native array of {@code void*} of the given size.
     *
     * @param size the number of {@code void*} elements to allocate
     */
    public PointerPointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    /** @see Pointer#Pointer(Pointer) */
    public PointerPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    /** This is just to keep references to Pointer objects we create. */
    private Pointer[] pointerArray;

    /** @see Pointer#position(int) */
    @Override public PointerPointer position(int position) {
        return super.position(position);
    }
    /** @see Pointer#limit(int) */
    @Override public PointerPointer limit(int limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(int) */
    @Override public PointerPointer capacity(int capacity) {
        return super.capacity(capacity);
    }

    /**
     * Writes the Pointer values into the native {@code void*} array.
     *
     * @param array the array of Pointer values to read from
     * @return this
     */
    public PointerPointer put(Pointer ... array) {
        for (int i = 0; i < array.length; i++) {
            put(i, array[i]);
        }
        return this;
    }
    /**
     * Creates one by one a new Pointer for each {@code byte[]},
     * and writes them into the native {@code void*} array.
     *
     * @param array the array of {@code byte[]} to read from
     * @return this
     */
    public PointerPointer put(byte[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new BytePointer(array[i]);
        }
        return put(pointerArray);
    }
    /**
     * Creates one by one a new Pointer for each {@code short[]},
     * and writes them into the native {@code void*} array.
     *
     * @param array the array of {@code short[]} to read from
     * @return this
     */
    public PointerPointer put(short[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new ShortPointer(array[i]);
        }
        return put(pointerArray);
    }
    /**
     * Creates one by one a new Pointer for each {@code int[]},
     * and writes them into the native {@code void*} array.
     *
     * @param array the array of {@code int[]} to read from
     * @return this
     */
    public PointerPointer put(int[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new IntPointer(array[i]);
        }
        return put(pointerArray);
    }
    /**
     * Creates one by one a new Pointer for each {@code long[]},
     * and writes them into the native {@code void*} array.
     *
     * @param array the array of {@code long[]} to read from
     * @return this
     */
    public PointerPointer put(long[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new LongPointer(array[i]);
        }
        return put(pointerArray);
    }
    /**
     * Creates one by one a new Pointer for each {@code float[]},
     * and writes them into the native {@code void*} array.
     *
     * @param array the array of {@code float[]} to read from
     * @return this
     */
    public PointerPointer put(float[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new FloatPointer(array[i]);
        }
        return put(pointerArray);
    }
    /**
     * Creates one by one a new Pointer for each {@code double[]},
     * and writes them into the native {@code void*} array.
     *
     * @param array the array of {@code double[]} to read from
     * @return this
     */
    public PointerPointer put(double[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new DoublePointer(array[i]);
        }
        return put(pointerArray);
    }
    /**
     * Creates one by one a new Pointer for each {@code char[]},
     * and writes them into the native {@code void*} array.
     *
     * @param array the array of {@code char[]} to read from
     * @return this
     */
    public PointerPointer put(char[] ... array) {
        pointerArray = new Pointer[array.length];
        for (int i = 0; i < array.length; i++) {
            pointerArray[i] = new CharPointer(array[i]);
        }
        return put(pointerArray);
    }

    /** @return {@code get(0)} */
    public Pointer get() { return get(0); }
    /** @return the i-th Pointer value of a native array */
    public native Pointer get(int i);
    /** @return {@code put(0, p)} */
    public PointerPointer put(Pointer p) { return put(0, p); }
    /**
     * Copies the Pointer value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param p the Pointer value to copy
     * @return this
     */
    public native PointerPointer put(int i, Pointer p);
}
