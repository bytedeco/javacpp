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

import java.nio.Buffer;
import java.nio.ShortBuffer;

/**
 * An indexer for a {@link ShortBuffer}, treated as unsigned.
 *
 * @author Samuel Audet
 */
public class UShortBufferIndexer extends UShortIndexer {
    /** The backing buffer. */
    protected ShortBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public UShortBufferIndexer(ShortBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public int get(int i) {
        return buffer.get(i) & 0xFFFF;
    }
    @Override public UShortIndexer get(int i, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(i * strides[0] + n) & 0xFFFF;
        }
        return this;
    }
    @Override public int get(int i, int j) {
        return buffer.get(i * strides[0] + j) & 0xFFFF;
    }
    @Override public UShortIndexer get(int i, int j, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n) & 0xFFFF;
        }
        return this;
    }
    @Override public int get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k) & 0xFFFF;
    }
    @Override public int get(int ... indices) {
        return buffer.get(index(indices)) & 0xFFFF;
    }
    @Override public UShortIndexer get(int[] indices, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            s[offset + n] = buffer.get(index(indices) + n) & 0xFFFF;
        }
        return this;
    }

    @Override public UShortIndexer put(int i, int s) {
        buffer.put(i, (short)s);
        return this;
    }
    @Override public UShortIndexer put(int i, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, (short)s[offset + n]);
        }
        return this;
    }
    @Override public UShortIndexer put(int i, int j, int s) {
        buffer.put(i * strides[0] + j, (short)s);
        return this;
    }
    @Override public UShortIndexer put(int i, int j, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, (short)s[offset + n]);
        }
        return this;
    }
    @Override public UShortIndexer put(int i, int j, int k, int s) {
        buffer.put(i * strides[0] + j * strides[1] + k, (short)s);
        return this;
    }
    @Override public UShortIndexer put(int[] indices, int s) {
        buffer.put(index(indices), (short)s);
        return this;
    }
    @Override public UShortIndexer put(int[] indices, int[] s, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, (short)s[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
