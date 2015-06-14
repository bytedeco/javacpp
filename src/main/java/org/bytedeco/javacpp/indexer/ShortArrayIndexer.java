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
