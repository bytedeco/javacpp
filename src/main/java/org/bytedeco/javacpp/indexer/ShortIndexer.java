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
import org.bytedeco.javacpp.ShortPointer;

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
    /** @return {@code create(pointer, sizes, strides, true)} */
    public static ShortIndexer create(ShortPointer pointer, int[] sizes, int[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a short indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new short array backed by a buffer or an array
     */
    public static ShortIndexer create(final ShortPointer pointer, int[] sizes, int[] strides, boolean direct) {
        if (direct) {
            return new ShortBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final int position = pointer.position();
            short[] array = new short[pointer.limit() - position];
            pointer.get(array);
            return new ShortArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** @return {@code array/buffer[i]} */
    public abstract short get(int i);
    /** @return {@code this} where {@code s = array/buffer[i]} */
    public ShortIndexer get(int i, short[] s) { return get(i, s, 0, s.length); }
    /** @return {@code this} where {@code s[offset:offset + length] = array/buffer[i]} */
    public abstract ShortIndexer get(int i, short[] s, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract short get(int i, int j);
    /** @return {@code this} where {@code s = array/buffer[i * strides[0] + j]} */
    public ShortIndexer get(int i, int j, short[] s) { return get(i, j, s, 0, s.length); }
    /** @return {@code this} where {@code s[offset:offset + length] = array/buffer[i * strides[0] + j]} */
    public abstract ShortIndexer get(int i, int j, short[] s, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract short get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract short get(int ... indices);
    /** @return {@code this} where {@code s = array/buffer[index(indices)]} */
    public ShortIndexer get(int[] indices, short[] s) { return get(indices, s, 0, s.length); }
    /** @return {@code this} where {@code s[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract ShortIndexer get(int[] indices, short[] s, int offset, int length);

    /** @return {@code this} where {@code array/buffer[i] = s} */
    public abstract ShortIndexer put(int i, short s);
    /** @return {@code this} where {@code array/buffer[i] = s} */
    public ShortIndexer put(int i, short ... s) { return put(i, s, 0, s.length); }
    /** @return {@code this} where {@code array/buffer[i] = s[offset:offset + length]} */
    public abstract ShortIndexer put(int i, short[] s, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = s} */
    public abstract ShortIndexer put(int i, int j, short s);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = s} */
    public ShortIndexer put(int i, int j, short ... s) { return put(i, j, s, 0, s.length); }
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = s[offset:offset + length]} */
    public abstract ShortIndexer put(int i, int j, short[] s, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = s} */
    public abstract ShortIndexer put(int i, int j, int k, short s);
    /** @return {@code this} where {@code array/buffer[index(indices)] = s} */
    public abstract ShortIndexer put(int[] indices, short s);
    /** @return {@code this} where {@code array/buffer[index(indices)] = s} */
    public ShortIndexer put(int[] indices, short ... s) { return put(indices, s, 0, s.length); }
    /** @return {@code this} where {@code array/buffer[index(indices)] = s[offset:offset + length]} */
    public abstract ShortIndexer put(int[] indices, short[] s, int offset, int length);

    @Override public double getDouble(int ... indices) { return get(indices); }
}
