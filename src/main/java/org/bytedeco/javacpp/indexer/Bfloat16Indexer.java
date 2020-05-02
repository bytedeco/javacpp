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

import java.nio.ShortBuffer;
import org.bytedeco.javacpp.ShortPointer;

/**
 * Abstract indexer for the {@code short} primitive type, treated as bfloat16.
 *
 * @see <a href="https://software.intel.com/sites/default/files/managed/40/8b/bf16-hardware-numerics-definition-white-paper.pdf">BFLOAT16 – Hardware Numerics Definition</a>
 *
 * @author Samuel Audet
 */
public abstract class Bfloat16Indexer extends Indexer {
    /** The number of bytes used to represent a short. */
    public static final int VALUE_BYTES = 2;

    protected Bfloat16Indexer(Index index) {
        super(index);
    }

    protected Bfloat16Indexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /** Returns {@code new Bfloat16ArrayIndexer(array)} */
    public static Bfloat16Indexer create(short[] array) {
        return new Bfloat16ArrayIndexer(array);
    }
    /** Returns {@code new Bfloat16BufferIndexer(buffer)} */
    public static Bfloat16Indexer create(ShortBuffer buffer) {
        return new Bfloat16BufferIndexer(buffer);
    }
    /** Returns {@code new Bfloat16RawIndexer(pointer)} */
    public static Bfloat16Indexer create(ShortPointer pointer) {
        return new Bfloat16RawIndexer(pointer);
    }

    /** Returns {@code new Bfloat16ArrayIndexer(array, index)} */
    public static Bfloat16Indexer create(short[] array, Index index) {
        return new Bfloat16ArrayIndexer(array, index);
    }
    /** Returns {@code new Bfloat16BufferIndexer(buffer, index)} */
    public static Bfloat16Indexer create(ShortBuffer buffer, Index index) {
        return new Bfloat16BufferIndexer(buffer, index);
    }
    /** Returns {@code new Bfloat16RawIndexer(pointer, index)} */
    public static Bfloat16Indexer create(ShortPointer pointer, Index index) {
        return new Bfloat16RawIndexer(pointer, index);
    }

    /** Returns {@code new Bfloat16ArrayIndexer(array, sizes)} */
    public static Bfloat16Indexer create(short[] array, long... sizes) {
        return new Bfloat16ArrayIndexer(array, sizes);
    }
    /** Returns {@code new Bfloat16BufferIndexer(buffer, sizes)} */
    public static Bfloat16Indexer create(ShortBuffer buffer, long... sizes) {
        return new Bfloat16BufferIndexer(buffer, sizes);
    }
    /** Returns {@code new Bfloat16RawIndexer(pointer, sizes)} */
    public static Bfloat16Indexer create(ShortPointer pointer, long... sizes) {
        return new Bfloat16RawIndexer(pointer, sizes);
    }

