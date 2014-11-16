/*
 * Copyright (C) 2014 Samuel Audet
 *
 * This file is part of JavaCPP.
 *
 * JavaCPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bytedeco.javacpp.indexer;

/**
 * An indexer for a {@code long[]} array.
 *
 * @author Samuel Audet
 */
public class LongArrayIndexer extends LongIndexer {
    /** The backing array. */
    protected long[] array;

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public LongArrayIndexer(long[] array, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public long[] array() {
        return array;
    }

    @Override public long get(int i) {
        return array[i];
    }
    @Override public LongIndexer get(int i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = array[i * strides[0] + n];
        }
        return this;
    }
    @Override public long get(int i, int j) {
        return array[i * strides[0] + j];
    }
    @Override public LongIndexer get(int i, int j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = array[i * strides[0] + j * strides[1] + n];
        }
        return this;
    }
    @Override public long get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k];
    }
    @Override public long get(int ... indices) {
        return array[index(indices)];
    }
    @Override public LongIndexer get(int[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            l[offset + n] = array[index(indices) + n];
        }
        return this;
    }

    @Override public LongIndexer put(int i, long l) {
        array[i] = l;
        return this;
    }
    @Override public LongIndexer put(int i, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + n] = l[offset + n];
        }
        return this;
    }
    @Override public LongIndexer put(int i, int j, long l) {
        array[i * strides[0] + j] = l;
        return this;
    }
    @Override public LongIndexer put(int i, int j, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + j * strides[1] + n] = l[offset + n];
        }
        return this;
    }
    @Override public LongIndexer put(int i, int j, int k, long l) {
        array[i * strides[0] + j * strides[1] + k] = l;
        return this;
    }
    @Override public LongIndexer put(int[] indices, long l) {
        array[index(indices)] = l;
        return this;
    }
    @Override public LongIndexer put(int[] indices, long[] l, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[index(indices) + n] = l[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
