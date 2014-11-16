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
 * An indexer for a {@code double[]} array.
 *
 * @author Samuel Audet
 */
public class DoubleArrayIndexer extends DoubleIndexer {
    /** The backing array. */
    protected double[] array;

    /** Constructor to set the {@link #array}, {@link #sizes} and {@link #strides}. */
    public DoubleArrayIndexer(double[] array, int[] sizes, int[] strides) {
        super(sizes, strides);
        this.array = array;
    }

    @Override public double[] array() {
        return array;
    }

    @Override public double get(int i) {
        return array[i];
    }
    @Override public DoubleIndexer get(int i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = array[i * strides[0] + n];
        }
        return this;
    }
    @Override public double get(int i, int j) {
        return array[i * strides[0] + j];
    }
    @Override public DoubleIndexer get(int i, int j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = array[i * strides[0] + j * strides[1] + n];
        }
        return this;
    }
    @Override public double get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k];
    }
    @Override public double get(int ... indices) {
        return array[index(indices)];
    }
    @Override public DoubleIndexer get(int[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            d[offset + n] = array[index(indices) + n];
        }
        return this;
    }

    @Override public DoubleIndexer put(int i, double d) {
        array[i] = d;
        return this;
    }
    @Override public DoubleIndexer put(int i, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + n] = d[offset + n];
        }
        return this;
    }
    @Override public DoubleIndexer put(int i, int j, double d) {
        array[i * strides[0] + j] = d;
        return this;
    }
    @Override public DoubleIndexer put(int i, int j, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[i * strides[0] + j * strides[1] + n] = d[offset + n];
        }
        return this;
    }
    @Override public DoubleIndexer put(int i, int j, int k, double d) {
        array[i * strides[0] + j * strides[1] + k] = d;
        return this;
    }
    @Override public DoubleIndexer put(int[] indices, double d) {
        array[index(indices)] = d;
        return this;
    }
    @Override public DoubleIndexer put(int[] indices, double[] d, int offset, int length) {
        for (int n = 0; n < length; n++) {
            array[index(indices) + n] = d[offset + n];
        }
        return this;
    }

    @Override public void release() { array = null; }
}