    /** Returns {@code new Bfloat16ArrayIndexer(array, sizes, strides)} */
    public static Bfloat16Indexer create(short[] array, long[] sizes, long[] strides) {
        return new Bfloat16ArrayIndexer(array, sizes, strides);
    }
    /** Returns {@code new Bfloat16BufferIndexer(buffer, sizes, strides)} */
    public static Bfloat16Indexer create(ShortBuffer buffer, long[] sizes, long[] strides) {
        return new Bfloat16BufferIndexer(buffer, sizes, strides);
    }
    /** Returns {@code new Bfloat16RawIndexer(pointer, sizes, strides)} */
    public static Bfloat16Indexer create(ShortPointer pointer, long[] sizes, long[] strides) {
        return new Bfloat16RawIndexer(pointer, sizes, strides);
    }
    /** Returns {@code create(pointer, Index.create(sizes, strides), direct)} */
    public static Bfloat16Indexer create(final ShortPointer pointer, long[] sizes, long[] strides, boolean direct) {
        return create(pointer, Index.create(sizes, strides), direct);
    }
    /**
     * Creates a bfloat16 indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param index to use
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new bfloat16 indexer backed by the raw memory interface, a buffer, or an array
     */
    public static Bfloat16Indexer create(final ShortPointer pointer, Index index, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new Bfloat16RawIndexer(pointer, index)
                                             : new Bfloat16BufferIndexer(pointer.asBuffer(), index);
        } else {
            final long position = pointer.position();
            short[] array = new short[(int)Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new Bfloat16ArrayIndexer(array, index) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** ignores the higher 16 bits */
    public static float toFloat(int h) {
        return Float.intBitsToFloat(h << 16);
    }

    /** returns all higher 16 bits as 0 for all results */
    public static int fromFloat(float h) {
        return Float.floatToIntBits(h) >>> 16;
    }

    /** Returns {@code array/buffer[index(i)]} */
    public abstract float get(long i);
    /** Returns {@code this} where {@code h = array/buffer[index(i)]} */
    public Bfloat16Indexer get(long i, float[] h) { return get(i, h, 0, h.length); }
    /** Returns {@code this} where {@code h[offset:offset + length] = array/buffer[index(i)]} */
    public abstract Bfloat16Indexer get(long i, float[] h, int offset, int length);
    /** Returns {@code array/buffer[index(i, j)]} */
    public abstract float get(long i, long j);
    /** Returns {@code this} where {@code h = array/buffer[index(i, j)]} */
    public Bfloat16Indexer get(long i, long j, float[] h) { return get(i, j, h, 0, h.length); }
    /** Returns {@code this} where {@code h[offset:offset + length] = array/buffer[index(i, j)]} */
    public abstract Bfloat16Indexer get(long i, long j, float[] h, int offset, int length);
    /** Returns {@code array/buffer[index(i, j, k)]} */
    public abstract float get(long i, long j, long k);
    /** Returns {@code array/buffer[index(indices)]} */
    public abstract float get(long... indices);
    /** Returns {@code this} where {@code h = array/buffer[index(indices)]} */
    public Bfloat16Indexer get(long[] indices, float[] h) { return get(indices, h, 0, h.length); }
    /** Returns {@code this} where {@code h[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract Bfloat16Indexer get(long[] indices, float[] h, int offset, int length);

    /** Returns {@code this} where {@code array/buffer[index(i)] = h} */
    public abstract Bfloat16Indexer put(long i, float h);
    /** Returns {@code this} where {@code array/buffer[index(i)] = h} */
    public Bfloat16Indexer put(long i, float... h) { return put(i, h, 0, h.length); }
    /** Returns {@code this} where {@code array/buffer[index(i)] = h[offset:offset + length]} */
    public abstract Bfloat16Indexer put(long i, float[] h, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = h} */
    public abstract Bfloat16Indexer put(long i, long j, float h);
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = h} */
    public Bfloat16Indexer put(long i, long j, float... h) { return put(i, j, h, 0, h.length); }
    /** Returns {@code this} where {@code array/buffer[index(i, j)] = h[offset:offset + length]} */
    public abstract Bfloat16Indexer put(long i, long j, float[] h, int offset, int length);
    /** Returns {@code this} where {@code array/buffer[index(i, j, k)] = h} */
    public abstract Bfloat16Indexer put(long i, long j, long k, float h);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = h} */
    public abstract Bfloat16Indexer put(long[] indices, float h);
    /** Returns {@code this} where {@code array/buffer[index(indices)] = h} */
    public Bfloat16Indexer put(long[] indices, float... h) { return put(indices, h, 0, h.length); }
    /** Returns {@code this} where {@code array/buffer[index(indices)] = h[offset:offset + length]} */
    public abstract Bfloat16Indexer put(long[] indices, float[] h, int offset, int length);

    @Override public double getDouble(long... indices) { return get(indices); }
    @Override public Bfloat16Indexer putDouble(long[] indices, double h) { return put(indices, (float)h); }
}
