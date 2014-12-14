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
import org.bytedeco.javacpp.DoublePointer;

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
    /** @return {@code create(pointer, sizes, strides, true)} */
    public static DoubleIndexer create(DoublePointer pointer, int[] sizes, int[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a double indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new double array backed by a buffer or an array
     */
    public static DoubleIndexer create(final DoublePointer pointer, int[] sizes, int[] strides, boolean direct) {
        if (direct) {
            return new DoubleBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final int position = pointer.position();
            double[] array = new double[pointer.limit() - position];
            pointer.get(array);
            return new DoubleArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** @return {@code array/buffer[i]} */
    public abstract double get(int i);
    /** @return {@code this} where {@code d = array/buffer[i]} */
    public DoubleIndexer get(int i, double[] d) { return get(i, d, 0, d.length); }
    /** @return {@code this} where {@code d[offset:offset + length] = array/buffer[i]} */
    public abstract DoubleIndexer get(int i, double[] d, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract double get(int i, int j);
    /** @return {@code this} where {@code d = array/buffer[i * strides[0] + j]} */
    public DoubleIndexer get(int i, int j, double[] d) { return get(i, j, d, 0, d.length); }
    /** @return {@code this} where {@code d[offset:offset + length] = array/buffer[i * strides[0] + j]} */
    public abstract DoubleIndexer get(int i, int j, double[] d, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract double get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract double get(int ... indices);
    /** @return {@code this} where {@code d = array/buffer[index(indices)]} */
    public DoubleIndexer get(int[] indices, double[] d) { return get(indices, d, 0, d.length); }
    /** @return {@code this} where {@code d[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract DoubleIndexer get(int[] indices, double[] d, int offset, int length);

    /** @return {@code this} where {@code array/buffer[i] = d} */
    public abstract DoubleIndexer put(int i, double d);
    /** @return {@code this} where {@code array/buffer[i] = d} */
    public DoubleIndexer put(int i, double ... d) { return put(i, d, 0, d.length); }
    /** @return {@code this} where {@code array/buffer[i] = d[offset:offset + length]} */
    public abstract DoubleIndexer put(int i, double[] d, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = d} */
    public abstract DoubleIndexer put(int i, int j, double d);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = d} */
    public DoubleIndexer put(int i, int j, double ... d) { return put(i, j, d, 0, d.length); }
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = d[offset:offset + length]} */
    public abstract DoubleIndexer put(int i, int j, double[] d, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = d} */
    public abstract DoubleIndexer put(int i, int j, int k, double d);
    /** @return {@code this} where {@code array/buffer[index(indices)] = d} */
    public abstract DoubleIndexer put(int[] indices, double d);
    /** @return {@code this} where {@code array/buffer[index(indices)] = d} */
    public DoubleIndexer put(int[] indices, double ... d) { return put(indices, d, 0, d.length); }
    /** @return {@code this} where {@code array/buffer[index(indices)] = d[offset:offset + length]} */
    public abstract DoubleIndexer put(int[] indices, double[] d, int offset, int length);

    @Override public double getDouble(int ... indices) { return get(indices); }
}
