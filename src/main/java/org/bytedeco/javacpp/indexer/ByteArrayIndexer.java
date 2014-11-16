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
 * An indexer for a {@code byte[]} array.
 *
 * @author Samuel Audet
 */
public class ByteArrayIndexer extends ByteIndexer {
    /** The backing array. */
    protected byte[] array;

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public ByteArrayIndexer(byte[] array, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public byte[] array() {
        return array;
    }

    @Override public byte get(int i) {
        return array[i];
    }
    @Override public ByteIndexer get(int i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[i * strides[0] + n];
        }
        return this;
    }
    @Override public byte get(int i, int j) {
        return array[i * strides[0] + j];
    }
    @Override public ByteIndexer get(int i, int j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[i * strides[0] + j * strides[1] + n];
        }
        return this;
    }
    @Override public byte get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k];
    }
    @Override public byte get(int ... indices) {
        return array[index(indices)];
    }
    @Override public ByteIndexer get(int[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[index(indices) + n];
        }
        return this;
    }

    @Override public ByteIndexer put(int i, byte b) {
        array[i] = b;
        return this;
    }
    @Override public ByteIndexer put(int i, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + n] = b[offset + n];
        }
        return this;
    }
    @Override public ByteIndexer put(int i, int j, byte b) {
        array[i * strides[0] + j] = b;
        return this;
    }
    @Override public ByteIndexer put(int i, int j, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + j * strides[1] + n] = b[offset + n];
        }
        return this;
    }
    @Override public ByteIndexer put(int i, int j, int k, byte b) {
        array[i * strides[0] + j * strides[1] + k] = b;
        return this;
    }
    @Override public ByteIndexer put(int[] indices, byte b) {
        array[index(indices)] = b;
        return this;
    }
    @Override public ByteIndexer put(int[] indices, byte[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[index(indices) + n] = b[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
