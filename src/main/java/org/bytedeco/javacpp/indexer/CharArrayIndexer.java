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
 * An indexer for a {@code char[]} array.
 *
 * @author Samuel Audet
 */
public class CharArrayIndexer extends CharIndexer {
    /** The backing array. */
    protected char[] array;

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public CharArrayIndexer(char[] array, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public char[] array() {
        return array;
    }

    @Override public char get(int i) {
        return array[i];
    }
    @Override public CharIndexer get(int i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = array[i * strides[0] + n];
        }
        return this;
    }
    @Override public char get(int i, int j) {
        return array[i * strides[0] + j];
    }
    @Override public CharIndexer get(int i, int j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = array[i * strides[0] + j * strides[1] + n];
        }
        return this;
    }
    @Override public char get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k];
    }
    @Override public char get(int ... indices) {
        return array[index(indices)];
    }
    @Override public CharIndexer get(int[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            c[offset + n] = array[index(indices) + n];
        }
        return this;
    }

    @Override public CharIndexer put(int i, char c) {
        array[i] = c;
        return this;
    }
    @Override public CharIndexer put(int i, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + n] = c[offset + n];
        }
        return this;
    }
    @Override public CharIndexer put(int i, int j, char c) {
        array[i * strides[0] + j] = c;
        return this;
    }
    @Override public CharIndexer put(int i, int j, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + j * strides[1] + n] = c[offset + n];
        }
        return this;
    }
    @Override public CharIndexer put(int i, int j, int k, char c) {
        array[i * strides[0] + j * strides[1] + k] = c;
        return this;
    }
    @Override public CharIndexer put(int[] indices, char c) {
        array[index(indices)] = c;
        return this;
    }
    @Override public CharIndexer put(int[] indices, char[] c, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[index(indices) + n] = c[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
