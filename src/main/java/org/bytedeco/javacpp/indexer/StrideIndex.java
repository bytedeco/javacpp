/*
 * Copyright (C) 2020 Matteo Di Giovinazzo, Samuel Audet
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
 * An Index that computes a linear index from given array sizes and strides.
 *
 * @author Matteo Di Giovinazzo
 */
public class StrideIndex extends Index {

    /**
     * Returns default (row-major contiguous) strides for the given sizes.
     */
    public static long[] defaultStrides(long... sizes) {
        long[] strides = new long[sizes.length];
        strides[sizes.length - 1] = 1;
        for (int i = sizes.length - 2; i >= 0; i--) {
            strides[i] = strides[i + 1] * sizes[i + 1];
        }
        return strides;
    }

    /**
     * The number of elements to skip to reach the next element in a given dimension.
     * {@code strides[i] > strides[i + 1] && strides[strides.length - 1] == 1} preferred.
     */
    protected final long[] strides;

    /** Calls {@code StrideIndex(sizes, defaultStrides(sizes))}. */
    public StrideIndex(long... sizes) {
        this(sizes, defaultStrides(sizes));
    }

    /** Constructor to set the {@link #sizes} and {@link #strides}. */
    public StrideIndex(long[] sizes, long[] strides) {
        super(sizes);
        this.strides = strides;
    }

    /** Returns {@link #strides}. */
    public long[] strides() {
        return strides;
    }

    /** Returns {@code i * strides[0]}. */
    @Override public long index(long i) {
        return i * strides[0];
    }

    /** Returns {@code i * strides[0] + j * strides[1]}. */
    @Override public long index(long i, long j) {
        return i * strides[0] + j * strides[1];
    }

    /** Returns {@code i * strides[0] + j * strides[1] + k * strides[2]}. */
    @Override public long index(long i, long j, long k) {
        return i * strides[0] + j * strides[1] + k * strides[2];
    }

    /**
     * Computes the linear index as the dot product of indices and strides.
     *
     * @param indices of each dimension
     * @return index to access array or buffer
     */
    @Override public long index(long... indices) {
        long index = 0;
        for (int i = 0; i < indices.length && i < strides.length; i++) {
            index += indices[i] * strides[i];
        }
        return index;
    }
}
