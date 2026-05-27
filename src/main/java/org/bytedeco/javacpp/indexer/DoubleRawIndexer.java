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

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Pointer;

/**
 * An indexer for a {@link DoublePointer}.
 *
 * @author Samuel Audet
 */
public class DoubleRawIndexer extends DoubleIndexer {
    /** The backing pointer. */
    protected DoublePointer pointer;

    /** Calls {@code DoubleRawIndexer(pointer, Index.create(pointer.limit()))}. */
    public DoubleRawIndexer(DoublePointer pointer) {
        this(pointer, Index.create(pointer.limit()));
    }

    /** Calls {@code DoubleRawIndexer(pointer, Index.create(sizes))}. */
    public DoubleRawIndexer(DoublePointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code DoubleRawIndexer(pointer, Index.create(sizes, strides))}. */
    public DoubleRawIndexer(DoublePointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public DoubleRawIndexer(DoublePointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public DoubleIndexer reindex(Index index) {
        return new DoubleRawIndexer(pointer, index);
    }

    @Override public double get(long i) {
        return pointer.get((int)index(i));
    }
    @Override public DoubleIndexer get(long i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = pointer.get((int)index(i) + n);
        }
        return this;
    }
    @Override public double get(long i, long j) {
        return pointer.get((int)index(i, j));
    }
    @Override public DoubleIndexer get(long i, long j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = pointer.get((int)index(i, j) + n);
        }
        return this;
    }
    @Override public double get(long i, long j, long k) {
        return pointer.get((int)index(i, j, k));
    }
    @Override public double get(long... indices) {
        return pointer.get((int)index(indices));
    }
    @Override public DoubleIndexer get(long[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = pointer.get((int)index(indices) + n);
        }
        return this;
    }

    @Override public DoubleIndexer put(long i, double d) {
        pointer.put((int)index(i), d);
        return this;
    }
    @Override public DoubleIndexer put(long i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i) + n, d[offset + n]);
        }
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, double d) {
        pointer.put((int)index(i, j), d);
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(i, j) + n, d[offset + n]);
        }
        return this;
    }
    @Override public DoubleIndexer put(long i, long j, long k, double d) {
        pointer.put((int)index(i, j, k), d);
        return this;
    }
    @Override public DoubleIndexer put(long[] indices, double d) {
        pointer.put((int)index(indices), d);
        return this;
    }
    @Override public DoubleIndexer put(long[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put((int)index(indices) + n, d[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
