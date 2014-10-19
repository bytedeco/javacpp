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

import java.nio.ShortBuffer;

/**
 * Abstract indexer for the {@code short} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class ShortIndexer extends Indexer {
    protected ShortIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new ShortArrayIndexer(array, sizes, strides)} */
    public static ShortIndexer create(short[] array, int[] sizes, int[] strides) {
        return new ShortArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new ShortBufferIndexer(buffer, sizes, strides)} */
    public static ShortIndexer create(ShortBuffer buffer, int[] sizes, int[] strides) {
        return new ShortBufferIndexer(buffer, sizes, strides);
    }

    /** @return {@code array/buffer[i]} */
    public abstract short get(int i);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract short get(int i, int j);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract short get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract short get(int ... indices);

    /** @return {@code this} where {@code array/buffer[i] = s} */
    public abstract ShortIndexer put(int i, short s);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = s} */
    public abstract ShortIndexer put(int i, int j, short s);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = s} */
    public abstract ShortIndexer put(int i, int j, int k, short s);
    /** @return {@code this} where {@code array/buffer[index(indices)] = s} */
    public abstract ShortIndexer put(int[] indices, short s);
}
