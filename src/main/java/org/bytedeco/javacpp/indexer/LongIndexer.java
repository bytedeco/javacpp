/*
 * Copyright (C) 2014 Samuel Audet
 *
 * This file is part of JavaCPP.
 *
 * JavaCPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bytedeco.javacpp.indexer;

import java.nio.LongBuffer;

/**
 * Abstract indexer for the {@code long} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class LongIndexer extends Indexer {
    protected LongIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new LongArrayIndexer(array, sizes, strides)} */
    public static LongIndexer create(long[] array, int[] sizes, int[] strides) {
        return new LongArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new LongBufferIndexer(buffer, sizes, strides)} */
    public static LongIndexer create(LongBuffer buffer, int[] sizes, int[] strides) {
        return new LongBufferIndexer(buffer, sizes, strides);
    }

    /** @return {@code array/buffer[i]} */
    public abstract long get(int i);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract long get(int i, int j);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract long get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract long get(int ... indices);

    /** @return {@code this} where {@code array/buffer[i] = l} */
    public abstract LongIndexer put(int i, long l);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = l} */
    public abstract LongIndexer put(int i, int j, long l);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = l} */
    public abstract LongIndexer put(int i, int j, int k, long l);
    /** @return {@code this} where {@code array/buffer[index(indices)] = l} */
    public abstract LongIndexer put(int[] indices, long l);
}
