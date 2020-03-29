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

import java.nio.ShortBuffer;
import org.bytedeco.javacpp.tools.Logger;

/**
 * The peer class to native pointers and arrays of {@code short}.
 * All operations take into account the position and limit, when appropriate.
 *
 * @author Samuel Audet
 */
@org.bytedeco.javacpp.annotation.Properties(inherit = org.bytedeco.javacpp.presets.javacpp.class)
public class ShortPointer extends Pointer {
    private static final Logger logger = Logger.create(ShortPointer.class);

    static {
        try {
            Loader.load();
        } catch (Throwable t) {
            logger.warn("Could not load ShortPointer: " + t);
        }
    }

    /**
     * Allocates enough memory for the array and copies it.
     *
     * @param array the array to copy
     * @see #put(short[])
     */
    public ShortPointer(short ... array) {
        this(array.length);
        put(array);
    }
    /**
     * For direct buffers, calls {@link Pointer#Pointer(Buffer)}, while for buffers
     * backed with an array, allocates enough memory for the array and copies it.
     *
     * @param buffer the Buffer to reference or copy
     * @see #put(short[])
     */
    public ShortPointer(ShortBuffer buffer) {
        super(buffer);
        if (buffer != null && !buffer.isDirect() && buffer.hasArray()) {
            short[] array = buffer.array();
            allocateArray(array.length - buffer.arrayOffset());
            put(array, buffer.arrayOffset(), array.length - buffer.arrayOffset());
            position(buffer.position());
            limit(buffer.limit());
        }
    }
    /**
     * Allocates a native {@code short} array of the given size.
     *
     * @param size the number of {@code short} elements to allocate
     */
    public ShortPointer(long size) {
        try {
            allocateArray(size);
            if (size > 0 && address == 0) {
                throw new OutOfMemoryError("Native allocator returned address == 0");
            }
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("No native JavaCPP library in memory. (Has Loader.load() been called?)", e);
        } catch (OutOfMemoryError e) {
            OutOfMemoryError e2 = new OutOfMemoryError("Cannot allocate new ShortPointer(" + size + "): "
                    + "totalBytes = " + formatBytes(totalBytes()) + ", physicalBytes = " + formatBytes(physicalBytes()));
            e2.initCause(e);
            throw e2;
        }
    }
    /** @see Pointer#Pointer() */
    public ShortPointer() { }
    /** @see Pointer#Pointer(Pointer) */
    public ShortPointer(Pointer p) { super(p); }
    private native void allocateArray(long size);

    /** @see Pointer#position(long) */
    @Override public ShortPointer position(long position) {
        return super.position(position);
    }
    /** @see Pointer#limit(long) */
    @Override public ShortPointer limit(long limit) {
        return super.limit(limit);
    }
    /** @see Pointer#capacity(long) */
    @Override public ShortPointer capacity(long capacity) {
        return super.capacity(capacity);
    }
    @Override public int sizeof() {
        return Short.SIZE / Byte.SIZE;
    }

    /** @return {@code get(0)} */
    public short get() { return get(0); }
    /** @return the i-th {@code short} value of a native array */
    public native short get(long i);
    /** @return {@code put(0, s)} */
    public ShortPointer put(short s) { return put(0, s); }
    /**
     * Copies the {@code short} value to the i-th element of a native array.
     *
     * @param i the index into the array
     * @param s the {@code short} value to copy
     * @return this
     */
    public native ShortPointer put(long i, short s);

    /** @return {@code get(array, 0, array.length)} */
    public ShortPointer get(short[] array) { return get(array, 0, array.length); }
    /** @return {@code put(array, 0, array.length)} */
    public ShortPointer put(short ... array) { return put(array, 0, array.length); }
    /**
     * Reads a portion of the native array into a Java array.
     *
     * @param array the array to write to
     * @param offset the offset into the array where to start writing
     * @param length the length of data to read and write
     * @return this
     */
    public native ShortPointer get(short[] array, int offset, int length);
    /**
     * Writes a portion of a Java array into the native array.
     *
     * @param array the array to read from
     * @param offset the offset into the array where to start reading
     * @param length the length of data to read and write
     * @return this
     */
    public native ShortPointer put(short[] array, int offset, int length);

    /** @return {@code asByteBuffer().asShortBuffer()} */
    @Override public final ShortBuffer asBuffer() {
        return asByteBuffer().asShortBuffer();
    }
}
