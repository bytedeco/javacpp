/*
 * Copyright (C) 2016-2018 Samuel Audet
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

/**
 * The raw memory interface supporting long indexing.
 *
 * @author Samuel Audet
 */
abstract class Raw {

    static final Raw INSTANCE;
    static {
        if (UnsafeRaw.isAvailable()) {
            INSTANCE = new UnsafeRaw();
        } else {
            INSTANCE = null;
        }
    }
    /** Returns {@link UnsafeRaw} if {@code UnsafeRaw.isAvailable()} or null otherwise. */
    static Raw getInstance() {
        return INSTANCE;
    }

    abstract byte getByte(long address);
    abstract void putByte(long address, byte b);
    abstract short getShort(long address);
    abstract void putShort(long address, short s);
    abstract int getInt(long address);
    abstract void putInt(long address, int i);
    abstract long getLong(long address);
    abstract void putLong(long address, long l);
    abstract float getFloat(long address);
    abstract void putFloat(long address, float f);
    abstract double getDouble(long address);
    abstract void putDouble(long address, double d);
    abstract char getChar(long address);
    abstract void putChar(long address, char c);
    abstract boolean getBoolean(long address);
    abstract void putBoolean(long address, boolean b);

    abstract byte getByte(byte[] array, long offset);
    abstract void putByte(byte[] array, long offset, byte b);
    abstract short getShort(byte[] array, long offset);
    abstract void putShort(byte[] array, long offset, short s);
    abstract int getInt(byte[] array, long offset);
    abstract void putInt(byte[] array, long offset, int i);
    abstract long getLong(byte[] array, long offset);
    abstract void putLong(byte[] array, long offset, long l);
    abstract float getFloat(byte[] array, long offset);
    abstract void putFloat(byte[] array, long offset, float f);
    abstract double getDouble(byte[] array, long offset);
    abstract void putDouble(byte[] array, long offset, double d);
    abstract char getChar(byte[] array, long offset);
    abstract void putChar(byte[] array, long offset, char c);
    abstract boolean getBoolean(byte[] array, long offset);
    abstract void putBoolean(byte[] array, long offset, boolean b);
}
