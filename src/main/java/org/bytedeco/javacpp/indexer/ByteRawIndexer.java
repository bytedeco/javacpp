/*
 * Copyright (C) 2014-2019 Samuel Audet
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
 * An indexer for a {@link BytePointer}.
 *
 * @author Samuel Audet
 */
public class ByteRawIndexer extends ByteIndexer {
    /** The backing pointer. */
    protected BytePointer pointer;

    /** Calls {@code ByteRawIndexer(pointer, Index.create(pointer.limit()))}. */
    public ByteRawIndexer(BytePointer pointer) {
        this(pointer, Index.create(pointer.limit()));
    }

    /** Calls {@code ByteRawIndexer(pointer, Index.create(sizes))}. */
    public ByteRawIndexer(BytePointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code ByteRawIndexer(pointer, Index.create(sizes, strides))}. */
    public ByteRawIndexer(BytePointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public ByteRawIndexer(BytePointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public ByteIndexer reindex(Index index) {
        return new ByteRawIndexer(pointer, index);
    }

    @Override public byte get(long i) {
        return pointer.get((int)index(i));
    }
    @Override public ByteIndexer get(long i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = pointer.get((int)index(i) + n);
        }
        return this;
    }
    @Override public byte get(long i, long j) {
        return pointer.get((int)index(i, j));
    }
    @Override public ByteIndexer get(long i, long j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = pointer.get((int)index(i, j) + n);
        }
        return this;
    }
    @Override public byte get(long i, long j, long k) {
        return pointer.get((int)index(i, j, k));
    }
    @Override public byte get(long... indices) {
        return pointer.get((int)index(indices));
    }
    @Override public ByteIndexer get(long[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = pointer.get((int)index(indices) + n);
        }
        return this;
    }

    @Override public ByteIndexer put(long i, byte b) {
        pointer.put((int)index(i), b);
        return this;
    }
    @Override public ByteIndexer put(long i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i) + n, b[offset + n]);
        }
        return this;
    }
    @Override public ByteIndexer put(long i, long j, byte b) {
        pointer.put((int)index(i, j), b);
        return this;
    }
    @Override public ByteIndexer put(long i, long j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i, j) + n, b[offset + n]);
        }
        return this;
    }
    @Override public ByteIndexer put(long i, long j, long k, byte b) {
        pointer.put((int)index(i, j, k), b);
        return this;
    }
    @Override public ByteIndexer put(long[] indices, byte b) {
        pointer.put((int)index(indices), b);
        return this;
    }
    @Override public ByteIndexer put(long[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(indices) + n, b[offset + n]);
        }
        return this;
    }

    @Override public byte getByte(long i) {
        return pointer.get((int)i);
    }
    @Override public ByteIndexer putByte(long i, byte b) {
        pointer.put((int)i, b);
        return this;
    }

    @Override public short getShort(long i) {
        return pointer.getShort((int)i);
    }
    @Override public ByteIndexer putShort(long i, short s) {
        pointer.putShort((int)i, s);
        return this;
    }

    @Override public int getInt(long i) {
        return pointer.getInt((int)i);
    }
    @Override public ByteIndexer putInt(long i, int j) {
        pointer.putInt((int)i, j);
        return this;
    }

    @Override public long getLong(long i) {
        return pointer.getLong((int)i);
    }
    @Override public ByteIndexer putLong(long i, long j) {
        pointer.putLong((int)i, j);
        return this;
    }

    @Override public float getFloat(long i) {
        return pointer.getFloat((int)i);
    }
    @Override public ByteIndexer putFloat(long i, float f) {
        pointer.putFloat((int)i, f);
        return this;
    }

    @Override public double getDouble(long i) {
        return pointer.getDouble((int)i);
    }
    @Override public ByteIndexer putDouble(long i, double d) {
        pointer.putDouble((int)i, d);
        return this;
    }

    @Override public char getChar(long i) {
        return pointer.getChar((int)i);
    }
    @Override public ByteIndexer putChar(long i, char c) {
        pointer.putChar((int)i, c);
        return this;
    }

    @Override public void release() { pointer = null; }
}
