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
import java.nio.CharBuffer;

/**
 * An indexer for a {@link CharBuffer}.
 *
 * @author Samuel Audet
 */
public class CharBufferIndexer extends CharIndexer {
    /** The backing buffer. */
    protected CharBuffer buffer;

    /** Calls {@code CharBufferIndexer(buffer, Index.create(buffer.limit()))}. */
    public CharBufferIndexer(CharBuffer buffer) {
        this(buffer, Index.create(buffer.limit()));
    }

    /** Calls {@code CharBufferIndexer(buffer, Index.create(sizes))}. */
    public CharBufferIndexer(CharBuffer buffer, long... sizes) {
        this(buffer, Index.create(sizes));
    }

    /** Calls {@code CharBufferIndexer(buffer, Index.create(sizes, strides))}. */
    public CharBufferIndexer(CharBuffer buffer, long[] sizes, long[] strides) {
        this(buffer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #buffer} and {@link #index}. */
    public CharBufferIndexer(CharBuffer buffer, Index index) {
        super(index);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public CharIndexer reindex(Index index) {
        return new CharBufferIndexer(buffer, index);
    }

    @Override public char get(long i) {
        return buffer.get((int)index(i));
    }
    @Override public CharIndexer get(long i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = buffer.get((int)index(i) + n);
        }
        return this;
    }
    @Override public char get(long i, long j) {
        return buffer.get((int)index(i, j));
    }
    @Override public CharIndexer get(long i, long j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = buffer.get((int)index(i, j) + n);
        }
        return this;
    }
    @Override public char get(long i, long j, long k) {
        return buffer.get((int)index(i, j, k));
    }
    @Override public char get(long... indices) {
        return buffer.get((int)index(indices));
    }
    @Override public CharIndexer get(long[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = buffer.get((int)index(indices) + n);
        }
        return this;
    }

    @Override public CharIndexer put(long i, char c) {
        buffer.put((int)index(i), c);
        return this;
    }
    @Override public CharIndexer put(long i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i) + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(long i, long j, char c) {
        buffer.put((int)index(i, j), c);
        return this;
    }
    @Override public CharIndexer put(long i, long j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i, j) + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(long i, long j, long k, char c) {
        buffer.put((int)index(i, j, k), c);
        return this;
    }
    @Override public CharIndexer put(long[] indices, char c) {
        buffer.put((int)index(indices), c);
        return this;
    }
    @Override public CharIndexer put(long[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(indices) + n, c[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
