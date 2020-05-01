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

import java.nio.ByteBuffer;
import org.bytedeco.javacpp.BytePointer;

/**
 * Abstract indexer for the {@code byte} primitive type, treated as unsigned.
 *
 * @author Samuel Audet
 */
public abstract class UByteIndexer extends Indexer {
    /** The number of bytes used to represent a byte. */
    public static final int VALUE_BYTES = 1;

    protected UByteIndexer(Index index) {
        super(index);
    }

    protected UByteIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new UByteArrayIndexer(array)} */
    public static UByteIndexer create(byte[] array) {
        return new UByteArrayIndexer(array);
    }
    /** Returns {@code new UByteBufferIndexer(buffer)} */
    public static UByteIndexer create(ByteBuffer buffer) {
        return new UByteBufferIndexer(buffer);
    }
    /** Returns {@code new UByteRawIndexer(pointer)} */
    public static UByteIndexer create(BytePointer pointer) {
        return new UByteRawIndexer(pointer);
    }

    /** Returns {@code new UByteArrayIndexer(array, index)} */
    public static UByteIndexer create(byte[] array, Index index) {
        return new UByteArrayIndexer(array, index);
    }
    /** Returns {@code new UByteBufferIndexer(buffer, index)} */
    public static UByteIndexer create(ByteBuffer buffer, Index index) {
        return new UByteBufferIndexer(buffer, index);
    }
    /** Returns {@code new UByteRawIndexer(pointer, index)} */
    public static UByteIndexer create(BytePointer pointer, Index index) {
        return new UByteRawIndexer(pointer, index);
    }

    /** Returns {@code new UByteArrayIndexer(array, sizes)} */
    public static UByteIndexer create(byte[] array, long... sizes) {
        return new UByteArrayIndexer(array, sizes);
    }
    /** Returns {@code new UByteBufferIndexer(buffer, sizes)} */
    public static UByteIndexer create(ByteBuffer buffer, long... sizes) {
        return new UByteBufferIndexer(buffer, sizes);
    }
    /** Returns {@code new UByteRawIndexer(pointer, index)} */
    public static UByteIndexer create(BytePointer pointer, long... sizes) {
        return new UByteRawIndexer(pointer, sizes);
    }

    /** Returns {@code new ByteArrayIndexer(array, sizes, strides)} */
    public static UByteIndexer create(byte[] array, long[] sizes, long[] strides) {
        return new UByteArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new ByteBufferIndexer(buffer, sizes, strides)} */
    public static UByteIndexer create(ByteBuffer buffer, long[] sizes, long[] strides) {
        return new UByteBufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code new UByteRawIndexer(pointer, sizes, strides)} */
    public static UByteIndexer create(BytePointer pointer, long[] sizes, long[] strides) {
        return new UByteRawIndexer(pointer, sizes, strides);
    }
    /** Returns {@code create(pointer, Index.create(sizes, strides), direct)} */
    public static UByteIndexer create(final BytePointer pointer, long[] sizes, long[] strides, boolean direct) {
        return create(pointer, Index.create(sizes, strides), direct);
    }
    /**
     * Creates a byte indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param index to use
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new byte indexer backed by the raw memory interface, a buffer, or an array
     */
    public static UByteIndexer create(final BytePointer pointer, Index index, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new UByteRawIndexer(pointer, index)
                                             : new UByteBufferIndexer(pointer.asBuffer(), index);
        } else {
            final long position = pointer.position();
            byte[] array = new byte[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new UByteArrayIndexer(array, index) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract int get(long i);
    /** Returns {@code this} where {@code b = array/buffer[index(i)]} */
    public UByteIndexer get(long i, int[] b) { return get(i, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(i)]} */
    public abstract UByteIndexer get(long i, int[] b, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract int get(long i, long j);
    /** Returns {@code this} where {@code b = array/buffer[index(i, j)]} */
    public UByteIndexer get(long i, long j, int[] b) { return get(i, j, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract UByteIndexer get(long i, long j, int[] b, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract int get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract int get(long... indices);
    /** Returns {@code this} where {@code b = array/buffer[index(indices)]} */
    public UByteIndexer get(long[] indices, int[] b) { return get(indices, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract UByteIndexer get(long[] indices, int[] b, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = b} */
    public abstract UByteIndexer put(long i, int b);
    /** Returns {@code this} where {@code array/buffer[index(i)] = b} */
    public UByteIndexer put(long i, int... b) { return put(i, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = b[offset:offset + length]} */
    public abstract UByteIndexer put(long i, int[] b, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b} */
    public abstract UByteIndexer put(long i, long j, int b);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b} */
    public UByteIndexer put(long i, long j, int... b) { return put(i, j, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b[offset:offset + length]} */
    public abstract UByteIndexer put(long i, long j, int[] b, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = b} */
    public abstract UByteIndexer put(long i, long j, long k, int b);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b} */
    public abstract UByteIndexer put(long[] indices, int b);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b} */
    public UByteIndexer put(long[] indices, int... b) { return put(indices, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b[offset:offset + length]} */
    public abstract UByteIndexer put(long[] indices, int[] b, int offset, int length);

    @Override public double getDouble(long... indices) { return get(indices); }
    @Override public UByteIndexer putDouble(long[] indices, double b) { return put(indices, (int)b); }
}
