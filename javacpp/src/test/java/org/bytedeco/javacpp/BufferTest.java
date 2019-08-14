/*
 * Copyright (C) 2018 Samuel Audet
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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.bytedeco.javacpp.annotation.CriticalRegion;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

/**
 * Test cases for the set of NIO Buffer classes. Relies on other classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform(compiler = "cpp11", include = "BufferTest.h")
public class BufferTest {

    public static class BufferCallback extends FunctionPointer {
        static { Loader.load(); }
        protected BufferCallback() { allocate(); }
        private native void allocate();

        static ByteBuffer value = new BytePointer(new byte[] {34}).asBuffer();
        public ByteBuffer call(ByteBuffer buffer) {
            assertEquals(12, buffer.get(0));
            return value;
        }
    }

    public static native ByteBuffer bufferCallback(BufferCallback f);

    @CriticalRegion
    public static native byte getByte(ByteBuffer buffer);
    public static native void putByte(ByteBuffer buffer, byte value);
    public static native short getShort(ShortBuffer buffer);
    @CriticalRegion
    public static native void putShort(ShortBuffer buffer, short value);
    public static native int getInt(IntBuffer buffer);
    public static native void putInt(IntBuffer buffer, int value);
    @CriticalRegion
    public static native long getLong(LongBuffer buffer);
    public static native void putLong(LongBuffer buffer, long value);
    public static native float getFloat(FloatBuffer buffer);
    @CriticalRegion
    public static native void putFloat(FloatBuffer buffer, float value);
    public static native double getDouble(DoubleBuffer buffer);
    public static native void putDouble(DoubleBuffer buffer, double value);

    @BeforeClass public static void setUpClass() throws Exception {
        System.out.println("Builder");
        Class c = BufferTest.class;
        Builder builder = new Builder().classesOrPackages(c.getName());
        File[] outputFiles = builder.build();

        System.out.println("Loader");
        Loader.load(c);
    }

    @Test public void testBufferCallback() {
        System.out.println("BufferCallback");
        assertEquals(34, bufferCallback(new BufferCallback()).get(0));
    }

    @Test public void testByteBuffer() {
        System.out.println("ByteBuffer");

        byte[] array = {-22, -11, 0, 11, 22};
        ByteBuffer arrayBuffer = ByteBuffer.wrap(array, 2, 3);
        putByte(arrayBuffer, (byte)42);
        assertEquals(42, array[2]);

        BytePointer pointer = new BytePointer(arrayBuffer);
        ByteBuffer directBuffer = pointer.asBuffer();
        assertEquals(pointer.address(), new BytePointer(directBuffer).address());

        assertTrue(directBuffer.compareTo(arrayBuffer) == 0);
        assertEquals(arrayBuffer.position(), directBuffer.position());
        assertEquals(arrayBuffer.limit(), directBuffer.limit());
        assertEquals(arrayBuffer.capacity(), directBuffer.capacity());

        assertEquals(getByte(arrayBuffer), getByte(directBuffer));
        assertEquals(getByte((ByteBuffer)arrayBuffer.position(arrayBuffer.position() + 2)),
                     getByte((ByteBuffer)directBuffer.position(directBuffer.position() + 2)));
    }

    @Test public void testShortBuffer() {
        System.out.println("ShortBuffer");

        short[] array = {-222, -111, 0, 111, 222};
        ShortBuffer arrayBuffer = ShortBuffer.wrap(array, 2, 3);
        putShort(arrayBuffer, (short)420);
        assertEquals(420, array[2]);

        ShortPointer pointer = new ShortPointer(arrayBuffer);
        ShortBuffer directBuffer = pointer.asBuffer();
        assertEquals(pointer.address() + pointer.position() * pointer.sizeof(), new ShortPointer(directBuffer).address());

        assertTrue(directBuffer.compareTo(arrayBuffer) == 0);
        assertEquals(arrayBuffer.limit() - arrayBuffer.position(), directBuffer.limit());
        assertEquals(arrayBuffer.capacity() - arrayBuffer.position(), directBuffer.capacity());

        assertEquals(getShort(arrayBuffer), getShort(directBuffer));
        assertEquals(getShort((ShortBuffer)arrayBuffer.position(arrayBuffer.position() + 2)),
                     getShort((ShortBuffer)directBuffer.position(directBuffer.position() + 2)));
    }

    @Test public void testIntBuffer() {
        System.out.println("IntBuffer");

        int[] array = {-2222, -1111, 0, 1111, 2222};
        IntBuffer arrayBuffer = IntBuffer.wrap(array, 2, 3);
        putInt(arrayBuffer, 4200);
        assertEquals(4200, array[2]);

        IntPointer pointer = new IntPointer(arrayBuffer);
        IntBuffer directBuffer = pointer.asBuffer();
        assertEquals(pointer.address() + pointer.position() * pointer.sizeof(), new IntPointer(directBuffer).address());

        assertTrue(directBuffer.compareTo(arrayBuffer) == 0);
        assertEquals(arrayBuffer.limit() - arrayBuffer.position(), directBuffer.limit());
        assertEquals(arrayBuffer.capacity() - arrayBuffer.position(), directBuffer.capacity());

        assertEquals(getInt(arrayBuffer), getInt(directBuffer));
        assertEquals(getInt((IntBuffer)arrayBuffer.position(arrayBuffer.position() + 2)),
                     getInt((IntBuffer)directBuffer.position(directBuffer.position() + 2)));
    }

    @Test public void testLongBuffer() {
        System.out.println("LongBuffer");

        long[] array = {-22222, -11111, 0, 11111, 22222};
        LongBuffer arrayBuffer = LongBuffer.wrap(array, 2, 3);
        putLong(arrayBuffer, 42000);
        assertEquals(42000, array[2]);

        LongPointer pointer = new LongPointer(arrayBuffer);
        LongBuffer directBuffer = pointer.asBuffer();
        assertEquals(pointer.address() + pointer.position() * pointer.sizeof(), new LongPointer(directBuffer).address());

        assertTrue(directBuffer.compareTo(arrayBuffer) == 0);
        assertEquals(arrayBuffer.limit() - arrayBuffer.position(), directBuffer.limit());
        assertEquals(arrayBuffer.capacity() - arrayBuffer.position(), directBuffer.capacity());

        assertEquals(getLong(arrayBuffer), getLong(directBuffer));
        assertEquals(getLong((LongBuffer)arrayBuffer.position(arrayBuffer.position() + 2)),
                     getLong((LongBuffer)directBuffer.position(directBuffer.position() + 2)));
    }

    @Test public void testFloatBuffer() {
        System.out.println("FloatBuffer");

        float[] array = {-222222, -111111, 0, 111111, 222222};
        FloatBuffer arrayBuffer = FloatBuffer.wrap(array, 2, 3);
        putFloat(arrayBuffer, 420000);
        assertEquals(420000, array[2], 0.0);

        FloatPointer pointer = new FloatPointer(arrayBuffer);
        FloatBuffer directBuffer = pointer.asBuffer();
        assertEquals(pointer.address() + pointer.position() * pointer.sizeof(), new FloatPointer(directBuffer).address());

        assertTrue(directBuffer.compareTo(arrayBuffer) == 0);
        assertEquals(arrayBuffer.limit() - arrayBuffer.position(), directBuffer.limit());
        assertEquals(arrayBuffer.capacity() - arrayBuffer.position(), directBuffer.capacity());

        assertEquals(getFloat(arrayBuffer), getFloat(directBuffer), 0.0);
        assertEquals(getFloat((FloatBuffer)arrayBuffer.position(arrayBuffer.position() + 2)),
                     getFloat((FloatBuffer)directBuffer.position(directBuffer.position() + 2)), 0.0);
    }

    @Test public void testDoubleBuffer() {
        System.out.println("DoubleBuffer");

        double[] array = {-2222222, -1111111, 0, 1111111, 2222222};
        DoubleBuffer arrayBuffer = DoubleBuffer.wrap(array, 2, 3);
        putDouble(arrayBuffer, 4200000);
        assertEquals(4200000, array[2], 0.0);

        DoublePointer pointer = new DoublePointer(arrayBuffer);
        DoubleBuffer directBuffer = pointer.asBuffer();
        assertEquals(pointer.address() + pointer.position() * pointer.sizeof(), new DoublePointer(directBuffer).address());

        assertTrue(directBuffer.compareTo(arrayBuffer) == 0);
        assertEquals(arrayBuffer.limit() - arrayBuffer.position(), directBuffer.limit());
        assertEquals(arrayBuffer.capacity() - arrayBuffer.position(), directBuffer.capacity());

        assertEquals(getDouble(arrayBuffer), getDouble(directBuffer), 0.0);
        assertEquals(getDouble((DoubleBuffer)arrayBuffer.position(arrayBuffer.position() + 2)),
                     getDouble((DoubleBuffer)directBuffer.position(directBuffer.position() + 2)), 0.0);
    }
}
