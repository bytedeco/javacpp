/*
 * Copyright (C) 2017-2019 Samuel Audet
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
import java.net.URL;
import java.util.Properties;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.tools.BuildEnabled;
import org.bytedeco.javacpp.tools.Builder;
import org.bytedeco.javacpp.tools.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for the building phase. Uses other classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform(extension = {"-ext1", "-ext2"})
public class BuilderTest implements BuildEnabled, LoadEnabled {

    static int initCount = 0;
    static Logger logger = null;
    static Properties properties = null;
    static String encoding = null;

    @Override public void init(ClassProperties properties) {
        initCount++;
    }

    @Override public void init(Logger logger, Properties properties, String encoding) {
        this.logger = logger;
        this.properties = properties;
        this.encoding = encoding;
    }

    @Test public void testExtensions() throws Exception {
        System.out.println("Builder");
        Class c = BuilderTest.class;
        String[] extensions = {"", "-ext1", "-ext2"};
        for (String extension : extensions) {
            URL u = Loader.findResource(c, Loader.getPlatform() + extension);
            if (u != null) {
                for (File f : new File(u.toURI()).listFiles()) {
                    if (f.getName().contains("jniBuilderTest")) {
                        f.delete();
                    }
                }
            }
        }

        Builder builder0 = new Builder().classesOrPackages(c.getName());
        File[] outputFiles0 = builder0.build();
        assertEquals(0, outputFiles0.length);

        System.out.println("Builder");
        Builder builder = new Builder().property("platform.extension", "-ext1").classesOrPackages(c.getName());
        File[] outputFiles = builder.build();

        System.out.println("Loader");
        try {
            System.out.println("Note: UnsatisfiedLinkError should get thrown here and printed below.");
            Loader.loadGlobal("/path/to/nowhere");
            fail("UnsatisfiedLinkError should have been thrown.");
        } catch (Throwable t) {
            System.out.println(t);
        }
        Loader.loadProperties().remove("platform.extension");
        String filename = Loader.load(c);
        Loader.loadGlobal(filename);
        assertTrue(Loader.getLoadedLibraries().get("jniBuilderTest").contains("-ext1"));
        Loader.foundLibraries.clear();
        Loader.loadedLibraries.clear();

        System.out.println("Builder");
        Builder builder2 = new Builder().property("platform.extension", "-ext2").classesOrPackages(c.getName());
        File[] outputFiles2 = builder2.build();

        System.out.println("Loader");
        Loader.loadProperties().remove("platform.extension");
        filename = Loader.load(c);
        Loader.loadGlobal(filename);
        assertTrue(Loader.getLoadedLibraries().get("jniBuilderTest").contains("-ext2"));
        Loader.foundLibraries.clear();
        Loader.loadedLibraries.clear();

        Loader.loadProperties().put("platform.extension", "-ext1");
        filename = Loader.load(c);
        Loader.loadGlobal(filename);
        assertTrue(Loader.getLoadedLibraries().get("jniBuilderTest").contains("-ext1"));

        System.out.println(initCount);
        assertTrue(initCount >= 6);
        assertNotNull(logger);
        assertNotNull(properties);
    }

}
