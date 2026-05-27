/*
 * Copyright (C) 2015-2019 Samuel Audet
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
 * An indexer for a {@link ShortPointer}, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UShortRawIndexer extends UShortIndexer {
    /** The backing pointer. */
    protected ShortPointer pointer;

    /** Calls {@code UShortRawIndexer(pointer, Index.create(pointer.limit()))}. */
    public UShortRawIndexer(ShortPointer pointer) {
        this(pointer, Index.create(pointer.limit()));
    }

    /** Calls {@code UShortRawIndexer(pointer, Index.create(sizes))}. */
    public UShortRawIndexer(ShortPointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code UShortRawIndexer(pointer, Index.create(sizes, strides))}. */
    public UShortRawIndexer(ShortPointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public UShortRawIndexer(ShortPointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public UShortIndexer reindex(Index index) {
        return new UShortRawIndexer(pointer, index);
    }

    @Override public int get(long i) {
        return pointer.get((int)index(i)) & 0xFFFF;
    }
    @Override public UShortIndexer get(long i, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = pointer.get((int)index(i) + n) & 0xFFFF;
        }
        return this;
    }
    @Override public int get(long i, long j) {
        return pointer.get((int)index(i, j)) & 0xFFFF;
    }
    @Override public UShortIndexer get(long i, long j, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = pointer.get((int)index(i, j) + n) & 0xFFFF;
        }
        return this;
    }
    @Override public int get(long i, long j, long k) {
        return pointer.get((int)index(i, j, k)) & 0xFFFF;
    }
    @Override public int get(long... indices) {
        return pointer.get((int)index(indices)) & 0xFFFF;
    }
    @Override public UShortIndexer get(long[] indices, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = pointer.get((int)index(indices) + n) & 0xFFFF;
        }
        return this;
    }

    @Override public UShortIndexer put(long i, int s) {
        pointer.put((int)index(i), (short)s);
        return this;
    }
    @Override public UShortIndexer put(long i, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i) + n, (short)s[offset + n]);
        }
        return this;
    }
    @Override public UShortIndexer put(long i, long j, int s) {
        pointer.put((int)index(i, j), (short)s);
        return this;
    }
    @Override public UShortIndexer put(long i, long j, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i, j) + n, (short)s[offset + n]);
        }
        return this;
    }
    @Override public UShortIndexer put(long i, long j, long k, int s) {
        pointer.put((int)index(i, j, k), (short)s);
        return this;
    }
    @Override public UShortIndexer put(long[] indices, int s) {
        pointer.put((int)index(indices), (short)s);
        return this;
    }
    @Override public UShortIndexer put(long[] indices, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(indices) + n, (short)s[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
