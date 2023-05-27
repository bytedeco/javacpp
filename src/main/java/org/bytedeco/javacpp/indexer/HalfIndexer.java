/*
 * Copyright (C) 2016-2019 Samuel Audet
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
 * Abstract indexer for the {@code short} primitive type, treated as half-precision float.
 *
 * @author Samuel Audet
 */
public abstract class HalfIndexer extends Indexer {

    /**
     * The number of bytes used to represent a short.
     */
    public static final int VALUE_BYTES = 2;

    protected HalfIndexer(Index index) {
        super(index);
    }

    protected HalfIndexer(long[] sizes, long[] strides) {
        super(sizes, strides);
    }

    /**
     * Returns {@code new HalfArrayIndexer(array)}
     */
    public static HalfIndexer create(short[] array) {
        return new HalfArrayIndexer(array);
    }

    /**
     * Returns {@code new HalfBufferIndexer(buffer)}
     */
    public static HalfIndexer create(ShortBuffer buffer) {
        return new HalfBufferIndexer(buffer);
    }

    /**
     * Returns {@code new HalfRawIndexer(pointer)}
     */
    public static HalfIndexer create(ShortPointer pointer) {
        return new HalfRawIndexer(pointer);
    }

    /**
     * Returns {@code new HalfArrayIndexer(array, index)}
     */
    public static HalfIndexer create(short[] array, Index index) {
        return new HalfArrayIndexer(array, index);
    }

    /**
     * Returns {@code new HalfBufferIndexer(buffer, index)}
     */
    public static HalfIndexer create(ShortBuffer buffer, Index index) {
        return new HalfBufferIndexer(buffer, index);
    }

    /**
     * Returns {@code new HalfRawIndexer(pointer, index)}
     */
    public static HalfIndexer create(ShortPointer pointer, Index index) {
        return new HalfRawIndexer(pointer, index);
    }

    /**
     * Returns {@code new HalfArrayIndexer(array, sizes)}
     */
    public static HalfIndexer create(short[] array, long... sizes) {
        return new HalfArrayIndexer(array, sizes);
    }

    /**
     * Returns {@code new HalfBufferIndexer(buffer, sizes)}
     */
    public static HalfIndexer create(ShortBuffer buffer, long... sizes) {
        return new HalfBufferIndexer(buffer, sizes);
    }

    /**
     * Returns {@code new HalfRawIndexer(pointer, sizes)}
     */
    public static HalfIndexer create(ShortPointer pointer, long... sizes) {
        return new HalfRawIndexer(pointer, sizes);
    }

    /**
     * Returns {@code new HalfArrayIndexer(array, sizes, strides)}
     */
    public static HalfIndexer create(short[] array, long[] sizes, long[] strides) {
        return new HalfArrayIndexer(array, sizes, strides);
    }

    /**
     * Returns {@code new HalfBufferIndexer(buffer, sizes, strides)}
     */
    public static HalfIndexer create(ShortBuffer buffer, long[] sizes, long[] strides) {
        return new HalfBufferIndexer(buffer, sizes, strides);
    }

    /**
     * Returns {@code new HalfRawIndexer(pointer, sizes, strides)}
     */
    public static HalfIndexer create(ShortPointer pointer, long[] sizes, long[] strides) {
        return new HalfRawIndexer(pointer, sizes, strides);
    }

    /**
     * Returns {@code create(pointer, Index.create(sizes, strides), direct)}
     */
    public static HalfIndexer create(final ShortPointer pointer, long[] sizes, long[] strides, boolean direct) {
        return create(pointer, Index.create(sizes, strides), direct);
    }

