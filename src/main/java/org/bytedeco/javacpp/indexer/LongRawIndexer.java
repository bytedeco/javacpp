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

import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link LongPointer} using the {@link Raw} instance.
 *
 * @author Samuel Audet
 */
public class LongRawIndexer extends LongIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected LongPointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code LongRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public LongRawIndexer(LongPointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Calls {@code LongRawIndexer(pointer, sizes, strides(sizes))}. */
    public LongRawIndexer(LongPointer pointer, long... sizes) {
        this(pointer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public LongRawIndexer(LongPointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position() * VALUE_BYTES;
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    public long getRaw(long i) {
        return RAW.getLong(base + checkIndex(i, size) * VALUE_BYTES);
    }
    @Override public long get(long i) {
        return getRaw(index(i));
    }
    @Override public LongIndexer get(long i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = getRaw(index(i) + n);
        }
        return this;
    }
    @Override public long get(long i, long j) {
        return getRaw(index(i, j));
    }
    @Override public LongIndexer get(long i, long j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = getRaw(index(i, j) + n);
        }
        return this;
    }
    @Override public long get(long i, long j, long k) {
        return getRaw(index(i, j, k));
    }
    @Override public long get(long... indices) {
        return getRaw(index(indices));
    }
    @Override public LongIndexer get(long[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = getRaw(index(indices) + n);
        }
        return this;
    }

    public LongIndexer putRaw(long i, long l) {
        RAW.putLong(base + checkIndex(i, size) * VALUE_BYTES, l);
        return this;
    }
    @Override public LongIndexer put(long i, long l) {
        return putRaw(index(i), l);
    }
    @Override public LongIndexer put(long i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, l[offset + n]);
        }
        return this;
    }
    @Override public LongIndexer put(long i, long j, long l) {
        putRaw(index(i, j), l);
        return this;
    }
    @Override public LongIndexer put(long i, long j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, l[offset + n]);
        }
        return this;
    }
    @Override public LongIndexer put(long i, long j, long k, long l) {
        putRaw(index(i, j, k), l);
        return this;
    }
    @Override public LongIndexer put(long[] indices, long l) {
        putRaw(index(indices), l);
        return this;
    }
    @Override public LongIndexer put(long[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, l[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
