/*
 * Copyright (C) 2016-2017 Samuel Audet
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
 * An indexer for a {@link BytePointer} using the {@link Raw} instance.
 *
 * @author Samuel Audet
 */
public class ByteRawIndexer extends ByteIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected BytePointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code ByteRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public ByteRawIndexer(BytePointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public ByteRawIndexer(BytePointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position();
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public byte get(long i) {
        return RAW.getByte(base + checkIndex(i, size));
    }
    @Override public ByteIndexer get(long i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = get(i * strides[0] + n);
        }
        return this;
    }
    @Override public byte get(long i, long j) {
        return get(i * strides[0] + (int)j);
    }
    @Override public ByteIndexer get(long i, long j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public byte get(long i, long j, long k) {
        return get(i * strides[0] + j * strides[1] + k);
    }
    @Override public byte get(long... indices) {
        return get(index(indices));
    }
    @Override public ByteIndexer get(long[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = get(index(indices) + n);
        }
        return this;
    }

    @Override public ByteIndexer put(long i, byte b) {
        RAW.putByte(base + checkIndex(i, size), b);
        return this;
    }
    @Override public ByteIndexer put(long i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            put(i * strides[0] + n, b[offset + n]);
        }
        return this;
    }
    @Override public ByteIndexer put(long i, long j, byte b) {
        put(i * strides[0] + j, b);
        return this;
    }
    @Override public ByteIndexer put(long i, long j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            put(i * strides[0] + j * strides[1] + n, b[offset + n]);
        }
        return this;
    }
    @Override public ByteIndexer put(long i, long j, long k, byte b) {
        put(i * strides[0] + j * strides[1] + k, b);
        return this;
    }
    @Override public ByteIndexer put(long[] indices, byte b) {
        put(index(indices), b);
        return this;
    }
    @Override public ByteIndexer put(long[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            put(index(indices) + n, b[offset + n]);
        }
        return this;
    }

    @Override public short getShort(long i) {
        return RAW.getShort(base + checkIndex(i, size - 1));
    }
    @Override public ByteIndexer putShort(long i, short s) {
        RAW.putShort(base + checkIndex(i, size - 1), s);
        return this;
    }

    @Override public int getInt(long i) {
        return RAW.getInt(base + checkIndex(i, size - 3));
    }
    @Override public ByteIndexer putInt(long i, int j) {
        RAW.putInt(base + checkIndex(i, size - 3), j);
        return this;
    }

    @Override public long getLong(long i) {
        return RAW.getLong(base + checkIndex(i, size - 7));
    }
    @Override public ByteIndexer putLong(long i, long j) {
        RAW.putLong(base + checkIndex(i, size - 7), j);
        return this;
    }

    @Override public float getFloat(long i) {
        return RAW.getFloat(base + checkIndex(i, size - 3));
    }
    @Override public ByteIndexer putFloat(long i, float f) {
        RAW.putFloat(base + checkIndex(i, size - 3), f);
        return this;
    }

    @Override public double getDouble(long i) {
        return RAW.getDouble(base + checkIndex(i, size - 7));
    }
    @Override public ByteIndexer putDouble(long i, double d) {
        RAW.putDouble(base + checkIndex(i, size - 7), d);
        return this;
    }

    @Override public char getChar(long i) {
        return RAW.getChar(base + checkIndex(i, size - 1));
    }
    @Override public ByteIndexer putChar(long i, char c) {
        RAW.putChar(base + checkIndex(i, size - 1), c);
        return this;
    }

    @Override public void release() { pointer = null; }
}
