/*
 * Copyright (C) 2016-2019 Samuel Audet
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
import java.nio.ShortBuffer;

/**
 * An indexer for a {@link ShortBuffer}, treated as half-precision float.
 *
 * @author Samuel Audet
 */
public class HalfBufferIndexer extends HalfIndexer {
    /** The backing buffer. */
    protected ShortBuffer buffer;

    /** Calls {@code HalfBufferIndexer(buffer, Index.create(buffer.limit()))}. */
    public HalfBufferIndexer(ShortBuffer buffer) {
        this(buffer, Index.create(buffer.limit()));
    }

    /** Calls {@code HalfBufferIndexer(buffer, Index.create(sizes))}. */
    public HalfBufferIndexer(ShortBuffer buffer, long... sizes) {
        this(buffer, Index.create(sizes));
    }

    /** Calls {@code HalfBufferIndexer(buffer, Index.create(sizes, strides))}. */
    public HalfBufferIndexer(ShortBuffer buffer, long[] sizes, long[] strides) {
        this(buffer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #buffer} and {@link #index}. */
    public HalfBufferIndexer(ShortBuffer buffer, Index index) {
        super(index);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public HalfIndexer reindex(Index index) {
        return new HalfBufferIndexer(buffer, index);
    }

    @Override public float get(long i) {
        return toFloat(buffer.get((int)index(i)));
    }
    @Override public HalfIndexer get(long i, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            h[offset + n] = toFloat(buffer.get((int)index(i) + n));
        }
        return this;
    }
    @Override public float get(long i, long j) {
        return toFloat(buffer.get((int)index(i, j)));
    }
    @Override public HalfIndexer get(long i, long j, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            h[offset + n] = toFloat(buffer.get((int)index(i, j) + n));
        }
        return this;
    }
    @Override public float get(long i, long j, long k) {
        return toFloat(buffer.get((int)index(i, j, k)));
    }
    @Override public float get(long... indices) {
        return toFloat(buffer.get((int)index(indices)));
    }
    @Override public HalfIndexer get(long[] indices, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            h[offset + n] = toFloat(buffer.get((int)index(indices) + n));
        }
        return this;
    }

    @Override public HalfIndexer put(long i, float h) {
        buffer.put((int)index(i), (short)fromFloat(h));
        return this;
    }
    @Override public HalfIndexer put(long i, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i) + n, (short)fromFloat(h[offset + n]));
        }
        return this;
    }
    @Override public HalfIndexer put(long i, long j, float h) {
        buffer.put((int)index(i, j), (short)fromFloat(h));
        return this;
    }
    @Override public HalfIndexer put(long i, long j, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i, j) + n, (short)fromFloat(h[offset + n]));
        }
        return this;
    }
    @Override public HalfIndexer put(long i, long j, long k, float h) {
        buffer.put((int)index(i, j, k), (short)fromFloat(h));
        return this;
    }
    @Override public HalfIndexer put(long[] indices, float h) {
        buffer.put((int)index(indices), (short)fromFloat(h));
        return this;
    }
    @Override public HalfIndexer put(long[] indices, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(indices) + n, (short)fromFloat(h[offset + n]));
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
