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

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * An indexer for a {@link FloatBuffer}.
 *
 * @author Samuel Audet
 */
public class FloatBufferIndexer extends FloatIndexer {
    /** The backing buffer. */
    protected FloatBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public FloatBufferIndexer(FloatBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public float get(int i) {
        return buffer.get(i);
    }
    @Override public FloatIndexer get(int i, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public float get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public FloatIndexer get(int i, int j, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public float get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public float get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public FloatIndexer get(int[] indices, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            f[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public FloatIndexer put(int i, float f) {
        buffer.put(i, f);
        return this;
    }
    @Override public FloatIndexer put(int i, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, f[offset + n]);
        }
        return this;
    }
    @Override public FloatIndexer put(int i, int j, float f) {
        buffer.put(i * strides[0] + j, f);
        return this;
    }
    @Override public FloatIndexer put(int i, int j, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, f[offset + n]);
        }
        return this;
    }
    @Override public FloatIndexer put(int i, int j, int k, float f) {
        buffer.put(i * strides[0] + j * strides[1] + k, f);
        return this;
    }
    @Override public FloatIndexer put(int[] indices, float f) {
        buffer.put(index(indices), f);
        return this;
    }
    @Override public FloatIndexer put(int[] indices, float[] f, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, f[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
