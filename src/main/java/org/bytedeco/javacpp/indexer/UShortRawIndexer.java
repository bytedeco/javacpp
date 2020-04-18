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
 * An indexer for a {@link ShortPointer} using the {@link Raw} instance, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UShortRawIndexer extends UShortIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected ShortPointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code UShortRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public UShortRawIndexer(ShortPointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Calls {@code UShortRawIndexer(pointer, sizes, strides(sizes))}. */
    public UShortRawIndexer(ShortPointer pointer, long... sizes) {
        this(pointer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public UShortRawIndexer(ShortPointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position() * VALUE_BYTES;
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    public int getRaw(long i) {
        return RAW.getShort(base + checkIndex(i, size) * VALUE_BYTES) & 0xFFFF;
    }
    @Override public int get(long i) {
        return getRaw(index(i));
    }
    @Override public UShortIndexer get(long i, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = getRaw(index(i) + n) & 0xFFFF;
        }
        return this;
    }
    @Override public int get(long i, long j) {
        return getRaw(index(i, j)) & 0xFFFF;
    }
    @Override public UShortIndexer get(long i, long j, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = getRaw(index(i, j) + n) & 0xFFFF;
        }
        return this;
    }
    @Override public int get(long i, long j, long k) {
        return getRaw(index(i, j, k)) & 0xFFFF;
    }
    @Override public int get(long... indices) {
        return getRaw(index(indices)) & 0xFFFF;
    }
    @Override public UShortIndexer get(long[] indices, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = getRaw(index(indices) + n) & 0xFFFF;
        }
        return this;
    }

    public UShortIndexer putRaw(long i, int s) {
        RAW.putShort(base + checkIndex(i, size) * VALUE_BYTES, (short)s);
        return this;
    }
    @Override public UShortIndexer put(long i, int s) {
        return putRaw(index(i), s);
    }
    @Override public UShortIndexer put(long i, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, (short)s[offset + n]);
        }
        return this;
    }
    @Override public UShortIndexer put(long i, long j, int s) {
        putRaw(index(i, j), (short)s);
        return this;
    }
    @Override public UShortIndexer put(long i, long j, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, (short)s[offset + n]);
        }
        return this;
    }
    @Override public UShortIndexer put(long i, long j, long k, int s) {
        putRaw(index(i, j, k), (short)s);
        return this;
    }
    @Override public UShortIndexer put(long[] indices, int s) {
        putRaw(index(indices), (short)s);
        return this;
    }
    @Override public UShortIndexer put(long[] indices, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, (short)s[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
