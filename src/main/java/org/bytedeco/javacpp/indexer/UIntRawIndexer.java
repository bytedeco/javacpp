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

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link IntPointer} using the {@link Raw} instance, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UIntRawIndexer extends UIntIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected IntPointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code UIntRawIndexer(pointer, Index.create(pointer.limit() - pointer.position()))}. */
    public UIntRawIndexer(IntPointer pointer) {
        this(pointer, Index.create(pointer.limit() - pointer.position()));
    }

    /** Calls {@code UIntRawIndexer(pointer, Index.create(sizes))}. */
    public UIntRawIndexer(IntPointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code UIntRawIndexer(pointer, Index.create(sizes, strides))}. */
    public UIntRawIndexer(IntPointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public UIntRawIndexer(IntPointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
        this.base = pointer.address() + pointer.position() * VALUE_BYTES;
        this.size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public UIntIndexer reindex(Index index) {
        return new UIntRawIndexer(pointer, index);
    }

    public long getRaw(long i) {
        return RAW.getInt(base + checkIndex(i, size) * VALUE_BYTES) & 0xFFFFFFFFL;
    }
    @Override public long get(long i) {
        return getRaw(index(i));
    }
    @Override public UIntIndexer get(long i, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = getRaw(index(i) + n) & 0xFFFFFFFFL;
        }
        return this;
    }
    @Override public long get(long i, long j) {
        return getRaw(index(i, j)) & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long i, long j, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = getRaw(index(i, j) + n) & 0xFFFFFFFFL;
        }
        return this;
    }
    @Override public long get(long i, long j, long k) {
        return getRaw(index(i, j, k)) & 0xFFFFFFFFL;
    }
    @Override public long get(long... indices) {
        return getRaw(index(indices)) & 0xFFFFFFFFL;
    }
    @Override public UIntIndexer get(long[] indices, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = getRaw(index(indices) + n) & 0xFFFFFFFFL;
        }
        return this;
    }

    public UIntIndexer putRaw(long i, long n) {
        RAW.putInt(base + checkIndex(i, size) * VALUE_BYTES, (int)n);
        return this;
    }
    @Override public UIntIndexer put(long i, long n) {
        return putRaw(index(i), n);
    }
    @Override public UIntIndexer put(long i, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, m[offset + n]);
        }
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long n) {
        putRaw(index(i, j), n);
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, m[offset + n]);
        }
        return this;
    }
    @Override public UIntIndexer put(long i, long j, long k, long n) {
        putRaw(index(i, j, k), n);
        return this;
    }
    @Override public UIntIndexer put(long[] indices, long n) {
        putRaw(index(indices), n);
        return this;
    }
    @Override public UIntIndexer put(long[] indices, long[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, m[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
