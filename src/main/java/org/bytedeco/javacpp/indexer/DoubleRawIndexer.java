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

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link DoublePointer} using the {@link Raw} instance.
 *
 * @author Samuel Audet
 */
public class DoubleRawIndexer extends DoubleIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected DoublePointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code DoubleRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public DoubleRawIndexer(DoublePointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Calls {@code DoubleRawIndexer(pointer, sizes, strides(sizes))}. */
    public DoubleRawIndexer(DoublePointer pointer, long... sizes) {
        this(pointer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public DoubleRawIndexer(DoublePointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position() * VALUE_BYTES;
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    public double getRaw(long i) {
        return RAW.getDouble(base + checkIndex(i, size) * VALUE_BYTES);
    }
    @Override public double get(long i) {
        return getRaw(index(i));
    }
    @Override public DoubleIndexer get(long i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = getRaw(index(i) + n);
        }
        return this;
    }
    @Override public double get(long i, long j) {
        return getRaw(index(i, j));
    }
    @Override public DoubleIndexer get(long i, long j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = getRaw(index(i, j) + n);
        }
        return this;
    }
    @Override public double get(long i, long j, long k) {
        return getRaw(index(i, j, k));
    }
    @Override public double get(long... indices) {
        return getRaw(index(indices));
    }
    @Override public DoubleIndexer get(long[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = getRaw(index(indices) + n);
        }
        return this;
    }

    public DoubleIndexer putRaw(long i, double d) {
        RAW.putDouble(base + checkIndex(i, size) * VALUE_BYTES, d);
        return this;
    }
    @Override public DoubleIndexer put(long i, double d) {
        return put(index(i), d);
    }
    @Override public DoubleIndexer put(long i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, d[offset + n]);
        }
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, double d) {
        putRaw(index(i, j), d);
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, d[offset + n]);
        }
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, long k, double d) {
        putRaw(index(i, j, k), d);
        return this;
    }
    @Override public DoubleIndexer put(long[] indices, double d) {
        putRaw(index(indices), d);
        return this;
    }
    @Override public DoubleIndexer put(long[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, d[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
