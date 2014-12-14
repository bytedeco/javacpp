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
import org.bytedeco.javacpp.LongPointer;

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
    /** @return {@code create(pointer, sizes, strides, true)} */
    public static LongIndexer create(LongPointer pointer, int[] sizes, int[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a long indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new long array backed by a buffer or an array
     */
    public static LongIndexer create(final LongPointer pointer, int[] sizes, int[] strides, boolean direct) {
        if (direct) {
            return new LongBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final int position = pointer.position();
            long[] array = new long[pointer.limit() - position];
            pointer.get(array);
            return new LongArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** @return {@code array/buffer[i]} */
    public abstract long get(int i);
    /** @return {@code this} where {@code l = array/buffer[i]} */
    public LongIndexer get(int i, long[] l) { return get(i, l, 0, l.length); }
    /** @return {@code this} where {@code l[offset:offset + length] = array/buffer[i]} */
    public abstract LongIndexer get(int i, long[] l, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract long get(int i, int j);
    /** @return {@code this} where {@code l = array/buffer[i * strides[0] + j]} */
    public LongIndexer get(int i, int j, long[] l) { return get(i, j, l, 0, l.length); }
    /** @return {@code this} where {@code l[offset:offset + length] = array/buffer[i * strides[0] + j]} */
    public abstract LongIndexer get(int i, int j, long[] l, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract long get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract long get(int ... indices);
    /** @return {@code this} where {@code l = array/buffer[index(indices)]} */
    public LongIndexer get(int[] indices, long[] l) { return get(indices, l, 0, l.length); }
    /** @return {@code this} where {@code l[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract LongIndexer get(int[] indices, long[] l, int offset, int length);

    /** @return {@code this} where {@code array/buffer[i] = l} */
    public abstract LongIndexer put(int i, long l);
    /** @return {@code this} where {@code array/buffer[i] = l} */
    public LongIndexer put(int i, long ... l) { return put(i, l, 0, l.length); }
    /** @return {@code this} where {@code array/buffer[i] = l[offset:offset + length]} */
    public abstract LongIndexer put(int i, long[] l, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = l} */
    public abstract LongIndexer put(int i, int j, long l);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = l} */
    public LongIndexer put(int i, int j, long ... l) { return put(i, j, l, 0, l.length); }
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = l[offset:offset + length]} */
    public abstract LongIndexer put(int i, int j, long[] l, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = l} */
    public abstract LongIndexer put(int i, int j, int k, long l);
    /** @return {@code this} where {@code array/buffer[index(indices)] = l} */
    public abstract LongIndexer put(int[] indices, long l);
    /** @return {@code this} where {@code array/buffer[index(indices)] = l} */
    public LongIndexer put(int[] indices, long ... l) { return put(indices, l, 0, l.length); }
    /** @return {@code this} where {@code array/buffer[index(indices)] = l[offset:offset + length]} */
    public abstract LongIndexer put(int[] indices, long[] l, int offset, int length);

    @Override public double getDouble(int ... indices) { return get(indices); }
}
