/*
 * Copyright (C) 2011,2012 Samuel Audet
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.WeakHashMap;

/**
 *
 * @author Samuel Audet
 */
public class Loader {

    private static String platformName = null;
    private static Properties platformProperties = null;

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
    public static void setPlatformName(String platformName) {
        Loader.platformName = platformName;
        Loader.platformProperties = null;
    }

    public static Properties getProperties() {
        if (platformProperties != null) {
            return platformProperties;
        }
        return platformProperties = getProperties(getPlatformName());
    }
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

    public static void appendProperties(Properties properties, Class cls) {
        String platformName = properties.getProperty("platform.name");
        Class<?> c = cls;
        com.googlecode.javacpp.annotation.Properties classProperties =
                c.getAnnotation(com.googlecode.javacpp.annotation.Properties.class);
        Platform[] platforms;
        if (classProperties == null) {
            try {
                Platform platform = c.getAnnotation(Platform.class);
                if (platform == null) {
                    return;
                } else {
                    platforms = new Platform[] { platform };
                }
            } catch (Throwable t) {
                System.err.println("Could not append properties for " + c.getCanonicalName() + ": " + t);
                return;
            }
        } else {
            platforms = classProperties.value();
        }

        String[] define = {}, include = {}, cinclude = {}, includepath = {}, options = {},
                 linkpath = {}, link = {}, framework = {}, preloadpath = {}, preload = {};
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
                if (options[i].length() > 0) {
                    o = properties.getProperty("compiler.options." + options[i]);
                }
                appendProperty(properties, "compiler.options", " ", o);
            }
        }
        appendProperty(properties, "compiler.linkpath",         s, linkpath);
        appendProperty(properties, "compiler.link",             s, link);
        appendProperty(properties, "compiler.framework",        s, framework);
        appendProperty(properties, "loader.preloadpath",        s, linkpath);
        appendProperty(properties, "loader.preloadpath",        s, preloadpath);
        appendProperty(properties, "loader.preload",            s, link);
        appendProperty(properties, "loader.preload",            s, preload);
    }

    public static void appendProperty(Properties properties, String name,
            String separator, String ... values) {
        if (values == null || values.length == 0) {
            return;
        } else if (values.length == 1) {
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

    public static String getLibraryName(Class cls) {
        return "jni" + cls.getSimpleName();
    }

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

    public static File extractResource(String name, File directory,
            String prefix, String suffix) throws IOException {
        Class cls = getCallerClass(2);
        return extractResource(cls, name, directory, prefix, suffix);
    }
    public static File extractResource(Class cls, String name, File directory,
            String prefix, String suffix) throws IOException {
        return extractResource(cls.getResource(name), directory, prefix, suffix);
    }
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
            byte[] data = new byte[is.available()];
            int n;
            while ((n = is.read(data)) > 0) {
                os.write(data, 0, n);
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


    static File tempDir = null;
    static boolean loadLibraries = true;
    static Map<String,String> loadedLibraries = Collections.synchronizedMap(new HashMap<String,String>());

    static File getTempDir() {
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

    public static String load() {
        Class cls = getCallerClass(2);
        return load(cls);
    }
    public static String load(Class cls) {
        if (!loadLibraries || cls == null) {
            return null;
        }

        // Find the top enclosing class, to match the library filename
//        while (cls.getDeclaringClass() != null) {
//            cls = cls.getDeclaringClass();
//        }
        String className = cls.getName();
        int topIndex = className.indexOf('$');
        if (topIndex > 0) {
            className = className.substring(0, topIndex);
        }

        // Force initialization of the class in case it needs it
        try {
            cls = Class.forName(className, true, cls.getClassLoader());
        } catch (ClassNotFoundException ex) {
            Error e = new NoClassDefFoundError(ex.toString());
            e.initCause(ex);
            throw e;
        }

        // Preload native libraries desired by our class
        Properties p = (Properties)getProperties().clone();
        appendProperties(p, cls);
        String pathSeparator = p.getProperty("path.separator");
        String platformRoot  = p.getProperty("platform.root");
        if (platformRoot != null && !platformRoot.endsWith(File.separator)) {
            platformRoot += File.separator;
        }
        String preloadPath      = p.getProperty("loader.preloadpath");
        String preloadLibraries = p.getProperty("loader.preload");
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
                } catch (UnsatisfiedLinkError e) { }
            }
        }

        return loadLibrary(cls, null, getLibraryName(cls));
    }

    public static String loadLibrary(String libnameversion) {
        Class cls = getCallerClass(2);
        return loadLibrary(cls, null, libnameversion);
    }
    public static String loadLibrary(String[] paths, String libnameversion) {
        Class cls = getCallerClass(2);
        return loadLibrary(cls, paths, libnameversion);
    }
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
            if (tempFile != null) {
                tempFile.delete();
            }
            throw e;
        } catch (IOException ex) {
            loadedLibraries.remove(hashkey);
            if (tempFile != null) {
                tempFile.delete();
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


    static WeakHashMap<Class<? extends Pointer>,HashMap<String,Integer>> memberOffsets =
            new WeakHashMap<Class<? extends Pointer>,HashMap<String,Integer>>();

    static void putMemberOffset(String typeName, String member, int offset) throws ClassNotFoundException {
        Class<?> c = Class.forName(typeName.replace('/', '.'), false, Loader.class.getClassLoader());
        putMemberOffset(c.asSubclass(Pointer.class), member, offset);
    }
    static synchronized void putMemberOffset(Class<? extends Pointer> type, String member, int offset) {
        HashMap<String,Integer> offsets = memberOffsets.get(type);
        if (offsets == null) {
            memberOffsets.put(type, offsets = new HashMap<String,Integer>());
        }
        offsets.put(member, offset);
    }

    // Do we really need to synchronize those?
    public static int offsetof(Class<? extends Pointer> type, String member) {
        return memberOffsets.get(type).get(member);
    }

    public static int sizeof(Class<? extends Pointer> type) {
        return memberOffsets.get(type).get("sizeof");
    }
}
