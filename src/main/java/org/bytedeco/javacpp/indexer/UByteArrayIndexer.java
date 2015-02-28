/*
 * Copyright (C) 2015 Samuel Audet
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
 * An indexer for a {@code byte[]} array, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UByteArrayIndexer extends UByteIndexer {
    /** The backing array. */
    protected byte[] array;

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public UByteArrayIndexer(byte[] array, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public byte[] array() {
        return array;
    }

    @Override public int get(int i) {
        return array[i] & 0xFF;
    }
    @Override public UByteIndexer get(int i, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[i * strides[0] + n] & 0xFF;
        }
        return this;
    }
    @Override public int get(int i, int j) {
        return array[i * strides[0] + j] & 0xFF;
    }
    @Override public UByteIndexer get(int i, int j, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[i * strides[0] + j * strides[1] + n] & 0xFF;
        }
        return this;
    }
    @Override public int get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k] & 0xFF;
    }
    @Override public int get(int ... indices) {
        return array[index(indices)] & 0xFF;
    }
    @Override public UByteIndexer get(int[] indices, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            b[offset + n] = array[index(indices) + n] & 0xFF;
        }
        return this;
    }

    @Override public UByteIndexer put(int i, int b) {
        array[i] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(int i, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + n] = (byte)b[offset + n];
        }
        return this;
    }
    @Override public UByteIndexer put(int i, int j, int b) {
        array[i * strides[0] + j] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(int i, int j, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + j * strides[1] + n] = (byte)b[offset + n];
        }
        return this;
    }
    @Override public UByteIndexer put(int i, int j, int k, int b) {
        array[i * strides[0] + j * strides[1] + k] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(int[] indices, int b) {
        array[index(indices)] = (byte)b;
        return this;
    }
    @Override public UByteIndexer put(int[] indices, int[] b, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[index(indices) + n] = (byte)b[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
