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
import java.nio.ShortBuffer;

/**
 * An indexer for a {@link ShortBuffer}.
 *
 * @author Samuel Audet
 */
public class ShortBufferIndexer extends ShortIndexer {
    /** The backing buffer. */
    protected ShortBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public ShortBufferIndexer(ShortBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public short get(int i) {
        return buffer.get(i);
    }
    @Override public ShortIndexer get(int i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public short get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public ShortIndexer get(int i, int j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public short get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public short get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public ShortIndexer get(int[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public ShortIndexer put(int i, short s) {
        buffer.put(i, s);
        return this;
    }
    @Override public ShortIndexer put(int i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(int i, int j, short s) {
        buffer.put(i * strides[0] + j, s);
        return this;
    }
    @Override public ShortIndexer put(int i, int j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(int i, int j, int k, short s) {
        buffer.put(i * strides[0] + j * strides[1] + k, s);
        return this;
    }
    @Override public ShortIndexer put(int[] indices, short s) {
        buffer.put(index(indices), s);
        return this;
    }
    @Override public ShortIndexer put(int[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, s[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
