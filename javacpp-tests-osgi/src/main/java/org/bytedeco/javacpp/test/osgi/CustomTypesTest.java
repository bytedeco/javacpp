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
package org.bytedeco.javacpp.test.osgi;

import static org.junit.Assert.assertEquals;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Const;
import org.bytedeco.javacpp.annotation.Function;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.SharedPtr;
import org.bytedeco.javacpp.annotation.UniquePtr;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test validates that Custom object types can be shared between
 * Java and Native code when using OSGi. This further validates that
 * JavaCPP is able to load custom pointer types that are only visible
 * in the target classloader 
 * 
 * It is based upon AdapterTest from the JavaCPP unit tests
 *
 * @author Samuel Audet
 */
@Platform(compiler = "cpp11", define = {"SHARED_PTR_NAMESPACE std", "UNIQUE_PTR_NAMESPACE std"}, include = "CustomTypes.h")
public class CustomTypesTest {

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

    @BeforeClass public static void setUpClass() throws Exception {
        System.out.println("Loader");
        Loader.load();
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
}
