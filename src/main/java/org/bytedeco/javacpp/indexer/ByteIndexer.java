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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.bytedeco.javacpp.BytePointer;

/**
 * Abstract indexer for the {@code byte} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class ByteIndexer extends Indexer {
    /** The number of bytes used to represent a byte. */
    public static final int VALUE_BYTES = 1;

    protected ByteIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new ByteArrayIndexer(array)} */
    public static ByteIndexer create(byte[] array) {
        return new ByteArrayIndexer(array);
    }
    /** Returns {@code new ByteBufferIndexer(buffer)} */
    public static ByteIndexer create(ByteBuffer buffer) {
        return new ByteBufferIndexer(buffer);
    }
    /** Returns {@code create(pointer, { pointer.limit() - pointer.position() }, { 1 }, true)} */
    public static ByteIndexer create(BytePointer pointer) {
        return create(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Returns {@code new ByteArrayIndexer(array, sizes)} */
    public static ByteIndexer create(byte[] array, long... sizes) {
        return new ByteArrayIndexer(array, sizes);
    }
    /** Returns {@code new ByteBufferIndexer(buffer, sizes)} */
    public static ByteIndexer create(ByteBuffer buffer, long... sizes) {
        return new ByteBufferIndexer(buffer, sizes);
    }
    /** Returns {@code create(pointer, sizes, strides(sizes))} */
    public static ByteIndexer create(BytePointer pointer, long... sizes) {
        return create(pointer, sizes, strides(sizes));
    }

    /** Returns {@code new ByteArrayIndexer(array, sizes, strides)} */
    public static ByteIndexer create(byte[] array, long[] sizes, long[] strides) {
        return new ByteArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new ByteBufferIndexer(buffer, sizes, strides)} */
    public static ByteIndexer create(ByteBuffer buffer, long[] sizes, long[] strides) {
        return new ByteBufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code create(pointer, sizes, strides, true)} */
    public static ByteIndexer create(BytePointer pointer, long[] sizes, long[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a byte indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new byte indexer backed by the raw memory interface, a buffer, or an array
     */
    public static ByteIndexer create(final BytePointer pointer, long[] sizes, long[] strides, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new ByteRawIndexer(pointer, sizes, strides)
                                             : new ByteBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final long position = pointer.position();
            byte[] array = new byte[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new ByteArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract byte get(long i);
    /** Returns {@code this} where {@code b = array/buffer[index(i)]} */
    public ByteIndexer get(long i, byte[] b) { return get(i, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(i)]} */
    public abstract ByteIndexer get(long i, byte[] b, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract byte get(long i, long j);
    /** Returns {@code this} where {@code b = array/buffer[index(i, j)]} */
    public ByteIndexer get(long i, long j, byte[] b) { return get(i, j, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract ByteIndexer get(long i, long j, byte[] b, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract byte get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract byte get(long... indices);
    /** Returns {@code this} where {@code b = array/buffer[index(indices)]} */
    public ByteIndexer get(long[] indices, byte[] b) { return get(indices, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract ByteIndexer get(long[] indices, byte[] b, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = b} */
    public abstract ByteIndexer put(long i, byte b);
    /** Returns {@code this} where {@code array/buffer[index(i)] = b} */
    public ByteIndexer put(long i, byte... b) { return put(i, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = b[offset:offset + length]} */
    public abstract ByteIndexer put(long i, byte[] b, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b} */
    public abstract ByteIndexer put(long i, long j, byte b);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b} */
    public ByteIndexer put(long i, long j, byte... b) { return put(i, j, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b[offset:offset + length]} */
    public abstract ByteIndexer put(long i, long j, byte[] b, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = b} */
    public abstract ByteIndexer put(long i, long j, long k, byte b);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b} */
    public abstract ByteIndexer put(long[] indices, byte b);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b} */
    public ByteIndexer put(long[] indices, byte... b) { return put(indices, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b[offset:offset + length]} */
    public abstract ByteIndexer put(long[] indices, byte[] b, int offset, int length);

    /** Returns the {@code byte} value at {@code array/buffer[i]} */
    public abstract byte getByte(long i);
    /** Sets the {@code byte} value at {@code array/buffer[i]} */
    public abstract ByteIndexer putByte(long i, byte b);

    /** Returns the {@code short} value at {@code array/buffer[i]} */
    public abstract short getShort(long i);
    /** Sets the {@code short} value at {@code array/buffer[i]} */
    public abstract ByteIndexer putShort(long i, short s);

    /** Returns the {@code int} value at {@code array/buffer[i]} */
    public abstract int getInt(long i);
    /** Sets the {@code int} value at {@code array/buffer[i]} */
    public abstract ByteIndexer putInt(long i, int j);

    /** Returns the {@code long} value at {@code array/buffer[i]} */
    public abstract long getLong(long i);
    /** Sets the {@code long} value at {@code array/buffer[i]} */
    public abstract ByteIndexer putLong(long i, long j);

    /** Returns the {@code float} value at {@code array/buffer[i]} */
    public abstract float getFloat(long i);
    /** Sets the {@code float} value at {@code array/buffer[i]} */
    public abstract ByteIndexer putFloat(long i, float f);

    /** Returns the {@code double} value at {@code array/buffer[i]} */
    public abstract double getDouble(long i);
    /** Sets the {@code double} value at {@code array/buffer[i]} */
    public abstract ByteIndexer putDouble(long i, double d);

    /** Returns the {@code char} value at {@code array/buffer[i]} */
    public abstract char getChar(long i);
    /** Sets the {@code char} value at {@code array/buffer[i]} */
    public abstract ByteIndexer putChar(long i, char c);

    /** Returns the {@code byte} value at {@code array/buffer[i]}, treated as unsigned */
    public int getUByte(long i) { return getByte(i) & 0xFF; }
    /** Sets the {@code byte} value at {@code array/buffer[i]}, treated as unsigned */
    public ByteIndexer putUByte(long i, int b) { return putByte(i, (byte)b); }

    /** Returns the {@code short} value at {@code array/buffer[i]}, treated as unsigned */
    public int getUShort(long i) { return getShort(i) & 0xFFFF; }
    /** Sets the {@code short} value at {@code array/buffer[i]}, treated as unsigned */
    public ByteIndexer putUShort(long i, int s) { return putShort(i, (short)s); }

    /** Returns the {@code int} value at {@code array/buffer[i]}, treated as unsigned */
    public long getUInt(long i) { return getInt(i) & 0xFFFFFFFFL; }
    /** Sets the {@code int} value at {@code array/buffer[i]}, treated as unsigned */
    public ByteIndexer putUInt(long i, long n) { return putInt(i, (int)n); }

    /** Returns the {@code long} value at {@code array/buffer[i]}, treated as unsigned */
    public BigInteger getULong(long i) { return ULongIndexer.toBigInteger(getLong(i)); }
    /** Sets the {@code long} value at {@code array/buffer[i]}, treated as unsigned */
    public ByteIndexer putULong(long i, BigInteger l) { return putLong(i, ULongIndexer.fromBigInteger(l)); }

    /** Returns the {@code short} value at {@code array/buffer[i]}, treated as half-precision float */
    public float getHalf(long i) { return HalfIndexer.toFloat(getShort(i)); }
    /** Sets the {@code short} value at {@code array/buffer[i]}, treated as half-precision float */
    public ByteIndexer putHalf(long i, float h) { return putShort(i, (short)HalfIndexer.fromFloat(h)); }

    /** Returns the {@code short} value at {@code array/buffer[i]}, treated as bfloat16 */
    public float getBfloat16(long i) { return Bfloat16Indexer.toFloat(getShort(i)); }
    /** Sets the {@code short} value at {@code array/buffer[i]}, treated as bfloat16 */
    public ByteIndexer putBfloat16(long i, float h) { return putShort(i, (short)Bfloat16Indexer.fromFloat(h)); }

    /** Returns the {@code boolean} value at {@code array/buffer[i]} */
    public boolean getBoolean(long i) { return get(i) != 0; }
    /** Sets the {@code boolean} value at {@code array/buffer[i]} */
    public ByteIndexer putBoolean(long i, boolean b) { return put(i, b ? (byte)1 : (byte)0); }

    @Override public double getDouble(long... indices) { return get(indices); }
    @Override public ByteIndexer putDouble(long[] indices, double b) { return put(indices, (byte)b); }
}
