/*
 * Copyright (C) 2011,2012,2013 Samuel Audet
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

package com.googlecode.javacpp;

import com.googlecode.javacpp.annotation.Platform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.WeakHashMap;

/**
 * The Loader contains functionality to load native libraries, but also has a bit
 * of everything that does not fit anywhere else. In addition to its library loading
 * features, it also has utility methods to get the platform name, to load properties
 * from Java resources and from Class annotations, to extract file resources to the
 * temporary directory, and to get the {@code offsetof()} or {@code sizeof()} a native
 * {@code struct}, {@code class}, or {@code union} with its {@link Pointer} peer class
 * and a {@link HashMap} initialized by the native libraries inside {@code JNI_OnLoad()}.
 *
 * @author Samuel Audet
 */
public class Loader {

    /** Value created out of "java.vm.name", "os.name", and "os.arch" system properties.
     *  Returned by {@link #getPlatformName()} as default. */
    private static final String platformName;
    /** Default platform properties loaded and returned by {@link #loadProperties()}. */
    private static Properties platformProperties = null;

    static {
        String jvmName = System.getProperty("java.vm.name").toLowerCase();
        String osName  = System.getProperty("os.name").toLowerCase();
        String osArch  = System.getProperty("os.arch").toLowerCase();
        if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
            osName = "android";
        } else if (osName.startsWith("mac os x")) {
            osName = "macosx";
        } else {
            int spaceIndex = osName.indexOf(' ');
            if (spaceIndex > 0) {
                osName = osName.substring(0, spaceIndex);
            }
        }
        if (osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586") || osArch.equals("i686")) {
            osArch = "x86";
        } else if (osArch.equals("amd64") || osArch.equals("x86-64") || osArch.equals("x64")) {
            osArch = "x86_64";
        } else if (osArch.startsWith("arm")) {
            osArch = "arm";
        }
        platformName = osName + "-" + osArch;
    }

    /**
     * Returns either the value of the "com.googlecode.javacpp.platform.name"
     * system property, or {@link #platformName} when the former is not set.
     *
     * @return {@code System.getProperty("com.googlecode.javacpp.platform.name", platformName)}
     * @see #platformName
     */
    public static String getPlatformName() {
        return System.getProperty("com.googlecode.javacpp.platform.name", platformName);
    }

    /**
     * Loads the {@link Properties} associated with the default {@link #getPlatformName()}.
     *
     * @see #loadProperties(String)
     */
    public static Properties loadProperties() {
        String name = getPlatformName();
        if (platformProperties != null && name.equals(platformProperties.getProperty("platform.name"))) {
            return platformProperties;
        }
        return platformProperties = loadProperties(name);
    }
    /**
     * Loads from resources the default {@link Properties} of the specified platform name.
     *
     * @param name the platform name
     * @return the Properties from resources
     */
    public static Properties loadProperties(String name) {
        Properties p = new Properties();
        p.put("platform.name", name);
        name = "properties/" + name + ".properties";
        InputStream is = Loader.class.getResourceAsStream(name);
        try {
            try {
                p.load(new InputStreamReader(is));
            } catch (NoSuchMethodError e) {
                p.load(is);
            }
        } catch (Exception e) {
            name = "properties/generic.properties";
            is = Loader.class.getResourceAsStream(name);
            try {
                try {
                    p.load(new InputStreamReader(is));
                } catch (NoSuchMethodError e2) {
                    p.load(is);
                }
            } catch (Exception e2) {
                throw new MissingResourceException("Could not even get generic properties: " +
                        e2.getMessage(), Loader.class.getName(), name);
            }
        }
        return p;
    }

    /**
     * If annotated with properties, returns the argument as "enclosing Class".
     * If no properties are found on the Class, makes a search for the first Class
     * with properties that we can use, and returns it as the enclosing Class found.
     *
     * @param cls the Class to start the search from
     * @return the enclosing Class
     * @see com.googlecode.javacpp.annotation.Platform
     * @see com.googlecode.javacpp.annotation.Properties
     */
    public static Class getEnclosingClass(Class cls) {
        Class<?> c = cls;
        // Find first enclosing declaring class with some properties to use
        while (c.getDeclaringClass() != null) {
            if (c.isAnnotationPresent(com.googlecode.javacpp.annotation.Properties.class)) {
                break;
            }
            if (c.isAnnotationPresent(Platform.class)) {
                Platform p = c.getAnnotation(Platform.class);
                if (p.define().length > 0 || p.include().length > 0 || p.cinclude().length > 0 ||
                        p.includepath().length > 0 || p.options().length > 0 || p.linkpath().length > 0 ||
                        p.link().length > 0 || p.framework().length > 0 ||  p.preloadpath().length > 0 ||
                        p.preload().length > 0 || p.library().length() > 0) {
                    break;
                }
            }
            c = c.getDeclaringClass();
        }
        return c;
    }

    /**
     * Does the heavy lifting of collecting values off Properties annotations found
     * on enclosing classes. Operates for the desired "platform.name" value specified
     * in {@link java.util.Properties}. As a {@link HashMap}, it makes the result
     * easily accessible, and mutable.
     *
     * @see #loadProperties(Class, java.util.Properties, boolean)
     */
    public static class ClassProperties extends HashMap<String,LinkedList<String>> {
        public ClassProperties() { }
        public ClassProperties(Properties properties) {
            platformName  = properties.getProperty("platform.name");
            platformRoot  = properties.getProperty("platform.root");
            pathSeparator = properties.getProperty("path.separator");
            if (platformRoot == null || platformRoot.length() == 0) {
                platformRoot = ".";
            }
            if (!platformRoot.endsWith(File.separator)) {
                platformRoot += File.separator;
            }
            for (Map.Entry e : properties.entrySet()) {
                String k = (String)e.getKey(), v = (String)e.getValue();
                if (v == null || v.length() == 0) {
                    continue;
                }
                if (k.equals("compiler.includepath") || k.equals("compiler.include") ||
                        k.equals("compiler.linkpath") || k.equals("compiler.link") ||
                        k.equals("compiler.framework")) {
                    addAll(k, v.split(pathSeparator));
                } else {
                    setProperty(k, v);
                }
            }
        }

        String platformName, platformRoot, pathSeparator;

        public LinkedList<String> get(String key) {
            LinkedList<String> list = super.get(key);
            if (list == null) {
                put((String)key, list = new LinkedList<String>());
            }
            return list;
        }

        public void addAll(String key, String ... values) {
            if (values != null) {
                addAll(key, Arrays.asList(values));
            }
        }
        public void addAll(String key, Collection<String> values) {
            if (values != null) {
                String root = null;
                if (key.equals("compiler.path") || key.equals("compiler.sysroot") ||
                        key.equals("compiler.includepath") || key.equals("compiler.linkpath")) {
                    root = platformRoot;
                }

                LinkedList<String> values2 = get(key);
                for (String value : values) {
                    if (value != null && !values2.contains(value)) {
                        if (root != null && !new File(value).isAbsolute() &&
                                new File(root + value).exists()) {
                            value = root + value;
                        }
                        values2.add(value);
                    }
                }
            }
        }

        public String getProperty(String key) {
            return getProperty(key, null);
        }
        public String getProperty(String key, String defaultValue) {
            LinkedList<String> values = get(key);
            return values.isEmpty() ? defaultValue : values.get(0);
        }
        public String setProperty(String key, String value) {
            LinkedList<String> values = get(key);
            String oldValue = values.isEmpty() ? null : values.get(0);
            values.clear();
            addAll(key, value);
            return oldValue;
        }

        public void load(Class cls, boolean inherit) {
            Class<?> c = getEnclosingClass(cls);
            while (!c.isAnnotationPresent(com.googlecode.javacpp.annotation.Properties.class)
                    && !c.isAnnotationPresent(Platform.class) && c.getSuperclass() != null) {
                c = c.getSuperclass();
            }
            com.googlecode.javacpp.annotation.Properties classProperties =
                    c.getAnnotation(com.googlecode.javacpp.annotation.Properties.class);
            Platform[] platforms;
            if (classProperties == null) {
                Platform platform = c.getAnnotation(Platform.class);
                if (platform == null) {
                    return;
                } else {
                    platforms = new Platform[] { platform };
                }
            } else {
                Class[] classes = classProperties.inherit();
                if (inherit && classes != null) {
                    for (Class c2 : classes) {
                        load(c2, inherit);
                    }
                }
                String target = classProperties.target();
                if (target.length() > 0) {
                    addAll("parser.target", target);
                }
                platforms = classProperties.value();
                if (platforms == null) {
                    return;
                }
            }

            String[] define = {}, include = {}, cinclude = {}, includepath = {}, options = {},
                     linkpath = {}, link = {}, framework = {}, preloadpath = {}, preload = {};
            String library = "jni" + c.getSimpleName();
            for (Platform p : platforms) {
                String[][] names = { p.value(), p.not() };
                boolean[] matches = { false, false };
                for (int i = 0; i < names.length; i++) {
                    for (String s : names[i]) {
                        if (platformName.startsWith(s)) {
                            matches[i] = true;
                            break;
                        }
                    }
                }
                if ((names[0].length == 0 || matches[0]) && (names[1].length == 0 || !matches[1])) {
                    if (p.define()     .length > 0) { define      = p.define();      }
                    if (p.include()    .length > 0) { include     = p.include();     }
                    if (p.cinclude()   .length > 0) { cinclude    = p.cinclude();    }
                    if (p.includepath().length > 0) { includepath = p.includepath(); }
                    if (p.options()    .length > 0) { options     = p.options();     }
                    if (p.linkpath()   .length > 0) { linkpath    = p.linkpath();    }
                    if (p.link()       .length > 0) { link        = p.link();        }
                    if (p.framework()  .length > 0) { framework   = p.framework();   }
                    if (p.preloadpath().length > 0) { preloadpath = p.preloadpath(); }
                    if (p.preload()    .length > 0) { preload     = p.preload();     }
                    if (p.library().length() > 0)   { library     = p.library();     }
                }
            }
            addAll("generator.define", define);
            addAll("generator.include", include);
            addAll("generator.cinclude", cinclude);
            addAll("compiler.includepath", includepath);
            addAll("compiler.options", options);
            addAll("compiler.linkpath", linkpath);
            addAll("compiler.link", link);
            addAll("compiler.framework", framework);
            addAll("loader.preloadpath", preloadpath);
            addAll("loader.preload", preload);
            setProperty("loader.library", library);
        }

        LinkedList<File> getHeaderFiles() throws FileNotFoundException {
            LinkedList<String> paths = get("compiler.includepath");
            LinkedList<String> includes = new LinkedList<String>();
            includes.addAll(get("generator.include"));
            includes.addAll(get("generator.cinclude"));
            LinkedList<File> files = new LinkedList<File>();
            for (String include : includes) {
                boolean found = false;
                if (include.startsWith("<") && include.endsWith(">")) {
                    include = include.substring(1, include.length() - 1);
                } else {
                    File f = new File(include);
                    if (f.exists()) {
                        found = true;
                        files.add(f);
                        continue;
                    }
                }
                for (String path : paths) {
                    File f = new File(path, include);
                    if (f.exists()) {
                        found = true;
                        files.add(f);
                        break;
                    }
                }
                if (!found) {
                    throw new FileNotFoundException("Could not find header file: " + include);
                }
            }
            return files;
        }
    }

    /**
     * For all the classes, loads all properties from each Class annotations for the given platform.
     * @see #loadProperties(Class, java.util.Properties, boolean)
     */
    public static ClassProperties loadProperties(Class[] cls, Properties properties, boolean inherit) {
        ClassProperties cp = new ClassProperties(properties);
        for (Class c : cls) {
            cp.load(c, inherit);
        }
        return cp;
    }
    /**
     * Loads all properties from Class annotations for the given platform. The platform
     * of interest needs to be specified as the value of the "platform.name" key in the
     * properties argument. It is also possible to indicate whether to load all the classes
     * specified in the {@link com.googlecode.javacpp.annotation.Properties#inherit()}
     * annotation recursively via the inherit argument.
     *
     * @param cls the Class of which to return Properties
     * @param properties the platform Properties to inherit
     * @param inherit indicates whether or not to inherit properties from other classes
     * @return all the properties associated with the Class for the given platform
     */
    public static ClassProperties loadProperties(Class cls, Properties properties, boolean inherit) {
        ClassProperties cp = new ClassProperties(properties);
        cp.load(cls, inherit);
        return cp;
    }

    /**
     * Returns the {@link Class} object that contains a caller's method.
     *
     * @param i the offset on the call stack of the method of interest
     * @return the Class found from the calling context, or {@code null} if not found
     */
    public static Class getCallerClass(int i) {
        Class[] classContext = new SecurityManager() {
            @Override public Class[] getClassContext() {
                return super.getClassContext();
            }
        }.getClassContext();
        if (classContext != null) {
            for (int j = 0; j < classContext.length; j++) {
                if (classContext[j] == Loader.class) {
                    return classContext[i+j];
                }
            }
        } else {
            // SecurityManager.getClassContext() returns null on Android 4.0
            try {
                StackTraceElement[] classNames = Thread.currentThread().getStackTrace();
                for (int j = 0; j < classNames.length; j++) {
                    if (Class.forName(classNames[j].getClassName()) == Loader.class) {
                        return Class.forName(classNames[i+j].getClassName());
                    }
                }
            } catch (ClassNotFoundException e) { }
        }
        return null;
    }

    /**
     * Extracts by name a resource using the {@link ClassLoader} of the caller.
     *
     * @param name the name of the resource passed to {@link Class#getResource(String)}
     * @see #extractResource(URL, File, String, String)
     */
    public static File extractResource(String name, File directory,
            String prefix, String suffix) throws IOException {
        Class cls = getCallerClass(2);
        return extractResource(cls, name, directory, prefix, suffix);
    }
    /**
     * Extracts by name a resource using the {@link ClassLoader} of the specified {@link Class}.
     *
     * @param cls the Class from which to load resources
     * @param name the name of the resource passed to {@link Class#getResource(String)}
     * @see #extractResource(URL, File, String, String)
     */
    public static File extractResource(Class cls, String name, File directory,
            String prefix, String suffix) throws IOException {
        return extractResource(cls.getResource(name), directory, prefix, suffix);
    }
    /**
     * Extracts a resource into the specified directory and with the specified
     * prefix and suffix for the filename. If both prefix and suffix are {@code null},
     * the original filename is used, so the directory must not be {@code null}.
     *
     * @param resourceURL the URL of the resource to extract
     * @param directory the output directory ({@code null == System.getProperty("java.io.tmpdir")})
     * @param prefix the prefix of the temporary filename to use
     * @param suffix the suffix of the temporary filename to use
     * @return the File object representing the extracted file
     * @throws IOException if fails to extract resource properly
     */
    public static File extractResource(URL resourceURL, File directory,
            String prefix, String suffix) throws IOException {
        InputStream is = resourceURL != null ? resourceURL.openStream() : null;
        if (is == null) {
            return null;
        }
        File file = null;
        boolean fileExisted = false;
        try {
            if (prefix == null && suffix == null) {
                if (directory == null) {
                    directory = new File(System.getProperty("java.io.tmpdir"));
                }
                file = new File(directory, new File(resourceURL.getPath()).getName());
                fileExisted = file.exists();
            } else {
                file = File.createTempFile(prefix, suffix, directory);
            }
            FileOutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            if (file != null && !fileExisted) {
                file.delete();
            }
            throw e;
        }
        return file;
    }


    /** Temporary directory set and returned by {@link #getTempDir()}. */
    static File tempDir = null;
    /** Flag set by the {@link Builder} to tell us not to try to load anything. */
    static boolean loadLibraries = true;
    /** Contains all the native libraries that we have loaded to avoid reloading them. */
    static Map<String,String> loadedLibraries = Collections.synchronizedMap(new HashMap<String,String>());

    /**
     * Creates a unique name for {@link #tempDir} out of
     * {@code System.getProperty("java.io.tmpdir")} and {@code System.nanoTime()}.
     *
     * @return {@link #tempDir}
     */
    public static File getTempDir() {
        if (tempDir == null) {
            File tmpdir = new File(System.getProperty("java.io.tmpdir"));
            File f = null;
            for (int i = 0; i < 1000; i++) {
                f = new File(tmpdir, "javacpp" + System.nanoTime());
                if (f.mkdir()) {
                    tempDir = f;
                    tempDir.deleteOnExit();
                    break;
                }
            }
        }
        return tempDir;
    }

    /** @return {@link #loadLibraries} */
    public static boolean isLoadLibraries() {
        return loadLibraries;
    }

    /**
     * Loads native libraries associated with the {@link Class} of the caller.
     * @return {@code load(getCallerClass(2)) }
     * @see #getCallerClass(int)
     * @see #load(Class)
     */
    public static String load() {
        Class cls = getCallerClass(2);
        return load(cls);
    }
    /**
     * Loads native libraries associated with the given {@link Class}.
     *
     * @param cls the Class to get native library information from
     * @return the full path to the main file loaded, or the library name if unknown
     *         (but {@code if (!loadLibraries || cls == null) { return null; }})
     * @throws NoClassDefFoundError on Class initialization failure
     * @throws UnsatisfiedLinkError on native library loading failure
     */
    public static String load(Class cls) {
        if (!loadLibraries || cls == null) {
            return null;
        }

        // Find the top enclosing class, to match the library filename
        cls = getEnclosingClass(cls);

        // Force initialization of the class in case it needs it
        try {
            cls = Class.forName(cls.getName(), true, cls.getClassLoader());
        } catch (ClassNotFoundException ex) {
            Error e = new NoClassDefFoundError(ex.toString());
            e.initCause(ex);
            throw e;
        }

        // Preload native libraries desired by our class
        ClassProperties p = loadProperties(cls, loadProperties(), true);
        LinkedList<String> preloads = new LinkedList<String>();
        preloads.addAll(p.get("loader.preload"));
        preloads.addAll(p.get("compiler.link"));
        UnsatisfiedLinkError preloadError = null;
        for (String preload : preloads) {
            try {
                URL[] urls = findLibrary(cls, p, preload);
                loadLibrary(urls, preload);
            } catch (UnsatisfiedLinkError e) {
                preloadError = e;
            }
        }

        try {
            String library = p.getProperty("loader.library");
            URL[] urls = findLibrary(cls, p, library);
            return loadLibrary(urls, library);
        } catch (UnsatisfiedLinkError e) {
            if (preloadError != null && e.getCause() == null) {
                e.initCause(preloadError);
            }
            throw e;
        }
    }

    /**
     * Finds where the library may be extracted and loaded among the {@link Class}
     * resources. But in case that fails, also searches the paths found in the
     * "loader.preloadpath" and "compiler.linkpath" properties.
     *
     * @param cls the Class whose package name and {@link ClassLoader} are used to extract from resources
     * @param properties contains the directories to scan for if we fail to extract the library from resources
     * @param libnameversion the name of the library + "@" + optional version tag
     * @return URLs that point to potential locations of the library
     */
    public static URL[] findLibrary(Class cls, ClassProperties properties, String libnameversion) {
        String[] s = libnameversion.split("@");
        String libname = s[0];
        String version = s.length > 1 ? s[s.length-1] : "";

        // If we do not already have the native library file ...
        String filename = loadedLibraries.get(libnameversion);
        if (filename != null) {
            try {
                return new URL[] { new File(filename).toURI().toURL() };
            } catch (IOException ex) {
                return new URL[] { };
            }
        }

        String subdir = properties.getProperty("platform.name") + '/';
        String prefix = properties.getProperty("library.prefix", "") + libname;
        String suffix = properties.getProperty("library.suffix", "");
        String[] styles = {
            prefix + suffix + version, // Linux style
            prefix + version + suffix, // Mac OS X style
            prefix + suffix            // without version
        };

        int k = 0;
        LinkedList<String> paths = new LinkedList<String>();
        paths.addAll(properties.get("loader.preloadpath"));
        paths.addAll(properties.get("compiler.linkpath"));
        URL[] urls = new URL[styles.length * (1 + paths.size())];
        for (int i = 0; cls != null && i < styles.length; i++) {
            // ... then find it from in our resources ...
            URL u = cls.getResource(subdir + styles[i]);
            if (u != null) {
                urls[k++] = u;
            }
        }
        // ... and in case of bad resources, search the paths as well.
        for (int i = 0; paths.size() > 0 && i < styles.length; i++) {
            for (String path : paths) {
                File file = new File(path, styles[i]);
                if (file.exists()) {
                    try {
                        urls[k++] = file.toURI().toURL();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        return Arrays.copyOf(urls, k);
    }

    /**
     * Tries to load the library from the URLs in order, extracting resources as necessary.
     * Finally, if all fails, falls back on {@link System#loadLibrary(String)}.
     *
     * @param urls the URLs to try loading the library from
     * @param libnameversion the name of the library + "@" + optional version tag
     * @return the full path of the file loaded, or the library name if unknown
     *         (but {@code if (!loadLibraries) { return null; }})
     * @throws UnsatisfiedLinkError on failure
     */
    public static String loadLibrary(URL[] urls, String libnameversion) {
        if (!loadLibraries) {
            return null;
        }

        // If we do not already have the native library file ...
        String filename = loadedLibraries.get(libnameversion);
        if (filename != null) {
            return filename;
        }

        File tempFile = null;
        UnsatisfiedLinkError loadError = null;
        try {
            for (URL url : urls) {
                File file;
                try {
                    // ... if the URL is not already a file ...
                    file = new File(url.toURI());
                } catch (Exception e) {
                    if (tempFile != null && tempFile.exists()) {
                        tempFile.deleteOnExit();
                    }
                    // ... then extract it from our resources ...
                    file = tempFile = extractResource(url, getTempDir(), null, null);
                }
                if (file != null && file.exists()) {
                    filename = file.getAbsolutePath();
                    try {
                        // ... and load it!
                        loadedLibraries.put(libnameversion, filename);
                        System.load(filename);
                        return filename;
                    } catch (UnsatisfiedLinkError e) {
                        loadError = e;
                        loadedLibraries.remove(libnameversion);
                    }
                }
            }
            // ... or as last resort, try to load it via the system.
            String libname = libnameversion.split("@")[0];
            loadedLibraries.put(libnameversion, libname);
            System.loadLibrary(libname);
            return libname;
        } catch (UnsatisfiedLinkError e) {
            loadedLibraries.remove(libnameversion);
            if (loadError != null && e.getCause() == null) {
                e.initCause(loadError);
            }
            throw e;
        } catch (IOException ex) {
            loadedLibraries.remove(libnameversion);
            if (loadError != null && ex.getCause() == null) {
                ex.initCause(loadError);
            }
            Error e = new UnsatisfiedLinkError(ex.toString());
            e.initCause(ex);
            throw e;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.deleteOnExit();
            }
            // But under Windows, it won't get deleted!
        }
    }

    // So, let's use a shutdown hook...
    static {
        if (getPlatformName().startsWith("windows")) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override public void run() {
                    if (tempDir == null) {
                        return;
                    }
                    try {
                        // ... to launch a separate process ...
                        LinkedList<String> command = new LinkedList<String>();
                        command.add(System.getProperty("java.home") + "/bin/java");
                        command.add("-classpath");
                        command.add(System.getProperty("java.class.path"));
                        command.add(Loader.class.getName());
                        command.add(tempDir.getAbsolutePath());
                        new ProcessBuilder(command).start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    // ... that makes sure to delete all our files.
    public static void main(String[] args) {
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        File tempDir = new File(args[0]);
        if (!tmpdir.equals(tempDir.getParentFile()) ||
                !tempDir.getName().startsWith("javacpp")) {
            // Someone is trying to break us ... ?
            return;
        }
        for (File file : tempDir.listFiles()) {
            while (file.exists() && !file.delete()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) { }
            }
        }
        tempDir.delete();
    }


    /**
     * Contains {@code offsetof()} and {@code sizeof()} values of native types
     * of {@code struct}, {@code class}, and {@code union}. A {@link WeakHashMap}
     * is used to prevent the Loader from hanging onto Class objects the user may
     * be trying to unload.
     */
    static WeakHashMap<Class<? extends Pointer>,HashMap<String,Integer>> memberOffsets =
            new WeakHashMap<Class<? extends Pointer>,HashMap<String,Integer>>();

    /**
     * Called by native libraries to put {@code offsetof()} and {@code sizeof()} values in {@link #memberOffsets}.
     * Tries to load the Class object for typeName using the {@link ClassLoader} of the Loader.
     *
     * @param typeName the name of the peer Class acting as interface to the native type
     * @param member the name of the native member variable
     * @param offset the value of {@code offsetof()} (or {@code sizeof()} when {@code member.equals("sizeof")})
     * @throws ClassNotFoundException on Class initialization failure
     */
    static void putMemberOffset(String typeName, String member, int offset) throws ClassNotFoundException {
        Class<?> c = Class.forName(typeName.replace('/', '.'), false, Loader.class.getClassLoader());
        putMemberOffset(c.asSubclass(Pointer.class), member, offset);
    }
    /**
     * Called by native libraries to put {@code offsetof()} and {@code sizeof()} values in {@link #memberOffsets}.
     *
     * @param type the peer Class acting as interface to the native type
     * @param member the name of the native member variable
     * @param offset the value of {@code offsetof()} (or {@code sizeof()} when {@code member.equals("sizeof")})
     */
    static synchronized void putMemberOffset(Class<? extends Pointer> type, String member, int offset) {
        HashMap<String,Integer> offsets = memberOffsets.get(type);
        if (offsets == null) {
            memberOffsets.put(type, offsets = new HashMap<String,Integer>());
        }
        offsets.put(member, offset);
    }

    /**
     * Gets {@code offsetof()} values from {@link #memberOffsets} filled by native libraries.
     *
     * @param type the peer Class acting as interface to the native type
     * @param member the name of the native member variable
     * @return {@code memberOffsets.get(type).get(member)}
     */
    public static int offsetof(Class<? extends Pointer> type, String member) {
        // Should we synchronize that?
        return memberOffsets.get(type).get(member);
    }

    /**
     * Gets {@code sizeof()} values from {@link #memberOffsets} filled by native libraries.
     *
     * @param type the peer Class acting as interface to the native type
     * @return {@code memberOffsets.get(type).get("sizeof")}
     */
    public static int sizeof(Class<? extends Pointer> type) {
        // Should we synchronize that?
        return memberOffsets.get(type).get("sizeof");
    }
}
