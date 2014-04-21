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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * The peer class to native pointers and arrays of {@code signed char}, including strings.
 * All operations take into account the position and limit, when appropriate.
 *
 * @author Samuel Audet
 */
public class BytePointer extends Pointer {
    /**
     * Allocates enough memory for the encoded string and actually encodes it
     * in the named charset before copying it.
     *
     * @param s the String to encode and copy
     * @param charsetName the charset in which the bytes are encoded
     * @throws UnsupportedEncodingException
     * @see #putString(String, String)
     */
    public BytePointer(String s, String charsetName)
            throws UnsupportedEncodingException {
        this(s.getBytes(charsetName).length+1);
        putString(s, charsetName);
    }
    /**
     * Allocates enough memory for the encoded string and actually encodes it
     * in the platform's default charset before copying it.
     *
     * @param s the String to encode and copy
     * @see #putString(String)
     */
    public BytePointer(String s) {
        this(s.getBytes().length+1);
        putString(s);
    }
    /**
     * Allocates enough memory for the array and copies it.
     *
     * @param array the array to copy
     * @see #put(byte[])
     */
    public BytePointer(byte ... array) {
        this(array.length);
        put(array);
    }
    /**
     * For direct buffers, calls {@link Pointer#Pointer(Buffer)}, while for buffers
     * backed with an array, allocates enough memory for the array and copies it.
     *
     * @param buffer the Buffer to reference or copy
     * @see #put(byte[])
     */
    public BytePointer(ByteBuffer buffer) {
        super(buffer);
        if (buffer != null && buffer.hasArray()) {
            byte[] array = buffer.array();
            allocateArray(array.length);
            put(array);
            position(buffer.position());
            limit(buffer.limit());
        }
    }
    /**
     * Allocates a native {@code signed char} array of the given size.
     *
     * @param size the number of {@code signed char} elements to allocate
     */
    public BytePointer(int size) {
        try {
            allocateArray(size);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        }
    }
    /** @see Pointer#Pointer() */
    public BytePointer() { }
    /** @see Pointer#Pointer(Pointer) */
    public BytePointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    /** @see Pointer#position(int) */
    @Override public BytePointer position(int position) {
        return super.position(position);
    }
    /** @see Pointer#limit(int) */
    @Override public BytePointer limit(int limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(int) */
    @Override public BytePointer capacity(int capacity) {
        return super.capacity(capacity);
    }

    /** @return the bytes from the null-terminated string */
    public byte[] getStringBytes() {
        // This may be kind of slow, and should be moved to a JNI function.
        byte[] buffer = new byte[16];
        int i = 0, j = position();
        while ((buffer[i] = position(j).get()) != 0) {
            i++; j++;
            if (i >= buffer.length) {
                byte[] newbuffer = new byte[2*buffer.length];
                System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
                buffer = newbuffer;
            }
        }
        byte[] newbuffer = new byte[i];
        System.arraycopy(buffer, 0, newbuffer, 0, i);
        return newbuffer;
    }
    /**
     * Decodes the native bytes assuming they are encoded in the named charset.
     * @param charsetName the charset in which the bytes are encoded
     * @return a String from the null-terminated string
     * @throws UnsupportedEncodingException
     */
    public String getString(String charsetName)
            throws UnsupportedEncodingException {
        return new String(getStringBytes(), charsetName);
    }
    /**
     * Decodes the native bytes assuming they are encoded in the platform's default charset.
     * @return a String from the null-terminated string
     */
    public String getString() {
        return new String(getStringBytes());
    }

    /**
     * Encodes the String into the named charset and copies it in native memory,
     * including a terminating null byte.
     *
     * @param s the String to encode and copy
     * @param charsetName the charset in which to encode the bytes
     * @return this
     * @throws UnsupportedEncodingException
     * @see String#getBytes(String)
     * @see #put(byte[])
     */
    public BytePointer putString(String s, String charsetName)
            throws UnsupportedEncodingException {
        byte[] bytes = s.getBytes(charsetName);
        //capacity(bytes.length+1);
        put(bytes).put(bytes.length, (byte)0);
        return this;
    }
    /**
     * Encodes the String into the platform's default charset and copies it in
     * native memory, including a terminating null byte.
     *
     * @param s the String to encode and copy
     * @return this
     * @see String#getBytes()
     * @see #put(byte[])
     */
    public BytePointer putString(String s) {
        byte[] bytes = s.getBytes();
        return put(bytes).put(bytes.length, (byte)0);
    }

    /** @return {@code get(0)} */
    public byte get() { return get(0); }
    /** @return the i-th {@code byte} value of a native array */
    public native byte get(int i);
    /** @return {@code put(0, b)} */
    public BytePointer put(byte b) { return put(0, b); }
    /**
     * Copies the {@code byte} value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param b the {@code byte} value to copy
     * @return this
     */
    public native BytePointer put(int i, byte b);

    /** @return {@code get(array, 0, array.length)} */
    public BytePointer get(byte[] array) { return get(array, 0, array.length); }
    /** @return {@code put(array, 0, array.length)} */
    public BytePointer put(byte ... array) { return put(array, 0, array.length); }
    /**
     * Reads a portion of the native array into a Java array.
     *
     * @param array the array to write to
     * @param offset the offset into the array where to start writing
     * @param length the length of data to read and write
     * @return this
     */
    public native BytePointer get(byte[] array, int offset, int length);
    /**
     * Writes a portion of a Java array into the native array.
     *
     * @param array the array to read from
     * @param offset the offset into the array where to start reading
     * @param length the length of data to read and write
     * @return this
     */
    public native BytePointer put(byte[] array, int offset, int length);

    /** @return {@code asByteBuffer()} */
    @Override public final ByteBuffer asBuffer() {
        return asByteBuffer();
    }
}
