/*
 * Copyright (C) 2016-2022 Samuel Audet
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
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bytedeco.javacpp.annotation.Allocator;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for the set of base Pointer classes. Relies on other classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform(define = {"NATIVE_ALLOCATOR malloc", "NATIVE_DEALLOCATOR free"})
public class PointerTest {

    static final int allocatorMax = 11;
    static final long maxBytes = 1024 * 1024 * 1024; /* 1g */

    @Allocator(max = allocatorMax)
    static class TestFunction extends FunctionPointer {
        public TestFunction(Pointer p) { super(p); }
        public TestFunction() { allocate(); }
        private native void allocate();
        public native int call(String s);
        public native Pointer get();
        public native TestFunction put(Pointer address);
    }

    @BeforeClass public static void setUpClass() throws Exception {
        System.out.println("Builder");
        Class c = PointerTest.class;
        Builder builder = new Builder().classesOrPackages(c.getName());
        File[] outputFiles = builder.build();

        System.out.println("Loader");
        Loader.load(c);

        int totalProcessors = Loader.totalProcessors();
        int totalCores = Loader.totalCores();
        int totalChips = Loader.totalChips();
        System.out.println(totalProcessors + " " + totalCores + " " + totalChips);
        assertTrue(totalProcessors > 0 && totalProcessors >= Runtime.getRuntime().availableProcessors());
        assertTrue(totalCores > 0 && totalCores <= totalProcessors);
        assertTrue(totalChips > 0 && totalChips <= totalCores);

        assertNotEquals(null, Loader.getJavaVM());
    }

    @Test public void testFunctionPointer() {
        System.out.println("FunctionPointer");

        Pointer address = Loader.addressof("strlen");
        assertNotNull(address);
        TestFunction function = new TestFunction().put(address);
        assertEquals(address, function.get());
        assertEquals(5, function.call("12345"));
        function.deallocate();

        TestFunction[] functions = new TestFunction[allocatorMax];
        Pointer prevp = new Pointer();
        for (int i = 0; i < allocatorMax; i++) {
            final int n = i;
            functions[i] = new TestFunction() {
                @Override public int call(String s) { return n; }
            };
            Pointer p = functions[i].get();
            System.out.println(p);
            assertNotNull(p);
            assertNotEquals(prevp, p);
            prevp = p;
        }

        TestFunction f = new TestFunction() {
            @Override public int call(String s) { return allocatorMax; }
        };
        assertNull(f.get());

        for (int i = 0; i < allocatorMax; i++) {
            functions[i].deallocate();
        }

        TestFunction f2 = new TestFunction() {
            @Override public int call(String s) { return allocatorMax; }
        };
        assertNotNull(f2.get());
    }

    static Object fieldReference;

    @Test public void testPointer() {
        System.out.println("Pointer");
        assertEquals(maxBytes, Pointer.maxBytes);
        assertEquals(3, Pointer.maxRetries);
        assertTrue(new Pointer().equals(null));
        Pointer p = new Pointer() { { address = 0xDEADBEEF; }};
        assertEquals(p, new Pointer(p));

        Pointer p2 = p.getPointer(Pointer.class, 10);
        assertEquals(p.address() + 10, p2.address());
        assertEquals(0, p2.limit());
        assertEquals(0, p2.capacity());

        long physicalBytes = Pointer.physicalBytes();
        long totalPhysicalBytes = Pointer.totalPhysicalBytes();
        long availablePhysicalBytes = Pointer.availablePhysicalBytes();
        System.out.println(physicalBytes);
        System.out.println(totalPhysicalBytes);
        System.out.println(availablePhysicalBytes);
        assertTrue(physicalBytes > 0);
        assertTrue(totalPhysicalBytes > 0 && physicalBytes < totalPhysicalBytes);
        assertTrue(availablePhysicalBytes > 0 && availablePhysicalBytes < totalPhysicalBytes);

        p = Pointer.malloc(1000);
        assertTrue(!p.isNull());
        p = Pointer.calloc(1000, 1000);
        assertTrue(!p.isNull());
        p = Pointer.realloc(p, 1000);
        assertTrue(!p.isNull());
        Pointer.free(p);
    }

    @Test public void testBytePointer() {
        System.out.println("BytePointer");

        int byteSize = Byte.SIZE / 8;
        assertEquals(byteSize, Loader.sizeof(BytePointer.class));

        byte[] array = new byte[8192];
        BytePointer pointer = new BytePointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        BytePointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 10, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        Pointer p3 = pointer.getPointer(Pointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(1 * array.length - 20, p3.limit());
        assertEquals(1 * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = (byte)i;
            pointer.put(i, (byte)i);
            assertEquals(array[i], pointer.get(i));
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get());
        }

        short shortValue = 0x0102;
        int intValue = 0x01020304;
        long longValue = 0x0102030405060708L;
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            shortValue = Short.reverseBytes(shortValue);
            intValue = Integer.reverseBytes(intValue);
            longValue = Long.reverseBytes(longValue);
        }
        float floatValue = Float.intBitsToFloat(intValue);
        double doubleValue = Double.longBitsToDouble(longValue);
        pointer.position(0);
        assertEquals(shortValue, pointer.getShort(1));
        assertEquals(intValue, pointer.getInt(1));
        assertEquals(longValue, pointer.getLong(1));
        assertEquals(floatValue, pointer.getFloat(1), 0.0);
        assertEquals(doubleValue, pointer.getDouble(1), 0.0);
        assertEquals(false, pointer.getBool(0));
        assertEquals(true, pointer.getBool(1));
        assertEquals(shortValue, pointer.getChar(1));
        assertEquals(Loader.sizeof(Pointer.class) == 4 ? intValue : longValue, pointer.getPointerValue(1).address);

        byte[] array2 = new byte[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2);

        ByteBuffer buffer = pointer.position(5 + 7).asBuffer();
        ByteBuffer arrayBuffer = ByteBuffer.wrap(array, 5, array.length - 5);
        assertTrue(buffer.compareTo((ByteBuffer)arrayBuffer.position(arrayBuffer.position() + 7)) == 0);
        assertEquals(pointer.address(), new BytePointer(buffer).address());
        assertEquals(pointer.position(), new BytePointer(buffer).position());
        assertEquals(pointer.limit(), new BytePointer(buffer).limit());
        assertEquals(pointer.capacity(), new BytePointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2);

        BytePointer pointer2 = new BytePointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(0, pointer2.get(i));
            } else if (i < 20) {
                assertEquals((byte)0xFF, pointer2.get(i));
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i));
            } else {
                assertEquals(0, pointer2.get(i));
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        BytePointer[] pointers = new BytePointer[chunks];
        long chunkSize = Pointer.maxBytes / byteSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new BytePointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * byteSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new BytePointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new BytePointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * byteSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * byteSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * byteSize);
    }

    @Test public void testShortPointer() {
        System.out.println("ShortPointer");

        int shortSize = Short.SIZE / 8;
        assertEquals(shortSize, Loader.sizeof(ShortPointer.class));

        short[] array = new short[8192];
        ShortPointer pointer = new ShortPointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        ShortPointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 20, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        Pointer p3 = pointer.getPointer(Pointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(2 * array.length - 20, p3.limit());
        assertEquals(2 * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = (short)i;
            pointer.put(i, (short)i);
            assertEquals(array[i], pointer.get(i));
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get());
        }

        short[] array2 = new short[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2);

        ShortBuffer buffer = pointer.position(5 + 7).asBuffer();
        ShortBuffer arrayBuffer = ShortBuffer.wrap(array, 5, array.length - 5);
        assertTrue(buffer.compareTo((ShortBuffer)arrayBuffer.position(arrayBuffer.position() + 7)) == 0);
        assertEquals(pointer.address() + arrayBuffer.position() * pointer.sizeof(), new ShortPointer(buffer).address());
        assertEquals(pointer.limit() - arrayBuffer.position(), new ShortPointer(buffer).limit());
        assertEquals(pointer.capacity() - arrayBuffer.position(), new ShortPointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2);

        ShortPointer pointer2 = new ShortPointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(0, pointer2.get(i));
            } else if (i < 20) {
                assertEquals((short)0xFFFF, pointer2.get(i));
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i));
            } else {
                assertEquals(0, pointer2.get(i));
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        ShortPointer[] pointers = new ShortPointer[chunks];
        long chunkSize = Pointer.maxBytes / shortSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new ShortPointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * shortSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new ShortPointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new ShortPointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * shortSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * shortSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * shortSize);
    }

    @Test public void testIntPointer() {
        System.out.println("IntPointer");

        int intSize = Integer.SIZE / 8;
        assertEquals(intSize, Loader.sizeof(IntPointer.class));

        int[] array = new int[8192];
        IntPointer pointer = new IntPointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        IntPointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 40, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        Pointer p3 = pointer.getPointer(Pointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(4 * array.length - 20, p3.limit());
        assertEquals(4 * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = i;
            pointer.put(i, i);
            assertEquals(array[i], pointer.get(i));
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get());
        }

        int[] array2 = new int[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2);

        IntBuffer buffer = pointer.position(5 + 7).asBuffer();
        IntBuffer arrayBuffer = IntBuffer.wrap(array, 5, array.length - 5);
        assertTrue(buffer.compareTo((IntBuffer)arrayBuffer.position(arrayBuffer.position() + 7)) == 0);
        assertEquals(pointer.address() + arrayBuffer.position() * pointer.sizeof(), new IntPointer(buffer).address());
        assertEquals(pointer.limit() - arrayBuffer.position(), new IntPointer(buffer).limit());
        assertEquals(pointer.capacity() - arrayBuffer.position(), new IntPointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2);

        IntPointer pointer2 = new IntPointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(0, pointer2.get(i));
            } else if (i < 20) {
                assertEquals(0xFFFFFFFF, pointer2.get(i));
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i));
            } else {
                assertEquals(0, pointer2.get(i));
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        IntPointer[] pointers = new IntPointer[chunks];
        long chunkSize = Pointer.maxBytes / intSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new IntPointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * intSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new IntPointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new IntPointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * intSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * intSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * intSize);
    }

    @Test public void testLongPointer() {
        System.out.println("LongPointer");

        int longSize = Long.SIZE / 8;
        assertEquals(longSize, Loader.sizeof(LongPointer.class));

        long[] array = new long[8192];
        LongPointer pointer = new LongPointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        LongPointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 80, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        Pointer p3 = pointer.getPointer(Pointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(8 * array.length - 20, p3.limit());
        assertEquals(8 * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = i;
            pointer.put(i, i);
            assertEquals(array[i], pointer.get(i));
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get());
        }

        long[] array2 = new long[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2);

        LongBuffer buffer = pointer.position(5 + 7).asBuffer();
        LongBuffer arrayBuffer = LongBuffer.wrap(array, 5, array.length - 5);
        assertTrue(buffer.compareTo((LongBuffer)arrayBuffer.position(arrayBuffer.position() + 7)) == 0);
        assertEquals(pointer.address() + arrayBuffer.position() * pointer.sizeof(), new LongPointer(buffer).address());
        assertEquals(pointer.limit() - arrayBuffer.position(), new LongPointer(buffer).limit());
        assertEquals(pointer.capacity() - arrayBuffer.position(), new LongPointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2);

        LongPointer pointer2 = new LongPointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(0, pointer2.get(i));
            } else if (i < 20) {
                assertEquals((long)-1, pointer2.get(i));
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i));
            } else {
                assertEquals(0, pointer2.get(i));
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        LongPointer[] pointers = new LongPointer[chunks];
        long chunkSize = Pointer.maxBytes / longSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new LongPointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * longSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new LongPointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new LongPointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * longSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * longSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * longSize);
    }

    @Test public void testFloatPointer() {
        System.out.println("FloatPointer");

        int floatSize = Float.SIZE / 8;
        assertEquals(floatSize, Loader.sizeof(FloatPointer.class));

        float[] array = new float[8192];
        FloatPointer pointer = new FloatPointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        FloatPointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 40, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        Pointer p3 = pointer.getPointer(Pointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(4 * array.length - 20, p3.limit());
        assertEquals(4 * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = i;
            pointer.put(i, i);
            assertEquals(array[i], pointer.get(i), 0);
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get(), 0);
        }

        float[] array2 = new float[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2, 0);

        FloatBuffer buffer = pointer.position(5 + 7).asBuffer();
        FloatBuffer arrayBuffer = FloatBuffer.wrap(array, 5, array.length - 5);
        assertTrue(buffer.compareTo((FloatBuffer)arrayBuffer.position(arrayBuffer.position() + 7)) == 0);
        assertEquals(pointer.address() + arrayBuffer.position() * pointer.sizeof(), new FloatPointer(buffer).address());
        assertEquals(pointer.limit() - arrayBuffer.position(), new FloatPointer(buffer).limit());
        assertEquals(pointer.capacity() - arrayBuffer.position(), new FloatPointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2, 0);

        FloatPointer pointer2 = new FloatPointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(0, pointer2.get(i), 0);
            } else if (i < 20) {
                assertEquals(Float.intBitsToFloat(0xFFFFFFFF), pointer2.get(i), 0);
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i), 0);
            } else {
                assertEquals(0, pointer2.get(i), 0);
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        FloatPointer[] pointers = new FloatPointer[chunks];
        long chunkSize = Pointer.maxBytes / floatSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new FloatPointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * floatSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new FloatPointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new FloatPointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * floatSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * floatSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * floatSize);
    }

    @Test public void testDoublePointer() {
        System.out.println("DoublePointer");

        int doubleSize = Double.SIZE / 8;
        assertEquals(doubleSize, Loader.sizeof(DoublePointer.class));

        double[] array = new double[8192];
        DoublePointer pointer = new DoublePointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        DoublePointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 80, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        Pointer p3 = pointer.getPointer(Pointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(8 * array.length - 20, p3.limit());
        assertEquals(8 * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = i;
            pointer.put(i, i);
            assertEquals(array[i], pointer.get(i), 0);
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get(), 0);
        }

        double[] array2 = new double[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2, 0);

        DoubleBuffer buffer = pointer.position(5 + 7).asBuffer();
        DoubleBuffer arrayBuffer = DoubleBuffer.wrap(array, 5, array.length - 5);
        assertTrue(buffer.compareTo((DoubleBuffer)arrayBuffer.position(arrayBuffer.position() + 7)) == 0);
        assertEquals(pointer.address() + arrayBuffer.position() * pointer.sizeof(), new DoublePointer(buffer).address());
        assertEquals(pointer.limit() - arrayBuffer.position(), new DoublePointer(buffer).limit());
        assertEquals(pointer.capacity() - arrayBuffer.position(), new DoublePointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2, 0);

        DoublePointer pointer2 = new DoublePointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(0, pointer2.get(i), 0);
            } else if (i < 20) {
                assertEquals(Double.longBitsToDouble((long)-1), pointer2.get(i), 0);
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i), 0);
            } else {
                assertEquals(0, pointer2.get(i), 0);
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        DoublePointer[] pointers = new DoublePointer[chunks];
        long chunkSize = Pointer.maxBytes / doubleSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new DoublePointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * doubleSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new DoublePointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new DoublePointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * doubleSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * doubleSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * doubleSize);
    }

    @Test public void testCharPointer() {
        System.out.println("CharPointer");

        int charSize = Character.SIZE / 8;
        assertEquals(charSize, Loader.sizeof(CharPointer.class));

        char[] array = new char[8192];
        CharPointer pointer = new CharPointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        CharPointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 20, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        Pointer p3 = pointer.getPointer(Pointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(2 * array.length - 20, p3.limit());
        assertEquals(2 * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = (char)i;
            pointer.put(i, (char)i);
            assertEquals(array[i], pointer.get(i));
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get());
        }

        char[] array2 = new char[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2);

        CharBuffer buffer = pointer.position(5 + 7).asBuffer();
        CharBuffer arrayBuffer = CharBuffer.wrap(array, 5, array.length - 5);
        assertTrue(buffer.compareTo((CharBuffer)arrayBuffer.position(arrayBuffer.position() + 7)) == 0);
        assertEquals(pointer.address() + arrayBuffer.position() * pointer.sizeof(), new CharPointer(buffer).address());
        assertEquals(pointer.limit() - arrayBuffer.position(), new CharPointer(buffer).limit());
        assertEquals(pointer.capacity() - arrayBuffer.position(), new CharPointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2);

        CharPointer pointer2 = new CharPointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(0, pointer2.get(i));
            } else if (i < 20) {
                assertEquals((char)0xFFFF, pointer2.get(i));
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i));
            } else {
                assertEquals(0, pointer2.get(i));
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        CharPointer[] pointers = new CharPointer[chunks];
        long chunkSize = Pointer.maxBytes / charSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new CharPointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * charSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new CharPointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new CharPointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * charSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * charSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * charSize);
    }

    @Test public void testBooleanPointer() {
        System.out.println("BooleanPointer");

        int booleanSize = 1;
        assertEquals(booleanSize, Loader.sizeof(BooleanPointer.class));

        boolean[] array = new boolean[8192];
        BooleanPointer pointer = new BooleanPointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        BooleanPointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 10, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        LongPointer p3 = pointer.getPointer(LongPointer.class, 20);
        assertEquals(pointer.address() + 8 * 20, p3.address());
        assertEquals(array.length / 8 - 20, p3.limit());
        assertEquals(array.length / 8 - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            array[i] = i % 2 != 0;
            pointer.put(i, i % 2 != 0);
            assertEquals(array[i], pointer.get(i));
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get());
        }

        boolean[] array2 = new boolean[array.length];
        pointer.position(0).get(array2);
        assertArrayEquals(array, array2);

        ByteBuffer buffer = pointer.asByteBuffer();
        assertEquals(pointer.address(), new BooleanPointer(buffer).address());
        assertEquals(pointer.position(), new BooleanPointer(buffer).position());
        assertEquals(pointer.limit(), new BooleanPointer(buffer).limit());
        assertEquals(pointer.capacity(), new BooleanPointer(buffer).capacity());

        int offset = 42;
        pointer.put(array, offset, array.length - offset);
        pointer.get(array2, offset, array.length - offset);
        assertArrayEquals(array, array2);

        BooleanPointer pointer2 = new BooleanPointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer2.position(20).put(pointer.position(20).limit(30));
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertEquals(false, pointer2.get(i));
            } else if (i < 20) {
                assertEquals(true, pointer2.get(i));
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i));
            } else {
                assertEquals(false, pointer2.get(i));
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        BooleanPointer[] pointers = new BooleanPointer[chunks];
        long chunkSize = Pointer.maxBytes / booleanSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new BooleanPointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * booleanSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new BooleanPointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new BooleanPointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * booleanSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * booleanSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * booleanSize);
    }

    @Test public void testPointerPointer() {
        System.out.println("PointerPointer");

        int pointerSize = Loader.sizeof(Pointer.class);
        assertEquals(pointerSize, Loader.sizeof(PointerPointer.class));

        Pointer[] array = new Pointer[8192];
        PointerPointer pointer = new PointerPointer(array);
        assertEquals(array.length, pointer.limit());
        assertEquals(array.length, pointer.capacity());

        PointerPointer p2 = pointer.getPointer(10);
        assertEquals(pointer.address() + 10 * pointerSize, p2.address());
        assertEquals(array.length - 10, p2.limit());
        assertEquals(array.length - 10, p2.capacity());
        BytePointer p3 = pointer.getPointer(BytePointer.class, 20);
        assertEquals(pointer.address() + 20, p3.address());
        assertEquals(pointerSize * array.length - 20, p3.limit());
        assertEquals(pointerSize * array.length - 20, p3.capacity());

        for (int i = 0; i < array.length; i++) {
            final int j = i;
            Pointer p = new Pointer() { { address = j; } };
            array[i] = p;
            pointer.put(i, p);
            assertEquals(array[i], pointer.get(i));
        }

        for (int i = 0; i < array.length; i++) {
            pointer.position(i).put(array[i]);
            assertEquals(array[i], pointer.position(i).get());
        }

        Pointer[] array2 = new Pointer[array.length];
        for (int i = 0; i < array2.length; i++) {
            array2[i] = pointer.position(0).get(i);
        }
        assertArrayEquals(array, array2);

        PointerPointer stringArray = new PointerPointer("one", "two");
        assertEquals(2, stringArray.capacity());
        assertEquals("one", stringArray.getString(0));
        assertEquals("two", stringArray.getString(1));

        int offset = 42;
        for (int i = 0; i < array.length - offset; i++) {
            pointer.put(i, array[offset + i]);
            array2[offset + i] = pointer.get(i);
        }
        assertArrayEquals(array, array2);

        PointerPointer pointer2 = new PointerPointer(array.length).zero();
        pointer2.position(10).limit(30).fill(0xFF);
        pointer.position(20);
        pointer2.position(20);
        for (int i = 0; i < 10; i++) {
            pointer.put(i, pointer2.get(i));
        }
        pointer.position(0);
        pointer2.position(0);
        for (int i = 0; i < array.length; i++) {
            if (i < 10) {
                assertNull(pointer2.get(i));
            } else if (i < 20) {
                assertEquals(0xFFFFFFFFL, pointer2.get(i).address() & 0xFFFFFFFFL);
            } else if (i < 30) {
                assertEquals(pointer.get(i), pointer2.get(i));
            } else {
                assertNull(pointer2.get(i));
            }
        }

        assertEquals(maxBytes, Pointer.maxBytes);
        int chunks = 8;
        PointerPointer[] pointers = new PointerPointer[chunks];
        long chunkSize = Pointer.maxBytes / pointerSize / chunks + 1;
        for (int j = 0; j < chunks - 1; j++) {
            pointers[j] = new PointerPointer(chunkSize);
        }
        assertTrue(Pointer.DeallocatorReference.totalBytes >= (chunks - 1) * chunkSize * pointerSize);
        try {
            fieldReference = pointers;
            System.out.println("Note: OutOfMemoryError should get thrown here and printed below.");
            new PointerPointer(chunkSize);
            fail("OutOfMemoryError should have been thrown.");
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.out.println(e.getCause());
        }
        for (int j = 0; j < chunks; j++) {
            pointers[j] = null;
        }
        // make sure garbage collection runs
        fieldReference = null;
        pointers[0] = new PointerPointer(chunkSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes < (chunks - 1) * chunkSize * pointerSize);
        assertTrue(Pointer.DeallocatorReference.totalBytes >= chunkSize * pointerSize);
        System.out.println(Pointer.DeallocatorReference.totalBytes + " " + chunkSize * pointerSize);
    }

    @Test public void testDeallocator() throws InterruptedException {
        System.out.println("Deallocator");
        System.out.println("maxBytes = " + Pointer.maxBytes());
        System.out.println("maxPhysicalBytes = " + Pointer.maxPhysicalBytes());

        final boolean[] failed = { false };
        ExecutorService pool = Executors.newFixedThreadPool(24);
        long time = System.nanoTime();
        for (int i = 0; i < 24; i++) {
            pool.execute(new Runnable() {
                @Override public void run() {
                    try {
                        for (int i = 0; i < 2000; i++) {
                            BytePointer p = new BytePointer(2000000);
                            if (i % 3 == 0) {
                                p.retainReference();
                            }
                            if (Pointer.deallocatorThread == null) {
                                if (i % 2 == 0) {
                                    p.deallocate();
                                } else {
                                    p.releaseReference();
                                }
                            }
                        }
                    } catch (OutOfMemoryError e) {
                        failed[0] = true;
                        fail(e.getMessage());
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        if (failed[0]) {
            fail("OutOfMemoryError should not have been thrown.");
        }
        if (Pointer.deallocatorThread == null) {
            assertEquals(0, Pointer.totalBytes());
        }
        System.out.println("Took " + (System.nanoTime() - time) / 1000000 + " ms");
    }

    @Test public void testPointerScope() {
        System.out.println("PointerScope");
        IntPointer outside = new IntPointer(1);
        IntPointer attached = new IntPointer(1), detached, inside, inside1, inside2, retained1, retained2, inside5;

        try (PointerScope scope = new PointerScope()) {
            scope.attach(attached);

            detached = new IntPointer(1);
            detached.retainReference();
            scope.detach(detached);

            inside = new IntPointer(1);
            try (PointerScope scope1 = new PointerScope()) {
                inside1 = new IntPointer(1);
                inside2 = new IntPointer(1);
            }
            try (PointerScope scope2 = new PointerScope()) {
                retained1 = new IntPointer(1);
                retained2 = new IntPointer(1);
                retained1.retainReference();
                scope.attach(retained2);
            }
            retained2.retainReference();
            inside5 = new IntPointer(1);
        }

        IntPointer outside2 = new IntPointer(1);

        assertFalse(outside.isNull());
        assertTrue(attached.isNull());
        assertFalse(detached.isNull());
        assertTrue(inside.isNull());
        assertTrue(inside1.isNull());
        assertTrue(inside2.isNull());
        assertFalse(retained1.isNull());
        assertFalse(retained2.isNull());
        assertTrue(inside5.isNull());
        assertFalse(outside2.isNull());

        outside.releaseReference();
        detached.releaseReference();
        retained1.releaseReference();
        retained2.releaseReference();
        outside2.releaseReference();

        assertTrue(outside.isNull());
        assertTrue(detached.isNull());
        assertTrue(retained1.isNull());
        assertTrue(retained2.isNull());
        assertTrue(outside2.isNull());

        IntPointer intPointer;
        FloatPointer floatPointer;
        try (PointerScope globalScope = new PointerScope()) {
            PointerScope extendedLocalScope = null;
            try (PointerScope localScope = new PointerScope(IntPointer.class)) {
                intPointer = new IntPointer(1);
                floatPointer = new FloatPointer(1);

                try {
                    System.out.println("Note: IllegalArgumentException should get thrown here and printed below.");
                    localScope.attach(floatPointer);
                    fail("IllegalArgumentException should have been thrown.");
                } catch (IllegalArgumentException e) {
                    System.out.println(e);
                }
                extendedLocalScope = localScope.extend();
            }
            assertFalse(intPointer.isNull());
            extendedLocalScope.close();
            assertTrue(intPointer.isNull());
            assertFalse(floatPointer.isNull());
        }
        assertTrue(floatPointer.isNull());
    }

    @Test public void testParseBytesWithRelativeUnits() {
        System.out.println("ParseBytesWithRelativeUnits");
        long arbitraryAmountOfMemory = 300000;

        assertEquals(0, Pointer.parseBytes("0%", arbitraryAmountOfMemory));
        assertEquals(arbitraryAmountOfMemory * 10 / 100, Pointer.parseBytes("10%", arbitraryAmountOfMemory));
        try {
            System.out.println("Note: NumberFormatException should get thrown here and printed below.");
            Pointer.parseBytes("%", arbitraryAmountOfMemory);
            fail("NumberFormatException should have been thrown.");
        } catch (NumberFormatException e) {
            System.out.println(e);
        }
    }

    @Test public void testNoFileBytes() throws Exception {
        System.out.println("NoFileBytes");

        System.gc();
        Thread.sleep(100);
        long bytesBefore = Pointer.physicalBytes();

        Path file = Files.createTempFile("javacpp", "tmp");
        FileChannel channel = FileChannel.open(file,
                StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 100_000_000);
        for (int i = 0; i < 100_000_000; i++) {
            buffer.put(i, (byte)i);
        }

        System.gc();
        Thread.sleep(100);
        long bytesAfter = Pointer.physicalBytes();

        System.out.println(bytesBefore + " " + bytesAfter);
        assertTrue(Math.abs(bytesAfter - bytesBefore) < 100_000_000 + buffer.get(0));
    }
}
