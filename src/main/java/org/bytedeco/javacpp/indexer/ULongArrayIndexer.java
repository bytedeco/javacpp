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

import java.math.BigInteger;

/**
 * An indexer for a {@code long[]} array, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class ULongArrayIndexer extends ULongIndexer {
    /** The backing array. */
    protected long[] array;

    /** Calls {@code ULongArrayIndexer(array, { array.length }, { 1 })}. */
    public ULongArrayIndexer(long[] array) {
        this(array, new long[] { array.length }, ONE_STRIDE);
    }

    /** Calls {@code ULongArrayIndexer(array, sizes, strides(sizes))}. */
    public ULongArrayIndexer(long[] array, long... sizes) {
        this(array, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public ULongArrayIndexer(long[] array, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public long[] array() {
        return array;
    }

    @Override public BigInteger get(long i) {
        return toBigInteger(array[(int)index(i)]);
    }
    @Override public ULongIndexer get(long i, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(array[(int)index(i) + n]);
        }
        return this;
    }
    @Override public BigInteger get(long i, long j) {
        return toBigInteger(array[(int)index(i, j)]);
    }
    @Override public ULongIndexer get(long i, long j, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(array[(int)index(i, j) + n]);
        }
        return this;
    }
    @Override public BigInteger get(long i, long j, long k) {
        return toBigInteger(array[(int)index(i, j, k)]);
    }
    @Override public BigInteger get(long... indices) {
        return toBigInteger(array[(int)index(indices)]);
    }
    @Override public ULongIndexer get(long[] indices, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(array[(int)index(indices) + n]);
        }
        return this;
    }

    @Override public ULongIndexer put(long i, BigInteger l) {
        array[(int)index(i)] = fromBigInteger(l);
        return this;
    }
    @Override public ULongIndexer put(long i, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i) + n] = fromBigInteger(l[offset + n]);
        }
        return this;
    }
    @Override public ULongIndexer put(long i, long j, BigInteger l) {
        array[(int)index(i, j)] = fromBigInteger(l);
        return this;
    }
    @Override public ULongIndexer put(long i, long j, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i, j) + n] = fromBigInteger(l[offset + n]);
        }
        return this;
    }
    @Override public ULongIndexer put(long i, long j, long k, BigInteger l) {
        array[(int)index(i, j, k)] = fromBigInteger(l);
        return this;
    }
    @Override public ULongIndexer put(long[] indices, BigInteger l) {
        array[(int)index(indices)] = fromBigInteger(l);
        return this;
    }
    @Override public ULongIndexer put(long[] indices, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(indices) + n] = fromBigInteger(l[offset + n]);
        }
        return this;
    }

    @Override public void release() { array = null; }
}
