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
 * TODO
 *
 * @author Matteo Di Giovinazzo
 */
class StrideIndex extends Index {

    protected final long[] sizes;

    protected final long[] strides;

    /**
     * TODO
     *
     * @param sizes
     * @param strides The number of elements to skip to reach the next element in a given dimension.
     *                {@code strides[i] > strides[i + 1] && strides[strides.length - 1] == 1} preferred.
     */
    protected StrideIndex(long[] sizes, long[] strides) {
        this.sizes = sizes;
        this.strides = strides;
    }

    @Override
    public long[] sizes() { return sizes; }

    @Deprecated
    @Override
    long[] strides() { return strides; }

    @Override
    public long index(long i) {
        return i * strides[0];
    }

    @Override
    public long index(long i, long j) {
        return i * strides[0] + j * strides[1];
    }

    @Override
    public long index(long i, long j, long k) {
        return i * strides[0] + j * strides[1] + k * strides[2];
    }

    /**
     * Computes the linear index as the dot product of indices and strides.
     *
     * @param indices of each dimension
     * @return index to access array or buffer
     */
    @Override
    public long index(long... indices) {
        long index = 0;
        for (int i = 0; i < indices.length && i < strides.length; i++) {
            index += indices[i] * strides[i];
        }
        return index;
    }
}
