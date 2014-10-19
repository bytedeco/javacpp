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

import java.nio.Buffer;

/**
 * Top-level class of all data indexers, providing easy-to-use and efficient
 * multidimensional access to primitive arrays and NIO buffers.
 *
 * @author Samuel Audet
 */
public abstract class Indexer {
    /**
     * The number of elements in each dimension.
     * These values are not typically used by the indexer.
     */
    protected int[] sizes;
    /**
     * The number of elements to skip to reach the next element in a given dimension.
     * {@code strides[i] > strides[i + 1] && strides[strides.length - 1] == 1} must hold.
     */
    protected int[] strides;

    /** Constructor to set the {@link #sizes} and {@link #strides}. */
    protected Indexer(int[] sizes, int[] strides) {
        this.sizes = sizes;
        this.strides = strides;
    }

    /** @return {@link #sizes} */
    public int[] sizes() { return sizes; }
    /** @return {@link #strides} */
    public int[] strides() { return strides; }

    /** @return {@code sizes[0]} */
    public int rows() { return sizes[0]; }
    /** @return {@code sizes[1]} */
    public int cols() { return sizes[1]; }

    /** @return {@code sizes[1]} */
    public int width() { return sizes[1]; }
    /** @return {@code sizes[0]} */
    public int height() { return sizes[0]; }
    /** @return {@code sizes[2]} */
    public int channels() { return sizes[2]; }

    /**
     * Computes the linear index as the dot product of indices and strides.
     *
     * @param indices of each dimension
     * @return index to access array or buffer
     */
    public int index(int ... indices) {
        int index = indices[indices.length - 1]; // assuming stride == 1
        for (int i = 0; i < indices.length - 1; i++) {
            index += indices[i] * strides[i];
        }
        return index;
    }

    /** @return the backing array, or {@code null} if none */
    public Object array() { return null; }
    /** @return the backing buffer, or {@code null} if none */
    public Buffer buffer() { return null; }
    /** Should write back changes to the underlying data, if required. */
    public void release() { }
}
