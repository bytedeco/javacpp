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
import java.nio.IntBuffer;

/**
 * An indexer for an {@link IntBuffer}.
 *
 * @author Samuel Audet
 */
public class IntBufferIndexer extends IntIndexer {
    /** The backing buffer. */
    protected IntBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public IntBufferIndexer(IntBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public int get(int i) {
        return buffer.get(i);
    }
    @Override public IntIndexer get(int i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public int get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public IntIndexer get(int i, int j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public int get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public int get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public IntIndexer get(int[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public IntIndexer put(int i, int n) {
        buffer.put(i, n);
        return this;
    }
    @Override public IntIndexer put(int i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, m[offset + n]);
        }
        return this;
    }
    @Override public IntIndexer put(int i, int j, int n) {
        buffer.put(i * strides[0] + j, n);
        return this;
    }
    @Override public IntIndexer put(int i, int j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, m[offset + n]);
        }
        return this;
    }
    @Override public IntIndexer put(int i, int j, int k, int n) {
        buffer.put(i * strides[0] + j * strides[1] + k, n);
        return this;
    }
    @Override public IntIndexer put(int[] indices, int n) {
        buffer.put(index(indices), n);
        return this;
    }
    @Override public IntIndexer put(int[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, m[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
