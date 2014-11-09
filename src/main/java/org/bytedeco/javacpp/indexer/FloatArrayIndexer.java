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
    @Override public float get(int i, int j) {
        return array[i * strides[0] + j];
    }
    @Override public float get(int i, int j, int k) {
        return array[i * strides[0] + j * strides[1] + k];
    }
    @Override public float get(int ... indices) {
        return array[index(indices)];
    }

    @Override public FloatIndexer put(int i, float f) {
        array[i] = f;
        return this;
    }
    @Override public FloatIndexer put(int i, int j, float f) {
        array[i * strides[0] + j] = f;
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

    @Override public void release() { array = null; }
}
