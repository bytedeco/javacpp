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
package org.bytedeco.javacpp;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.bytedeco.javacpp.annotation.MemberGetter;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.indexer.ByteArrayIndexer;
import org.bytedeco.javacpp.indexer.ByteBufferIndexer;
import org.bytedeco.javacpp.indexer.CharArrayIndexer;
import org.bytedeco.javacpp.indexer.CharBufferIndexer;
import org.bytedeco.javacpp.indexer.DoubleArrayIndexer;
import org.bytedeco.javacpp.indexer.DoubleBufferIndexer;
import org.bytedeco.javacpp.indexer.FloatArrayIndexer;
import org.bytedeco.javacpp.indexer.FloatBufferIndexer;
import org.bytedeco.javacpp.indexer.IntArrayIndexer;
import org.bytedeco.javacpp.indexer.IntBufferIndexer;
import org.bytedeco.javacpp.indexer.LongArrayIndexer;
import org.bytedeco.javacpp.indexer.LongBufferIndexer;
import org.bytedeco.javacpp.indexer.ShortArrayIndexer;
import org.bytedeco.javacpp.indexer.ShortBufferIndexer;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for the indexer package. Also uses other classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform(include="errno.h")
public class IndexerTest {

    /** Just something to get the {@link Builder} to compile a library for us. */
    @MemberGetter public static native int errno();

    @BeforeClass public static void setUpClass() throws Exception {
        Class c = IndexerTest.class;
        Builder builder = new Builder().classesOrPackages(c.getName());
        File[] outputFiles = builder.build();
        Loader.loadLibraries = true;
        Loader.load(c);
    }

