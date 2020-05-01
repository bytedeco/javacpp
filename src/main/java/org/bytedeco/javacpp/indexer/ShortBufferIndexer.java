/*
 * Copyright (C) 2014-2019 Samuel Audet
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
 * An indexer for a {@link ShortBuffer}.
 *
 * @author Samuel Audet
 */
public class ShortBufferIndexer extends ShortIndexer {
    /** The backing buffer. */
    protected ShortBuffer buffer;

    /** Calls {@code ShortBufferIndexer(buffer, Index.create(buffer.limit()))}. */
    public ShortBufferIndexer(ShortBuffer buffer) {
        this(buffer, Index.create(buffer.limit()));
    }

    /** Calls {@code ShortBufferIndexer(buffer, Index.create(sizes))}. */
    public ShortBufferIndexer(ShortBuffer buffer, long... sizes) {
        this(buffer, Index.create(sizes));
    }

    /** Calls {@code ShortBufferIndexer(buffer, Index.create(sizes, strides))}. */
    public ShortBufferIndexer(ShortBuffer buffer, long[] sizes, long[] strides) {
        this(buffer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #buffer} and {@link #index}. */
    public ShortBufferIndexer(ShortBuffer buffer, Index index) {
        super(index);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public ShortIndexer reindex(Index index) {
        return new ShortBufferIndexer(buffer, index);
    }

    @Override public short get(long i) {
        return buffer.get((int)index(i));
    }
    @Override public ShortIndexer get(long i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get((int)index(i) + n);
        }
        return this;
    }
    @Override public short get(long i, long j) {
        return buffer.get((int)index(i, j));
    }
    @Override public ShortIndexer get(long i, long j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get((int)index(i, j) + n);
        }
        return this;
    }
    @Override public short get(long i, long j, long k) {
        return buffer.get((int)index(i, j, k));
    }
    @Override public short get(long... indices) {
        return buffer.get((int)index(indices));
    }
    @Override public ShortIndexer get(long[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get((int)index(indices) + n);
        }
        return this;
    }

    @Override public ShortIndexer put(long i, short s) {
        buffer.put((int)index(i), s);
        return this;
    }
    @Override public ShortIndexer put(long i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i) + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(long i, long j, short s) {
        buffer.put((int)index(i, j), s);
        return this;
    }
    @Override public ShortIndexer put(long i, long j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i, j) + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(long i, long j, long k, short s) {
        buffer.put((int)index(i, j, k), s);
        return this;
    }
    @Override public ShortIndexer put(long[] indices, short s) {
        buffer.put((int)index(indices), s);
        return this;
    }
    @Override public ShortIndexer put(long[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(indices) + n, s[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
