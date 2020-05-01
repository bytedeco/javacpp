/*
 * Copyright (C) 2014-2019 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bytedeco.javacpp.indexer;

import java.nio.IntBuffer;
import org.bytedeco.javacpp.IntPointer;

/**
 * Abstract indexer for the {@code int} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class IntIndexer extends Indexer {
    /** The number of bytes used to represent an int. */
    public static final int VALUE_BYTES = 4;

    protected IntIndexer(Index index) {
        super(index);
    }

    protected IntIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new IntArrayIndexer(array)} */
    public static IntIndexer create(int[] array) {
        return new IntArrayIndexer(array);
    }
    /** Returns {@code new IntBufferIndexer(buffer)} */
    public static IntIndexer create(IntBuffer buffer) {
        return new IntBufferIndexer(buffer);
    }
    /** Returns {@code new IntRawIndexer(pointer)} */
    public static IntIndexer create(IntPointer pointer) {
        return new IntRawIndexer(pointer);
    }

    /** Returns {@code new IntArrayIndexer(array, index)} */
    public static IntIndexer create(int[] array, Index index) {
        return new IntArrayIndexer(array, index);
    }
    /** Returns {@code new IntBufferIndexer(buffer, index)} */
    public static IntIndexer create(IntBuffer buffer, Index index) {
        return new IntBufferIndexer(buffer, index);
    }
    /** Returns {@code new IntRawIndexer(pointer, index)} */
    public static IntIndexer create(IntPointer pointer, Index index) {
        return new IntRawIndexer(pointer, index);
    }

    /** Returns {@code new IntArrayIndexer(array, sizes)} */
    public static IntIndexer create(int[] array, long... sizes) {
        return new IntArrayIndexer(array, sizes);
    }
    /** Returns {@code new IntBufferIndexer(buffer, sizes)} */
    public static IntIndexer create(IntBuffer buffer, long... sizes) {
        return new IntBufferIndexer(buffer, sizes);
    }
    /** Returns {@code new IntRawIndexer(pointer, sizes)} */
    public static IntIndexer create(IntPointer pointer, long... sizes) {
        return new IntRawIndexer(pointer, sizes);
    }

    /** Returns {@code new IntArrayIndexer(array, sizes, strides)} */
    public static IntIndexer create(int[] array, long[] sizes, long[] strides) {
        return new IntArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new IntBufferIndexer(buffer, sizes, strides)} */
    public static IntIndexer create(IntBuffer buffer, long[] sizes, long[] strides) {
        return new IntBufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code new IntRawIndexer(pointer, sizes, strides)} */
    public static IntIndexer create(IntPointer pointer, long[] sizes, long[] strides) {
        return new IntRawIndexer(pointer, sizes, strides);
    }
    /** Returns {@code create(pointer, Index.create(sizes, strides), direct)} */
    public static IntIndexer create(final IntPointer pointer, long[] sizes, long[] strides, boolean direct) {
        return create(pointer, Index.create(sizes, strides), direct);
    }
    /**
     * Creates a int indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param index to use
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new int indexer backed by the raw memory interface, a buffer, or an array
     */
    public static IntIndexer create(final IntPointer pointer, Index index, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new IntRawIndexer(pointer, index)
                                             : new IntBufferIndexer(pointer.asBuffer(), index);
        } else {
            final long position = pointer.position();
            int[] array = new int[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new IntArrayIndexer(array, index) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract int get(long i);
    /** Returns {@code this} where {@code n = array/buffer[index(i)]} */
    public IntIndexer get(long i, int[] n) { return get(i, n, 0, n.length); }
    /** Returns {@code this} where {@code n[offset:offset + length] = array/buffer[index(i)]} */
    public abstract IntIndexer get(long i, int[] n, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract int get(long i, long j);
    /** Returns {@code this} where {@code n = array/buffer[index(i, j)]} */
    public IntIndexer get(long i, long j, int[] n) { return get(i, j, n, 0, n.length); }
    /** Returns {@code this} where {@code n[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract IntIndexer get(long i, long j, int[] n, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract int get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract int get(long... indices);
    /** Returns {@code this} where {@code n = array/buffer[index(indices)]} */
    public IntIndexer get(long[] indices, int[] n) { return get(indices, n, 0, n.length); }
    /** Returns {@code this} where {@code n[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract IntIndexer get(long[] indices, int[] n, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = n} */
    public abstract IntIndexer put(long i, int n);
    /** Returns {@code this} where {@code array/buffer[index(i)] = n} */
    public IntIndexer put(long i, int... n) { return put(i, n, 0, n.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = n[offset:offset + length]} */
    public abstract IntIndexer put(long i, int[] n, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = n} */
    public abstract IntIndexer put(long i, long j, int n);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = n} */
    public IntIndexer put(long i, long j, int... n) { return put(i, j, n, 0, n.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = n[offset:offset + length]} */
    public abstract IntIndexer put(long i, long j, int[] n, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = n} */
    public abstract IntIndexer put(long i, long j, long k, int n);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = n} */
    public abstract IntIndexer put(long[] indices, int n);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = n} */
    public IntIndexer put(long[] indices, int... n) { return put(indices, n, 0, n.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = n[offset:offset + length]} */
    public abstract IntIndexer put(long[] indices, int[] n, int offset, int length);

    @Override public double getDouble(long... indices) { return get(indices); }
    @Override public IntIndexer putDouble(long[] indices, double n) { return put(indices, (int)n); }
}
