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
 * An indexer for a {@code short[]} array.
 *
 * @author Samuel Audet
 */
public class ShortArrayIndexer extends ShortIndexer {
    /** The backing array. */
    protected short[] array;

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public ShortArrayIndexer(short[] array, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public short[] array() {
        return array;
    }

    @Override public short get(int i) {
        return array[i];
    }
    @Override public ShortIndexer get(int i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = array[i * strides[0] + n];
        }
        return this;
    }
    @Override public short get(int i, int j) {
        return array[i * strides[0] + j];
    }
    @Override public ShortIndexer get(int i, int j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = array[i * strides[0] + j * strides[1] + n];
        }
        return this;
    }
    @Override public short get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k];
    }
    @Override public short get(int ... indices) {
        return array[index(indices)];
    }
    @Override public ShortIndexer get(int[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = array[index(indices) + n];
        }
        return this;
    }

    @Override public ShortIndexer put(int i, short s) {
        array[i] = s;
        return this;
    }
    @Override public ShortIndexer put(int i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + n] = s[offset + n];
        }
        return this;
    }
    @Override public ShortIndexer put(int i, int j, short s) {
        array[i * strides[0] + j] = s;
        return this;
    }
    @Override public ShortIndexer put(int i, int j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + j * strides[1] + n] = s[offset + n];
        }
        return this;
    }
    @Override public ShortIndexer put(int i, int j, int k, short s) {
        array[i * strides[0] + j * strides[1] + k] = s;
        return this;
    }
    @Override public ShortIndexer put(int[] indices, short s) {
        array[index(indices)] = s;
        return this;
    }
    @Override public ShortIndexer put(int[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[index(indices) + n] = s[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
