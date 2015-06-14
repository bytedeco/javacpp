/*
 * Copyright (C) 2014 Samuel Audet
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
