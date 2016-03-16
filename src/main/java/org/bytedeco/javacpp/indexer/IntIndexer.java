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

import java.nio.IntBuffer;
import org.bytedeco.javacpp.IntPointer;

/**
 * Abstract indexer for the {@code int} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class IntIndexer extends Indexer {
    protected IntIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new IntArrayIndexer(array, sizes, strides)} */
    public static IntIndexer create(int[] array, int[] sizes, int[] strides) {
        return new IntArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new IntBufferIndexer(buffer, sizes, strides)} */
    public static IntIndexer create(IntBuffer buffer, int[] sizes, int[] strides) {
        return new IntBufferIndexer(buffer, sizes, strides);
    }
    /** @return {@code create(pointer, sizes, strides, true)} */
    public static IntIndexer create(IntPointer pointer, int[] sizes, int[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a int indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new int array backed by a buffer or an array
     */
    public static IntIndexer create(final IntPointer pointer, int[] sizes, int[] strides, boolean direct) {
        if (direct) {
            return new IntBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final long position = pointer.position();
            int[] array = new int[(int) (pointer.limit() - position)];
            pointer.get(array);
            return new IntArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** @return {@code array/buffer[i]} */
    public abstract int get(int i);
    /** @return {@code this} where {@code n = array/buffer[i]} */
    public IntIndexer get(int i, int[] n) { return get(i, n, 0, n.length); }
    /** @return {@code this} where {@code n[offset:offset + length] = array/buffer[i]} */
    public abstract IntIndexer get(int i, int[] n, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract int get(int i, int j);
    /** @return {@code this} where {@code n = array/buffer[i * strides[0] + j]} */
    public IntIndexer get(int i, int j, int[] n) { return get(i, j, n, 0, n.length); }
    /** @return {@code this} where {@code n[offset:offset + length] = array/buffer[i * strides[0] + j]} */
    public abstract IntIndexer get(int i, int j, int[] n, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract int get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract int get(int ... indices);
    /** @return {@code this} where {@code n = array/buffer[index(indices)]} */
    public IntIndexer get(int[] indices, int[] n) { return get(indices, n, 0, n.length); }
    /** @return {@code this} where {@code n[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract IntIndexer get(int[] indices, int[] n, int offset, int length);

    /** @return {@code this} where {@code array/buffer[i] = n} */
    public abstract IntIndexer put(int i, int n);
    /** @return {@code this} where {@code array/buffer[i] = n} */
    public IntIndexer put(int i, int ... n) { return put(i, n, 0, n.length); }
    /** @return {@code this} where {@code array/buffer[i] = n[offset:offset + length]} */
    public abstract IntIndexer put(int i, int[] n, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = n} */
    public abstract IntIndexer put(int i, int j, int n);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = n} */
    public IntIndexer put(int i, int j, int ... n) { return put(i, j, n, 0, n.length); }
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = n[offset:offset + length]} */
    public abstract IntIndexer put(int i, int j, int[] n, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = n} */
    public abstract IntIndexer put(int i, int j, int k, int n);
    /** @return {@code this} where {@code array/buffer[index(indices)] = n} */
    public abstract IntIndexer put(int[] indices, int n);
    /** @return {@code this} where {@code array/buffer[index(indices)] = n} */
    public IntIndexer put(int[] indices, int ... n) { return put(indices, n, 0, n.length); }
    /** @return {@code this} where {@code array/buffer[index(indices)] = n[offset:offset + length]} */
    public abstract IntIndexer put(int[] indices, int[] n, int offset, int length);

    @Override public double getDouble(int ... indices) { return get(indices); }
    @Override public IntIndexer putDouble(int[] indices, double n) { return put(indices, (int)n); }
}
