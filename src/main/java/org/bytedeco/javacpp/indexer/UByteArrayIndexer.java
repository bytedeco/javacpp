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

/**
 * An indexer for a {@code byte[]} array, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UByteArrayIndexer extends UByteIndexer {
    /** The backing array. */
    protected byte[] array;

    /** Calls {@code UByteArrayIndexer(array, Index.create(array.length))}. */
    public UByteArrayIndexer(byte[] array) {
        this(array, Index.create(array.length));
    }

    /** Calls {@code UByteArrayIndexer(array, Index.create(sizes))}. */
    public UByteArrayIndexer(byte[] array, long... sizes) {
        this(array, Index.create(sizes));
    }

    /** Calls {@code UByteArrayIndexer(array, Index.create(sizes, strides))}. */
    public UByteArrayIndexer(byte[] array, long[] sizes, long[] strides) {
        this(array, Index.create(sizes, strides));
    }

    /** Constructor to set the {@link #array} and {@link #index}. */
    public UByteArrayIndexer(byte[] array, Index index) {
        super(index);
        this.array = array;
    }

    @Override public byte[] array() {
        return array;
    }

    @Override public UByteIndexer reindex(Index index) {
        return new UByteArrayIndexer(array, index);
    }

    @Override public int get(long i) {
        return array[(int)index(i)] & 0xFF;
    }
    @Override public UByteIndexer get(long i, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[(int)index(i) + n] & 0xFF;
        }
        return this;
    }
    @Override public int get(long i, long j) {
        return array[(int)index(i, j)] & 0xFF;
    }
    @Override public UByteIndexer get(long i, long j, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[(int)index(i, j) + n] & 0xFF;
        }
        return this;
    }
    @Override public int get(long i, long j, long k) {
        return array[(int)index(i, j, k)] & 0xFF;
    }
    @Override public int get(long... indices) {
        return array[(int)index(indices)] & 0xFF;
    }
    @Override public UByteIndexer get(long[] indices, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[(int)index(indices) + n] & 0xFF;
        }
        return this;
    }

    @Override public UByteIndexer put(long i, int b) {
        array[(int)index(i)] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(long i, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i) + n] = (byte)b[offset + n];
        }
        return this;
    }
    @Override public UByteIndexer put(long i, long j, int b) {
        array[(int)index(i, j)] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(long i, long j, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(i, j) + n] = (byte)b[offset + n];
        }
        return this;
    }
    @Override public UByteIndexer put(long i, long j, long k, int b) {
        array[(int)index(i, j, k)] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(long[] indices, int b) {
        array[(int)index(indices)] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(long[] indices, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[(int)index(indices) + n] = (byte)b[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
