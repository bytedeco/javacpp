/*
 * Copyright (C) 2014-2019 Samuel Audet
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

package org.bytedeco.javacpp.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Given a {@link UserClassLoader}, attempts to match and fill in a {@link Collection}
 * of {@link Class}, in various ways in which users may wish to do so.
 */
class ClassScanner {
    ClassScanner(Logger logger, Collection<Class> classes, UserClassLoader loader) {
        this(logger, classes, loader, null);
    }
    ClassScanner(Logger logger, Collection<Class> classes, UserClassLoader loader, ClassFilter classFilter) {
        this.logger  = logger;
        this.classes = classes;
        this.loader  = loader;
        this.classFilter = classFilter;
    }

    final Logger logger;
    final Collection<Class> classes;
    final UserClassLoader loader;
    final ClassFilter classFilter;

    public Collection<Class> getClasses() {
        return classes;
    }
    public UserClassLoader getClassLoader() {
        return loader;
    }

    public void addClass(String className) throws ClassNotFoundException, NoClassDefFoundError {
        if (className == null) {
            return;
        } else if (className.endsWith(".class")) {
            className = className.substring(0, className.length()-6);
        }
        Class c = Class.forName(className, false, loader);
        addClass(c);
    }

    public void addClass(Class c) {
        if (!classes.contains(c)) {
            classes.add(c);
        }
    }

    public void addMatchingFile(String filename, String packagePath, boolean recursive, byte... data) throws ClassNotFoundException, NoClassDefFoundError {
        if (filename != null && filename.endsWith(".class") && !filename.contains("-") &&
                (classFilter == null || classFilter.keep(filename, data)) &&
                (packagePath == null || (recursive && filename.startsWith(packagePath)) ||
                filename.regionMatches(0, packagePath, 0, Math.max(filename.lastIndexOf('/'), packagePath.lastIndexOf('/'))))) {
            addClass(filename.replace('/', '.'));
        }
    }

    public void addMatchingDir(String parentName, File dir, String packagePath, boolean recursive) throws ClassNotFoundException, IOException, NoClassDefFoundError {
        File[] files = dir.listFiles();
        Arrays.sort(files);
        for (File f : files) {
            String pathName = parentName == null ? f.getName() : parentName + f.getName();
            if (f.isDirectory()) {
                addMatchingDir(pathName + "/", f, packagePath, recursive);
            } else {
                byte[] data = Files.readAllBytes(f.toPath());
                addMatchingFile(pathName, packagePath, recursive, data);
            }
        }
    }

    public void addPackage(String packageName, boolean recursive) throws IOException, ClassNotFoundException, NoClassDefFoundError {
        String[] paths = loader.getPaths();
        final String packagePath = packageName != null && packageName.length() > 0 ? (packageName.replace('.', '/') + "/") : packageName;
        int prevSize = classes.size();
        for (String p : paths) {
            File file = new File(p);
            if (file.isDirectory()) {
                addMatchingDir(null, file, packagePath, recursive);
            } else {
                try (JarFile jarFile = new JarFile(file)) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        long entrySize = entry.getSize();
                        long entryTimestamp = entry.getTime();
                        if (entrySize > 0) {
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                byte[] data = new byte[(int)entrySize];
                                int i = 0;
                                while (i < data.length) {
                                    int n = is.read(data, i, data.length - i);
                                    if (n < 0) {
                                        break;
                                    }
                                    i += n;
                                }
                                addMatchingFile(entryName, packagePath, recursive, data);
                            }
                        }
                    }
                }
            }
        }
        if (classes.size() == 0 && (packageName == null || packageName.length() == 0)) {
            logger.warn("No classes found in the unnamed package");
        } else if (prevSize == classes.size() && packageName != null) {
            logger.warn("No classes found in package " + packageName);
        }
    }

    public void addClassOrPackage(String name) throws IOException, ClassNotFoundException, NoClassDefFoundError {
        if (name == null) {
            return;
        }
        name = name.replace('/', '.');
        if (name.endsWith(".**")) {
            addPackage(name.substring(0, name.length()-3), true);
        } else if (name.endsWith(".*")) {
            addPackage(name.substring(0, name.length()-2), false);
        } else {
            addClass(name);
        }
    }
}
