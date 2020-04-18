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

import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link CharPointer} using the {@link Raw} instance.
 *
 * @author Samuel Audet
 */
public class CharRawIndexer extends CharIndexer {
    /** The instance for the raw memory interface. */
    protected static final Raw RAW = Raw.getInstance();
    /** The backing pointer. */
    protected CharPointer pointer;
    /** Base address and number of elements accessible. */
    final long base, size;

    /** Calls {@code CharRawIndexer(pointer, { pointer.limit() - pointer.position() }, { 1 })}. */
    public CharRawIndexer(CharPointer pointer) {
        this(pointer, new long[] { pointer.limit() - pointer.position() }, ONE_STRIDE);
    }

    /** Calls {@code CharRawIndexer(pointer, sizes, strides(sizes))}. */
    public CharRawIndexer(CharPointer pointer, long... sizes) {
        this(pointer, sizes, strides(sizes));
    }

    /** Constructor to set the {@link #pointer}, {@link #sizes} and {@link #strides}. */
    public CharRawIndexer(CharPointer pointer, long[] sizes, long[] strides) {
        super(sizes, strides);
        this.pointer = pointer;
        base = pointer.address() + pointer.position() * VALUE_BYTES;
        size = pointer.limit() - pointer.position();
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    public char getRaw(long i) {
        return RAW.getChar(base + checkIndex(i, size) * VALUE_BYTES);
    }
    @Override public char get(long i) {
        return getRaw(index(i));
    }
    @Override public CharIndexer get(long i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = getRaw(index(i) + n);
        }
        return this;
    }
    @Override public char get(long i, long j) {
        return getRaw(index(i, j));
    }
    @Override public CharIndexer get(long i, long j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = getRaw(index(i, j) + n);
        }
        return this;
    }
    @Override public char get(long i, long j, long k) {
        return getRaw(index(i, j, k));
    }
    @Override public char get(long... indices) {
        return getRaw(index(indices));
    }
    @Override public CharIndexer get(long[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = getRaw(index(indices) + n);
        }
        return this;
    }

    public CharIndexer putRaw(long i, char c) {
        RAW.putChar(base + checkIndex(i, size) * VALUE_BYTES, c);
        return this;
    }
    @Override public CharIndexer put(long i, char c) {
        return putRaw(index(i), c);
    }
    @Override public CharIndexer put(long i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i) + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(long i, long j, char c) {
        putRaw(index(i, j), c);
        return this;
    }
    @Override public CharIndexer put(long i, long j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(i, j) + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(long i, long j, long k, char c) {
        putRaw(index(i, j, k), c);
        return this;
    }
    @Override public CharIndexer put(long[] indices, char c) {
        putRaw(index(indices), c);
        return this;
    }
    @Override public CharIndexer put(long[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            putRaw(index(indices) + n, c[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
