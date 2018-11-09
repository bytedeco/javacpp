/*
 * Copyright (C) 2018 Samuel Audet
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

package org.bytedeco.javacpp.indexer;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * An indexer for a {@link ByteBuffer} as {@code boolean} values.
 *
 * @author Samuel Audet
 */
public class BooleanBufferIndexer extends BooleanIndexer {
    /** The backing buffer. */
    protected ByteBuffer buffer;

    /** Calls {@code BooleanBufferIndexer(buffer, { buffer.limit() }, { 1 })}. */
    public BooleanBufferIndexer(ByteBuffer buffer) {
        this(buffer, new long[] { buffer.limit() }, ONE_STRIDE);
    }

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public BooleanBufferIndexer(ByteBuffer buffer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public boolean get(long i) {
        return buffer.get((int)i) != 0;
    }
    @Override public BooleanIndexer get(long i, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = buffer.get((int)i * (int)strides[0] + n) != 0;
        }
        return this;
    }
    @Override public boolean get(long i, long j) {
        return buffer.get((int)i * (int)strides[0] + (int)j) != 0;
    }
    @Override public BooleanIndexer get(long i, long j, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = buffer.get((int)i * (int)strides[0] + (int)j * (int)strides[1] + n) != 0;
        }
        return this;
    }
    @Override public boolean get(long i, long j, long k) {
        return buffer.get((int)i * (int)strides[0] + (int)j * (int)strides[1] + (int)k) != 0;
    }
    @Override public boolean get(long... indices) {
        return buffer.get((int)index(indices)) != 0;
    }
    @Override public BooleanIndexer get(long[] indices, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = buffer.get((int)index(indices) + n) != 0;
        }
        return this;
    }

    @Override public BooleanIndexer put(long i, boolean b) {
        buffer.put((int)i, b ? (byte)1 : (byte)0);
        return this;
    }
    @Override public BooleanIndexer put(long i, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)i * (int)strides[0] + n, b[offset + n] ? (byte)1 : (byte)0);
        }
        return this;
    }
    @Override public BooleanIndexer put(long i, long j, boolean b) {
        buffer.put((int)i * (int)strides[0] + (int)j, b ? (byte)1 : (byte)0);
        return this;
    }
    @Override public BooleanIndexer put(long i, long j, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)i * (int)strides[0] + (int)j * (int)strides[1] + n, b[offset + n] ? (byte)1 : (byte)0);
        }
        return this;
    }
    @Override public BooleanIndexer put(long i, long j, long k, boolean b) {
        buffer.put((int)i * (int)strides[0] + (int)j * (int)strides[1] + (int)k, b ? (byte)1 : (byte)0);
        return this;
    }
    @Override public BooleanIndexer put(long[] indices, boolean b) {
        buffer.put((int)index(indices), b ? (byte)1 : (byte)0);
        return this;
    }
    @Override public BooleanIndexer put(long[] indices, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(indices) + n, b[offset + n] ? (byte)1 : (byte)0);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
