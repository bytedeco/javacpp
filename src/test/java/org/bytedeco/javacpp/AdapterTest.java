/*
 * Copyright (C) 2015 Samuel Audet
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
import java.nio.IntBuffer;
import org.bytedeco.javacpp.annotation.ByRef;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Const;
import org.bytedeco.javacpp.annotation.Function;
import org.bytedeco.javacpp.annotation.Optional;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.SharedPtr;
import org.bytedeco.javacpp.annotation.StdBasicString;
import org.bytedeco.javacpp.annotation.StdMove;
import org.bytedeco.javacpp.annotation.StdString;
import org.bytedeco.javacpp.annotation.StdU16String;
import org.bytedeco.javacpp.annotation.StdU32String;
import org.bytedeco.javacpp.annotation.StdVector;
import org.bytedeco.javacpp.annotation.StdWString;
import org.bytedeco.javacpp.annotation.UniquePtr;
import org.bytedeco.javacpp.annotation.AsUtf16;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for text and data strings. Uses various classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform(compiler = "cpp17", define = {"OPTIONAL_NAMESPACE std", "SHARED_PTR_NAMESPACE std", "UNIQUE_PTR_NAMESPACE std"}, include = "AdapterTest.h")
public class AdapterTest {

    static native @StdString String testStdString(@StdString String str);
    static native @StdString BytePointer testStdString(@StdString BytePointer str);

    static native @StdWString CharPointer testStdWString(@StdWString CharPointer str);
    static native @StdWString @AsUtf16 String testStdWString(@StdWString @AsUtf16 String str);
    static native @StdWString IntPointer testStdWString(@StdWString IntPointer str);

    static native @StdBasicString("char") String testStdString2(@StdBasicString("char") String str);
    static native @Cast("char*") @StdBasicString("char") BytePointer testStdString2(@Cast("char*") @StdBasicString("char") BytePointer str);
    static native @Cast("wchar_t*") @StdBasicString("wchar_t") CharPointer testStdWString2(@Cast("wchar_t*") @StdBasicString("wchar_t") CharPointer str);
    static native @Cast("wchar_t*") @StdBasicString("wchar_t") IntPointer testStdWString2(@Cast("wchar_t*") @StdBasicString("wchar_t") IntPointer str);

    static native @StdU16String CharPointer testStdU16String(@StdU16String CharPointer str);
    static native @StdU16String @AsUtf16 String testStdU16String(@StdU16String @AsUtf16 String str);
    static native @StdU32String IntPointer testStdU32String(@StdU32String IntPointer str);

    static native String testCharString(String str);
    static native @Cast("char*") BytePointer testCharString(@Cast("char*") BytePointer str);

    static native CharPointer testShortString(CharPointer str);
    static native @AsUtf16 String testShortString(@AsUtf16 String str);

    static native IntPointer testIntString(IntPointer str);

    static native @Const @ByRef @StdString byte[] getConstStdString();
    static native @Const @ByRef @Cast("char*") @StdBasicString("char") byte[] getConstStdString2();

    static class SharedData extends Pointer {
        SharedData(Pointer p) { super(p); }
        SharedData(int data) { allocate(data); }
        native void allocate(int data);

        native int data(); native SharedData data(int data);
    }

    static native @SharedPtr SharedData createSharedData();
    static native void storeSharedData(@SharedPtr SharedData s);
    static native @SharedPtr SharedData fetchSharedData();

    static class UniqueData extends Pointer {
        UniqueData(Pointer p) { super(p); }
        UniqueData(int data) { allocate(data); }
        native void allocate(int data);

        native int data(); native UniqueData data(int data);
    }

    @Function static native @UniquePtr UniqueData createUniqueData();
    static native void createUniqueData(@UniquePtr UniqueData u);
    static native void storeUniqueData(@Const @UniquePtr UniqueData u);
    static native @Const @UniquePtr UniqueData fetchUniqueData();

    static native int constructorCount(); static native void constructorCount(int c);
    static native int destructorCount(); static native void destructorCount(int c);

    static native @StdVector IntPointer testStdVectorByVal(@StdVector IntPointer v);
    static native @StdVector IntPointer testStdVectorByRef(@StdVector IntBuffer v);
    static native @StdVector int[] testStdVectorByPtr(@StdVector int[] v);
    static native @Cast("const char**") @StdVector PointerPointer testStdVectorConstPointer(@Cast("const char**") @StdVector PointerPointer v);

    static class MovedData extends Pointer {
        MovedData(Pointer p) { super(p); }
        MovedData(int data) { allocate(data); }
        native void allocate(int data);

        native int data(); native MovedData data(int data);
    }

    static native @StdMove MovedData getMovedData();
    static native void putMovedData(@StdMove MovedData m);

    static native @Optional IntPointer testOptionalInt(@Optional IntPointer o);

    @BeforeClass public static void setUpClass() throws Exception {
        System.out.println("Builder");
        Class c = AdapterTest.class;
        Builder builder = new Builder().classesOrPackages(c.getName());
        File[] outputFiles = builder.build();

        System.out.println("Loader");
        Loader.load(c);
    }

    @Test public void testStdString() {
        System.out.println("StdString");

        byte[] data = new byte[42];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)i;
        }
        BytePointer dataPtr1 = new BytePointer(data);
        BytePointer dataPtr2 = testStdString(dataPtr1);
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], dataPtr1.get(i));
            assertEquals(data[i], dataPtr2.get(i));
        }

        String textStr1 = "This is a normal ASCII string.";
        String textStr2 = testStdString(textStr1);
        assertEquals(textStr1, textStr2);
        String textStr3 = testStdString2(textStr1);
        assertEquals(textStr1, textStr3);

        BytePointer textPtr1 = new BytePointer(textStr1);
        BytePointer textPtr2 = testStdString(textPtr1);
        assertEquals(textStr1, textPtr1.getString());
        assertEquals(textStr1, textPtr2.getString());
        BytePointer textPtr3 = testStdString2(textPtr1);
        assertEquals(textStr1, textPtr3.getString());

        CharPointer textCharPtr1 = new CharPointer(textStr1);
        assertEquals(textStr1, textCharPtr1.getString());

        IntPointer textIntPtr1 = new IntPointer(textStr1);
        assertEquals(textStr1, textIntPtr1.getString());

        if (Loader.getPlatform().startsWith("windows")) {
            // UTF-16
            CharPointer textCharPtr2 = testStdWString(textCharPtr1);
            assertEquals(textStr1, textCharPtr2.getString());

            CharPointer textCharPtr3 = testStdWString2(textCharPtr1);
            assertEquals(textStr1, textCharPtr3.getString());

            String textStr4 = testStdWString(textStr1);
            assertEquals(textStr1, textStr4);
        } else {
            // UTF-32
            IntPointer textIntPtr2 = testStdWString(textIntPtr1);
            assertEquals(textStr1, textIntPtr2.getString());
            IntPointer textIntPtr3 = testStdWString2(textIntPtr1);
            assertEquals(textStr1, textIntPtr3.getString());
        }

        CharPointer textCharPtr4 = testStdU16String(textCharPtr1);
        assertEquals(textStr1, textCharPtr4.getString());

        String textStr5 = testStdU16String(textStr1);
        assertEquals(textStr1, textStr5);

        IntPointer textIntPtr4 = testStdU32String(textIntPtr1);
        assertEquals(textStr1, textIntPtr4.getString());

        byte[] test = getConstStdString();
        assertEquals("test", new String(test));
        byte[] test2 = getConstStdString2();
        assertEquals("test", new String(test2));
        System.gc();
    }

    @Test public void testCharString() {
        System.out.println("CharString");

        String textStr1 = "This is a normal ASCII string.";
        String textStr2 = testCharString(textStr1);
        assertEquals(textStr1, textStr2);

        BytePointer textPtr1 = new BytePointer(textStr1);
        BytePointer textPtr2 = testCharString(textPtr1);
        assertEquals(textStr1, textPtr1.getString());
        assertEquals(textStr1, textPtr2.getString());
        System.gc();
    }

    @Test public void testShortString() {
        System.out.println("ShortString");

        String textStr1 = "This is a normal ASCII string.";
        CharPointer textPtr1 = new CharPointer(textStr1);
        CharPointer textPtr2 = testShortString(textPtr1);
        assertEquals(textStr1, textPtr1.getString());
        assertEquals(textStr1, textPtr2.getString());

        String textStr2 = testShortString(textStr1);
        assertEquals(textStr1, textStr2);

        System.gc();
    }

    @Test public void testIntString() {
        System.out.println("IntString");

        String textStr = "This is a normal ASCII string.";
        IntPointer textPtr1 = new IntPointer(textStr);
        IntPointer textPtr2 = testIntString(textPtr1);
        assertEquals(textStr, textPtr1.getString());
        assertEquals(textStr, textPtr2.getString());
        System.gc();
    }

    @Test public void testSharedPtr() {
        System.out.println("SharedPtr");
        SharedData sharedData = createSharedData();
        assertEquals(42, sharedData.data());

        storeSharedData(sharedData);
        sharedData.deallocate();

        sharedData = fetchSharedData();
        assertEquals(13, sharedData.data());
        sharedData.deallocate();

        assertEquals(1, constructorCount());
        assertEquals(1, destructorCount());
        System.gc();
    }

    @Test public void testUniquePtr() {
        System.out.println("UniquePtr");

        UniqueData uniqueData = fetchUniqueData();
        assertEquals(13, uniqueData.data());

        uniqueData = createUniqueData();
        assertEquals(5, uniqueData.data());

        storeUniqueData(uniqueData);

        uniqueData = fetchUniqueData();
        assertEquals(5, uniqueData.data());

        uniqueData = new UniqueData(null);
        createUniqueData(uniqueData);
        assertEquals(42, uniqueData.data());

        storeUniqueData(uniqueData);

        uniqueData = fetchUniqueData();
        assertEquals(42, uniqueData.data());
        System.gc();
    }

    @Test public void testStdVector() {
        System.out.println("StdVector");
        int[] arr = {5, 7, 13, 37, 42};
        IntPointer ptr = new IntPointer(arr);
        IntBuffer buf = ptr.asBuffer();
        PointerPointer ptrptr = new PointerPointer(ptr, ptr, ptr, ptr, ptr);

        IntPointer ptr2 = testStdVectorByVal(ptr);
        IntBuffer buf2 = testStdVectorByRef(buf).asBuffer();
        int[] arr2 = testStdVectorByPtr(arr);
        PointerPointer ptrptr2 = testStdVectorConstPointer(ptrptr);

        for (int i = 0; i < arr.length; i++) {
            assertEquals(ptr.get(i), ptr2.get(i));
            assertEquals(buf.get(i), buf2.get(i));
            assertEquals(arr[i], arr2[i]);
            assertEquals(ptrptr.get(i), ptrptr2.get(i));
        }
        System.gc();
    }

    @Test public void testStdMove() {
        System.out.println("StdMove");
        MovedData m = getMovedData();
        System.out.println(m);
        System.out.println(m.data());
        assertEquals(13, m.data());
        assertNotNull(m.deallocator());
        m.deallocate();

        m = new MovedData(42);
        putMovedData(m);
        System.out.println(m);
        System.out.println(m.data()); // probably 42, but undefined
        MovedData m2 = getMovedData();
        System.out.println(m2);
        System.out.println(m2.data());
        assertEquals(42, m2.data());
        assertNotEquals(m.address(), m2.address());
        assertNotNull(m.deallocator());
        assertNotNull(m2.deallocator());
        m.deallocate();
        m2.deallocate();
    }

    @Test public void testOptional() {
        System.out.println("Optional");
        assertTrue(testOptionalInt(new IntPointer((Pointer)null)).isNull());
        assertEquals(42, testOptionalInt(new IntPointer(1).put(42)).get(0));
    }

}
