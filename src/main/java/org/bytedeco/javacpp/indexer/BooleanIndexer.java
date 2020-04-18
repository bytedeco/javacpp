/*
 * Copyright (C) 2018-2019 Samuel Audet
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
import org.bytedeco.javacpp.BooleanPointer;

/**
 * Abstract indexer for the {@code boolean} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class BooleanIndexer extends Indexer {
    /** The number of bytes used to represent a boolean. */
    public static final int VALUE_BYTES = 1;

    protected BooleanIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new BooleanArrayIndexer(array)} */
    public static BooleanIndexer create(boolean[] array) {
        return new BooleanArrayIndexer(array);
    }
    /** Returns {@code new BooleanBufferIndexer(buffer)} */
    public static BooleanIndexer create(ByteBuffer buffer) {
        return new BooleanBufferIndexer(buffer);
    }
    /** Returns {@code create(pointer, { pointer.limit() - pointer.position() }, { 1 }, true)} */
    public static BooleanIndexer create(BooleanPointer pointer) {
        return create(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Returns {@code new BooleanArrayIndexer(array, sizes)} */
    public static BooleanIndexer create(boolean[] array, long... sizes) {
        return new BooleanArrayIndexer(array, sizes);
    }
    /** Returns {@code new BooleanBufferIndexer(buffer, sizes)} */
    public static BooleanIndexer create(ByteBuffer buffer, long... sizes) {
        return new BooleanBufferIndexer(buffer, sizes);
    }
    /** Returns {@code create(pointer, sizes, strides(sizes))} */
    public static BooleanIndexer create(BooleanPointer pointer, long... sizes) {
        return create(pointer, sizes, strides(sizes));
    }

    /** Returns {@code new BooleanArrayIndexer(array, sizes, strides)} */
    public static BooleanIndexer create(boolean[] array, long[] sizes, long[] strides) {
        return new BooleanArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new BooleanBufferIndexer(buffer, sizes, strides)} */
    public static BooleanIndexer create(ByteBuffer buffer, long[] sizes, long[] strides) {
        return new BooleanBufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code create(pointer, sizes, strides, true)} */
    public static BooleanIndexer create(BooleanPointer pointer, long[] sizes, long[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a boolean indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new boolean indexer backed by the raw memory interface, a buffer, or an array
     */
    public static BooleanIndexer create(final BooleanPointer pointer, long[] sizes, long[] strides, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new BooleanRawIndexer(pointer, sizes, strides)
                                             : new BooleanBufferIndexer(pointer.asByteBuffer(), sizes, strides);
        } else {
            final long position = pointer.position();
            boolean[] array = new boolean[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new BooleanArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract boolean get(long i);
    /** Returns {@code this} where {@code b = array/buffer[index(i)]} */
    public BooleanIndexer get(long i, boolean[] b) { return get(i, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(i)]} */
    public abstract BooleanIndexer get(long i, boolean[] b, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract boolean get(long i, long j);
    /** Returns {@code this} where {@code b = array/buffer[index(i, j)]} */
    public BooleanIndexer get(long i, long j, boolean[] b) { return get(i, j, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract BooleanIndexer get(long i, long j, boolean[] b, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract boolean get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract boolean get(long... indices);
    /** Returns {@code this} where {@code b = array/buffer[index(indices)]} */
    public BooleanIndexer get(long[] indices, boolean[] b) { return get(indices, b, 0, b.length); }
    /** Returns {@code this} where {@code b[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract BooleanIndexer get(long[] indices, boolean[] b, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = b} */
    public abstract BooleanIndexer put(long i, boolean b);
    /** Returns {@code this} where {@code array/buffer[index(i)] = b} */
    public BooleanIndexer put(long i, boolean... b) { return put(i, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = b[offset:offset + length]} */
    public abstract BooleanIndexer put(long i, boolean[] b, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b} */
    public abstract BooleanIndexer put(long i, long j, boolean b);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b} */
    public BooleanIndexer put(long i, long j, boolean... b) { return put(i, j, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = b[offset:offset + length]} */
    public abstract BooleanIndexer put(long i, long j, boolean[] b, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = b} */
    public abstract BooleanIndexer put(long i, long j, long k, boolean b);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b} */
    public abstract BooleanIndexer put(long[] indices, boolean b);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b} */
    public BooleanIndexer put(long[] indices, boolean... b) { return put(indices, b, 0, b.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = b[offset:offset + length]} */
    public abstract BooleanIndexer put(long[] indices, boolean[] b, int offset, int length);

    @Override public double getDouble(long... indices) { return get(indices) ? 1.0 : 0.0; }
    @Override public BooleanIndexer putDouble(long[] indices, double b) { return put(indices, b != 0.0); }
}
