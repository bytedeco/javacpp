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

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link BytePointer} using the {@link Raw} instance, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UByteRawIndexer extends UByteIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected BytePointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code UByteRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public UByteRawIndexer(BytePointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Calls {@code UByteRawIndexer(pointer, sizes, strides(sizes))}. */
    public UByteRawIndexer(BytePointer pointer, long... sizes) {
        this(pointer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public UByteRawIndexer(BytePointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position();
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    public int getRaw(long i) {
        return RAW.getByte(base + checkIndex(i, size)) & 0xFF;
    }
    @Override public int get(long i) {
        return getRaw(index(i));
    }
    @Override public UByteIndexer get(long i, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = getRaw(index(i) + n) & 0xFF;
        }
        return this;
    }
    @Override public int get(long i, long j) {
        return getRaw(index(i, j)) & 0xFF;
    }
    @Override public UByteIndexer get(long i, long j, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = getRaw(index(i, j) + n) & 0xFF;
        }
        return this;
    }
    @Override public int get(long i, long j, long k) {
        return getRaw(index(i, j, k)) & 0xFF;
    }
    @Override public int get(long... indices) {
        return getRaw(index(indices)) & 0xFF;
    }
    @Override public UByteIndexer get(long[] indices, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = getRaw(index(indices) + n) & 0xFF;
        }
        return this;
    }

    public UByteIndexer putRaw(long i, int b) {
        RAW.putByte(base + checkIndex(i, size), (byte)b);
        return this;
    }
    @Override public UByteIndexer put(long i, int b) {
        RAW.putByte(base + checkIndex(i, size), (byte)b);
        return this;
    }
    @Override public UByteIndexer put(long i, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, (byte)b[offset + n]);
        }
        return this;
    }
    @Override public UByteIndexer put(long i, long j, int b) {
        putRaw(index(i, j), (byte)b);
        return this;
    }
    @Override public UByteIndexer put(long i, long j, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, (byte)b[offset + n]);
        }
        return this;
    }
    @Override public UByteIndexer put(long i, long j, long k, int b) {
        putRaw(index(i, j, k), (byte)b);
        return this;
    }
    @Override public UByteIndexer put(long[] indices, int b) {
        putRaw(index(indices), (byte)b);
        return this;
    }
    @Override public UByteIndexer put(long[] indices, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, (byte)b[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
