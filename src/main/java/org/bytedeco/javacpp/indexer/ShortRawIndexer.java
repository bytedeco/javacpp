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
 * An indexer for a {@link ShortPointer} using the {@link Raw} instance.
 *
 * @author Samuel Audet
 */
public class ShortRawIndexer extends ShortIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected ShortPointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code ShortRawIndexer(pointer, Index.create(pointer.limit() - pointer.position()))}. */
    public ShortRawIndexer(ShortPointer pointer) {
        this(pointer, Index.create(pointer.limit() - pointer.position()));
    }

    /** Calls {@code ShortRawIndexer(pointer, Index.create(sizes))}. */
    public ShortRawIndexer(ShortPointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code ShortRawIndexer(pointer, Index.create(sizes, strides))}. */
    public ShortRawIndexer(ShortPointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public ShortRawIndexer(ShortPointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
        this.base = pointer.address() + pointer.position() * VALUE_BYTES;
        this.size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public ShortIndexer reindex(Index index) {
        return new ShortRawIndexer(pointer, index);
    }

    public short getRaw(long i) {
        return RAW.getShort(base + checkIndex(i, size) * VALUE_BYTES);
    }
    @Override public short get(long i) {
        return getRaw(index(i));
    }
    @Override public ShortIndexer get(long i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = getRaw(index(i) + n);
        }
        return this;
    }
    @Override public short get(long i, long j) {
        return getRaw(index(i, j));
    }
    @Override public ShortIndexer get(long i, long j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = getRaw(index(i, j) + n);
        }
        return this;
    }
    @Override public short get(long i, long j, long k) {
        return getRaw(index(i, j, k));
    }
    @Override public short get(long... indices) {
        return getRaw(index(indices));
    }
    @Override public ShortIndexer get(long[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = getRaw(index(indices) + n);
        }
        return this;
    }

    public ShortIndexer putRaw(long i, short s) {
        RAW.putShort(base + checkIndex(i, size) * VALUE_BYTES, s);
        return this;
    }
    @Override public ShortIndexer put(long i, short s) {
        return putRaw(index(i), s);
    }
    @Override public ShortIndexer put(long i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(long i, long j, short s) {
        putRaw(index(i, j), s);
        return this;
    }
    @Override public ShortIndexer put(long i, long j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(long i, long j, long k, short s) {
        putRaw(index(i, j, k), s);
        return this;
    }
    @Override public ShortIndexer put(long[] indices, short s) {
        putRaw(index(indices), s);
        return this;
    }
    @Override public ShortIndexer put(long[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, s[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
