/*
 * Copyright (C) 2014 Samuel Audet
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

package org.bytedeco.javacpp.indexer;

import java.nio.Buffer;
import java.nio.CharBuffer;

/**
 * An indexer for a {@link CharBuffer}.
 *
 * @author Samuel Audet
 */
public class CharBufferIndexer extends CharIndexer {
    /** The backing buffer. */
    protected CharBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public CharBufferIndexer(CharBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public char get(int i) {
        return buffer.get(i);
    }
    @Override public CharIndexer get(int i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public char get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public CharIndexer get(int i, int j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public char get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public char get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public CharIndexer get(int[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public CharIndexer put(int i, char c) {
        buffer.put(i, c);
        return this;
    }
    @Override public CharIndexer put(int i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(int i, int j, char c) {
        buffer.put(i * strides[0] + j, c);
        return this;
    }
    @Override public CharIndexer put(int i, int j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(int i, int j, int k, char c) {
        buffer.put(i * strides[0] + j * strides[1] + k, c);
        return this;
    }
    @Override public CharIndexer put(int[] indices, char c) {
        buffer.put(index(indices), c);
        return this;
    }
    @Override public CharIndexer put(int[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, c[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
