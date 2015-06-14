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

import java.nio.ByteBuffer;
import org.bytedeco.javacpp.BytePointer;

/**
 * Abstract indexer for the {@code byte} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class ByteIndexer extends Indexer {
    protected ByteIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new ByteArrayIndexer(array, sizes, strides)} */
    public static ByteIndexer create(byte[] array, int[] sizes, int[] strides) {
        return new ByteArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new ByteBufferIndexer(buffer, sizes, strides)} */
    public static ByteIndexer create(ByteBuffer buffer, int[] sizes, int[] strides) {
        return new ByteBufferIndexer(buffer, sizes, strides);
    }
    /** @return {@code create(pointer, sizes, strides, true)} */
    public static ByteIndexer create(BytePointer pointer, int[] sizes, int[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a byte indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new byte array backed by a buffer or an array
     */
    public static ByteIndexer create(final BytePointer pointer, int[] sizes, int[] strides, boolean direct) {
        if (direct) {
            return new ByteBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final int position = pointer.position();
            byte[] array = new byte[pointer.limit() - position];
            pointer.get(array);
            return new ByteArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** @return {@code array/buffer[i]} */
    public abstract byte get(int i);
    /** @return {@code this} where {@code b = array/buffer[i]} */
    public ByteIndexer get(int i, byte[] b) { return get(i, b, 0, b.length); }
    /** @return {@code this} where {@code b[offset:offset + length] = array/buffer[i]} */
    public abstract ByteIndexer get(int i, byte[] b, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract byte get(int i, int j);
    /** @return {@code this} where {@code b = array/buffer[i * strides[0] + j]} */
    public ByteIndexer get(int i, int j, byte[] b) { return get(i, j, b, 0, b.length); }
    /** @return {@code this} where {@code b[offset:offset + length] = array/buffer[i * strides[0] + j]} */
    public abstract ByteIndexer get(int i, int j, byte[] b, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract byte get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract byte get(int ... indices);
    /** @return {@code this} where {@code b = array/buffer[index(indices)]} */
    public ByteIndexer get(int[] indices, byte[] b) { return get(indices, b, 0, b.length); }
    /** @return {@code this} where {@code b[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract ByteIndexer get(int[] indices, byte[] b, int offset, int length);

    /** @return {@code this} where {@code array/buffer[i] = b} */
    public abstract ByteIndexer put(int i, byte b);
    /** @return {@code this} where {@code array/buffer[i] = b} */
    public ByteIndexer put(int i, byte ... b) { return put(i, b, 0, b.length); }
    /** @return {@code this} where {@code array/buffer[i] = b[offset:offset + length]} */
    public abstract ByteIndexer put(int i, byte[] b, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = b} */
    public abstract ByteIndexer put(int i, int j, byte b);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = b} */
    public ByteIndexer put(int i, int j, byte ... b) { return put(i, j, b, 0, b.length); }
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = b[offset:offset + length]} */
    public abstract ByteIndexer put(int i, int j, byte[] b, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = b} */
    public abstract ByteIndexer put(int i, int j, int k, byte b);
    /** @return {@code this} where {@code array/buffer[index(indices)] = b} */
    public abstract ByteIndexer put(int[] indices, byte b);
    /** @return {@code this} where {@code array/buffer[index(indices)] = b} */
    public ByteIndexer put(int[] indices, byte ... b) { return put(indices, b, 0, b.length); }
    /** @return {@code this} where {@code array/buffer[index(indices)] = b[offset:offset + length]} */
    public abstract ByteIndexer put(int[] indices, byte[] b, int offset, int length);

    @Override public double getDouble(int ... indices) { return get(indices); }
    @Override public ByteIndexer putDouble(int[] indices, double b) { return put(indices, (byte)b); }
}
