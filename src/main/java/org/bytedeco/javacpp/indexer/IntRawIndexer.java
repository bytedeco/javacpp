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

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for an {@link IntPointer} using the {@link Raw} instance.
 *
 * @author Samuel Audet
 */
public class IntRawIndexer extends IntIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected IntPointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code IntRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public IntRawIndexer(IntPointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Calls {@code IntRawIndexer(pointer, sizes, strides(sizes))}. */
    public IntRawIndexer(IntPointer pointer, long... sizes) {
        this(pointer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public IntRawIndexer(IntPointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position() * VALUE_BYTES;
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    public int getRaw(long i) {
        return RAW.getInt(base + checkIndex(i, size) * VALUE_BYTES);
    }
    @Override public int get(long i) {
        return getRaw(index(i));
    }
    @Override public IntIndexer get(long i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = getRaw(index(i) + n);
        }
        return this;
    }
    @Override public int get(long i, long j) {
        return getRaw(index(i, j));
    }
    @Override public IntIndexer get(long i, long j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = getRaw(index(i, j) + n);
        }
        return this;
    }
    @Override public int get(long i, long j, long k) {
        return getRaw(index(i, j, k));
    }
    @Override public int get(long... indices) {
        return getRaw(index(indices));
    }
    @Override public IntIndexer get(long[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = getRaw(index(indices) + n);
        }
        return this;
    }

    public IntIndexer putRaw(long i, int n) {
        RAW.putInt(base + checkIndex(i, size) * VALUE_BYTES, n);
        return this;
    }
    @Override public IntIndexer put(long i, int n) {
        return putRaw(index(i), n);
    }
    @Override public IntIndexer put(long i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, m[offset + n]);
        }
        return this;
    }
    @Override public IntIndexer put(long i, long j, int n) {
        putRaw(index(i, j), n);
        return this;
    }
    @Override public IntIndexer put(long i, long j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, m[offset + n]);
        }
        return this;
    }
    @Override public IntIndexer put(long i, long j, long k, int n) {
        putRaw(index(i, j, k), n);
        return this;
    }
    @Override public IntIndexer put(long[] indices, int n) {
        putRaw(index(indices), n);
        return this;
    }
    @Override public IntIndexer put(long[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, m[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
