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

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for an {@link IntPointer}.
 *
 * @author Samuel Audet
 */
public class IntRawIndexer extends IntIndexer {
    /** The backing pointer. */
    protected IntPointer pointer;

    /** Calls {@code IntRawIndexer(pointer, Index.create(pointer.limit()))}. */
    public IntRawIndexer(IntPointer pointer) {
        this(pointer, Index.create(pointer.limit()));
    }

    /** Calls {@code IntRawIndexer(pointer, Index.create(sizes))}. */
    public IntRawIndexer(IntPointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code IntRawIndexer(pointer, Index.create(sizes, strides))}. */
    public IntRawIndexer(IntPointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public IntRawIndexer(IntPointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public IntIndexer reindex(Index index) {
        return new IntRawIndexer(pointer, index);
    }

    @Override public int get(long i) {
        return pointer.get((int)index(i));
    }
    @Override public IntIndexer get(long i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = pointer.get((int)index(i) + n);
        }
        return this;
    }
    @Override public int get(long i, long j) {
        return pointer.get((int)index(i, j));
    }
    @Override public IntIndexer get(long i, long j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = pointer.get((int)index(i, j) + n);
        }
        return this;
    }
    @Override public int get(long i, long j, long k) {
        return pointer.get((int)index(i, j, k));
    }
    @Override public int get(long... indices) {
        return pointer.get((int)index(indices));
    }
    @Override public IntIndexer get(long[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            m[offset + n] = pointer.get((int)index(indices) + n);
        }
        return this;
    }

    @Override public IntIndexer put(long i, int n) {
        pointer.put((int)index(i), n);
        return this;
    }
    @Override public IntIndexer put(long i, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i) + n, m[offset + n]);
        }
        return this;
    }
    @Override public IntIndexer put(long i, long j, int n) {
        pointer.put((int)index(i, j), n);
        return this;
    }
    @Override public IntIndexer put(long i, long j, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i, j) + n, m[offset + n]);
        }
        return this;
    }
    @Override public IntIndexer put(long i, long j, long k, int n) {
        pointer.put((int)index(i, j, k), n);
        return this;
    }
    @Override public IntIndexer put(long[] indices, int n) {
        pointer.put((int)index(indices), n);
        return this;
    }
    @Override public IntIndexer put(long[] indices, int[] m, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(indices) + n, m[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
