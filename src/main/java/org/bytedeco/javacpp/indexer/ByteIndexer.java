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

import java.nio.ByteBuffer;

/**
 * Abstract indexer for the {@code byte} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class ByteIndexer extends Indexer {
    protected ByteIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new ByteArrayIndexer(array, sizes, strides)} */
    public static ByteIndexer create(byte[] array, int[] sizes, int[] strides) {
        return new ByteArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new ByteBufferIndexer(buffer, sizes, strides)} */
    public static ByteIndexer create(ByteBuffer buffer, int[] sizes, int[] strides) {
        return new ByteBufferIndexer(buffer, sizes, strides);
    }

    /** @return {@code array/buffer[i]} */
    public abstract byte get(int i);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract byte get(int i, int j);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract byte get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract byte get(int ... indices);

    /** @return {@code this} where {@code array/buffer[i] = b} */
    public abstract ByteIndexer put(int i, byte b);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = b} */
    public abstract ByteIndexer put(int i, int j, byte b);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = b} */
    public abstract ByteIndexer put(int i, int j, int k, byte b);
    /** @return {@code this} where {@code array/buffer[index(indices)] = b} */
    public abstract ByteIndexer put(int[] indices, byte b);
}
