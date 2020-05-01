/*
 * Copyright (C) 2020 Samuel Audet
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
import java.nio.IntBuffer;

/**
 * An indexer for a {@link IntBuffer}, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UIntBufferIndexer extends UIntIndexer {
    /** The backing buffer. */
    protected IntBuffer buffer;

    /** Calls {@code UIntBufferIndexer(buffer, Index.create(buffer.limit()))}. */
    public UIntBufferIndexer(IntBuffer buffer) {
        this(buffer, Index.create(buffer.limit()));
    }

    /** Calls {@code UIntBufferIndexer(buffer, Index.create(sizes))}. */
    public UIntBufferIndexer(IntBuffer buffer, long... sizes) {
        this(buffer, Index.create(sizes));
    }

    /** Calls {@code UIntBufferIndexer(buffer, Index.create(sizes, strides))}. */
    public UIntBufferIndexer(IntBuffer buffer, long[] sizes, long[] strides) {
        this(buffer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #buffer} and {@link #index}. */
    public UIntBufferIndexer(IntBuffer buffer, Index index) {
        super(index);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public UIntIndexer reindex(Index index) {
        return new UIntBufferIndexer(buffer, index);
    }

    @Override public long get(long i) {
        return buffer.get((int)index(i)) & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long i, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = buffer.get((int)index(i) + n) & 0xFFFFFFFFL;
        }
        return this;
    }
    @Override public long get(long i, long j) {
        return buffer.get((int)index(i, j)) & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long i, long j, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = buffer.get((int)index(i, j) + n) & 0xFFFFFFFFL;
        }
        return this;
    }
    @Override public long get(long i, long j, long k) {
        return buffer.get((int)index(i, j, k)) & 0xFFFFFFFFL;
    }
    @Override public long get(long... indices) {
        return buffer.get((int)index(indices)) & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long[] indices, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = buffer.get((int)index(indices) + n) & 0xFFFFFFFFL;
        }
        return this;
    }

    @Override public UIntIndexer put(long i, long n) {
        buffer.put((int)index(i), (int)n);
        return this;
    }
    @Override public UIntIndexer put(long i, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i) + n, (int)m[offset + n]);
        }
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long n) {
        buffer.put((int)index(i, j), (int)n);
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i, j) + n, (int)m[offset + n]);
        }
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long k, long n) {
        buffer.put((int)index(i, j, k), (int)n);
        return this;
    }
    @Override public UIntIndexer put(long[] indices, long n) {
        buffer.put((int)index(indices), (int)n);
        return this;
    }
    @Override public UIntIndexer put(long[] indices, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(indices) + n, (int)m[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
