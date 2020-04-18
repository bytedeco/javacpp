/*
 * Copyright (C) 2020 Samuel Audet
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
 * Abstract indexer for the {@code int} primitive type, treated as unsigned.
 *
 * @author Samuel Audet
 */
public abstract class UIntIndexer extends Indexer {
    /** The number of bytes used to represent an int. */
    public static final int VALUE_BYTES = 4;

    protected UIntIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new UIntArrayIndexer(array)} */
    public static UIntIndexer create(int[] array) {
        return new UIntArrayIndexer(array);
    }
    /** Returns {@code new UIntBufferIndexer(buffer)} */
    public static UIntIndexer create(IntBuffer buffer) {
        return new UIntBufferIndexer(buffer);
    }
    /** Returns {@code create(pointer, { pointer.limit() - pointer.position() }, { 1 }, true)} */
    public static UIntIndexer create(IntPointer pointer) {
        return create(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Returns {@code new UIntArrayIndexer(array, sizes)} */
    public static UIntIndexer create(int[] array, long... sizes) {
        return new UIntArrayIndexer(array, sizes);
    }
    /** Returns {@code new UIntBufferIndexer(buffer, sizes)} */
    public static UIntIndexer create(IntBuffer buffer, long... sizes) {
        return new UIntBufferIndexer(buffer, sizes);
    }
    /** Returns {@code create(pointer, sizes, strides(sizes))} */
    public static UIntIndexer create(IntPointer pointer, long... sizes) {
        return create(pointer, sizes, strides(sizes));
    }

    /** Returns {@code new UIntArrayIndexer(array, sizes, strides)} */
    public static UIntIndexer create(int[] array, long[] sizes, long[] strides) {
        return new UIntArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new UIntBufferIndexer(buffer, sizes, strides)} */
    public static UIntIndexer create(IntBuffer buffer, long[] sizes, long[] strides) {
        return new UIntBufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code create(pointer, sizes, strides, true)} */
    public static UIntIndexer create(IntPointer pointer, long[] sizes, long[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a int indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new int indexer backed by the raw memory interface, a buffer, or an array
     */
    public static UIntIndexer create(final IntPointer pointer, long[] sizes, long[] strides, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new UIntRawIndexer(pointer, sizes, strides)
                                             : new UIntBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final long position = pointer.position();
            int[] array = new int[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new UIntArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract long get(long i);
    /** Returns {@code this} where {@code n = array/buffer[index(i)]} */
    public UIntIndexer get(long i, long[] n) { return get(i, n, 0, n.length); }
    /** Returns {@code this} where {@code n[offset:offset + length] = array/buffer[index(i)]} */
    public abstract UIntIndexer get(long i, long[] n, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract long get(long i, long j);
    /** Returns {@code this} where {@code n = array/buffer[index(i, j)]} */
    public UIntIndexer get(long i, long j, long[] n) { return get(i, j, n, 0, n.length); }
    /** Returns {@code this} where {@code n[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract UIntIndexer get(long i, long j, long[] n, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract long get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract long get(long... indices);
    /** Returns {@code this} where {@code n = array/buffer[index(indices)]} */
    public UIntIndexer get(long[] indices, long[] n) { return get(indices, n, 0, n.length); }
    /** Returns {@code this} where {@code n[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract UIntIndexer get(long[] indices, long[] n, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = n} */
    public abstract UIntIndexer put(long i, long n);
    /** Returns {@code this} where {@code array/buffer[index(i)] = n} */
    public UIntIndexer put(long i, long... n) { return put(i, n, 0, n.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = n[offset:offset + length]} */
    public abstract UIntIndexer put(long i, long[] n, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = n} */
    public abstract UIntIndexer put(long i, long j, long n);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = n} */
    public UIntIndexer put(long i, long j, long... n) { return put(i, j, n, 0, n.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = n[offset:offset + length]} */
    public abstract UIntIndexer put(long i, long j, long[] n, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = n} */
    public abstract UIntIndexer put(long i, long j, long k, long n);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = n} */
    public abstract UIntIndexer put(long[] indices, long n);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = n} */
    public UIntIndexer put(long[] indices, long... n) { return put(indices, n, 0, n.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = n[offset:offset + length]} */
    public abstract UIntIndexer put(long[] indices, long[] n, int offset, int length);

    @Override public double getDouble(long... indices) { return get(indices); }
    @Override public UIntIndexer putDouble(long[] indices, double n) { return put(indices, (int)n); }
}
