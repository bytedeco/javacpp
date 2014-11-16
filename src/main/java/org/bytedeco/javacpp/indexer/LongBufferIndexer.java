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
import java.nio.LongBuffer;

/**
 * An indexer for a {@link LongBuffer}.
 *
 * @author Samuel Audet
 */
public class LongBufferIndexer extends LongIndexer {
    /** The backing buffer. */
    protected LongBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public LongBufferIndexer(LongBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public long get(int i) {
        return buffer.get(i);
    }
    @Override public LongIndexer get(int i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public long get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public LongIndexer get(int i, int j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public long get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public long get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public LongIndexer get(int[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public LongIndexer put(int i, long l) {
        buffer.put(i, l);
        return this;
    }
    @Override public LongIndexer put(int i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, l[offset + n]);
        }
        return this;
    }
    @Override public LongIndexer put(int i, int j, long l) {
        buffer.put(i * strides[0] + j, l);
        return this;
    }
    @Override public LongIndexer put(int i, int j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, l[offset + n]);
        }
        return this;
    }
    @Override public LongIndexer put(int i, int j, int k, long l) {
        buffer.put(i * strides[0] + j * strides[1] + k, l);
        return this;
    }
    @Override public LongIndexer put(int[] indices, long l) {
        buffer.put(index(indices), l);
        return this;
    }
    @Override public LongIndexer put(int[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, l[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
