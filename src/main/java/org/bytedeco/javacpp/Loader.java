/*
 * Copyright (C) 2011-2015 Samuel Audet
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.tools.Builder;
import org.bytedeco.javacpp.tools.Coordinates;
import org.bytedeco.javacpp.tools.Logger;
import org.bytedeco.javacpp.tools.Repository;

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

    private static final Logger logger = Logger.create(Loader.class);

    /** Value created out of "java.vm.name", "os.name", and "os.arch" system properties.
     *  Returned by {@link #getPlatform()} as default. */
    private static final String platform;
    /** Default platform properties loaded and returned by {@link #loadProperties()}. */
    private static Properties platformProperties = null;

    static {
        String jvmName = System.getProperty("java.vm.name", "").toLowerCase();
        String osName  = System.getProperty("os.name", "").toLowerCase();
        String osArch  = System.getProperty("os.arch", "").toLowerCase();
        if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
            osName = "android";
        } else if (jvmName.startsWith("robovm") && osName.startsWith("darwin")) {
            osName = "ios";
            osArch = "arm";
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
        } else if (osArch.startsWith("aarch64") || osArch.startsWith("armv8") || osArch.startsWith("arm64")) {
            osArch = "arm64";
        } else if (osArch.startsWith("arm")) {
            osArch = "arm";
        }
        platform = osName + "-" + osArch;
    }

    /**
     * Returns either the value of the "org.bytedeco.javacpp.platform"
     * system property, or {@link #platform} when the former is not set.
     *
     * @return {@code System.getProperty("org.bytedeco.javacpp.platform", platform)}
     * @see #platform
     */
    public static String getPlatform() {
        return System.getProperty("org.bytedeco.javacpp.platform", platform);
    }

    /**
     * Loads the {@link Properties} associated with the default {@link #getPlatform()}.
     *
     * @see #loadProperties(String)
     */
    public static Properties loadProperties() {
        String name = getPlatform();
        if (platformProperties != null && name.equals(platformProperties.getProperty("platform"))) {
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
        p.put("platform", name);
        p.put("platform.path.separator", File.pathSeparator);
        String s = System.mapLibraryName("/");
        int i = s.indexOf('/');
        p.put("platform.library.prefix", s.substring(0, i));
        p.put("platform.library.suffix", s.substring(i + 1));
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
                // give up and return defaults
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
     * @see org.bytedeco.javacpp.annotation.Platform
     * @see org.bytedeco.javacpp.annotation.Properties
     */
    public static Class getEnclosingClass(Class cls) {
        Class<?> c = cls;
        // Find first enclosing declaring class with some properties to use
        while (c.getDeclaringClass() != null) {
            if (c.isAnnotationPresent(org.bytedeco.javacpp.annotation.Properties.class)) {
                break;
            }
            if (c.isAnnotationPresent(Platform.class)) {
                Platform p = c.getAnnotation(Platform.class);
                if (p.pragma().length > 0 || p.define().length > 0 || p.include().length > 0 || p.cinclude().length > 0 ||
                        p.includepath().length > 0 || p.compiler().length > 0 || p.linkpath().length > 0 ||
                        p.link().length > 0 || p.frameworkpath().length > 0 || p.framework().length > 0 ||
                        p.preloadpath().length > 0 || p.preload().length > 0 || p.library().length() > 0) {
                    break;
                }
            }
            c = c.getDeclaringClass();
        }
        return c;
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
     * of interest needs to be specified as the value of the "platform" key in the
     * properties argument. It is also possible to indicate whether to load all the classes
     * specified in the {@link org.bytedeco.javacpp.annotation.Properties#inherit()}
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
        Class[] classContext = null;
        try {
            new SecurityManager() {
                @Override public Class[] getClassContext() {
                    return super.getClassContext();
                }
            }.getClassContext();
        } catch (NoSuchMethodError e) { }
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

    /** Flag set by the {@link Builder} to tell us not to try to load anything. */
    static boolean loadLibraries = System.getProperty("org.bytedeco.javacpp.loadlibraries", "true").equals("true");
    /** Contains all the native libraries that we have loaded to avoid reloading them. */
    static Map<String,String> loadedLibraries = Collections.synchronizedMap(new HashMap<String,String>());

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
        ClassProperties p = loadProperties(cls, loadProperties(), true);

        // Get library coordinates if available
        Coordinates coordinates;
        try {
            Method method = cls.getMethod("coordinates");
            coordinates = (Coordinates) method.invoke(null);
        } catch(Exception _) {
            coordinates = null;
        }

        // Force initialization of all the target classes in case they need it
        List<String> targets = p.get("target");
        if (targets.isEmpty()) {
            if (p.getInheritedClasses() != null) {
                for (Class c : p.getInheritedClasses()) {
                    targets.add(c.getName());
                }
            }
            targets.add(cls.getName());
        }
        for (String s : targets) {
            try {
                Class.forName(s, true, cls.getClassLoader());
            } catch (ClassNotFoundException ex) {
                Error e = new NoClassDefFoundError(ex.toString());
                e.initCause(ex);
                throw e;
            }
        }

        // Preload native libraries desired by our class
        List<String> preloads = new ArrayList<String>();
        preloads.addAll(p.get("platform.preload"));
        preloads.addAll(p.get("platform.link"));
        UnsatisfiedLinkError preloadError = null;
        for (String preload : preloads) {
            try {
                URL[] urls = findLibrary(cls, p, preload);
                loadLibrary(urls, preload, coordinates);
            } catch (UnsatisfiedLinkError e) {
                preloadError = e;
            }
        }

        try {
            String library = p.getProperty("platform.library");
            URL[] urls = findLibrary(cls, p, library);
            return loadLibrary(urls, library, coordinates);
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
     * "platform.preloadpath" and "platform.linkpath" properties.
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

        String subdir = properties.getProperty("platform") + '/';
        String prefix = properties.getProperty("platform.library.prefix", "") + libname;
        String suffix = properties.getProperty("platform.library.suffix", "");
        String[] styles = {
            prefix + suffix + version, // Linux style
            prefix + version + suffix, // Mac OS X style
            prefix + suffix            // without version
        };

        String[] suffixes = properties.get("platform.library.suffix").toArray(new String[0]);
        if (suffixes.length > 1) {
            styles = new String[3 * suffixes.length];
            for (int i = 0; i < suffixes.length; i++) {
                styles[3 * i    ] = prefix + suffixes[i] + version; // Linux style
                styles[3 * i + 1] = prefix + version + suffixes[i]; // Mac OS X style
                styles[3 * i + 2] = prefix + suffixes[i];           // without version
            }
        }

        int k = 0;
        List<String> paths = new ArrayList<String>();
        paths.addAll(properties.get("platform.preloadpath"));
        paths.addAll(properties.get("platform.linkpath"));
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
        URL[] newurls = new URL[k];
        System.arraycopy(urls, 0, newurls, 0, k);
        return newurls;
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
    public static String loadLibrary(URL[] urls, String libnameversion, Coordinates coordinates) {
        if (!loadLibraries) {
            return null;
        }

        // If we do not already have the native library file ...
        String filename = loadedLibraries.get(libnameversion);
        if (filename != null) {
            return filename;
        }

        UnsatisfiedLinkError loadError = null;
        try {
            for (URL url : urls) {
                File file;
                try {
                    // ... if the URL is not already a file ...
                    file = new File(url.toURI());
                } catch (Exception e) {
                    // ... then extract if not already in repo ...
                    if (logger.isDebugEnabled()) {
                        logger.debug("Extracting " + url);
                    }
                    JarURLConnection c = (JarURLConnection) url.openConnection();
                    Path jar = Paths.get(c.getJarFileURL().getFile());

                    // For legacy jars without coordinates, use a hash to get consistent
                    // and unique file name
                    if (coordinates == null)
                        coordinates = new Coordinates(md5(jar), "id", "version");

                    Path repo = Repository.getPath(new Coordinates(coordinates, getPlatform(), jar));
                    file = new File(repo.toFile(), new File(url.getPath()).getName());
                }
                if (file != null && file.exists()) {
                    filename = file.getAbsolutePath();
                    try {
                        // ... and load it!
                        if (logger.isDebugEnabled()) {
                            logger.debug("Loading " + filename);
                        }
                        loadedLibraries.put(libnameversion, filename);
                        System.load(filename);
                        return filename;
                    } catch (UnsatisfiedLinkError e) {
                        loadError = e;
                        loadedLibraries.remove(libnameversion);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failed to load " + filename + ": " + e);
                        }
                    }
                }
            }
            // ... or as last resort, try to load it via the system.
            String libname = libnameversion.split("@")[0];
            if (logger.isDebugEnabled()) {
                logger.debug("Loading library " + libname);
            }
            loadedLibraries.put(libnameversion, libname);
            System.loadLibrary(libname);
            return libname;
        } catch (UnsatisfiedLinkError e) {
            loadedLibraries.remove(libnameversion);
            if (loadError != null && e.getCause() == null) {
                e.initCause(loadError);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to load for " + libnameversion + ": " + e);
            }
            throw e;
        } catch (IOException ex) {
            loadedLibraries.remove(libnameversion);
            if (loadError != null && ex.getCause() == null) {
                ex.initCause(loadError);
            }
            Error e = new UnsatisfiedLinkError(ex.toString());
            e.initCause(ex);
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to extract for " + libnameversion + ": " + e);
            }
            throw e;
        }
    }

    private static String md5(Path jar) throws IOException {
        byte[] hash;
        try (InputStream is = new FileInputStream(jar.toFile())) {
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
            byte[] buf = new byte[4096];
            int pos;
            while ((pos = is.read(buf)) > 0)
                md5.update(buf, 0, pos);
            hash = md5.digest();
        }
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash)
            hex.append(String.format("%02x", b & 0xff));
        return hex.toString();
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
