/*
 * Copyright (C) 2018-2019 Samuel Audet
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
 * An indexer for a {@code boolean[]} array.
 *
 * @author Samuel Audet
 */
public class BooleanArrayIndexer extends BooleanIndexer {
    /** The backing array. */
    protected boolean[] array;

    /** Calls {@code BooleanArrayIndexer(array, { array.length }, { 1 })}. */
    public BooleanArrayIndexer(boolean[] array) {
        this(array, new long[] { array.length }, ONE_STRIDE);
    }

    /** Calls {@code BooleanArrayIndexer(array, sizes, strides(sizes))}. */
    public BooleanArrayIndexer(boolean[] array, long... sizes) {
        this(array, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public BooleanArrayIndexer(boolean[] array, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public boolean[] array() {
        return array;
    }

    @Override public boolean get(long i) {
        return array[(int)index(i)];
    }
    @Override public BooleanIndexer get(long i, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[(int)index(i) + n];
        }
        return this;
    }
    @Override public boolean get(long i, long j) {
        return array[(int)index(i, j)];
    }
    @Override public BooleanIndexer get(long i, long j, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[(int)index(i, j) + n];
        }
        return this;
    }
    @Override public boolean get(long i, long j, long k) {
        return array[(int)index(i, j, k)];
    }
    @Override public boolean get(long... indices) {
        return array[(int)index(indices)];
    }
    @Override public BooleanIndexer get(long[] indices, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[(int)index(indices) + n];
        }
        return this;
    }

    @Override public BooleanIndexer put(long i, boolean b) {
        array[(int)index(i)] = b;
        return this;
    }
    @Override public BooleanIndexer put(long i, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i) + n] = b[offset + n];
        }
        return this;
    }
    @Override public BooleanIndexer put(long i, long j, boolean b) {
        array[(int)index(i, j)] = b;
        return this;
    }
    @Override public BooleanIndexer put(long i, long j, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i, j) + n] = b[offset + n];
        }
        return this;
    }
    @Override public BooleanIndexer put(long i, long j, long k, boolean b) {
        array[(int)index(i, j, k)] = b;
        return this;
    }
    @Override public BooleanIndexer put(long[] indices, boolean b) {
        array[(int)index(indices)] = b;
        return this;
    }
    @Override public BooleanIndexer put(long[] indices, boolean[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(indices) + n] = b[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
