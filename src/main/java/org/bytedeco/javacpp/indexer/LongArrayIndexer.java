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

/**
 * An indexer for a {@code long[]} array.
 *
 * @author Samuel Audet
 */
public class LongArrayIndexer extends LongIndexer {
    /** The backing array. */
    protected long[] array;

    /** Calls {@code LongArrayIndexer(array, Index.create(array.length))}. */
    public LongArrayIndexer(long[] array) {
        this(array, Index.create(array.length));
    }

    /** Calls {@code LongArrayIndexer(array, Index.create(sizes))}. */
    public LongArrayIndexer(long[] array, long... sizes) {
        this(array, Index.create(sizes));
    }

    /** Calls {@code LongArrayIndexer(array, Index.create(sizes, strides))}. */
    public LongArrayIndexer(long[] array, long[] sizes, long[] strides) {
        this(array, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #array} and {@link #index}. */
    public LongArrayIndexer(long[] array, Index index) {
        super(index);
        this.array = array;
    }

    @Override public long[] array() {
        return array;
    }

    @Override public LongIndexer reindex(Index index) {
        return new LongArrayIndexer(array, index);
    }

    @Override public long get(long i) {
        return array[(int)index(i)];
    }
    @Override public LongIndexer get(long i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = array[(int)index(i) + n];
        }
        return this;
    }
    @Override public long get(long i, long j) {
        return array[(int)index(i, j)];
    }
    @Override public LongIndexer get(long i, long j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = array[(int)index(i, j) + n];
        }
        return this;
    }
    @Override public long get(long i, long j, long k) {
        return array[(int)index(i, j, k)];
    }
    @Override public long get(long... indices) {
        return array[(int)index(indices)];
    }
    @Override public LongIndexer get(long[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = array[(int)index(indices) + n];
        }
        return this;
    }

    @Override public LongIndexer put(long i, long l) {
        array[(int)index(i)] = l;
        return this;
    }
    @Override public LongIndexer put(long i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i) + n] = l[offset + n];
        }
        return this;
    }
    @Override public LongIndexer put(long i, long j, long l) {
        array[(int)index(i, j)] = l;
        return this;
    }
    @Override public LongIndexer put(long i, long j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i, j) + n] = l[offset + n];
        }
        return this;
    }
    @Override public LongIndexer put(long i, long j, long k, long l) {
        array[(int)index(i, j, k)] = l;
        return this;
    }
    @Override public LongIndexer put(long[] indices, long l) {
        array[(int)index(indices)] = l;
        return this;
    }
    @Override public LongIndexer put(long[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(indices) + n] = l[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
