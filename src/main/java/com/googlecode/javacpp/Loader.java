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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Loader contains functionality to load native libraries, but also has a bit
 * of everything that does not fit anywhere else. In addition to its loading features,
 * it also has utility methods to get the platform name and its properties from Java
 * resources, to append to them properties from Class annotations, to extract file
 * resources to the temporary directory, and to get the {@code offsetof()} or
 * {@code sizeof()} a native {@code struct}, {@code class}, or {@code union} with
 * its {@link Pointer} peer class and a {@link HashMap} initialized by the native libraries.
 *
 * @author Samuel Audet
 */
public class Loader {

    private static final Logger logger = Logger.getLogger(Loader.class.getName());

    /** Platform name set and returned by {@link #getPlatformName()} and {@link #setPlatformName(String)}. */
    private static String platformName = null;
    /** Default platform properties loaded and returned by {@link #getProperties()}. */
    private static Properties platformProperties = null;

    /**
     * Creates a {@link #platformName} out of {@link System#getProperty(String)} if {@code null}.
     *
     * @return {@link #platformName}
     */
    public static String getPlatformName() {
        if (platformName != null) {
            return platformName;
        }
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
        } else if (osArch.equals("amd64") || osArch.equals("x86-64")) {
            osArch = "x86_64";
        } else if (osArch.startsWith("arm")) {
            osArch = "arm";
        }
        return platformName = osName + "-" + osArch;
    }
    /**
     * Lets the user set {@link #platformName} in case {@link #getPlatformName()} gets it wrong.
     *
     * @param platformName value of the platformName
     */
    public static void setPlatformName(String platformName) {
        Loader.platformName = platformName;
        Loader.platformProperties = null;
    }

    /**
     * Get the {@link Properties} associated with the default {@link #platformName}.
     *
     * @see #getProperties(String)
     */
    public static Properties getProperties() {
        if (platformProperties != null) {
            return platformProperties;
        }
        return platformProperties = getProperties(getPlatformName());
    }
    /**
     * Get from resources the default {@link Properties} of the specified platform name.
     *
     * @param name the platform name
     * @return the Properties from resources
     */
    public static Properties getProperties(String name) {
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
     * Appends to properties the ones from Class annotations. If no properties are
     * found on the class, makes a search for the first enclosing class with properties
     * that we can use, appends those properties, and returns the enclosing Class used.
     *
     * @param properties the Properties to update
     * @param cls the Class from which to take new Properties
     * @return the actual enclosing Class on which we found the Properties
     */
    public static Class appendProperties(Properties properties, Class cls) {
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
        String platformName = properties.getProperty("platform.name");
        com.googlecode.javacpp.annotation.Properties classProperties =
                c.getAnnotation(com.googlecode.javacpp.annotation.Properties.class);
        Platform[] platforms;
        if (classProperties == null) {
            try {
                Platform platform = c.getAnnotation(Platform.class);
                if (platform == null) {
                    return c;
                } else {
                    platforms = new Platform[] { platform };
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Could not append properties for " + c.getCanonicalName() + ": " + t);
                return c;
            }
        } else {
            platforms = classProperties.value();
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

        String s = properties.getProperty("path.separator");
        appendProperty(properties, "generator.define",   "\u0000", define);
        appendProperty(properties, "generator.include",  "\u0000", include);
        appendProperty(properties, "generator.cinclude", "\u0000", cinclude);
        appendProperty(properties, "compiler.includepath",      s, includepath);
        if (options.length > 0) {
            String defaultOptions = properties.getProperty("compiler.options");
            properties.setProperty("compiler.options", "");
            for (int i = 0; i < options.length; i++) {
                String o = defaultOptions;
                if (options[i].length() > 0 && ((o = properties.getProperty("compiler.options." + options[i])) == null)) {
                    logger.log(Level.WARNING, "Could not find a property name \"compiler.options." + options[i] + "\".");
                } else {
                    appendProperty(properties, "compiler.options", " ", o);
                }
            }
        }
        appendProperty(properties, "compiler.linkpath",         s, linkpath);
        appendProperty(properties, "compiler.link",             s, link);
        appendProperty(properties, "compiler.framework",        s, framework);
        appendProperty(properties, "loader.preloadpath",        s, linkpath);
        appendProperty(properties, "loader.preloadpath",        s, preloadpath);
        appendProperty(properties, "loader.preload",            s, link);
        appendProperty(properties, "loader.preload",            s, preload);
        properties.setProperty("loader.library", library);
        return c;
    }

    /**
     * Appends new property values to a given key name of the properties.
     *
     * @param properties the Properties to update
     * @param name the key name of the property to update
     * @param separator the separator character to use for splitting and merging values
     * @param values the property values to append, either an array or merged with separators
     */
    public static void appendProperty(Properties properties, String name,
            String separator, String ... values) {
        if (values == null || values.length == 0) {
            return;
        } else if (values.length == 1 && values[0] != null) {
            values = values[0].split(separator);
        }
        String oldValue = properties.getProperty(name, "");
        String[] oldValues = oldValue.split(separator);
        String value = "";
    next:
        for (String v : values) {
            if (v == null || v.length() == 0) {
                continue;
            }
            for (String ov : oldValues) {
                if (v.equals(ov)) {
                    continue next;
                }
            }
            if (value.length() > 0 && !value.endsWith(separator)) {
                value += separator;
            }
            value += v;
        }
        if (value.length() > 0 && oldValue.length() > 0) {
            value += separator;
        }
        properties.setProperty(name, value + oldValue);
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
     * @return the full path of the file, or the library name,  loaded
     *         (but {@code if (!loadLibraries || cls == null) { return null; }})
     * @throws NoClassDefFoundError on Class initialization failure
     * @throws UnsatisfiedLinkError on native library loading failure
     */
    public static String load(Class cls) {
        if (!loadLibraries || cls == null) {
            return null;
        }

        // Find the top enclosing class, to match the library filename
        Properties p = (Properties)getProperties().clone();
        cls = appendProperties(p, cls);

        // Force initialization of the class in case it needs it
        try {
            cls = Class.forName(cls.getName(), true, cls.getClassLoader());
        } catch (ClassNotFoundException ex) {
            Error e = new NoClassDefFoundError(ex.toString());
            e.initCause(ex);
            throw e;
        }

        // Preload native libraries desired by our class
        String pathSeparator = p.getProperty("path.separator");
        String platformRoot  = p.getProperty("platform.root");
        if (platformRoot != null && !platformRoot.endsWith(File.separator)) {
            platformRoot += File.separator;
        }
        String preloadPath      = p.getProperty("loader.preloadpath");
        String preloadLibraries = p.getProperty("loader.preload");
        UnsatisfiedLinkError preloadError = null;
        if (preloadLibraries != null) {
            String[] preloadPaths = preloadPath == null ? null : preloadPath.split(pathSeparator);
            if (preloadPaths != null && platformRoot != null) {
                for (int i = 0; i < preloadPaths.length; i++) {
                    if (!new File(preloadPaths[i]).isAbsolute()) {
                        preloadPaths[i] = platformRoot + preloadPaths[i];
                    }
                }
            }
            String[] libnames = preloadLibraries.split(pathSeparator);
            for (int i = 0; i < libnames.length; i++) {
                try {
                    loadLibrary(cls, preloadPaths, libnames[i]);
                } catch (UnsatisfiedLinkError e) {
                    preloadError = e;
                }
            }
        }

        try {
            return loadLibrary(cls, null, p.getProperty("loader.library"));
        } catch (UnsatisfiedLinkError e) {
            if (preloadError != null) {
                e.initCause(preloadError);
            }
            throw e;
        }
    }

    /**
     * Tries to load library from the caller's {@link Class} resources or {@link System#loadLibrary(String)}.
     * @see #loadLibrary(Class, String[], String)
     */
    public static String loadLibrary(String libnameversion) {
        Class cls = getCallerClass(2);
        return loadLibrary(cls, null, libnameversion);
    }
    /**
     * Tries to load library from the caller's {@link Class} resources, the paths, or {@link System#loadLibrary(String)}.
     * @see #loadLibrary(Class, String[], String)
     */
    public static String loadLibrary(String[] paths, String libnameversion) {
        Class cls = getCallerClass(2);
        return loadLibrary(cls, paths, libnameversion);
    }
    /**
     * First tries to extract and load the library from the {@link Class} resources,
     * but if fails, continues to try loading from the paths (if not {@code null}),
     * and finally {@link System#loadLibrary(String)}.
     *
     * @param cls the Class whose package name and {@link ClassLoader} are used to extract from resources
     * @param paths the directories to scan for if we fail to extract the library from resources
     * @param libnameversion the name of the library + "@" + optional version tag
     * @return the full path of the file, or the library name,  loaded
     *         (but {@code if (!loadLibraries || cls == null) { return null; }})
     * @throws UnsatisfiedLinkError on failure
     */
    public static String loadLibrary(Class cls, String[] paths, String libnameversion) {
        if (!loadLibraries || cls == null) {
            return null;
        }
        String className = cls.getName();
        int packageIndex = className.lastIndexOf('.');
        String packageName = packageIndex != -1 ? className.substring(0, packageIndex + 1) : "";
        String hashkey = packageName + libnameversion;

        // If we do not already have the native library file ...
        String filename = loadedLibraries.get(hashkey);
        if (filename != null) {
            return filename;
        }

        String[] s = libnameversion.split("@");
        String libname = s[0];
        String version = s.length > 1 ? s[s.length-1] : "";

        Properties p = getProperties();
        String subdir = p.getProperty("platform.name") + '/';
        String prefix = p.getProperty("library.prefix") + libname;
        String suffix = p.getProperty("library.suffix");
        URL resourceURL = cls.getResource(subdir + prefix + suffix + version); // Linux style
        if (resourceURL == null) {
            resourceURL = cls.getResource(subdir + prefix + version + suffix); // Mac OS X style
        }
        if (resourceURL == null) {
            resourceURL = cls.getResource(subdir + prefix + suffix); // without version
        }

        File tempFile = null;
        UnsatisfiedLinkError loadError = null;
        try {
            if (resourceURL != null) {
                // ... then extract it from our resources ...
                tempFile = extractResource(resourceURL, getTempDir(), null, null);
                // ... and load it!
                String tempFilename = tempFile.getAbsolutePath();
                loadedLibraries.put(hashkey, tempFilename);
                System.load(tempFilename);
                return tempFilename;
            } else {
                // throw new UnsatisfiedLinkError("Could not find library resource: " + resourceName);
                // If not found in resources, search the paths instead ...
                for (int j = 0; paths != null && j < paths.length; j++) {
                    File file = new File(paths[j], prefix + suffix + version); // Linux style
                    if (!file.exists()) {
                         file = new File(paths[j], prefix + version + suffix); // Mac OS X style
                    }
                    if (!file.exists()) {
                         file = new File(paths[j], prefix + suffix); // without version
                    }
                    if (file.exists()) {
                        filename = file.getPath();
                        try {
                            loadedLibraries.put(hashkey, filename);
                            System.load(filename);
                            return filename;
                        } catch (UnsatisfiedLinkError e) {
                            loadError = e;
                            loadedLibraries.remove(hashkey);
                        }
                    }
                }
                // ... or as last resort, try to load it via the system.
                loadedLibraries.put(hashkey, libname);
                System.loadLibrary(libname);
                return libname;
            }
        } catch (UnsatisfiedLinkError e) {
            loadedLibraries.remove(hashkey);
            if (loadError != null) {
                throw loadError;
            } else {
                throw e;
            }
        } catch (IOException ex) {
            loadedLibraries.remove(hashkey);
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
