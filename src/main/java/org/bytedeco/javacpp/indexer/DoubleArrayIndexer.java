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
 * An indexer for a {@code double[]} array.
 *
 * @author Samuel Audet
 */
public class DoubleArrayIndexer extends DoubleIndexer {
    /** The backing array. */
    protected double[] array;

    /** Calls {@code DoubleArrayIndexer(array, Index.create(array.length))}. */
    public DoubleArrayIndexer(double[] array) {
        this(array, Index.create(array.length));
    }

    /** Calls {@code DoubleArrayIndexer(array, Index.create(sizes))}. */
    public DoubleArrayIndexer(double[] array, long... sizes) {
        this(array, Index.create(sizes));
    }

    /** Calls {@code DoubleArrayIndexer(array, Index.create(sizes, strides))}. */
    public DoubleArrayIndexer(double[] array, long[] sizes, long[] strides) {
        this(array, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #array} and {@link #index}. */
    public DoubleArrayIndexer(double[] array, Index index) {
        super(index);
        this.array = array;
    }

    @Override public double[] array() {
        return array;
    }

    @Override public DoubleIndexer reindex(Index index) {
        return new DoubleArrayIndexer(array, index);
    }

    @Override public double get(long i) {
        return array[(int)index(i)];
    }
    @Override public DoubleIndexer get(long i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = array[(int)index(i) + n];
        }
        return this;
    }
    @Override public double get(long i, long j) {
        return array[(int)index(i, j)];
    }
    @Override public DoubleIndexer get(long i, long j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = array[(int)index(i, j) + n];
        }
        return this;
    }
    @Override public double get(long i, long j, long k) {
        return array[(int)index(i, j, k)];
    }
    @Override public double get(long... indices) {
        return array[(int)index(indices)];
    }
    @Override public DoubleIndexer get(long[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = array[(int)index(indices) + n];
        }
        return this;
    }

    @Override public DoubleIndexer put(long i, double d) {
        array[(int)index(i)] = d;
        return this;
    }
    @Override public DoubleIndexer put(long i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i) + n] = d[offset + n];
        }
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, double d) {
        array[(int)index(i, j)] = d;
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i, j) + n] = d[offset + n];
        }
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, long k, double d) {
        array[(int)index(i, j, k)] = d;
        return this;
    }
    @Override public DoubleIndexer put(long[] indices, double d) {
        array[(int)index(indices)] = d;
        return this;
    }
    @Override public DoubleIndexer put(long[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(indices) + n] = d[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
