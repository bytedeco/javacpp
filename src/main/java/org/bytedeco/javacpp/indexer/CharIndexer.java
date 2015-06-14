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

import java.nio.CharBuffer;
import org.bytedeco.javacpp.CharPointer;

/**
 * Abstract indexer for the {@code char} primitive type.
 *
 * @author Samuel Audet
 */
public abstract class CharIndexer extends Indexer {
    protected CharIndexer(int[] sizes, int[] strides) {
        super(sizes, strides);
    }

    /** @return {@code new CharArrayIndexer(array, sizes, strides)} */
    public static CharIndexer create(char[] array, int[] sizes, int[] strides) {
        return new CharArrayIndexer(array, sizes, strides);
    }
    /** @return {@code new CharBufferIndexer(buffer, sizes, strides)} */
    public static CharIndexer create(CharBuffer buffer, int[] sizes, int[] strides) {
        return new CharBufferIndexer(buffer, sizes, strides);
    }
    /** @return {@code create(pointer, sizes, strides, true)} */
    public static CharIndexer create(CharPointer pointer, int[] sizes, int[] strides) {
        return create(pointer, sizes, strides, true);
    }
    /**
     * Creates a char indexer to access efficiently the data of a pointer.
     *
     * @param pointer data to access via a buffer or to copy to an array
     * @param direct {@code true} to use a direct buffer, see {@link Indexer} for details
     * @return the new char array backed by a buffer or an array
     */
    public static CharIndexer create(final CharPointer pointer, int[] sizes, int[] strides, boolean direct) {
        if (direct) {
            return new CharBufferIndexer(pointer.asBuffer(), sizes, strides);
        } else {
            final int position = pointer.position();
            char[] array = new char[pointer.limit() - position];
            pointer.get(array);
            return new CharArrayIndexer(array, sizes, strides) {
                @Override public void release() {
                    pointer.position(position).put(array);
                    super.release();
                }
            };
        }
    }

    /** @return {@code array/buffer[i]} */
    public abstract char get(int i);
    /** @return {@code this} where {@code c = array/buffer[i]} */
    public CharIndexer get(int i, char[] c) { return get(i, c, 0, c.length); }
    /** @return {@code this} where {@code c[offset:offset + length] = array/buffer[i]} */
    public abstract CharIndexer get(int i, char[] c, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract char get(int i, int j);
    /** @return {@code this} where {@code c = array/buffer[i * strides[0] + j]} */
    public CharIndexer get(int i, int j, char[] c) { return get(i, j, c, 0, c.length); }
    /** @return {@code this} where {@code c[offset:offset + length] = array/buffer[i * strides[0] + j]} */
    public abstract CharIndexer get(int i, int j, char[] c, int offset, int length);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract char get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract char get(int ... indices);
    /** @return {@code this} where {@code c = array/buffer[index(indices)]} */
    public CharIndexer get(int[] indices, char[] c) { return get(indices, c, 0, c.length); }
    /** @return {@code this} where {@code c[offset:offset + length] = array/buffer[index(indices)]} */
    public abstract CharIndexer get(int[] indices, char[] c, int offset, int length);

    /** @return {@code this} where {@code array/buffer[i] = c} */
    public abstract CharIndexer put(int i, char c);
    /** @return {@code this} where {@code array/buffer[i] = c} */
    public CharIndexer put(int i, char ... c) { return put(i, c, 0, c.length); }
    /** @return {@code this} where {@code array/buffer[i] = c[offset:offset + length]} */
    public abstract CharIndexer put(int i, char[] c, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = c} */
    public abstract CharIndexer put(int i, int j, char c);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = c} */
    public CharIndexer put(int i, int j, char ... c) { return put(i, j, c, 0, c.length); }
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = c[offset:offset + length]} */
    public abstract CharIndexer put(int i, int j, char[] c, int offset, int length);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = c} */
    public abstract CharIndexer put(int i, int j, int k, char c);
    /** @return {@code this} where {@code array/buffer[index(indices)] = c} */
    public abstract CharIndexer put(int[] indices, char c);
    /** @return {@code this} where {@code array/buffer[index(indices)] = c} */
    public CharIndexer put(int[] indices, char ... c) { return put(indices, c, 0, c.length); }
    /** @return {@code this} where {@code array/buffer[index(indices)] = c[offset:offset + length]} */
    public abstract CharIndexer put(int[] indices, char[] c, int offset, int length);

    @Override public double getDouble(int ... indices) { return get(indices); }
    @Override public CharIndexer putDouble(int[] indices, double c) { return put(indices, (char)c); }
}
