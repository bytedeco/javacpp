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

import java.nio.CharBuffer;

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

    /** @return {@code array/buffer[i]} */
    public abstract char get(int i);
    /** @return {@code array/buffer[i * strides[0] + j]} */
    public abstract char get(int i, int j);
    /** @return {@code array/buffer[i * strides[0] + j * strides[1] + k]} */
    public abstract char get(int i, int j, int k);
    /** @return {@code array/buffer[index(indices)]} */
    public abstract char get(int ... indices);

    /** @return {@code this} where {@code array/buffer[i] = c} */
    public abstract CharIndexer put(int i, char c);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j] = c} */
    public abstract CharIndexer put(int i, int j, char c);
    /** @return {@code this} where {@code array/buffer[i * strides[0] + j * strides[1] + k] = c} */
    public abstract CharIndexer put(int i, int j, int k, char c);
    /** @return {@code this} where {@code array/buffer[index(indices)] = c} */
    public abstract CharIndexer put(int[] indices, char c);
}
