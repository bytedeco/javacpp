/*
 * Copyright (C) 2014-2015 Samuel Audet
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

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.indexer.ByteIndexer;
import org.bytedeco.javacpp.indexer.CharIndexer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.indexer.LongIndexer;
import org.bytedeco.javacpp.indexer.ShortIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.indexer.UShortIndexer;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for the indexer package. Also uses other classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform
public class IndexerTest {

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
        ByteIndexer arrayIndexer = ByteIndexer.create(ptr.position(0), sizes, strides, false);
        ByteIndexer bufferIndexer = ByteIndexer.create(ptr.position(0), sizes, strides, true);

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

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(i + 2, ptr.position(i).get() & 0xFF);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(i + 1, ptr.position(i).get() & 0xFF);
        }
        System.gc();
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
        ShortIndexer arrayIndexer = ShortIndexer.create(ptr.position(0), sizes, strides, false);
        ShortIndexer bufferIndexer = ShortIndexer.create(ptr.position(0), sizes, strides, true);

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

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();
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
        IntIndexer arrayIndexer = IntIndexer.create(ptr.position(0), sizes, strides, false);
        IntIndexer bufferIndexer = IntIndexer.create(ptr.position(0), sizes, strides, true);

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

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();
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
        LongIndexer arrayIndexer = LongIndexer.create(ptr.position(0), sizes, strides, false);
        LongIndexer bufferIndexer = LongIndexer.create(ptr.position(0), sizes, strides, true);

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

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();
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
        FloatIndexer arrayIndexer = FloatIndexer.create(ptr.position(0), sizes, strides, false);
        FloatIndexer bufferIndexer = FloatIndexer.create(ptr.position(0), sizes, strides, true);

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

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get(), 0);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get(), 0);
        }
        System.gc();
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
        DoubleIndexer arrayIndexer = DoubleIndexer.create(ptr.position(0), sizes, strides, false);
        DoubleIndexer bufferIndexer = DoubleIndexer.create(ptr.position(0), sizes, strides, true);

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

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get(), 0);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get(), 0);
        }
        System.gc();
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
        CharIndexer arrayIndexer = CharIndexer.create(ptr.position(0), sizes, strides, false);
        CharIndexer bufferIndexer = CharIndexer.create(ptr.position(0), sizes, strides, true);

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

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();
    }

    @Test public void testUByteIndexer() {
        System.out.println("UByteIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final BytePointer ptr = new BytePointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((byte)i);
        }
        UByteIndexer arrayIndexer = UByteIndexer.create(ptr.position(0), sizes, strides, false);
        UByteIndexer bufferIndexer = UByteIndexer.create(ptr.position(0), sizes, strides, true);

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
                        arrayIndexer.put(index, n + 1);
                        bufferIndexer.put(index, n + 2);
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(i + 2, ptr.position(i).get() & 0xFF);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(i + 1, ptr.position(i).get() & 0xFF);
        }
        System.gc();
    }

    @Test public void testUShortIndexer() {
        System.out.println("UShortIndexer");
        int size = 7 * 5 * 3 * 2;
        int[] sizes = { 7, 5, 3, 2 };
        int[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final ShortPointer ptr = new ShortPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((short)i);
        }
        UShortIndexer arrayIndexer = UShortIndexer.create(ptr.position(0), sizes, strides, false);
        UShortIndexer bufferIndexer = UShortIndexer.create(ptr.position(0), sizes, strides, true);

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
                        arrayIndexer.put(index, 2 * n);
                        bufferIndexer.put(index, 3 * n);
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            bufferIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("array" + arrayIndexer);
        System.out.println("buffer" + bufferIndexer);
        System.out.println();
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();
    }

}
