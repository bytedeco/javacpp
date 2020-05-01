/*
 * Copyright (C) 2015-2019 Samuel Audet
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

import java.nio.ShortBuffer;
import org.bytedeco.javacpp.ShortPointer;

/**
 * Abstract indexer for the {@code short} primitive type, treated as unsigned.
 *
 * @author Samuel Audet
 */
public abstract class UShortIndexer extends Indexer {
    /** The number of bytes used to represent a short. */
    public static final int VALUE_BYTES = 2;

    protected UShortIndexer(Index index) {
        super(index);
    }

    protected UShortIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new UShortArrayIndexer(array)} */
    public static UShortIndexer create(short[] array) {
        return new UShortArrayIndexer(array);
    }
    /** Returns {@code new UShortBufferIndexer(buffer)} */
    public static UShortIndexer create(ShortBuffer buffer) {
        return new UShortBufferIndexer(buffer);
    }
    /** Returns {@code new UShortRawIndexer(pointer)} */
    public static UShortIndexer create(ShortPointer pointer) {
        return new UShortRawIndexer(pointer);
    }

    /** Returns {@code new UShortArrayIndexer(array, index)} */
    public static UShortIndexer create(short[] array, Index index) {
        return new UShortArrayIndexer(array, index);
    }
    /** Returns {@code new UShortBufferIndexer(buffer, index)} */
    public static UShortIndexer create(ShortBuffer buffer, Index index) {
        return new UShortBufferIndexer(buffer, index);
    }
    /** Returns {@code new UShortRawIndexer(pointer, index)} */
    public static UShortIndexer create(ShortPointer pointer, Index index) {
        return new UShortRawIndexer(pointer, index);
    }

    /** Returns {@code new UShortArrayIndexer(array, sizes)} */
    public static UShortIndexer create(short[] array, long... sizes) {
        return new UShortArrayIndexer(array, sizes);
    }
    /** Returns {@code new UShortBufferIndexer(buffer, sizes)} */
    public static UShortIndexer create(ShortBuffer buffer, long... sizes) {
        return new UShortBufferIndexer(buffer, sizes);
    }
    /** Returns {@code new UShortRawIndexer(pointer, sizes)} */
    public static UShortIndexer create(ShortPointer pointer, long... sizes) {
        return new UShortRawIndexer(pointer, sizes);
    }

    /** Returns {@code new UShortArrayIndexer(array, sizes, strides)} */
    public static UShortIndexer create(short[] array, long[] sizes, long[] strides) {
        return new UShortArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new UShortBufferIndexer(buffer, sizes, strides)} */
    public static UShortIndexer create(ShortBuffer buffer, long[] sizes, long[] strides) {
        return new UShortBufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code new UShortRawIndexer(pointer, sizes, strides)} */
    public static UShortIndexer create(ShortPointer pointer, long[] sizes, long[] strides) {
        return new UShortRawIndexer(pointer, sizes, strides);
    }
    /** Returns {@code create(pointer, Index.create(sizes, strides), direct)} */
    public static UShortIndexer create(final ShortPointer pointer, long[] sizes, long[] strides, boolean direct) {
        return create(pointer, Index.create(sizes, strides), direct);
    }
    /**
     * Creates a short indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param index to use
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new short indexer backed by the raw memory interface, a buffer, or an array
     */
    public static UShortIndexer create(final ShortPointer pointer, Index index, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new UShortRawIndexer(pointer, index)
                                             : new UShortBufferIndexer(pointer.asBuffer(), index);
        } else {
            final long position = pointer.position();
            short[] array = new short[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new UShortArrayIndexer(array, index) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract int get(long i);
    /** Returns {@code this} where {@code s = array/buffer[index(i)]} */
    public UShortIndexer get(long i, int[] s) { return get(i, s, 0, s.length); }
    /** Returns {@code this} where {@code s[offset:offset + length] = array/buffer[index(i)]} */
    public abstract UShortIndexer get(long i, int[] s, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract int get(long i, long j);
    /** Returns {@code this} where {@code s = array/buffer[index(i, j)]} */
    public UShortIndexer get(long i, long j, int[] s) { return get(i, j, s, 0, s.length); }
    /** Returns {@code this} where {@code s[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract UShortIndexer get(long i, long j, int[] s, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract int get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract int get(long... indices);
    /** Returns {@code this} where {@code s = array/buffer[index(indices)]} */
    public UShortIndexer get(long[] indices, int[] s) { return get(indices, s, 0, s.length); }
    /** Returns {@code this} where {@code s[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract UShortIndexer get(long[] indices, int[] s, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = s} */
    public abstract UShortIndexer put(long i, int s);
    /** Returns {@code this} where {@code array/buffer[index(i)] = s} */
    public UShortIndexer put(long i, int... s) { return put(i, s, 0, s.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = s[offset:offset + length]} */
    public abstract UShortIndexer put(long i, int[] s, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = s} */
    public abstract UShortIndexer put(long i, long j, int s);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = s} */
    public UShortIndexer put(long i, long j, int... s) { return put(i, j, s, 0, s.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = s[offset:offset + length]} */
    public abstract UShortIndexer put(long i, long j, int[] s, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = s} */
    public abstract UShortIndexer put(long i, long j, long k, int s);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = s} */
    public abstract UShortIndexer put(long[] indices, int s);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = s} */
    public UShortIndexer put(long[] indices, int... s) { return put(indices, s, 0, s.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = s[offset:offset + length]} */
    public abstract UShortIndexer put(long[] indices, int[] s, int offset, int length);

    @Override public double getDouble(long... indices) { return get(indices); }
    @Override public UShortIndexer putDouble(long[] indices, double s) { return put(indices, (int)s); }
}
