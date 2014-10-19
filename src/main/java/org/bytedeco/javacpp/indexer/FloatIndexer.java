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

import java.nio.FloatBuffer;

/**
 * Abstract indexer for the {@code float} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class FloatIndexer extends Indexer {
    protected FloatIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new FloatArrayIndexer(array, sizes, strides)} */
    public static FloatIndexer create(float[] array, int[] sizes, int[] strides) {
        return new FloatArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new FloatBufferIndexer(buffer, sizes, strides)} */
    public static FloatIndexer create(FloatBuffer buffer, int[] sizes, int[] strides) {
        return new FloatBufferIndexer(buffer, sizes, strides);
    }

    /** @return {@code array/buffer[i]} */
    public abstract float get(int i);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract float get(int i, int j);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract float get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract float get(int ... indices);

    /** @return {@code this} where {@code array/buffer[i] = f} */
    public abstract FloatIndexer put(int i, float f);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = f} */
    public abstract FloatIndexer put(int i, int j, float f);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = f} */
    public abstract FloatIndexer put(int i, int j, int k, float f);
    /** @return {@code this} where {@code array/buffer[index(indices)] = f} */
    public abstract FloatIndexer put(int[] indices, float f);
}
