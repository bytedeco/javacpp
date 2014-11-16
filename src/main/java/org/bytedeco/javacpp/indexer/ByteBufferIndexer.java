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
import java.nio.ByteBuffer;

/**
 * An indexer for a {@link ByteBuffer}.
 *
 * @author Samuel Audet
 */
public class ByteBufferIndexer extends ByteIndexer {
    /** The backing buffer. */
    protected ByteBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public ByteBufferIndexer(ByteBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public byte get(int i) {
        return buffer.get(i);
    }
    @Override public ByteIndexer get(int i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public byte get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public ByteIndexer get(int i, int j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public byte get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public byte get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public ByteIndexer get(int[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public ByteIndexer put(int i, byte b) {
        buffer.put(i, b);
        return this;
    }
    @Override public ByteIndexer put(int i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, b[offset + n]);
        }
        return this;
    }
    @Override public ByteIndexer put(int i, int j, byte b) {
        buffer.put(i * strides[0] + j, b);
        return this;
    }
    @Override public ByteIndexer put(int i, int j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, b[offset + n]);
        }
        return this;
    }
    @Override public ByteIndexer put(int i, int j, int k, byte b) {
        buffer.put(i * strides[0] + j * strides[1] + k, b);
        return this;
    }
    @Override public ByteIndexer put(int[] indices, byte b) {
        buffer.put(index(indices), b);
        return this;
    }
    @Override public ByteIndexer put(int[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, b[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
