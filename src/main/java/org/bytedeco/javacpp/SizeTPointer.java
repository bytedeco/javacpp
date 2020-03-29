/*
 * Copyright (C) 2011-2020 Samuel Audet
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

package org.bytedeco.javacpp;

import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.tools.Logger;

/**
 * The peer class to native pointers and arrays of {@code size_t}.
 * All operations take into account the position and limit, when appropriate.
 * <p>
 * We need this class because the size differs between 32-bit and 64-bit platforms.
 *
 * @author Samuel Audet
 */
@Name("size_t")
@org.bytedeco.javacpp.annotation.Properties(inherit = org.bytedeco.javacpp.presets.javacpp.class)
public class SizeTPointer extends Pointer {
    private static final Logger logger = Logger.create(SizeTPointer.class);

    static {
        try {
            Loader.load();
        } catch (Throwable t) {
            logger.warn("Could not load SizeTPointer: " + t);
        }
    }

    /**
     * Allocates enough memory for the array and copies it.
     *
     * @param array the array to copy
     * @see #put(long[])
     */
    public SizeTPointer(long ... array) {
        this(array.length);
        put(array);
    }
    /**
     * Allocates a native {@code size_t} array of the given size.
     *
     * @param size the number of {@code size_t} elements to allocate
     */
    public SizeTPointer(long size) {
        try {
            allocateArray(size);
            if (size > 0 && address == 0) {
                throw new OutOfMemoryError("Native allocator returned address == 0");
            }
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        } catch (OutOfMemoryError e) {
            OutOfMemoryError e2 = new OutOfMemoryError("Cannot allocate new SizeTPointer(" + size + "): "
                    + "totalBytes = " + formatBytes(totalBytes()) + ", physicalBytes = " + formatBytes(physicalBytes()));
            e2.initCause(e);
            throw e2;
        }
    }
    /** @see Pointer#Pointer() */
    public SizeTPointer() { }
    /** @see Pointer#Pointer(Pointer) */
    public SizeTPointer(Pointer p) { super(p); }
    private native void allocateArray(long size);

    /** @see Pointer#position(long) */
    @Override public SizeTPointer position(long position) {
        return super.position(position);
    }
    /** @see Pointer#limit(long) */
    @Override public SizeTPointer limit(long limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(long) */
    @Override public SizeTPointer capacity(long capacity) {
        return super.capacity(capacity);
    }

    /** @return {@code get(0)} */
    public long get() { return get(0); }
    /** @return the i-th {@code size_t} value of a native array */
    @Cast("size_t") public native long get(long i);
    /** @return {@code put(0, s)} */
    public SizeTPointer put(long s) { return put(0, s); }
    /**
     * Copies the {@code size_t} value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param s the {@code size_t} value to copy
     * @return this
     */
    public native SizeTPointer put(long i, long s);

    /** @return {@code get(array, 0, array.length)} */
    public SizeTPointer get(long[] array) { return get(array, 0, array.length); }
    /** @return {@code put(array, 0, array.length)} */
    public SizeTPointer put(long ... array) { return put(array, 0, array.length); }
    /**
     * Reads a portion of the native array into a Java array.
     *
     * @param array the array to write to
     * @param offset the offset into the array where to start writing
     * @param length the length of data to read and write
     * @return this
     */
    public SizeTPointer get(long[] array, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            array[i] = get(i);
        }
        return this;
    }
    /**
     * Writes a portion of a Java array into the native array.
     *
     * @param array the array to read from
     * @param offset the offset into the array where to start reading
     * @param length the length of data to read and write
     * @return this
     */
    public SizeTPointer put(long[] array, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            put(i, array[i]);
        }
        return this;
    }
}
