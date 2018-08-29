/*
 * Copyright (C) 2016-2017 Samuel Audet
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
 * A raw memory interface based on {@link UnsafeRaw} that swaps the bytes.
 *
 * @author Samuel Audet
 */
class ReverseUnsafeRaw extends UnsafeRaw {
    @Override short getShort(long address) { return Short.reverseBytes(super.getShort(address)); }
    @Override void putShort(long address, short s) { super.putShort(address, Short.reverseBytes(s)); }
    @Override int getInt(long address) { return Integer.reverseBytes(super.getInt(address)); }
    @Override void putInt(long address, int i) { super.putInt(address, Integer.reverseBytes(i)); }
    @Override long getLong(long address) { return Long.reverseBytes(super.getLong(address)); }
    @Override void putLong(long address, long l) { super.putLong(address, Long.reverseBytes(l)); }
    @Override float getFloat(long address) { return Float.intBitsToFloat(Integer.reverseBytes(super.getInt(address))); }
    @Override void putFloat(long address, float f) { super.putFloat(address, Integer.reverseBytes(Float.floatToRawIntBits(f))); }
    @Override double getDouble(long address) { return Double.longBitsToDouble(Long.reverseBytes(super.getLong(address))); }
    @Override void putDouble(long address, double d) { super.putDouble(address, Long.reverseBytes(Double.doubleToRawLongBits(d))); }
    @Override char getChar(long address) { return Character.reverseBytes(super.getChar(address)); }
    @Override void putChar(long address, char c) { super.putChar(address, Character.reverseBytes(c)); }

    @Override short getShort(byte[] array, long offset) { return Short.reverseBytes(super.getShort(array, offset)); }
    @Override void putShort(byte[] array, long offset, short s) { super.putShort(array, offset, Short.reverseBytes(s)); }
    @Override int getInt(byte[] array, long offset) { return Integer.reverseBytes(super.getInt(array, offset)); }
    @Override void putInt(byte[] array, long offset, int i) { super.putInt(array, offset, Integer.reverseBytes(i)); }
    @Override long getLong(byte[] array, long offset) { return Long.reverseBytes(super.getLong(array, offset)); }
    @Override void putLong(byte[] array, long offset, long l) { super.putLong(array, offset, Long.reverseBytes(l)); }
    @Override float getFloat(byte[] array, long offset) { return Float.intBitsToFloat(Integer.reverseBytes(super.getInt(array, offset))); }
    @Override void putFloat(byte[] array, long offset, float f) { super.putFloat(array, offset, Integer.reverseBytes(Float.floatToRawIntBits(f))); }
    @Override double getDouble(byte[] array, long offset) { return Double.longBitsToDouble(Long.reverseBytes(super.getLong(array, offset))); }
    @Override void putDouble(byte[] array, long offset, double d) { super.putDouble(array, offset, Long.reverseBytes(Double.doubleToRawLongBits(d))); }
    @Override char getChar(byte[] array, long offset) { return Character.reverseBytes(super.getChar(array, offset)); }
    @Override void putChar(byte[] array, long offset, char c) { super.putChar(array, offset, Character.reverseBytes(c)); }
}
