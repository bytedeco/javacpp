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
import java.nio.Buffer;
import java.nio.LongBuffer;

/**
 * An indexer for a {@link LongBuffer}, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class ULongBufferIndexer extends ULongIndexer {
    /** The backing buffer. */
    protected LongBuffer buffer;

    /** Calls {@code ULongBufferIndexer(buffer, { buffer.limit() }, { 1 })}. */
    public ULongBufferIndexer(LongBuffer buffer) {
        this(buffer, new long[] { buffer.limit() }, ONE_STRIDE);
    }

    /** Calls {@code ULongBufferIndexer(buffer, sizes, strides(sizes))}. */
    public ULongBufferIndexer(LongBuffer buffer, long... sizes) {
        this(buffer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public ULongBufferIndexer(LongBuffer buffer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public BigInteger get(long i) {
        return toBigInteger(buffer.get((int)index(i)));
    }
    @Override public ULongIndexer get(long i, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(buffer.get((int)index(i) + n));
        }
        return this;
    }
    @Override public BigInteger get(long i, long j) {
        return toBigInteger(buffer.get((int)index(i, j)));
    }
    @Override public ULongIndexer get(long i, long j, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(buffer.get((int)index(i, j) + n));
        }
        return this;
    }
    @Override public BigInteger get(long i, long j, long k) {
        return toBigInteger(buffer.get((int)index(i, j, k)));
    }
    @Override public BigInteger get(long... indices) {
        return toBigInteger(buffer.get((int)index(indices)));
    }
    @Override public ULongIndexer get(long[] indices, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(buffer.get((int)index(indices) + n));
        }
        return this;
    }

    @Override public ULongIndexer put(long i, BigInteger l) {
        buffer.put((int)index(i), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long i, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i) + n, fromBigInteger(l[offset + n]));
        }
        return this;
    }
    @Override public ULongIndexer put(long i, long j, BigInteger l) {
        buffer.put((int)index(i, j), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long i, long j, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(i, j) + n, fromBigInteger(l[offset + n]));
        }
        return this;
    }
    @Override public ULongIndexer put(long i, long j, long k, BigInteger l) {
        buffer.put((int)index(i, j, k), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long[] indices, BigInteger l) {
        buffer.put((int)index(indices), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long[] indices, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put((int)index(indices) + n, fromBigInteger(l[offset + n]));
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
