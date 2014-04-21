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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;

/**
 * An extension of {@link URLClassLoader} that keeps a list of paths in memory.
 * Adds {@code System.getProperty("user.dir")} as default path if none are added.
 */
class UserClassLoader extends URLClassLoader {
    private LinkedList<String> paths = new LinkedList<String>();
    public UserClassLoader() {
        super(new URL[0]);
    }
    public UserClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }
    public void addPaths(String ... paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            File f = new File(path);
            if (!f.exists()) {
                continue;
            }
            this.paths.add(path);
            try {
                addURL(f.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public String[] getPaths() {
        if (paths.isEmpty()) {
            addPaths(System.getProperty("user.dir"));
        }
        return paths.toArray(new String[paths.size()]);
    }
    @Override protected Class<?> findClass(String name)
            throws ClassNotFoundException {
        if (paths.isEmpty()) {
            addPaths(System.getProperty("user.dir"));
        }
        return super.findClass(name);
    }
}
