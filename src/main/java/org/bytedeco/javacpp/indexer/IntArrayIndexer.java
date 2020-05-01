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
 * An indexer for an {@code int[]} array.
 *
 * @author Samuel Audet
 */
public class IntArrayIndexer extends IntIndexer {
    /** The backing array. */
    protected int[] array;

    /** Calls {@code IntArrayIndexer(array, Index.create(array.length))}. */
    public IntArrayIndexer(int[] array) {
        this(array, Index.create(array.length));
    }

    /** Calls {@code IntArrayIndexer(array, Index.create(sizes))}. */
    public IntArrayIndexer(int[] array, long... sizes) {
        this(array, Index.create(sizes));
    }

    /** Calls {@code IntArrayIndexer(array, Index.create(sizes, strides))}. */
    public IntArrayIndexer(int[] array, long[] sizes, long[] strides) {
        this(array, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #array} and {@link #index}. */
    public IntArrayIndexer(int[] array, Index index) {
        super(index);
        this.array = array;
    }

    @Override public int[] array() {
        return array;
    }

    @Override public IntIndexer reindex(Index index) {
        return new IntArrayIndexer(array, index);
    }

    @Override public int get(long i) {
        return array[(int)index(i)];
    }
    @Override public IntIndexer get(long i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = array[(int)index(i) + n];
        }
        return this;
    }
    @Override public int get(long i, long j) {
        return array[(int)index(i, j)];
    }
    @Override public IntIndexer get(long i, long j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = array[(int)index(i, j) + n];
        }
        return this;
    }
    @Override public int get(long i, long j, long k) {
        return array[(int)index(i, j, k)];
    }
    @Override public int get(long... indices) {
        return array[(int)index(indices)];
    }
    @Override public IntIndexer get(long[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = array[(int)index(indices) + n];
        }
        return this;
    }

    @Override public IntIndexer put(long i, int n) {
        array[(int)index(i)] = n;
        return this;
    }
    @Override public IntIndexer put(long i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i) + n] = m[offset + n];
        }
        return this;
    }
    @Override public IntIndexer put(long i, long j, int n) {
        array[(int)index(i, j)] = n;
        return this;
    }
    @Override public IntIndexer put(long i, long j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i, j) + n] = m[offset + n];
        }
        return this;
    }
    @Override public IntIndexer put(long i, long j, long k, int n) {
        array[(int)index(i, j, k)] = n;
        return this;
    }
    @Override public IntIndexer put(long[] indices, int n) {
        array[(int)index(indices)] = n;
        return this;
    }
    @Override public IntIndexer put(long[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(indices) + n] = m[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
