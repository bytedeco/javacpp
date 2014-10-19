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

import java.nio.DoubleBuffer;

/**
 * Abstract indexer for the {@code double} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class DoubleIndexer extends Indexer {
    protected DoubleIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new DoubleArrayIndexer(array, sizes, strides)} */
    public static DoubleIndexer create(double[] array, int[] sizes, int[] strides) {
        return new DoubleArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new DoubleBufferIndexer(buffer, sizes, strides)} */
    public static DoubleIndexer create(DoubleBuffer buffer, int[] sizes, int[] strides) {
        return new DoubleBufferIndexer(buffer, sizes, strides);
    }

    /** @return {@code array/buffer[i]} */
    public abstract double get(int i);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract double get(int i, int j);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract double get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract double get(int ... indices);

    /** @return {@code this} where {@code array/buffer[i] = d} */
    public abstract DoubleIndexer put(int i, double d);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = d} */
    public abstract DoubleIndexer put(int i, int j, double d);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = d} */
    public abstract DoubleIndexer put(int i, int j, int k, double d);
    /** @return {@code this} where {@code array/buffer[index(indices)] = d} */
    public abstract DoubleIndexer put(int[] indices, double d);
}