    /**
     * Creates a half float indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param index to use
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new half indexer backed by the raw memory interface, a buffer, or an array
     */
    public static HalfIndexer create(final ShortPointer pointer, Index index, boolean direct) {
        if (direct) {
            return Raw.getInstance() != null ? new HalfRawIndexer(pointer, index) : new HalfBufferIndexer(pointer.asBuffer(), index);
        } else {
            final long position = pointer.position();
            short[] array = new short[(int) Math.min(pointer.limit() - position, Integer.MAX_VALUE)];
            pointer.get(array);
            return new HalfArrayIndexer(array, index) {

                @Override
                public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    // Half-precision conversion code put in the public domain by x4u:
    // http://stackoverflow.com/a/6162687/523744
    /**
     * ignores the higher 16 bits
     */
    public static float toFloat(int hbits) {
        // 10 bits mantissa
        int mant = hbits & 0x03ff;
        // 5 bits exponent
        int exp = hbits & 0x7c00;
        if (// NaN/Inf
        exp == 0x7c00)
            // -> NaN/Inf
            exp = 0x3fc00;
        else if (// normalized value
        exp != 0) {
            // exp - 15 + 127
            exp += 0x1c000;
            // "smooth transition" is nonstandard behavior
            //            if( mant == 0 && exp > 0x1c400 )  // smooth transition
            //                return Float.intBitsToFloat( ( hbits & 0x8000 ) << 16
            //                                                | exp << 13 | 0x3ff );
        } else if (// && exp==0 -> subnormal
        mant != 0) {
            // make it normal
            exp = 0x1c400;
            do {
                // mantissa * 2
                mant <<= 1;
                // decrease exp by 1
                exp -= 0x400;
            } while (// while not normal
            (mant & 0x400) == 0);
            // discard subnormal bit
            mant &= 0x3ff;
        }
        // else +/-0 -> +/-0
        return // combine all parts
        Float.// combine all parts
        intBitsToFloat(// sign  << ( 31 - 15 )
        (hbits & 0x8000) << 16 | // value << ( 23 - 10 )
        (exp | mant) << 13);
    }

    /**
     * returns all higher 16 bits as 0 for all results
     */
    public static int fromFloat(float fval) {
        int fbits = Float.floatToIntBits(fval);
        // sign only
        int sign = fbits >>> 16 & 0x8000;
        // rounded value
        int val = (fbits & 0x7fffffff) + 0x1000;
        if (// might be or become NaN/Inf
        val >= 0x47800000) {
            // avoid Inf due to rounding
            if ((fbits & 0x7fffffff) >= 0x47800000) {
                // is or must become NaN/Inf
                if (// was value but too large
                val < 0x7f800000)
                    // make it +/-Inf
                    return sign | 0x7c00;
                return // remains +/-Inf or NaN
                sign | 0x7c00 | // keep NaN (and Inf) bits
                (fbits & 0x007fffff) >>> 13;
            }
            // unrounded not quite Inf
            return sign | 0x7bff;
        }
        if (// remains normalized value
        val >= 0x38800000)
            // exp - 127 + 15
            return sign | val - 0x38000000 >>> 13;
        if (// too small for subnormal
        val < 0x33000000)
            // becomes +/-0
            return sign;
        // tmp exp for subnormal calc
        val = (fbits & 0x7fffffff) >>> 23;
        return sign | (// add subnormal bit
        (fbits & 0x7fffff | 0x800000) + // round depending on cut off
        (0x800000 >>> val - 102) >>> // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
        126 - val);
    }

    /**
     * Returns {@code array/buffer[index(i)]}
     */
    public abstract float get(long i);

    /**
     * Returns {@code this} where {@code h = array/buffer[index(i)]}
     */
    public HalfIndexer get(long i, float[] h) {
        return get(i, h, 0, h.length);
    }

    /**
     * Returns {@code this} where {@code h[offset:offset + length] = array/buffer[index(i)]}
     */
    public abstract HalfIndexer get(long i, float[] h, int offset, int length);

    /**
     * Returns {@code array/buffer[index(i, j)]}
     */
    public abstract float get(long i, long j);

    /**
     * Returns {@code this} where {@code h = array/buffer[index(i, j)]}
     */
    public HalfIndexer get(long i, long j, float[] h) {
        return get(i, j, h, 0, h.length);
    }

    /**
     * Returns {@code this} where {@code h[offset:offset + length] = array/buffer[index(i, j)]}
     */
    public abstract HalfIndexer get(long i, long j, float[] h, int offset, int length);

    /**
     * Returns {@code array/buffer[index(i, j, k)]}
     */
    public abstract float get(long i, long j, long k);

    /**
     * Returns {@code array/buffer[index(indices)]}
     */
    public abstract float get(long... indices);

    /**
     * Returns {@code this} where {@code h = array/buffer[index(indices)]}
     */
    public HalfIndexer get(long[] indices, float[] h) {
        return get(indices, h, 0, h.length);
    }

    /**
     * Returns {@code this} where {@code h[offset:offset + length] = array/buffer[index(indices)]}
     */
    public abstract HalfIndexer get(long[] indices, float[] h, int offset, int length);

    /**
     * Returns {@code this} where {@code array/buffer[index(i)] = h}
     */
    public abstract HalfIndexer put(long i, float h);

    /**
     * Returns {@code this} where {@code array/buffer[index(i)] = h}
     */
    public HalfIndexer put(long i, float... h) {
        return put(i, h, 0, h.length);
    }

    /**
     * Returns {@code this} where {@code array/buffer[index(i)] = h[offset:offset + length]}
     */
    public abstract HalfIndexer put(long i, float[] h, int offset, int length);

    /**
     * Returns {@code this} where {@code array/buffer[index(i, j)] = h}
     */
    public abstract HalfIndexer put(long i, long j, float h);

    /**
     * Returns {@code this} where {@code array/buffer[index(i, j)] = h}
     */
    public HalfIndexer put(long i, long j, float... h) {
        return put(i, j, h, 0, h.length);
    }

    /**
     * Returns {@code this} where {@code array/buffer[index(i, j)] = h[offset:offset + length]}
     */
    public abstract HalfIndexer put(long i, long j, float[] h, int offset, int length);

    /**
     * Returns {@code this} where {@code array/buffer[index(i, j, k)] = h}
     */
    public abstract HalfIndexer put(long i, long j, long k, float h);

    /**
     * Returns {@code this} where {@code array/buffer[index(indices)] = h}
     */
    public abstract HalfIndexer put(long[] indices, float h);

    /**
     * Returns {@code this} where {@code array/buffer[index(indices)] = h}
     */
    public HalfIndexer put(long[] indices, float... h) {
        return put(indices, h, 0, h.length);
    }

    /**
     * Returns {@code this} where {@code array/buffer[index(indices)] = h[offset:offset + length]}
     */
    public abstract HalfIndexer put(long[] indices, float[] h, int offset, int length);

    @Override
    public double getDouble(long... indices) {
        return get(indices);
    }

    @Override
    public HalfIndexer putDouble(long[] indices, double h) {
        return put(indices, (float) h);
    }
}