    @Test public void testByteIndexer() {
        System.out.println("ByteIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final BytePointer ptr = new BytePointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((byte)i);
        }
        byte[] array = new byte[size];
        ptr.position(0).get(array);
        ByteBuffer buffer = ptr.asBuffer();
        ByteArrayIndexer arrayIndexer = new ByteArrayIndexer(array, sizes, strides) {
            @Override public void release() {
                ptr.position(0).put(array);
            }
        };
        ByteBufferIndexer bufferIndexer = new ByteBufferIndexer(buffer, sizes, strides);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]) & 0xFF);
            assertEquals(n, bufferIndexer.get(i * strides[0]) & 0xFF);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]) & 0xFF);
                assertEquals(n, bufferIndexer.get(i, j * strides[1]) & 0xFF);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]) & 0xFF);
                    assertEquals(n, bufferIndexer.get(i, j, k * strides[2]) & 0xFF);
                    for (int m = 0; m < sizes[3]; m++) {
                        int[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index) & 0xFF);
                        assertEquals(n, bufferIndexer.get(index) & 0xFF);
                        arrayIndexer.put(index, (byte)(n + 1));
                        bufferIndexer.put(index, (byte)(n + 2));
                        n++;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals(i + 2, ptr.position(i).get() & 0xFF);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(i + 1, ptr.position(i).get() & 0xFF);
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }
    }

    @Test public void testShortIndexer() {
        System.out.println("ShortIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final ShortPointer ptr = new ShortPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((short)i);
        }
        short[] array = new short[size];
        ptr.position(0).get(array);
        ShortBuffer buffer = ptr.asBuffer();
        ShortArrayIndexer arrayIndexer = new ShortArrayIndexer(array, sizes, strides) {
            @Override public void release() {
                ptr.position(0).put(array);
            }
        };
        ShortBufferIndexer bufferIndexer = new ShortBufferIndexer(buffer, sizes, strides);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, bufferIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, bufferIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, bufferIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        int[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, bufferIndexer.get(index));
                        arrayIndexer.put(index, (short)(2 * n));
                        bufferIndexer.put(index, (short)(3 * n));
                        n++;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }
    }

    @Test public void testIntIndexer() {
        System.out.println("IntIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final IntPointer ptr = new IntPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((int)i);
        }
        int[] array = new int[size];
        ptr.position(0).get(array);
        IntBuffer buffer = ptr.asBuffer();
        IntArrayIndexer arrayIndexer = new IntArrayIndexer(array, sizes, strides) {
            @Override public void release() {
                ptr.position(0).put(array);
            }
        };
        IntBufferIndexer bufferIndexer = new IntBufferIndexer(buffer, sizes, strides);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, bufferIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, bufferIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, bufferIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        int[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, bufferIndexer.get(index));
                        arrayIndexer.put(index, (int)(2 * n));
                        bufferIndexer.put(index, (int)(3 * n));
                        n++;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }
    }

    @Test public void testLongIndexer() {
        System.out.println("LongIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final LongPointer ptr = new LongPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((long)i);
        }
        long[] array = new long[size];
        ptr.position(0).get(array);
        LongBuffer buffer = ptr.asBuffer();
        LongArrayIndexer arrayIndexer = new LongArrayIndexer(array, sizes, strides) {
            @Override public void release() {
                ptr.position(0).put(array);
            }
        };
        LongBufferIndexer bufferIndexer = new LongBufferIndexer(buffer, sizes, strides);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, bufferIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, bufferIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, bufferIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        int[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, bufferIndexer.get(index));
                        arrayIndexer.put(index, (long)(2 * n));
                        bufferIndexer.put(index, (long)(3 * n));
                        n++;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }
    }

    @Test public void testFloatIndexer() {
        System.out.println("FloatIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final FloatPointer ptr = new FloatPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((float)i);
        }
        float[] array = new float[size];
        ptr.position(0).get(array);
        FloatBuffer buffer = ptr.asBuffer();
        FloatArrayIndexer arrayIndexer = new FloatArrayIndexer(array, sizes, strides) {
            @Override public void release() {
                ptr.position(0).put(array);
            }
        };
        FloatBufferIndexer bufferIndexer = new FloatBufferIndexer(buffer, sizes, strides);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]), 0);
            assertEquals(n, bufferIndexer.get(i * strides[0]), 0);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]), 0);
                assertEquals(n, bufferIndexer.get(i, j * strides[1]), 0);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]), 0);
                    assertEquals(n, bufferIndexer.get(i, j, k * strides[2]), 0);
                    for (int m = 0; m < sizes[3]; m++) {
                        int[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index), 0);
                        assertEquals(n, bufferIndexer.get(index), 0);
                        arrayIndexer.put(index, (float)(2 * n));
                        bufferIndexer.put(index, (float)(3 * n));
                        n++;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get(), 0);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get(), 0);
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }
    }

    @Test public void testDoubleIndexer() {
        System.out.println("DoubleIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final DoublePointer ptr = new DoublePointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((double)i);
        }
        double[] array = new double[size];
        ptr.position(0).get(array);
        DoubleBuffer buffer = ptr.asBuffer();
        DoubleArrayIndexer arrayIndexer = new DoubleArrayIndexer(array, sizes, strides) {
            @Override public void release() {
                ptr.position(0).put(array);
            }
        };
        DoubleBufferIndexer bufferIndexer = new DoubleBufferIndexer(buffer, sizes, strides);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]), 0);
            assertEquals(n, bufferIndexer.get(i * strides[0]), 0);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]), 0);
                assertEquals(n, bufferIndexer.get(i, j * strides[1]), 0);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]), 0);
                    assertEquals(n, bufferIndexer.get(i, j, k * strides[2]), 0);
                    for (int m = 0; m < sizes[3]; m++) {
                        int[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index), 0);
                        assertEquals(n, bufferIndexer.get(index), 0);
                        arrayIndexer.put(index, (double)(2 * n));
                        bufferIndexer.put(index, (double)(3 * n));
                        n++;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get(), 0);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get(), 0);
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }
    }

    @Test public void testCharIndexer() {
        System.out.println("CharIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final CharPointer ptr = new CharPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((char)i);
        }
        char[] array = new char[size];
        ptr.position(0).get(array);
        CharBuffer buffer = ptr.asBuffer();
        CharArrayIndexer arrayIndexer = new CharArrayIndexer(array, sizes, strides) {
            @Override public void release() {
                ptr.position(0).put(array);
            }
        };
        CharBufferIndexer bufferIndexer = new CharBufferIndexer(buffer, sizes, strides);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, bufferIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, bufferIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, bufferIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        int[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, bufferIndexer.get(index));
                        arrayIndexer.put(index, (char)(2 * n));
                        bufferIndexer.put(index, (char)(3 * n));
                        n++;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }
    }

}
