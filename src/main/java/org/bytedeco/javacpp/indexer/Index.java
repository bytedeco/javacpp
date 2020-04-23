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
public abstract class Index {

    public static Index create(long... sizes) {
        return new StrideIndex(sizes, defaultStrides(sizes));
    }

    public static Index create(long[] sizes, long[] strides) {
        return new StrideIndex(sizes, strides);
    }

    public static Index create(long[] sizes, long[] offsets, long[] hyperslabStrides, long[] counts, long[] blocks) {
        return new HyperslabIndex(sizes, offsets, hyperslabStrides, counts, blocks);
    }

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
     * TODO
     *
     * @param i
     * @return
     */
    public abstract long index(long i);

    /**
     * TODO
     *
     * @param i
     * @param j
     * @return
     */
    public abstract long index(long i, long j);

    /**
     * TODO
     *
     * @param i
     * @param j
     * @param k
     * @return
     */
    public abstract long index(long i, long j, long k);

    /**
     * TODO
     *
     * @param indices
     * @return
     */
    public abstract long index(long... indices);

    /** Returns {@link #sizes} */
    public abstract long[] sizes();

    /**
     * Needed for backward compatibility with {@link Indexer#strides()}
     * @return
     */
    @Deprecated
    abstract long[] strides();
}
