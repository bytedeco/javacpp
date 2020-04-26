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

/**
 * A hyperslab is a rectangular pattern defined by four arrays.
 * <p>
 * The {@code start} defines the origin of the hyperslab in the original coordinates.
 * The {@code stride} is the number of elements to increment between selected elements.
 * A stride of '1' is every element, a stride of '2' is every second element, etc.
 * The default stride is 1.
 * The {@code count} is the number of elements in the hyperslab selection.
 * When the stride is 1, the selection is a hyper rectangle with a corner at {@code start}
 * and size {@code count[0]} by {@code count[1]} by ...
 * When stride is greater than one, the hyperslab bounded by start and the corners
 * defined by {@code stride[n] * count[n]}.
 * The {@code block} is a count on the number of repetitions of the hyperslab.
 * The default block size is '1', which is one hyperslab. A block of 2 would be
 * two hyperslabs in that dimension, with the second starting at {@code start[n]+ (count[n] * stride[n]) + 1}.
 *
 * @author Matteo Di Giovinazzo
 * @see <a href="https://portal.hdfgroup.org/display/HDF5/Reading+From+or+Writing+To+a+Subset+of+a+Dataset">Reading From
 * or Writing To a Subset of a Dataset</a>
 * @see <a href="https://portal.hdfgroup.org/display/HDF5/H5S_SELECT_HYPERSLAB">H5S_SELECT_HYPERSLAB</a>
 * @see <a href="https://support.hdfgroup.org/HDF5/doc1.6/UG/12_Dataspaces.html">Dataspaces</a>
 */
class HyperslabIndex extends StrideIndex {

    protected long[] offsets;
    protected long[] hyperslabStrides;
    protected long[] counts;
    protected long[] blocks;

    protected HyperslabIndex(long[] sizes, long[] offsets, long[] hyperslabStrides, long[] counts, long[] blocks) {
        super(sizes, Index.defaultStrides(sizes));
        this.offsets = offsets;
        this.hyperslabStrides = hyperslabStrides;
        this.counts = counts;
        this.blocks = blocks;
    }

    @Override
    public long index(long i) {
        return (offsets[0] + hyperslabStrides[0] * (i / blocks[0]) + (i % blocks[0])) * strides[0];
    }

    @Override
    public long index(long i, long j) {
        return (offsets[0] + hyperslabStrides[0] * (i / blocks[0]) + (i % blocks[0])) * strides[0]
                + (offsets[1] + hyperslabStrides[1] * (j / blocks[1]) + (j % blocks[1])) * strides[1];
    }

    @Override
    public long index(long i, long j, long k) {
        return (offsets[0] + hyperslabStrides[0] * (i / blocks[0]) + (i % blocks[0])) * strides[0]
                + (offsets[1] + hyperslabStrides[1] * (j / blocks[1]) + (j % blocks[1])) * strides[1]
                + (offsets[2] + hyperslabStrides[2] * (k / blocks[2]) + (k % blocks[2])) * strides[2];
    }

    @Override
    public long index(long... indices) {
        long index = 0;
        for (int i = 0; i < indices.length; i++) {
            long coordinate = indices[i];
            long mappedCoordinate = offsets[i] + hyperslabStrides[i] * (coordinate / blocks[i]) + (coordinate % blocks[i]);
            index += mappedCoordinate * strides[i];
        }
        return index;
    }

    @Override
    public long[] sizes() {
        return sizes;
    }
}
