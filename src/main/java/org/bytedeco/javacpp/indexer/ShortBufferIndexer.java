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
import java.nio.ShortBuffer;

/**
 * An indexer for a {@link ShortBuffer}.
 *
 * @author Samuel Audet
 */
public class ShortBufferIndexer extends ShortIndexer {
    /** The backing buffer. */
    protected ShortBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public ShortBufferIndexer(ShortBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public short get(int i) {
        return buffer.get(i);
    }
    @Override public ShortIndexer get(int i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public short get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public ShortIndexer get(int i, int j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public short get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public short get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public ShortIndexer get(int[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public ShortIndexer put(int i, short s) {
        buffer.put(i, s);
        return this;
    }
    @Override public ShortIndexer put(int i, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(int i, int j, short s) {
        buffer.put(i * strides[0] + j, s);
        return this;
    }
    @Override public ShortIndexer put(int i, int j, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, s[offset + n]);
        }
        return this;
    }
    @Override public ShortIndexer put(int i, int j, int k, short s) {
        buffer.put(i * strides[0] + j * strides[1] + k, s);
        return this;
    }
    @Override public ShortIndexer put(int[] indices, short s) {
        buffer.put(index(indices), s);
        return this;
    }
    @Override public ShortIndexer put(int[] indices, short[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, s[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
