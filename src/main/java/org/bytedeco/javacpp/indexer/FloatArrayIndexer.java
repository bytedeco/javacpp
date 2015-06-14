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
 * An indexer for a {@code float[]} array.
 *
 * @author Samuel Audet
 */
public class FloatArrayIndexer extends FloatIndexer {
    /** The backing array. */
    protected float[] array;

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public FloatArrayIndexer(float[] array, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public float[] array() {
        return array;
    }

    @Override public float get(int i) {
        return array[i];
    }
    @Override public FloatIndexer get(int i, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = array[i * strides[0] + n];
        }
        return this;
    }
    @Override public float get(int i, int j) {
        return array[i * strides[0] + j];
    }
    @Override public FloatIndexer get(int i, int j, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = array[i * strides[0] + j * strides[1] + n];
        }
        return this;
    }
    @Override public float get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k];
    }
    @Override public float get(int ... indices) {
        return array[index(indices)];
    }
    @Override public FloatIndexer get(int[] indices, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = array[index(indices) + n];
        }
        return this;
    }

    @Override public FloatIndexer put(int i, float f) {
        array[i] = f;
        return this;
    }
    @Override public FloatIndexer put(int i, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + n] = f[offset + n];
        }
        return this;
    }
    @Override public FloatIndexer put(int i, int j, float f) {
        array[i * strides[0] + j] = f;
        return this;
    }
    @Override public FloatIndexer put(int i, int j, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + j * strides[1] + n] = f[offset + n];
        }
        return this;
    }
    @Override public FloatIndexer put(int i, int j, int k, float f) {
        array[i * strides[0] + j * strides[1] + k] = f;
        return this;
    }
    @Override public FloatIndexer put(int[] indices, float f) {
        array[index(indices)] = f;
        return this;
    }
    @Override public FloatIndexer put(int[] indices, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[index(indices) + n] = f[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
