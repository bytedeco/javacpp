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

/**
 * An indexer for a {@code int[]} array, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UIntArrayIndexer extends UIntIndexer {
    /** The backing array. */
    protected int[] array;

    /** Calls {@code UIntArrayIndexer(array, { array.length }, { 1 })}. */
    public UIntArrayIndexer(int[] array) {
        this(array, new long[] { array.length }, ONE_STRIDE);
    }

    /** Calls {@code UIntArrayIndexer(array, sizes, strides(sizes))}. */
    public UIntArrayIndexer(int[] array, long... sizes) {
        this(array, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public UIntArrayIndexer(int[] array, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public int[] array() {
        return array;
    }

    @Override public long get(long i) {
        return array[(int)index(i)] & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long i, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = array[(int)index(i) + n] & 0xFFFFFFFFL;
        }
        return this;
    }
    @Override public long get(long i, long j) {
        return array[(int)index(i, j)] & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long i, long j, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = array[(int)index(i, j) + n] & 0xFFFFFFFFL;
        }
        return this;
    }
    @Override public long get(long i, long j, long k) {
        return array[(int)index(i, j, k)] & 0xFFFFFFFFL;
    }
    @Override public long get(long... indices) {
        return array[(int)index(indices)] & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long[] indices, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = array[(int)index(indices) + n] & 0xFFFFFFFFL;
        }
        return this;
    }

    @Override public UIntIndexer put(long i, long n) {
        array[(int)index(i)] = (int)n;
        return this;
    }
    @Override public UIntIndexer put(long i, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i) + n] = (int)m[offset + n];
        }
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long n) {
        array[(int)index(i, j)] = (int)n;
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i, j) + n] = (int)m[offset + n];
        }
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long k, long n) {
        array[(int)index(i, j, k)] = (int)n;
        return this;
    }
    @Override public UIntIndexer put(long[] indices, long n) {
        array[(int)index(indices)] = (int)n;
        return this;
    }
    @Override public UIntIndexer put(long[] indices, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(indices) + n] = (int)m[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
