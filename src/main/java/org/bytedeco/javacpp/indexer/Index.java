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
 * Provides an interface for classes that compute a linear index from given array sizes.
 *
 * @see OneIndex
 * @see StrideIndex
 * @see HyperslabIndex
 *
 * @author Matteo Di Giovinazzo
 */
public abstract class Index {

    /** Returns {@code new OneIndex(size)}. */
    public static Index create(long size) {
        return new OneIndex(size);
    }

    /** Returns {@code new StrideIndex(sizes)}. */
    public static Index create(long... sizes) {
        return new StrideIndex(sizes);
    }

    /** Returns {@code new StrideIndex(sizes, strides)}. */
    public static Index create(long[] sizes, long[] strides) {
        return new StrideIndex(sizes, strides);
    }

    /** Returns {@code new HyperslabIndex(sizes, selectionOffsets, selectionStrides, selectionCounts, selectionBlocks)}. */
    public static Index create(long[] sizes, long[] selectionOffsets, long[] selectionStrides,
            long[] selectionCounts, long[] selectionBlocks) {
        return new HyperslabIndex(sizes, selectionOffsets, selectionStrides, selectionCounts, selectionBlocks);
    }

    /** Returns {@code new HyperslabIndex(sizes, strides, selectionOffsets, selectionStrides, selectionCounts, selectionBlocks)}. */
    public static Index create(long[] sizes, long[] strides, long[] selectionOffsets, long[] selectionStrides,
            long[] selectionCounts, long[] selectionBlocks) {
        return new HyperslabIndex(sizes, strides, selectionOffsets, selectionStrides, selectionCounts, selectionBlocks);
    }

    /**
     * The number of elements in each dimension.
     * These values are not typically used by the indexer.
     */
    protected final long[] sizes;

    /** Constructor to set the {@link #sizes}. */
    public Index(long... sizes) {
        this.sizes = sizes;
    }

    /** Returns {@code sizes.length}. */
    public int rank() {
        return sizes.length;
    }

    /** Returns {@link #sizes}. */
    public long[] sizes() {
        return sizes;
    }

    /** Returns {@code sizes[i]}. */
    public long size(int i) {
        return sizes[i];
    }

    /** Returns {@code index(new long[] {i})}. */
    public long index(long i) {
        return index(new long[] {i});
    }

    /** Returns {@code index(new long[] {i, j})}. */
    public long index(long i, long j) {
        return index(new long[] {i, j});
    }

    /** Returns {@code index(new long[] {i, j, k})}. */
    public long index(long i, long j, long k) {
        return index(new long[] {i, j, k});
    }

    /**
     * Computes the linear index.
     *
     * @param indices of each dimension
     * @return index to access array or buffer
     */
    public abstract long index(long... indices);
}
