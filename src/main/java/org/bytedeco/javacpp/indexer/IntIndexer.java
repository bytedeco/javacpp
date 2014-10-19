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

import java.nio.IntBuffer;

/**
 * Abstract indexer for the {@code int} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class IntIndexer extends Indexer {
    protected IntIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new IntArrayIndexer(array, sizes, strides)} */
    public static IntIndexer create(int[] array, int[] sizes, int[] strides) {
        return new IntArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new IntBufferIndexer(buffer, sizes, strides)} */
    public static IntIndexer create(IntBuffer buffer, int[] sizes, int[] strides) {
        return new IntBufferIndexer(buffer, sizes, strides);
    }

    /** @return {@code array/buffer[i]} */
    public abstract int get(int i);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract int get(int i, int j);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract int get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract int get(int ... indices);

    /** @return {@code this} where {@code array/buffer[i] = n} */
    public abstract IntIndexer put(int i, int n);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = n} */
    public abstract IntIndexer put(int i, int j, int n);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = n} */
    public abstract IntIndexer put(int i, int j, int k, int n);
    /** @return {@code this} where {@code array/buffer[index(indices)] = n} */
    public abstract IntIndexer put(int[] indices, int n);
}
