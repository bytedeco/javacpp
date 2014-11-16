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

import java.nio.Buffer;
import java.nio.DoubleBuffer;

/**
 * An indexer for a {@link DoubleBuffer}.
 *
 * @author Samuel Audet
 */
public class DoubleBufferIndexer extends DoubleIndexer {
    /** The backing buffer. */
    protected DoubleBuffer buffer;

    /** Constructor to set the {@link #buffer}, {@link #sizes} and {@link #strides}. */
    public DoubleBufferIndexer(DoubleBuffer buffer, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.buffer = buffer;
    }

    @Override public Buffer buffer() {
        return buffer;
    }

    @Override public double get(int i) {
        return buffer.get(i);
    }
    @Override public DoubleIndexer get(int i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = buffer.get(i * strides[0] + n);
        }
        return this;
    }
    @Override public double get(int i, int j) {
        return buffer.get(i * strides[0] + j);
    }
    @Override public DoubleIndexer get(int i, int j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = buffer.get(i * strides[0] + j * strides[1] + n);
        }
        return this;
    }
    @Override public double get(int i, int j, int k) {
        return buffer.get(i * strides[0] + j * strides[1] + k);
    }
    @Override public double get(int ... indices) {
        return buffer.get(index(indices));
    }
    @Override public DoubleIndexer get(int[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = buffer.get(index(indices) + n);
        }
        return this;
    }

    @Override public DoubleIndexer put(int i, double d) {
        buffer.put(i, d);
        return this;
    }
    @Override public DoubleIndexer put(int i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + n, d[offset + n]);
        }
        return this;
    }
    @Override public DoubleIndexer put(int i, int j, double d) {
        buffer.put(i * strides[0] + j, d);
        return this;
    }
    @Override public DoubleIndexer put(int i, int j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(i * strides[0] + j * strides[1] + n, d[offset + n]);
        }
        return this;
    }
    @Override public DoubleIndexer put(int i, int j, int k, double d) {
        buffer.put(i * strides[0] + j * strides[1] + k, d);
        return this;
    }
    @Override public DoubleIndexer put(int[] indices, double d) {
        buffer.put(index(indices), d);
        return this;
    }
    @Override public DoubleIndexer put(int[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            buffer.put(index(indices) + n, d[offset + n]);
        }
        return this;
    }

    @Override public void release() { buffer = null; }
}
