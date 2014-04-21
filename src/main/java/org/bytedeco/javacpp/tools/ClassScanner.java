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

package org.bytedeco.javacpp.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * Given a {@link UserClassLoader}, attempts to match and fill in a {@link Collection}
 * of {@link Class}, in various ways in which users may wish to do so.
 */
class ClassScanner {
    ClassScanner(Logger logger, Collection<Class> classes, UserClassLoader loader) {
        this.logger  = logger;
        this.classes = classes;
        this.loader  = loader;
    }

    private Logger logger;
    private Collection<Class> classes;
    private UserClassLoader loader;

    public Collection<Class> getClasses() {
        return classes;
    }
    public UserClassLoader getClassLoader() {
        return loader;
    }

    public void addClass(String className) {
        if (className == null) {
            return;
        } else if (className.endsWith(".class")) {
            className = className.substring(0, className.length()-6);
        }
        try {
            Class c = Class.forName(className, false, loader);
            if (!classes.contains(c)) {
                classes.add(c);
            }
        } catch (ClassNotFoundException e) {
            logger.warn("Could not find class " + className + ": " + e);
        } catch (NoClassDefFoundError e) {
            logger.warn("Could not load class " + className + ": " + e);
        }
    }

    public void addMatchingFile(String filename, String packagePath, boolean recursive) {
        if (filename != null && filename.endsWith(".class") &&
                (packagePath == null || (recursive && filename.startsWith(packagePath)) ||
                filename.regionMatches(0, packagePath, 0, Math.max(filename.lastIndexOf('/'), packagePath.lastIndexOf('/'))))) {
            addClass(filename.replace('/', '.'));
        }
    }

    public void addMatchingDir(String parentName, File dir, String packagePath, boolean recursive) {
        File[] files = dir.listFiles();
        Arrays.sort(files);
        for (File f : files) {
            String pathName = parentName == null ? f.getName() : parentName + f.getName();
            if (f.isDirectory()) {
                addMatchingDir(pathName + "/", f, packagePath, recursive);
            } else {
                addMatchingFile(pathName, packagePath, recursive);
            }
        }
    }

    public void addPackage(String packageName, boolean recursive) throws IOException {
        String[] paths = loader.getPaths();
        final String packagePath = packageName == null ? null : (packageName.replace('.', '/') + "/");
        int prevSize = classes.size();
        for (String p : paths) {
            File file = new File(p);
            if (file.isDirectory()) {
                addMatchingDir(null, file, packagePath, recursive);
            } else {
                JarInputStream jis = new JarInputStream(new FileInputStream(file));
                ZipEntry e = jis.getNextEntry();
                while (e != null) {
                    addMatchingFile(e.getName(), packagePath, recursive);
                    jis.closeEntry();
                    e = jis.getNextEntry();
                }
                jis.close();
            }
        }
        if (classes.size() == 0 && packageName == null) {
            logger.warn("No classes found in the unnamed package");
            Builder.printHelp();
        } else if (prevSize == classes.size() && packageName != null) {
            logger.warn("No classes found in package " + packageName);
        }
    }

    public void addClassOrPackage(String name) throws IOException {
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
