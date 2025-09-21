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

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Pointer;


/**
 * An indexer for a {@link FloatPointer}.
 *
 * @author Samuel Audet
 */
public class FloatRawIndexer extends FloatIndexer {
    /** The backing pointer. */
    protected FloatPointer pointer;

    /** Calls {@code FloatRawIndexer(pointer, Index.create(pointer.limit()))}. */
    public FloatRawIndexer(FloatPointer pointer) {
        this(pointer, Index.create(pointer.limit()));
    }

    /** Calls {@code FloatRawIndexer(pointer, Index.create(sizes))}. */
    public FloatRawIndexer(FloatPointer pointer, long... sizes) {
        this(pointer, Index.create(sizes));
    }

    /** Calls {@code FloatRawIndexer(pointer, Index.create(sizes, strides))}. */
    public FloatRawIndexer(FloatPointer pointer, long[] sizes, long[] strides) {
        this(pointer, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #pointer} and {@link #index}. */
    public FloatRawIndexer(FloatPointer pointer, Index index) {
        super(index);
        this.pointer = pointer;
    }

    @Override public Pointer pointer() {
        return pointer;
    }

    @Override public FloatIndexer reindex(Index index) {
        return new FloatRawIndexer(pointer, index);
    }

    @Override public float get(long i) {
        return pointer.get(index(i));
    }
    @Override public FloatIndexer get(long i, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = pointer.get(index(i) + n);
        }
        return this;
    }
    @Override public float get(long i, long j) {
        return pointer.get(index(i, j));
    }
    @Override public FloatIndexer get(long i, long j, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = pointer.get(index(i, j) + n);
        }
        return this;
    }
    @Override public float get(long i, long j, long k) {
        return pointer.get(index(i, j, k));
    }
    @Override public float get(long... indices) {
        return pointer.get(index(indices));
    }
    @Override public FloatIndexer get(long[] indices, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = pointer.get(index(indices) + n);
        }
        return this;
    }

    @Override public FloatIndexer put(long i, float f) {
        pointer.put(index(i), f);
        return this;
    }
    @Override public FloatIndexer put(long i, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put(index(i) + n, f[offset + n]);
        }
        return this;
    }
    @Override public FloatIndexer put(long i, long j, float f) {
        pointer.put(index(i, j), f);
        return this;
    }
    @Override public FloatIndexer put(long i, long j, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put(index(i, j) + n, f[offset + n]);
        }
        return this;
    }
    @Override public FloatIndexer put(long i, long j, long k, float f) {
        pointer.put(index(i, j, k), f);
        return this;
    }
    @Override public FloatIndexer put(long[] indices, float f) {
        pointer.put(index(indices), f);
        return this;
    }
    @Override public FloatIndexer put(long[] indices, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            pointer.put(index(indices) + n, f[offset + n]);
        }
        return this;
    }

    @Override public void release() { pointer = null; }
}
