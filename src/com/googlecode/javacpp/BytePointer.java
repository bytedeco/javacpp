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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 *
 * @author Samuel Audet
 */
public class BytePointer extends Pointer {
    public BytePointer(String s, String charsetName)
            throws UnsupportedEncodingException {
        this(s.getBytes(charsetName).length+1);
        putString(s, charsetName);
    }
    public BytePointer(String s) {
        this(s.getBytes().length+1);
        putString(s);
    }
    public BytePointer(byte ... array) {
        this(array.length);
        asBuffer().put(array);
    }
    public BytePointer(ByteBuffer buffer) {
        super(buffer);
        if (buffer.hasArray()) {
            byte[] array = buffer.array();
            allocateArray(array.length);
            asBuffer().put(array);
            position(buffer.position());
        }
    }
    public BytePointer(int size) { allocateArray(size); }
    public BytePointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public BytePointer position(int position) {
        return (BytePointer)super.position(position);
    }
    @Override public BytePointer capacity(int capacity) {
        return (BytePointer)super.capacity(capacity);
    }

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
    public String getString(String charsetName)
            throws UnsupportedEncodingException {
        return new String(getStringBytes(), charsetName);
    }
    public String getString() {
        return new String(getStringBytes());
    }

    public BytePointer putString(String s, String charsetName)
            throws UnsupportedEncodingException {
        byte[] bytes = s.getBytes(charsetName);
        //capacity(bytes.length+1);
        asBuffer().put(bytes).put((byte)0);
        return this;
    }
    public BytePointer putString(String s) {
        byte[] bytes = s.getBytes();
        //capacity(bytes.length+1);
        asBuffer().put(bytes).put((byte)0);
        return this;
    }

    public byte get() { return get(0); }
    public native byte get(int i);
    public BytePointer put(byte b) { return put(0, b); }
    public native BytePointer put(int i, byte b);

    public BytePointer get(byte[] array) { return get(array, 0, array.length); }
    public BytePointer put(byte[] array) { return put(array, 0, array.length); }
    public native BytePointer get(byte[] array, int offset, int length);
    public native BytePointer put(byte[] array, int offset, int length);

    @Override public final ByteBuffer asBuffer() {
        return (ByteBuffer)super.asBuffer();
    }
}
