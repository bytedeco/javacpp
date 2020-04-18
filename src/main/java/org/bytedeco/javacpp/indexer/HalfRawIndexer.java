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

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.ShortPointer;

/**
 * An indexer for a {@link ShortPointer} using the {@link Raw} instance, treated as half-precision float.
 *
 * @author Samuel Audet
 */
public class HalfRawIndexer extends HalfIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected ShortPointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code HalfRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public HalfRawIndexer(ShortPointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Calls {@code HalfRawIndexer(pointer, sizes, strides(sizes))}. */
    public HalfRawIndexer(ShortPointer pointer, long... sizes) {
        this(pointer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public HalfRawIndexer(ShortPointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position() * VALUE_BYTES;
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    public float getRaw(long i) {
        return toFloat(RAW.getShort(base + checkIndex(i, size) * VALUE_BYTES));
    }
    @Override public float get(long i) {
        return getRaw(index(i));
    }
    @Override public HalfIndexer get(long i, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            h[offset + n] = getRaw(index(i) + n);
        }
        return this;
    }
    @Override public float get(long i, long j) {
        return getRaw(index(i, j));
    }
    @Override public HalfIndexer get(long i, long j, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            h[offset + n] = getRaw(index(i, j) + n);
        }
        return this;
    }
    @Override public float get(long i, long j, long k) {
        return getRaw(index(i, j, k));
    }
    @Override public float get(long... indices) {
        return getRaw(index(indices));
    }
    @Override public HalfIndexer get(long[] indices, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            h[offset + n] = getRaw(index(indices) + n);
        }
        return this;
    }

    public HalfIndexer putRaw(long i, float h) {
        RAW.putShort(base + checkIndex(i, size) * VALUE_BYTES, (short)fromFloat(h));
        return this;
    }
    @Override public HalfIndexer put(long i, float h) {
        return putRaw(index(i), h);
    }
    @Override public HalfIndexer put(long i, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, h[offset + n]);
        }
        return this;
    }
    @Override public HalfIndexer put(long i, long j, float h) {
        putRaw(index(i, j), h);
        return this;
    }
    @Override public HalfIndexer put(long i, long j, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, h[offset + n]);
        }
        return this;
    }
    @Override public HalfIndexer put(long i, long j, long k, float h) {
        putRaw(index(i, j, k), h);
        return this;
    }
    @Override public HalfIndexer put(long[] indices, float h) {
        putRaw(index(indices), h);
        return this;
    }
    @Override public HalfIndexer put(long[] indices, float[] h, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, h[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
