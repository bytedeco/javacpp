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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.LongBuffer;
import org.bytedeco.javacpp.LongPointer;

/**
 * Abstract indexer for the {@code long} primitive type, treated as unsigned.
 *
 * @author Samuel Audet
 */
public abstract class ULongIndexer extends Indexer {
    /** The number of bytes used to represent a long. */
    public static final int VALUE_BYTES = 8;

    protected ULongIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new ULongArrayIndexer(array)} */
    public static ULongIndexer create(long[] array) {
        return new ULongArrayIndexer(array);
    }
    /** Returns {@code new ULongBufferIndexer(buffer)} */
    public static ULongIndexer create(LongBuffer buffer) {
        return new ULongBufferIndexer(buffer);
    }
    /** Returns {@code create(pointer, { pointer.limit() - pointer.position() }, { 1 }, true)} */
    public static ULongIndexer create(LongPointer pointer) {
        return create(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Returns {@code new ULongArrayIndexer(array, sizes)} */
    public static ULongIndexer create(long[] array, long... sizes) {
        return new ULongArrayIndexer(array, sizes);
    }
    /** Returns {@code new ULongBufferIndexer(buffer, sizes)} */
    public static ULongIndexer create(LongBuffer buffer, long... sizes) {
        return new ULongBufferIndexer(buffer, sizes);
    }
    /** Returns {@code create(pointer, sizes, strides(sizes))} */
    public static ULongIndexer create(LongPointer pointer, long... sizes) {
        return create(pointer, sizes, strides(sizes));
    }

    /** Returns {@code new ULongArrayIndexer(array, sizes, strides)} */
    public static ULongIndexer create(long[] array, long[] sizes, long[] strides) {
        return new ULongArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new ULongBufferIndexer(buffer, sizes, strides)} */
    public static ULongIndexer create(LongBuffer buffer, long[] sizes, long[] strides) {
        return new ULongBufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code create(pointer, sizes, strides, true)} */
    public static ULongIndexer create(LongPointer pointer, long[] sizes, long[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a long indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new long indexer backed by the raw memory interface, a buffer, or an array
     */
    public static ULongIndexer create(final LongPointer pointer, long[] sizes, long[] strides, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new ULongRawIndexer(pointer, sizes, strides)
                                             : new ULongBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final long position = pointer.position();
            long[] array = new long[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new ULongArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    public static BigInteger toBigInteger(long l) {
        BigInteger bi = BigInteger.valueOf(l & 0x7FFFFFFFFFFFFFFFL);
        if (l < 0) {
            bi = bi.setBit(63);
        }
        return bi;
    }

    public static long fromBigInteger(BigInteger l) {
        return l.longValue();
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract BigInteger get(long i);
    /** Returns {@code this} where {@code l = array/buffer[index(i)]} */
    public ULongIndexer get(long i, BigInteger[] l) { return get(i, l, 0, l.length); }
    /** Returns {@code this} where {@code l[offset:offset + length] = array/buffer[index(i)]} */
    public abstract ULongIndexer get(long i, BigInteger[] l, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract BigInteger get(long i, long j);
    /** Returns {@code this} where {@code l = array/buffer[index(i, j)]} */
    public ULongIndexer get(long i, long j, BigInteger[] l) { return get(i, j, l, 0, l.length); }
    /** Returns {@code this} where {@code l[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract ULongIndexer get(long i, long j, BigInteger[] l, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract BigInteger get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract BigInteger get(long... indices);
    /** Returns {@code this} where {@code l = array/buffer[index(indices)]} */
    public ULongIndexer get(long[] indices, BigInteger[] l) { return get(indices, l, 0, l.length); }
    /** Returns {@code this} where {@code l[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract ULongIndexer get(long[] indices, BigInteger[] l, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = l} */
    public abstract ULongIndexer put(long i, BigInteger l);
    /** Returns {@code this} where {@code array/buffer[index(i)] = l} */
    public ULongIndexer put(long i, BigInteger... l) { return put(i, l, 0, l.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = l[offset:offset + length]} */
    public abstract ULongIndexer put(long i, BigInteger[] l, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = l} */
    public abstract ULongIndexer put(long i, long j, BigInteger l);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = l} */
    public ULongIndexer put(long i, long j, BigInteger... l) { return put(i, j, l, 0, l.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = l[offset:offset + length]} */
    public abstract ULongIndexer put(long i, long j, BigInteger[] l, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = l} */
    public abstract ULongIndexer put(long i, long j, long k, BigInteger l);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = l} */
    public abstract ULongIndexer put(long[] indices, BigInteger l);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = l} */
    public ULongIndexer put(long[] indices, BigInteger... l) { return put(indices, l, 0, l.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = l[offset:offset + length]} */
    public abstract ULongIndexer put(long[] indices, BigInteger[] l, int offset, int length);

    @Override public double getDouble(long... indices) { return get(indices).doubleValue(); }
    @Override public ULongIndexer putDouble(long[] indices, double l) { return put(indices, BigDecimal.valueOf(l).toBigInteger()); }
}
