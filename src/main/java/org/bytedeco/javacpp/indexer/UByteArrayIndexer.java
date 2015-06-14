/*
 * Copyright (C) 2015 Samuel Audet
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
