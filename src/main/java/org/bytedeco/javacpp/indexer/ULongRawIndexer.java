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
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link LongPointer}, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class ULongRawIndexer extends ULongIndexer {
    /** The backing pointer. */
    protected LongPointer pointer;

    /** Calls {@code ULongRawIndexer(pointer, Index.create(pointer.limit()))}. */
    public ULongRawIndexer(LongPointer pointer) {
        this(pointer, Index.create(pointer.limit()));
    }

    /** Calls {@code ULongRawIndexer(pointer, Index.create(sizes))}. */
    public ULongRawIndexer(LongPointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code ULongRawIndexer(pointer, Index.create(sizes, strides))}. */
    public ULongRawIndexer(LongPointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public ULongRawIndexer(LongPointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public ULongIndexer reindex(Index index) {
        return new ULongRawIndexer(pointer, index);
    }

    @Override public BigInteger get(long i) {
        return toBigInteger(pointer.get((int)index(i)));
    }
    @Override public ULongIndexer get(long i, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(pointer.get((int)index(i) + n));
        }
        return this;
    }
    @Override public BigInteger get(long i, long j) {
        return toBigInteger(pointer.get((int)index(i, j)));
    }
    @Override public ULongIndexer get(long i, long j, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(pointer.get((int)index(i, j) + n));
        }
        return this;
    }
    @Override public BigInteger get(long i, long j, long k) {
        return toBigInteger(pointer.get((int)index(i, j, k)));
    }
    @Override public BigInteger get(long... indices) {
        return toBigInteger(pointer.get((int)index(indices)));
    }
    @Override public ULongIndexer get(long[] indices, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = toBigInteger(pointer.get((int)index(indices) + n));
        }
        return this;
    }

    @Override public ULongIndexer put(long i, BigInteger l) {
        pointer.put((int)index(i), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long i, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i) + n, fromBigInteger(l[offset + n]));
        }
        return this;
    }
    @Override public ULongIndexer put(long i, long j, BigInteger l) {
        pointer.put((int)index(i, j), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long i, long j, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i, j) + n, fromBigInteger(l[offset + n]));
        }
        return this;
    }
    @Override public ULongIndexer put(long i, long j, long k, BigInteger l) {
        pointer.put((int)index(i, j, k), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long[] indices, BigInteger l) {
        pointer.put((int)index(indices), fromBigInteger(l));
        return this;
    }
    @Override public ULongIndexer put(long[] indices, BigInteger[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(indices) + n, fromBigInteger(l[offset + n]));
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
