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

import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link CharPointer}.
 *
 * @author Samuel Audet
 */
public class CharRawIndexer extends CharIndexer {
    /** The backing pointer. */
    protected CharPointer pointer;

    /** Calls {@code CharRawIndexer(pointer, Index.create(pointer.limit()))}. */
    public CharRawIndexer(CharPointer pointer) {
        this(pointer, Index.create(pointer.limit()));
    }

    /** Calls {@code CharRawIndexer(pointer, Index.create(sizes))}. */
    public CharRawIndexer(CharPointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code CharRawIndexer(pointer, Index.create(sizes, strides))}. */
    public CharRawIndexer(CharPointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public CharRawIndexer(CharPointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public CharIndexer reindex(Index index) {
        return new CharRawIndexer(pointer, index);
    }

    @Override public char get(long i) {
        return pointer.get((int)index(i));
    }
    @Override public CharIndexer get(long i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = pointer.get((int)index(i) + n);
        }
        return this;
    }
    @Override public char get(long i, long j) {
        return pointer.get((int)index(i, j));
    }
    @Override public CharIndexer get(long i, long j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = pointer.get((int)index(i, j) + n);
        }
        return this;
    }
    @Override public char get(long i, long j, long k) {
        return pointer.get((int)index(i, j, k));
    }
    @Override public char get(long... indices) {
        return pointer.get((int)index(indices));
    }
    @Override public CharIndexer get(long[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = pointer.get((int)index(indices) + n);
        }
        return this;
    }

    @Override public CharIndexer put(long i, char c) {
        pointer.put((int)index(i), c);
        return this;
    }
    @Override public CharIndexer put(long i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i) + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(long i, long j, char c) {
        pointer.put((int)index(i, j), c);
        return this;
    }
    @Override public CharIndexer put(long i, long j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i, j) + n, c[offset + n]);
        }
        return this;
    }
    @Override public CharIndexer put(long i, long j, long k, char c) {
        pointer.put((int)index(i, j, k), c);
        return this;
    }
    @Override public CharIndexer put(long[] indices, char c) {
        pointer.put((int)index(indices), c);
        return this;
    }
    @Override public CharIndexer put(long[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(indices) + n, c[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
