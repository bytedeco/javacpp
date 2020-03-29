/*
 * Copyright (C) 2011-2020 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bytedeco.javacpp;

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.ValueGetter;
import org.bytedeco.javacpp.annotation.ValueSetter;
import org.bytedeco.javacpp.tools.Logger;

/**
 * The peer class to native pointers and arrays of {@code signed char}, including strings.
 * All operations take into account the position and limit, when appropriate.
 *
 * @author Samuel Audet
 */
@org.bytedeco.javacpp.annotation.Properties(inherit = org.bytedeco.javacpp.presets.javacpp.class)
public class BytePointer extends Pointer {
    private static final Logger logger = Logger.create(BytePointer.class);

    static {
        try {
            Loader.load();
        } catch (Throwable t) {
            logger.warn("Could not load BytePointer: " + t);
        }
    }

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
        this(s.getBytes(charsetName).length + 1);
        putString(s, charsetName);
    }
    /**
     * Allocates enough memory for the encoded string and actually encodes it
     * in the given charset before copying it.
     *
     * @param s the String to encode and copy
     * @param charset the charset in which the bytes are encoded
     * @see #putString(String, Charset)
     */
    public BytePointer(String s, Charset charset) {
        this(s.getBytes(charset).length + 1);
        putString(s, charset);
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
        if (buffer != null && !buffer.isDirect() && buffer.hasArray()) {
            byte[] array = buffer.array();
            allocateArray(array.length - buffer.arrayOffset());
            put(array, buffer.arrayOffset(), array.length - buffer.arrayOffset());
            position(buffer.position());
            limit(buffer.limit());
        }
    }
    /**
     * Allocates a native {@code signed char} array of the given size.
     *
     * @param size the number of {@code signed char} elements to allocate
     */
    public BytePointer(long size) {
        try {
            allocateArray(size);
            if (size > 0 && address == 0) {
                throw new OutOfMemoryError("Native allocator returned address == 0");
            }
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        } catch (OutOfMemoryError e) {
            OutOfMemoryError e2 = new OutOfMemoryError("Cannot allocate new BytePointer(" + size + "): "
                    + "totalBytes = " + formatBytes(totalBytes()) + ", physicalBytes = " + formatBytes(physicalBytes()));
            e2.initCause(e);
            throw e2;
        }
    }
    /** @see Pointer#Pointer() */
    public BytePointer() { }
    /** @see Pointer#Pointer(Pointer) */
    public BytePointer(Pointer p) { super(p); }
    private native void allocateArray(long size);

    /** @see Pointer#position(long) */
    @Override public BytePointer position(long position) {
        return super.position(position);
    }
    /** @see Pointer#limit(long) */
    @Override public BytePointer limit(long limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(long) */
    @Override public BytePointer capacity(long capacity) {
        return super.capacity(capacity);
    }
    @Override public int sizeof() {
        return 1;
    }

    /** Returns the bytes, assuming a null-terminated string if {@code limit <= position}. */
    public byte[] getStringBytes() {
        long size = limit - position;
        if (size <= 0) {
            size = strlen(this);
        }
        byte[] array = new byte[(int)Math.min(size, Integer.MAX_VALUE)];
        get(array);
        return array;
    }
    /**
     * Decodes the native bytes assuming they are encoded in the named charset.
     * Assumes a null-terminated string if {@code limit <= position}.
     *
     * @param charsetName the charset in which the bytes are encoded
     * @return a String from the null-terminated string
     * @throws UnsupportedEncodingException
     */
    public String getString(String charsetName)
            throws UnsupportedEncodingException {
        return new String(getStringBytes(), charsetName);
    }
    /**
     * Decodes the native bytes assuming they are encoded in the given charset.
     * Assumes a null-terminated string if {@code limit <= position}.
     *
     * @param charset the charset in which the bytes are encoded
     * @return a String from the null-terminated string
     */
    public String getString(Charset charset) {
        return new String(getStringBytes(), charset);
    }
    /**
     * Decodes the native bytes assuming they are encoded in the platform's default charset.
     * Assumes a null-terminated string if {@code limit <= position}.
     *
     * @return a String from the null-terminated string
     */
    public String getString() {
        return new String(getStringBytes());
    }

    /**
     * Encodes the String into the named charset and copies it in native memory,
     * including a terminating null byte.
     * Sets the limit to just before the terminating null byte.
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
        return put(bytes).put(bytes.length, (byte)0).limit(bytes.length);
    }
    /**
     * Encodes the String into the given charset and copies it in native memory,
     * including a terminating null byte.
     * Sets the limit to just before the terminating null byte.
     *
     * @param s the String to encode and copy
     * @param charset the charset in which to encode the bytes
     * @return this
     * @see String#getBytes(Charset)
     * @see #put(byte[])
     */
    public BytePointer putString(String s, Charset charset) {
        byte[] bytes = s.getBytes(charset);
        return put(bytes).put(bytes.length, (byte)0).limit(bytes.length);
    }
    /**
     * Encodes the String into the platform's default charset and copies it in
     * native memory, including a terminating null byte.
     * Sets the limit to just before the terminating null byte.
     *
     * @param s the String to encode and copy
     * @return this
     * @see String#getBytes()
     * @see #put(byte[])
     */
    public BytePointer putString(String s) {
        byte[] bytes = s.getBytes();
        return put(bytes).put(bytes.length, (byte)0).limit(bytes.length);
    }

    /** @return {@code get(0)} */
    public byte get() { return get(0); }
    /** @return the i-th {@code byte} value of a native array */
    public native byte get(long i);
    /** @return {@code put(0, b)} */
    public BytePointer put(byte b) { return put(0, b); }
    /**
     * Copies the {@code byte} value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param b the {@code byte} value to copy
     * @return this
     */
    public native BytePointer put(long i, byte b);

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

    /** Returns {@code getShort(0)}. */
    public short getShort() { return getShort(0); }
    /** Returns the {@code short} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("short") public native short getShort(long i);
    /** Returns {@code putShort(0, s)}. */
    public BytePointer putShort(short s) { return putShort(0, s); }
    /** Sets the {@code short} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("short") public native BytePointer putShort(long i, short s);

    /** Returns {@code getInt(0)}. */
    public int getInt() { return getInt(0); }
    /** Returns the {@code int} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("int") public native int getInt(long i);
    /** Returns {@code putInt(0, s)}. */
    public BytePointer putInt(int j) { return putInt(0, j); }
    /** Sets the {@code int} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("int") public native BytePointer putInt(long i, int j);

    /** Returns {@code getLong(0)}. */
    public long getLong() { return getLong(0); }
    /** Returns the {@code long} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("long long") public native long getLong(long i);
    /** Returns {@code putLong(0, s)}. */
    public BytePointer putLong(long j) { return putLong(0, j); }
    /** Sets the {@code long} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("long long") public native BytePointer putLong(long i, long j);

    /** Returns {@code getFloat(0)}. */
    public float getFloat() { return getFloat(0); }
    /** Returns the {@code float} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("float") public native float getFloat(long i);
    /** Returns {@code putFloat(0, s)}. */
    public BytePointer putFloat(float f) { return putFloat(0, f); }
    /** Sets the {@code float} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("float") public native BytePointer putFloat(long i, float f);

    /** Returns {@code getDouble(0)}. */
    public double getDouble() { return getDouble(0); }
    /** Returns the {@code double} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("double") public native double getDouble(long i);
    /** Returns {@code putDouble(0, s)}. */
    public BytePointer putDouble(double d) { return putDouble(0, d); }
    /** Sets the {@code double} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("double") public native BytePointer putDouble(long i, double d);

    /** Returns {@code getBool(0)}. */
    public boolean getBool() { return getBool(0); }
    /** Returns the {@code bool} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("bool") public native boolean getBool(long i);
    /** Returns {@code putBool(0, s)}. */
    public BytePointer putBool(boolean b) { return putBool(0, b); }
    /** Sets the {@code bool} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("bool") public native BytePointer putBool(long i, boolean b);

    /** Returns {@code getChar(0)}. */
    public char getChar() { return getChar(0); }
    /** Returns the {@code char} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("short") public native char getChar(long i);
    /** Returns {@code putChar(0, s)}. */
    public BytePointer putChar(char c) { return putChar(0, c); }
    /** Sets the {@code char} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("short") public native BytePointer putChar(long i, char c);

    /** Returns {@code getPointer(0)}. */
    public Pointer getPointer() { return getPointer(0); }
    /** Returns the {@code Pointer} value at the i-th {@code byte} in the native array. */
    @ValueGetter @Cast("void*") public native Pointer getPointer(long i);
    /** Returns {@code putPointer(0, s)}. */
    public BytePointer putPointer(Pointer p) { return putPointer(0, p); }
    /** Sets the {@code Pointer} value at the i-th {@code byte} in the native array. */
    @ValueSetter @Cast("void*") public native BytePointer putPointer(long i, Pointer p);

    public static native @Cast("char*") BytePointer strcat(@Cast("char*") BytePointer dst, @Cast("char*") BytePointer src);
    public static native @Cast("char*") BytePointer strchr(@Cast("char*") BytePointer str, int ch);
    public static native int strcmp(@Cast("char*") BytePointer str1, @Cast("char*") BytePointer str2);
    public static native int strcoll(@Cast("char*") BytePointer str1, @Cast("char*") BytePointer str2);
    public static native @Cast("char*") BytePointer strcpy(@Cast("char*") BytePointer dst, @Cast("char*") BytePointer src);
    public static native @Cast("size_t") long strcspn(@Cast("char*") BytePointer str1, @Cast("char*") BytePointer str2);
    public static native @Cast("char*") BytePointer strerror(int errnum);
    public static native @Cast("size_t") long strlen(@Cast("char*") BytePointer str);
    public static native @Cast("char*") BytePointer strncat(@Cast("char*") BytePointer dst, @Cast("char*") BytePointer src, @Cast("size_t") long n);
    public static native int strncmp(@Cast("char*") BytePointer str1, @Cast("char*") BytePointer str2, @Cast("size_t") long n);
    public static native @Cast("char*") BytePointer strncpy(@Cast("char*") BytePointer dst, @Cast("char*") BytePointer src, @Cast("size_t") long n);
    public static native @Cast("char*") BytePointer strpbrk(@Cast("char*") BytePointer str1, @Cast("char*") BytePointer str2);
    public static native @Cast("char*") BytePointer strrchr(@Cast("char*") BytePointer str, int ch);
    public static native @Cast("size_t") long strspn(@Cast("char*") BytePointer str1, @Cast("char*") BytePointer str2);
    public static native @Cast("char*") BytePointer strstr(@Cast("char*") BytePointer str1, @Cast("char*") BytePointer str2);
    public static native @Cast("char*") BytePointer strtok(@Cast("char*") BytePointer str, @Cast("char*") BytePointer delim);
    public static native @Cast("size_t") long strxfrm (@Cast("char*") BytePointer dst, @Cast("char*") BytePointer src, @Cast("size_t") long n);
}
