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
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.StdString;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for text and data strings. Uses various classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform(include="StringTest.h")
public class StringTest {

    static native @StdString String testStdString(@StdString String str);
    static native @StdString BytePointer testStdString(@StdString BytePointer str);

    static native String testCharString(String str);
    static native @Cast("char*") BytePointer testCharString(@Cast("char*") BytePointer str);

    static native CharPointer testShortString(CharPointer str);

    static native IntPointer testIntString(IntPointer str);

    @BeforeClass public static void setUpClass() throws Exception {
        Class c = StringTest.class;
        Builder builder = new Builder().classesOrPackages(c.getName());
        File[] outputFiles = builder.build();
        Loader.loadLibraries = true;
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

        BytePointer textPtr1 = new BytePointer(textStr1);
        BytePointer textPtr2 = testStdString(textPtr1);
        assertEquals(textStr1, textPtr1.getString());
        assertEquals(textStr1, textPtr2.getString());
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
    }

    @Test public void testShortString() {
        System.out.println("ShortString");

        String textStr = "This is a normal ASCII string.";
        CharPointer textPtr1 = new CharPointer(textStr);
        CharPointer textPtr2 = testShortString(textPtr1);
        assertEquals(textStr, textPtr1.getString());
        assertEquals(textStr, textPtr2.getString());
    }

    @Test public void testIntString() {
        System.out.println("IntString");

        String textStr = "This is a normal ASCII string.";
        IntPointer textPtr1 = new IntPointer(textStr);
        IntPointer textPtr2 = testIntString(textPtr1);
        assertEquals(textStr, textPtr1.getString());
        assertEquals(textStr, textPtr2.getString());
    }

}
