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

import java.nio.CharBuffer;

/**
 *
 * @author Samuel Audet
 */
public class CharPointer extends Pointer {
    public CharPointer(String s) {
        this(s.toCharArray().length+1);
        putString(s);
    }
    public CharPointer(char ... array) {
        this(array.length);
        asBuffer().put(array);
    }
    public CharPointer(int size) { allocateArray(size); }
    public CharPointer(Pointer p) { super(p); }
    private native void allocateArray(int size);

    @Override public CharPointer position(int position) {
        return (CharPointer)super.position(position);
    }
    @Override public CharPointer capacity(int capacity) {
        return (CharPointer)super.capacity(capacity);
    }

    public char[] getStringChars() {
        // This may be kind of slow, and should be moved to a JNI function.
        char[] buffer = new char[16];
        int i = 0, j = position();
        while ((buffer[i] = position(j).get()) != 0) {
            i++; j++;
            if (i >= buffer.length) {
                char[] newbuffer = new char[2*buffer.length];
                System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
                buffer = newbuffer;
            }
        }
        char[] newbuffer = new char[i];
        System.arraycopy(buffer, 0, newbuffer, 0, i);
        return newbuffer;
    }
    public String getString() {
        return new String(getStringChars());
    }
    public CharPointer putString(String s) {
        char[] chars = s.toCharArray();
        //capacity(chars.length+1);
        asBuffer().put(chars).put((char)0);
        return this;
    }

    public char get() { return get(0); }
    public native char get(int i);
    public CharPointer put(char c) { return put(0, c); }
    public native CharPointer put(int i, char c);

    @Override public final CharBuffer asBuffer() {
        return asByteBuffer().asCharBuffer();
    }
}
