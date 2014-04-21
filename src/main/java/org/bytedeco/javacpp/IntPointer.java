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

package org.bytedeco.javacpp;

import java.nio.IntBuffer;

/**
 * The peer class to native pointers and arrays of {@code int}, also used for UTF-32.
 * All operations take into account the position and limit, when appropriate.
 *
 * @author Samuel Audet
 */
public class IntPointer extends Pointer {
    /**
     * Allocates enough memory for encoding the String in UTF-32 and copies it.
     *
     * @param s the String to copy
     * @see #putString(String)
     */
    public IntPointer(String s) {
        this(s.length()+1);
        putString(s);
    }
    /**
     * Allocates enough memory for the array and copies it.
     *
     * @param array the array to copy
     * @see #put(int[])
     */
    public IntPointer(int ... array) {
        this(array.length);
        put(array);
    }
    /**
     * For direct buffers, calls {@link Pointer#Pointer(Buffer)}, while for buffers
     * backed with an array, allocates enough memory for the array and copies it.
     *
     * @param buffer the Buffer to reference or copy
     * @see #put(int[])
     */
    public IntPointer(IntBuffer buffer) {
        super(buffer);
        if (buffer != null && buffer.hasArray()) {
            int[] array = buffer.array();
            allocateArray(array.length);
            put(array);
            position(buffer.position());
            limit(buffer.limit());
        }
    }
    /**
     * Allocates a native {@code int} array of the given size.
     *
     * @param size the number of {@code int} elements to allocate
     */
    public IntPointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    /** @see Pointer#Pointer() */
    public IntPointer() { }
    /** @see Pointer#Pointer(Pointer) */
    public IntPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    /** @see Pointer#position(int) */
    @Override public IntPointer position(int position) {
        return super.position(position);
    }
    /** @see Pointer#limit(int) */
    @Override public IntPointer limit(int limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(int) */
    @Override public IntPointer capacity(int capacity) {
        return super.capacity(capacity);
    }

    /** @return the code points from the null-terminated string */
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
    /** @return the String from the null-terminated string */
    public String getString() {
        int[] codePoints = getStringCodePoints();
        return new String(codePoints, 0, codePoints.length);
    }
    /**
     * Copies the String code points into native memory, including a terminating null int.
     *
     * @param s the String to copy
     * @return this
     * @see String#codePointAt(int)
     * @see #put(int[])
     */
    public IntPointer putString(String s) {
        int[] codePoints = new int[s.length()];
        for (int i = 0; i < codePoints.length; i++) {
            codePoints[i] = s.codePointAt(i);
        }
        return put(codePoints).put(codePoints.length, (int)0);
    }

    /** @return {@code get(0)} */
    public int get() { return get(0); }
    /** @return the i-th {@code int} value of a native array */
    public native int get(int i);
    /** @return {@code put(0, j)} */
    public IntPointer put(int j) { return put(0, j); }
    /**
     * Copies the {@code int} value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param j the {@code int} value to copy
     * @return this
     */
    public native IntPointer put(int i, int j);

    /** @return {@code get(array, 0, array.length)} */
    public IntPointer get(int[] array) { return get(array, 0, array.length); }
    /** @return {@code put(array, 0, array.length)} */
    public IntPointer put(int ... array) { return put(array, 0, array.length); }
    /**
     * Reads a portion of the native array into a Java array.
     *
     * @param array the array to write to
     * @param offset the offset into the array where to start writing
     * @param length the length of data to read and write
     * @return this
     */
    public native IntPointer get(int[] array, int offset, int length);
    /**
     * Writes a portion of a Java array into the native array.
     *
     * @param array the array to read from
     * @param offset the offset into the array where to start reading
     * @param length the length of data to read and write
     * @return this
     */
    public native IntPointer put(int[] array, int offset, int length);

    /** @return {@code asByteBuffer().asIntBuffer()} */
    @Override public final IntBuffer asBuffer() {
        return asByteBuffer().asIntBuffer();
    }
}
