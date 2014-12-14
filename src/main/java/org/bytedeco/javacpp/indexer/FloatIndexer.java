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
import org.bytedeco.javacpp.FloatPointer;

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
    /** @return {@code create(pointer, sizes, strides, true)} */
    public static FloatIndexer create(FloatPointer pointer, int[] sizes, int[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a float indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new float array backed by a buffer or an array
     */
    public static FloatIndexer create(final FloatPointer pointer, int[] sizes, int[] strides, boolean direct) {
        if (direct) {
            return new FloatBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final int position = pointer.position();
            float[] array = new float[pointer.limit() - position];
            pointer.get(array);
            return new FloatArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** @return {@code array/buffer[i]} */
    public abstract float get(int i);
    /** @return {@code this} where {@code f = array/buffer[i]} */
    public FloatIndexer get(int i, float[] f) { return get(i, f, 0, f.length); }
    /** @return {@code this} where {@code f[offset:offset + length] = array/buffer[i]} */
    public abstract FloatIndexer get(int i, float[] f, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract float get(int i, int j);
    /** @return {@code this} where {@code f = array/buffer[i * strides[0] + j]} */
    public FloatIndexer get(int i, int j, float[] f) { return get(i, j, f, 0, f.length); }
    /** @return {@code this} where {@code f[offset:offset + length] = array/buffer[i * strides[0] + j]} */
    public abstract FloatIndexer get(int i, int j, float[] f, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract float get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract float get(int ... indices);
    /** @return {@code this} where {@code f = array/buffer[index(indices)]} */
    public FloatIndexer get(int[] indices, float[] f) { return get(indices, f, 0, f.length); }
    /** @return {@code this} where {@code f[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract FloatIndexer get(int[] indices, float[] f, int offset, int length);

    /** @return {@code this} where {@code array/buffer[i] = f} */
    public abstract FloatIndexer put(int i, float f);
    /** @return {@code this} where {@code array/buffer[i] = f} */
    public FloatIndexer put(int i, float ... f) { return put(i, f, 0, f.length); }
    /** @return {@code this} where {@code array/buffer[i] = f[offset:offset + length]} */
    public abstract FloatIndexer put(int i, float[] f, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = f} */
    public abstract FloatIndexer put(int i, int j, float f);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = f} */
    public FloatIndexer put(int i, int j, float ... f) { return put(i, j, f, 0, f.length); }
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = f[offset:offset + length]} */
    public abstract FloatIndexer put(int i, int j, float[] f, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = f} */
    public abstract FloatIndexer put(int i, int j, int k, float f);
    /** @return {@code this} where {@code array/buffer[index(indices)] = f} */
    public abstract FloatIndexer put(int[] indices, float f);
    /** @return {@code this} where {@code array/buffer[index(indices)] = f} */
    public FloatIndexer put(int[] indices, float ... f) { return put(indices, f, 0, f.length); }
    /** @return {@code this} where {@code array/buffer[index(indices)] = f[offset:offset + length]} */
    public abstract FloatIndexer put(int[] indices, float[] f, int offset, int length);

    @Override public double getDouble(int ... indices) { return get(indices); }
}
