/*
 * Copyright (C) 2011-2020 Samuel Audet
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Raw;
import org.bytedeco.javacpp.tools.Builder;
import org.bytedeco.javacpp.tools.Logger;

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
@org.bytedeco.javacpp.annotation.Properties(inherit = org.bytedeco.javacpp.presets.javacpp.class)
public class Loader {
    private static final Logger logger = Logger.create(Loader.class);

    /** Value created out of "java.vm.name", "os.name", and "os.arch" system properties.
     *  Returned by {@link #getPlatform()} as default and initialized with {@link Detector#getPlatform()}. */
    private static final String PLATFORM = Detector.getPlatform();
    /** Default platform properties loaded and returned by {@link #loadProperties()}. */
    private static Properties platformProperties = null;
    /** The stack of classes currently being loaded to support more than one class loader. */
    private static final ThreadLocal<Deque<Class<?>>> classStack = new ThreadLocal<Deque<Class<?>>>() {
        @Override protected Deque<Class<?>> initialValue() {
            return new ArrayDeque<Class<?>>();
        }
    };

    public static class Detector {
        public static String getPlatform() {
            String jvmName = System.getProperty("java.vm.name", "").toLowerCase();
            String osName  = System.getProperty("os.name", "").toLowerCase();
            String osArch  = System.getProperty("os.arch", "").toLowerCase();
            String abiType = System.getProperty("sun.arch.abi", "").toLowerCase();
            String libPath = System.getProperty("sun.boot.library.path", "").toLowerCase();
            if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
                osName = "android";
            } else if (jvmName.startsWith("robovm") && osName.startsWith("darwin")) {
                osName = "ios";
                osArch = "arm";
            } else if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
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
            } else if ((osArch.startsWith("arm")) && ((abiType.equals("gnueabihf")) || (libPath.contains("openjdk-armhf")))) {
                osArch = "armhf";
            } else if (osArch.startsWith("arm")) {
                osArch = "arm";
            }
            return osName + "-" + osArch;
        }
    }

    /**
     * Returns either the value of the "org.bytedeco.javacpp.platform"
     * system property, or {@link #PLATFORM} when the former is not set.
     *
     * @return {@code System.getProperty("org.bytedeco.javacpp.platform", platform)}
     * @see #PLATFORM
     */
    public static String getPlatform() {
        return System.getProperty("org.bytedeco.javacpp.platform", PLATFORM);
    }

    /**
     * Loads the {@link Properties} associated with the default {@link #getPlatform()}.
     *
     * @return {@code loadProperties(getPlatform(), null)}
     * @see #loadProperties(String, String)
     */
    public static Properties loadProperties() {
        String name = getPlatform();
        if (platformProperties != null && name.equals(platformProperties.getProperty("platform"))) {
            return platformProperties;
        }
        return platformProperties = loadProperties(name, null);
    }
    /**
     * Loads from resources the default {@link Properties} of the specified platform name.
     * The resource must be at {@code "org/bytedeco/javacpp/properties/" + name + ".properties"}.
     *
     * @param name the platform name
     * @param defaults the fallback platform name (null == "generic")
     * @return the Properties from resources
     */
    public static Properties loadProperties(String name, String defaults) {
        if (defaults == null) {
            defaults = "generic";
        }
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
            name = "properties/" + defaults + ".properties";
            InputStream is2 = Loader.class.getResourceAsStream(name);
            try {
                try {
                    p.load(new InputStreamReader(is2));
                } catch (NoSuchMethodError e2) {
                    p.load(is2);
                }
            } catch (Exception e2) {
                // give up and return defaults
            } finally {
                try {
                    if (is2 != null) {
                        is2.close();
                    }
                } catch (IOException ex) {
                    logger.error("Unable to close resource : " + ex.getMessage());
                }
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close resource : " + ex.getMessage());
            }
        }
        for (Map.Entry e : System.getProperties().entrySet()) {
            if (e.getKey() instanceof String && e.getValue() instanceof String) {
                String key = (String)e.getKey();
                String value = (String)e.getValue();
                if (key != null && value != null && key.startsWith("org.bytedeco.javacpp.platform.")) {
                    p.put(key.substring(key.indexOf("platform.")), value);
                }
            }
        }
        return p;
    }

    /** Returns {@code checkVersion(groupId, artifactId, "-", true, getCallerClass(2))}. */
    public static boolean checkVersion(String groupId, String artifactId) {
        return checkVersion(groupId, artifactId, "-", true, getCallerClass(2));
    }

    /** Returns {@code getVersion(groupId, artifactId, cls).split(separator)[n].equals(getVersion().split(separator)[0])}
     *  where {@code n = versions.length - (versions[versions.length - 1].equals("SNAPSHOT") ? 2 : 1)} or false on error.
     *  Also calls {@link Logger#warn(String)} on error when {@code logWarnings && isLoadLibraries()}. */
    public static boolean checkVersion(String groupId, String artifactId, String separator, boolean logWarnings, Class cls) {
        try {
            String javacppVersion = getVersion();
            String version = getVersion(groupId, artifactId, cls);
            if (version == null) {
                if (logWarnings && isLoadLibraries()) {
                    logger.warn("Version of " + groupId + ":" + artifactId + " could not be found.");
                }
                return false;
            }
            String[] javacppVersions = javacppVersion.split(separator);
            String[] versions = version.split(separator);
            int n = versions.length - (versions[versions.length - 1].equals("SNAPSHOT") ? 2 : 1);
            boolean matches = versions[n].equals(javacppVersions[0]);
            if (!matches && logWarnings && isLoadLibraries()) {
                logger.warn("Versions of org.bytedeco:javacpp:" + javacppVersion + " and " + groupId + ":" + artifactId + ":" + version + " do not match.");
            }
            return matches;
        } catch (Exception ex) {
            if (logWarnings && isLoadLibraries()) {
                logger.warn("Unable to load properties : " + ex.getMessage());
            }
        }
        return false;
    }

    /** Returns {@code getVersion("org.bytedeco", "javacpp")}. */
    public static String getVersion() throws IOException {
        return getVersion("org.bytedeco", "javacpp");
    }

    /** Returns {@code getVersion(groupId, artifactId, getCallerClass(2))}. */
    public static String getVersion(String groupId, String artifactId) throws IOException {
        return getVersion(groupId, artifactId, getCallerClass(2));
    }

    /** Returns version property from {@code cls.getResource("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties")}. */
    public static String getVersion(String groupId, String artifactId, Class cls) throws IOException {
        Properties p = new Properties();
        // Need to call getClassLoader() for non-encapsulated resources under JPMS
        InputStream is = cls.getClassLoader().getResourceAsStream("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
        if (is == null) {
            return null;
        }
        try {
            try {
                p.load(new InputStreamReader(is));
            } catch (NoSuchMethodError e) {
                p.load(is);
            }
            return p.getProperty("version");
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close resource : " + ex.getMessage());
            }
        }
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
        while (c.getEnclosingClass() != null) {
            if (c.isAnnotationPresent(org.bytedeco.javacpp.annotation.Properties.class)) {
                break;
            }
            if (c.isAnnotationPresent(Platform.class)) {
                Platform p = c.getAnnotation(Platform.class);
                if (p.pragma().length > 0 || p.define().length > 0 || p.exclude().length > 0 || p.include().length > 0 || p.cinclude().length > 0
                    || p.includepath().length > 0 || p.includeresource().length > 0 || p.compiler().length > 0
                    || p.linkpath().length > 0 || p.linkresource().length > 0 || p.link().length > 0 || p.frameworkpath().length > 0
                    || p.framework().length > 0 || p.preloadresource().length > 0 || p.preloadpath().length > 0 || p.preload().length > 0
                    || p.resourcepath().length > 0 || p.resource().length > 0 || p.library().length() > 0) {
                    break;
                }
            }
            c = c.getEnclosingClass();
        }
        return c;
    }


    /**
     * For all the classes, loads all properties from each Class annotations for the given platform.
     * @see #loadProperties(Class, java.util.Properties, boolean)
     */
    public static ClassProperties loadProperties(Class[] cls, Properties properties, boolean inherit) {
        ClassProperties cp = new ClassProperties(properties);
        if (cls != null) {
            for (Class c : cls) {
                cp.load(c, inherit);
            }
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
        if (cls != null) {
            cp.load(cls, inherit);
        }
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
            classContext = new SecurityManager() {
                @Override public Class[] getClassContext() {
                    return super.getClassContext();
                }
            }.getClassContext();
        } catch (NoSuchMethodError | SecurityException e) {
            logger.warn("Could not create an instance of SecurityManager: " + e.getMessage());
        }
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
            } catch (ClassNotFoundException e) {
                logger.error("No definition for the class found : " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Extracts a resource using the {@link ClassLoader} of the caller class,
     * and returns the cached {@link File}.
     *
     * @param name the name of the resource passed to {@link Class#getResource(String)}
     * @see #cacheResource(Class, String)
     */
    public static File cacheResource(String name) throws IOException {
        Class cls = getCallerClass(2);
        return cacheResource(cls, name);
    }
    /**
     * Extracts a resource using the {@link ClassLoader} of the specified {@link Class},
     * and returns the cached {@link File}.
     *
     * @param cls the Class from which to load resources
     * @param name the name of the resource passed to {@link Class#getResource(String)}
     * @see #cacheResource(URL)
     */
    public static File cacheResource(Class cls, String name) throws IOException {
        URL u = findResource(cls, name);
        return u != null ? cacheResource(u) : null;
    }

    /**
     * Extracts resources using the {@link ClassLoader} of the caller class,
     * and returns the cached {@link File} objects.
     *
     * @param name of the resources passed to {@link #findResources(Class, String)}
     * @see #cacheResources(Class, String)
     */
    public static File[] cacheResources(String name) throws IOException {
        Class cls = getCallerClass(2);
        return cacheResources(cls, name);
    }
    /**
     * Extracts resources using the {@link ClassLoader} of the specified {@link Class},
     * and returns the cached {@link File} objects.
     *
     * @param cls the Class from which to load resources
     * @param name of the resources passed to {@link #findResources(Class, String)}
     * @see #cacheResource(URL)
     */
    public static File[] cacheResources(Class cls, String name) throws IOException {
        URL[] urls = findResources(cls, name);
        File[] files = new File[urls.length];
        for (int i = 0; i < urls.length; i++) {
            files[i] = cacheResource(urls[i]);
        }
        return files;
    }

    /** Returns {@code cacheResource(resourceUrl, null)} */
    public static File cacheResource(URL resourceURL) throws IOException {
        return cacheResource(resourceURL, null);
    }
    /**
     * Extracts a resource, if the size or last modified timestamp differs from what is in cache,
     * and returns the cached {@link File}. If target is not null, creates instead a symbolic link
     * where the resource would have been extracted.
     *
     * @param resourceURL the URL of the resource to extract and cache
     * @param target of the symbolic link to create (must be null to have the resource actually extracted)
     * @return the File object representing the extracted file from the cache
     * @throws IOException if fails to extract resource properly
     * @see #extractResource(URL, File, String, String)
     * @see #cacheDir
     */
    public static File cacheResource(URL resourceURL, String target) throws IOException {
        // Find appropriate subdirectory in cache for the resource ...
        File urlFile;
        try {
            urlFile = new File(new URI(resourceURL.toString().split("#")[0]));
        } catch (IllegalArgumentException | URISyntaxException e) {
            urlFile = new File(resourceURL.getPath());
        }
        String name = urlFile.getName();
        boolean reference = false;
        long size, timestamp;
        File cacheDir = getCacheDir();
        File cacheSubdir = cacheDir.getCanonicalFile();
        String s = System.getProperty("org.bytedeco.javacpp.cachedir.nosubdir", "false").toLowerCase();
        boolean noSubdir = s.equals("true") || s.equals("t") || s.equals("");
        URLConnection urlConnection = resourceURL.openConnection();
        if (urlConnection instanceof JarURLConnection) {
            JarFile jarFile = ((JarURLConnection)urlConnection).getJarFile();
            JarEntry jarEntry = ((JarURLConnection)urlConnection).getJarEntry();
            File jarFileFile = new File(jarFile.getName());
            File jarEntryFile = new File(jarEntry.getName());
            size = jarEntry.getSize();
            timestamp = jarEntry.getTime();
            if (!noSubdir) {
                String subdirName = jarFileFile.getName();
                String parentName = jarEntryFile.getParent();
                if (parentName != null) {
                    subdirName = subdirName + File.separator + parentName;
                }
                cacheSubdir = new File(cacheSubdir, subdirName);
            }
        } else if (urlConnection instanceof HttpURLConnection) {
            size = urlConnection.getContentLength();
            timestamp = urlConnection.getLastModified();
            if (!noSubdir) {
                String path = resourceURL.getHost() + resourceURL.getPath();
                cacheSubdir = new File(cacheSubdir, path.substring(0, path.lastIndexOf('/') + 1));
            }
        } else if (resourceURL.getProtocol().equals("jrt")) {
            String p = resourceURL.getPath();
            try {
                // urlConnection.getContentLength() would work on jrt URL, but not getLastModified()
                Path path = Paths.get(new URI("jrt", p, null)); // Remove fragment
                try {
                  size = Files.size(path);
                } catch (java.nio.file.NoSuchFileException e) {
                  // Work around bug JDK-8216553
                  path = Paths.get(new URI("jrt", "/modules" + p, null));
                  size = Files.size(path);
                }
                timestamp = Files.getLastModifiedTime(path).toMillis();
            } catch (URISyntaxException e) { // Should not happen
                size = 0;
                timestamp = 0;
            }
            if (!noSubdir) {
                cacheSubdir = new File(cacheSubdir, urlFile.getParentFile().getName());
            }
        } else {
            size = urlFile.length();
            timestamp = urlFile.lastModified();
            if (!noSubdir) {
                cacheSubdir = new File(cacheSubdir, urlFile.getParentFile().getName());
            }
        }
        if (resourceURL.getRef() != null) {
            // ... get the URL fragment to let users rename library files ...
            String newName = resourceURL.getRef();
            // ... but create a symbolic link only if the name does not change ...
            reference = newName.equals(name);
            name = newName;
        }
        File file = new File(cacheSubdir, name);
        File lockFile = new File(cacheDir, ".lock");
        FileChannel lockChannel = null;
        FileLock lock = null;
        if (target != null && target.length() > 0) {
            // ... create symbolic link to already extracted library or ...
            synchronized (Runtime.getRuntime()) {
            try {
                // file is already canonicalized, so normalized
                Path path = file.toPath(), targetPath = Paths.get(target).normalize();
                if ((!file.exists() || !Files.isSymbolicLink(path) || !Files.readSymbolicLink(path).equals(targetPath))
                        && targetPath.isAbsolute() && !targetPath.equals(path)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Locking " + cacheDir + " to create symbolic link");
                    }
                    lockChannel = new FileOutputStream(lockFile).getChannel();
                    lock = lockChannel.lock();
                    if ((!file.exists() || !Files.isSymbolicLink(path) || !Files.readSymbolicLink(path).equals(targetPath))
                            && targetPath.isAbsolute() && !targetPath.equals(path)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Creating symbolic link " + path + " to " + targetPath);
                        }
                        try {
                            file.getParentFile().mkdirs();
                            Files.createSymbolicLink(path, targetPath);
                        } catch (java.nio.file.FileAlreadyExistsException e) {
                            file.delete();
                            Files.createSymbolicLink(path, targetPath);
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                // ... (probably an unsupported operation on Windows, but DLLs never need links,
                // or other (filesystem?) exception: for example,
                // "sun.nio.fs.UnixException: No such file or directory" on File.toPath()) ...
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to create symbolic link " + file + ": " + e);
                }
                return null;
            } finally {
                if (lock != null) {
                    lock.release();
                }
                if (lockChannel != null) {
                    lockChannel.close();
                }
            }
            }
        } else {
            if (urlFile.exists() && reference) {
                // ... try to create a symbolic link to the existing file, if we can, ...
                synchronized (Runtime.getRuntime()) {
                try {
                    // file is already canonicalized, so normalized
                    Path path = file.toPath(), urlPath = urlFile.toPath().normalize();
                    if ((!file.exists() || !Files.isSymbolicLink(path) || !Files.readSymbolicLink(path).equals(urlPath))
                            && urlPath.isAbsolute() && !urlPath.equals(path)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Locking " + cacheDir + " to create symbolic link");
                        }
                        lockChannel = new FileOutputStream(lockFile).getChannel();
                        lock = lockChannel.lock();
                        if ((!file.exists() || !Files.isSymbolicLink(path) || !Files.readSymbolicLink(path).equals(urlPath))
                                && urlPath.isAbsolute() && !urlPath.equals(path)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Creating symbolic link " + path + " to " + urlPath);
                            }
                            try {
                                file.getParentFile().mkdirs();
                                Files.createSymbolicLink(path, urlPath);
                            } catch (java.nio.file.FileAlreadyExistsException e) {
                                file.delete();
                                Files.createSymbolicLink(path, urlPath);
                            }
                        }
                    }
                    return file;
                } catch (IOException | RuntimeException e) {
                    // ... (let's try to copy the file instead, such as on Windows) ...
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not create symbolic link " + file + ": " + e);
                    }
                } finally {
                    if (lock != null) {
                        lock.release();
                    }
                    if (lockChannel != null) {
                        lockChannel.close();
                    }
                }
                }
            }
            // ... check if it has not already been extracted, and if not ...
            if (!file.exists() || file.length() != size || file.lastModified() != timestamp
                    || !cacheSubdir.equals(file.getCanonicalFile().getParentFile())) {
                // ... add lock to avoid two JVMs access cacheDir simultaneously and ...
                synchronized (Runtime.getRuntime()) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Locking " + cacheDir + " before extracting");
                    }
                    lockChannel = new FileOutputStream(lockFile).getChannel();
                    lock = lockChannel.lock();
                    // ... check if other JVM has extracted it before this JVM get the lock ...
                    if (!file.exists() || file.length() != size || file.lastModified() != timestamp
                            || !cacheSubdir.equals(file.getCanonicalFile().getParentFile())) {
                        // ... extract it from our resources ...
                        if (logger.isDebugEnabled()) {
                            logger.debug("Extracting " + resourceURL);
                        }
                        file.delete();
                        extractResource(resourceURL, file, null, null, true);
                        file.setLastModified(timestamp);
                    }
                } finally {
                    if (lock != null) {
                        lock.release();
                    }
                    if (lockChannel != null) {
                        lockChannel.close();
                    }
                }
                }
            }
        }
        return file;
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
        URL u = findResource(cls, name);
        return u != null ? extractResource(u, directory, prefix, suffix) : null;
    }

    /**
     * Extracts by name resources using the {@link ClassLoader} of the caller.
     *
     * @param name of the resources passed to {@link #findResources(Class, String)}
     * @see #extractResources(Class, String, File, String, String)
     */
    public static File[] extractResources(String name, File directory,
            String prefix, String suffix) throws IOException {
        Class cls = getCallerClass(2);
        return extractResources(cls, name, directory, prefix, suffix);
    }
    /**
     * Extracts by name resources using the {@link ClassLoader} of the specified {@link Class}.
     *
     * @param cls the Class from which to load resources
     * @param name of the resources passed to {@link #findResources(Class, String)}
     * @see #extractResource(URL, File, String, String)
     */
    public static File[] extractResources(Class cls, String name, File directory,
            String prefix, String suffix) throws IOException {
        URL[] urls = findResources(cls, name);
        File[] files = new File[urls.length];
        for (int i = 0; i < urls.length; i++) {
            files[i] = extractResource(urls[i], directory, prefix, suffix);
        }
        return files;
    }

    /** Returns {@code extractResource(resourceURL, directoryOrFile, prefix, suffix, false)}. */
    public static File extractResource(URL resourceURL, File directoryOrFile,
            String prefix, String suffix) throws IOException {
        return extractResource(resourceURL, directoryOrFile, prefix, suffix, false);
    }

    /**
     * Extracts a resource into the specified directory and with the specified
     * prefix and suffix for the filename. If both prefix and suffix are {@code null},
     * the original filename is used, so directoryOrFile must not be {@code null}.
     *
     * @param resourceURL the URL of the resource to extract
     * @param directoryOrFile the output directory or file ({@code null == System.getProperty("java.io.tmpdir")})
     * @param prefix the prefix of the temporary filename to use
     * @param suffix the suffix of the temporary filename to use
     * @param cacheDirectory to extract files from directories only when size or last modified timestamp differs
     * @return the File object representing the extracted file
     * @throws IOException if fails to extract resource properly
     */
    public static File extractResource(URL resourceURL, File directoryOrFile,
            String prefix, String suffix, boolean cacheDirectory) throws IOException {
        URLConnection urlConnection = resourceURL != null ? resourceURL.openConnection() : null;
        if (urlConnection instanceof JarURLConnection) {
            JarFile jarFile = ((JarURLConnection)urlConnection).getJarFile();
            JarEntry jarEntry = ((JarURLConnection)urlConnection).getJarEntry();
            String jarFileName = jarFile.getName();
            String jarEntryName = jarEntry.getName();
            if (!jarEntryName.endsWith("/")) {
                jarEntryName += "/";
            }
            if (jarEntry.isDirectory() || jarFile.getJarEntry(jarEntryName) != null) {
                // Extract all files in directory of JAR file
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    long entrySize = entry.getSize();
                    long entryTimestamp = entry.getTime();
                    if (entryName.startsWith(jarEntryName)) {
                        File file = new File(directoryOrFile, entryName.substring(jarEntryName.length()));
                        if (entry.isDirectory()) {
                            file.mkdirs();
                        } else if (!cacheDirectory || !file.exists() || file.length() != entrySize
                                || file.lastModified() != entryTimestamp || !file.equals(file.getCanonicalFile())) {
                            // ... extract it from our resources ...
                            file.delete();
                            String s = resourceURL.toString();
                            URL u = new URL(s.substring(0, s.indexOf("!/") + 2) + entryName);
                            file = extractResource(u, file, prefix, suffix);
                        }
                        file.setLastModified(entryTimestamp);
                    }
                }
                return directoryOrFile;
            }
        }
        InputStream is = urlConnection != null ? urlConnection.getInputStream() : null;
        OutputStream os = null;
        if (is == null) {
            return null;
        }
        File file = null;
        boolean fileExisted = false;
        try {
            if (prefix == null && suffix == null) {
                if (directoryOrFile == null) {
                    directoryOrFile = new File(System.getProperty("java.io.tmpdir"));
                }
                File directory;
                if (directoryOrFile.isDirectory()) {
                    directory = directoryOrFile;
                    try {
                        file = new File(directoryOrFile, new File(new URI(resourceURL.toString().split("#")[0])).getName());
                    } catch (IllegalArgumentException | URISyntaxException ex) {
                        file = new File(directoryOrFile, new File(resourceURL.getPath()).getName());
                    }
                } else {
                    directory = directoryOrFile.getParentFile();
                    file = directoryOrFile;
                }
                if (directory != null) {
                    directory.mkdirs();
                }
                fileExisted = file.exists();
            } else {
                file = File.createTempFile(prefix, suffix, directoryOrFile);
            }
            file.delete();
            os = new FileOutputStream(file);
            byte[] buffer = new byte[64 * 1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            if (file != null && !fileExisted) {
                file.delete();
            }
            throw e;
        } finally {
            is.close();
            if (os != null) {
                os.close();
            }
        }
        return file;
    }

    /** Returns {@code findResources(cls, name, 1)[0]} or null if none. */
    public static URL findResource(Class cls, String name) throws IOException {
        URL[] url = findResources(cls, name, 1);
        return url.length > 0 ? url[0] : null;
    }

    /** Returns {@code findResources(cls, name, -1)}. */
    public static URL[] findResources(Class cls, String name) throws IOException {
        return findResources(cls, name, -1);
    }

    /**
     * Finds by name resources using the {@link Class} and its {@link ClassLoader}.
     * Names not prefixed with '/' are considered in priority relative to the Class,
     * but parent packages, including the default one, also get searched.
     *
     * @param cls the Class from whose ClassLoader to load resources
     * @param name of the resources passed to {@link Class#getResource(String)} and {@link ClassLoader#getResources(String)}
     * @param maxLength of the array to return, or -1 for no limit
     * @return URLs to the resources
     * @throws IOException
     */
    public static URL[] findResources(Class cls, String name, int maxLength) throws IOException {
        if (maxLength == 0) {
            return new URL[0];
        }
        while (name.contains("//")) {
            name = name.replace("//", "/");
        }

        // Under JPMS, Class.getResource() and ClassLoader.getResources() do not return the same URLs
        URL url = cls.getResource(name);
        if (url != null && maxLength == 1) {
            return new URL[] {url};
        }

        String path = "";
        if (!name.startsWith("/")) {
            String s = cls.getName().replace('.', '/');
            int n = s.lastIndexOf('/');
            if (n >= 0) {
                path = s.substring(0, n + 1);
            }
        } else {
            name = name.substring(1);
        }
        ClassLoader classLoader = cls.getClassLoader();
        if (classLoader == null) {
            // This is the bootstrap class loader, let's try the system class loader instead
            classLoader = ClassLoader.getSystemClassLoader();
        }
        Enumeration<URL> urls = classLoader.getResources(path + name);
        ArrayList<URL> array = new ArrayList<URL>();
        if (url != null) {
            array.add(url);
        }
        while (url == null && !urls.hasMoreElements() && path.length() > 0) {
            int n = path.lastIndexOf('/', path.length() - 2);
            if (n >= 0) {
                path = path.substring(0, n + 1);
            } else {
                path = "";
            }
            urls = classLoader.getResources(path + name);
        }
        while (urls.hasMoreElements() && (maxLength < 0 || array.size() < maxLength)) {
            url = urls.nextElement();
            if (!array.contains(url)) {
                array.add(url);
            }
        }
        return array.toArray(new URL[array.size()]);
    }

    /** User-specified cache directory set and returned by {@link #getCacheDir()}. */
    static File cacheDir = null;
    /** Temporary directory set and returned by {@link #getTempDir()}. */
    static File tempDir = null;
    /** Contains all the URLs of native libraries that we found to avoid searching for them again. */
    static Map<String,URL[]> foundLibraries = new HashMap<String,URL[]>();
    /** Contains all the native libraries that we have loaded to avoid reloading them. */
    static Map<String,String> loadedLibraries = new HashMap<String,String>();

    static boolean pathsFirst = false;
    static {
        String s = System.getProperty("org.bytedeco.javacpp.pathsfirst", "false").toLowerCase();
        s = System.getProperty("org.bytedeco.javacpp.pathsFirst", s).toLowerCase();
        pathsFirst = s.equals("true") || s.equals("t") || s.equals("");
    }

    /** Creates and returns {@code System.getProperty("org.bytedeco.javacpp.cachedir")} or {@code ~/.javacpp/cache/} when not set. */
    public static File getCacheDir() throws IOException {
        if (cacheDir == null) {
            String[] dirNames = {System.getProperty("org.bytedeco.javacpp.cachedir"),
                                 System.getProperty("org.bytedeco.javacpp.cacheDir"),
                                 System.getProperty("user.home") + "/.javacpp/cache/",
                                 System.getProperty("java.io.tmpdir") + "/.javacpp-" + System.getProperty("user.name") + "/cache/"};
            for (String dirName : dirNames) {
                if (dirName != null) {
                    try {
                        File f = new File(dirName);
                        if ((f.exists() || f.mkdirs()) && f.canRead() && f.canWrite() && f.canExecute()) {
                            cacheDir = f;
                            break;
                        }
                    } catch (SecurityException e) {
                        // No access, try the next option.
                    }
                }
            }
        }
        if (cacheDir == null) {
            throw new IOException("Could not create the cache: Set the \"org.bytedeco.javacpp.cachedir\" system property.");
        }
        return cacheDir;
    }

    /**
     * Creates a unique name for {@link #tempDir} out of
     * {@code System.getProperty("java.io.tmpdir")} and {@code System.nanoTime()}.
     *
     * @return {@link #tempDir}
     */
    public static File getTempDir() {
        if (tempDir == null) {
            File tmpdir = new File(System.getProperty("java.io.tmpdir"));
            File f;
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

    /** Returns a Map that relates each library name to the path of the loaded file. */
    public static synchronized Map<String,String> getLoadedLibraries() {
        return new HashMap<String,String>(loadedLibraries);
    }

    /** Returns {@code System.getProperty("org.bytedeco.javacpp.loadlibraries")}.
     *  Flag set by the {@link Builder} to tell us not to try to load anything. */
    public static boolean isLoadLibraries() {
        String s = System.getProperty("org.bytedeco.javacpp.loadlibraries", "true").toLowerCase();
        s = System.getProperty("org.bytedeco.javacpp.loadLibraries", s).toLowerCase();
        return s.equals("true") || s.equals("t") || s.equals("");
    }

    public static boolean checkPlatform(Class<?> cls, Properties properties) {
        // check in priority this class for platform information, before the enclosing class
        Class<?> enclosingClass = Loader.getEnclosingClass(cls);
        while (!cls.isAnnotationPresent(org.bytedeco.javacpp.annotation.Properties.class)
                && !cls.isAnnotationPresent(Platform.class) && cls.getSuperclass() != null) {
            if (enclosingClass != null && cls.getSuperclass() == Object.class) {
                cls = enclosingClass;
                enclosingClass = null;
            } else {
                cls = cls.getSuperclass();
            }
        }

        org.bytedeco.javacpp.annotation.Properties classProperties =
                cls.getAnnotation(org.bytedeco.javacpp.annotation.Properties.class);
        Platform classPlatform = cls.getAnnotation(Platform.class);
        boolean supported = classProperties == null && classPlatform == null;
        if (classProperties != null) {
            Class[] classes = classProperties.inherit();

            // get default platform names, searching in inherited classes as well
            String[] defaultNames = classProperties.names();
            Deque<Class> queue = new ArrayDeque<Class>(Arrays.asList(classes));
            while (queue.size() > 0 && (defaultNames == null || defaultNames.length == 0)) {
                Class<?> c = queue.removeFirst();
                org.bytedeco.javacpp.annotation.Properties p =
                        c.getAnnotation(org.bytedeco.javacpp.annotation.Properties.class);
                if (p != null) {
                    defaultNames = p.names();
                    queue.addAll(Arrays.asList(p.inherit()));
                }
            }

            // check in priority the platforms inside our properties annotation, before inherited ones
            Platform[] platforms = classProperties.value();
            if (platforms != null && platforms.length > 0) {
                for (Platform p : platforms) {
                    if (checkPlatform(p, properties, defaultNames)) {
                        supported = true;
                        break;
                    }
                }
            } else if (classes != null && classes.length > 0) {
                for (Class c : classes) {
                    if (checkPlatform(c, properties)) {
                        supported = true;
                        break;
                    }
                }
            }
        }
        if (classPlatform != null) {
            supported = checkPlatform(cls.getAnnotation(Platform.class), properties);
        }
        return supported;
    }

    public static boolean checkPlatform(Platform platform, Properties properties, String... defaultNames) {
        if (platform == null) {
            return true;
        }
        if (defaultNames == null) {
            defaultNames = new String[0];
        }
        String platform2 = properties.getProperty("platform");
        String platformExtension = properties.getProperty("platform.extension");
        String[][] names = { platform.value().length > 0 ? platform.value() : defaultNames, platform.not() };
        boolean[] matches = { false, false };
        for (int i = 0; i < names.length; i++) {
            for (String s : names[i]) {
                if (platform2.startsWith(s)) {
                    matches[i] = true;
                    break;
                }
            }
        }
        if ((names[0].length == 0 || matches[0]) && (names[1].length == 0 || !matches[1])) {
            // when no extensions are given by user, but we are in library loading mode, try to load extensions anyway
            boolean match = platform.extension().length == 0 || (Loader.isLoadLibraries() && platformExtension == null);
            for (String s : platform.extension()) {
                if (platformExtension != null && platformExtension.length() > 0 && platformExtension.endsWith(s)) {
                    match = true;
                    break;
                }
            }
            return match;
        }
        return false;
    }

    /** Returns {@code load(classes, true)}. **/
    public static String[] load(Class... classes) {
        return load(classes, true);
    }
    /**
     * Calls {@link #load(Class)} on all top-level enclosing classes found in the array.
     *
     * @param classes to try to load
     * @param logMessages on load or fail silently
     * @return filenames from each successful call to {@link #load(Class)} or null otherwise
     */
    public static String[] load(Class[] classes, boolean logMessages) {
        String[] filenames = new String[classes.length];
        Properties properties = Loader.loadProperties();
        ClassProperties libProperties = null;
        for (int i = 0; i < classes.length; i++) {
            Class c = classes[i];

            // only load top-level enclosing classes that can load something by themselves
            if (getEnclosingClass(c) != c) {
                continue;
            }
            libProperties = loadProperties(c, properties, false);
            if (!libProperties.isLoaded()) {
                continue;
            }
            libProperties = loadProperties(c, properties, true);
            if (!libProperties.isLoaded()) {
                if (logMessages) {
                    logger.warn("Could not load platform properties for " + c);
                }
                continue;
            }
            try {
                if (logMessages) {
                    logger.info("Loading " + c);
                }
                filenames[i] = load(c);
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                if (logMessages) {
                    logger.warn("Could not load " + c + ": " + e);
                }
            }
        }
        return filenames;
    }

    /** Returns {@code load(getCallerClass(2), loadProperties(), Loader.pathsFirst)}. */
    public static String load() {
        return load(getCallerClass(2), loadProperties(), Loader.pathsFirst);
    }
    /**
     * Loads native libraries associated with the {@link Class} of the caller and initializes it.
     *
     * @param pathsFirst search the paths first before bundled resources
     * @return {@code load(getCallerClass(2), loadProperties(), pathsFirst) }
     * @see #getCallerClass(int)
     * @see #load(Class, Properties, boolean)
     */
    public static String load(boolean pathsFirst) {
        Class cls = getCallerClass(2);
        return load(cls, loadProperties(), pathsFirst);
    }
    /** Returns {@code load(cls, loadProperties(), Loader.pathsFirst)}. */
    public static String load(Class cls) {
        return load(cls, loadProperties(), Loader.pathsFirst);
    }
    /**
     * Loads native libraries associated with the given {@link Class} and initializes it.
     *
     * @param cls the Class to get native library information from and to initialize
     * @param properties the platform Properties to inherit
     * @param pathsFirst search the paths first before bundled resources
     * @return the full path to the main file loaded, or the library name if unknown
     *         (but {@code if (!isLoadLibraries() || cls == null) { return null; }})
     * @throws NoClassDefFoundError on Class initialization failure
     * @throws UnsatisfiedLinkError on native library loading failure or when interrupted
     * @see #findLibrary(Class, ClassProperties, String, boolean)
     * @see #loadLibrary(URL[], String, String...)
     */
    public static String load(Class cls, Properties properties, boolean pathsFirst) {
        Class classToLoad = cls;

        if (!isLoadLibraries() || cls == null) {
            return null;
        }

        if (!checkPlatform(cls, properties)) {
            throw new UnsatisfiedLinkError("Platform \"" + properties.getProperty("platform") + "\" not supported by " + cls);
        }

        // Find the top enclosing class, to match the library filename
        cls = getEnclosingClass(cls);
        ClassProperties p = loadProperties(cls, properties, true);

        // Force initialization of all the target classes in case they need it
        List<String> targets = p.get("global");
        if (targets.isEmpty()) {
            if (p.getInheritedClasses() != null) {
                for (Class c : p.getInheritedClasses()) {
                    targets.add(c.getName());
                }
            }
            targets.add(cls.getName());
        }

        // Make sure that we also initialize the class that was passed explicitly
        if (!targets.contains(classToLoad.getName())) {
            targets.add(classToLoad.getName());
        }

        for (String s : targets) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Loading class " + s);
                }
                Class.forName(s, true, cls.getClassLoader());
            } catch (ClassNotFoundException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to load class " + s + ": " + ex);
                }
                Error e = new NoClassDefFoundError(ex.toString());
                e.initCause(ex);
                throw e;
            }
        }

        String cacheDir = null;
        try {
            cacheDir = getCacheDir().getCanonicalPath();
        } catch (IOException e) {
            // no cache dir, no worries
        }

        // Preload native libraries desired by our class
        List<String> preloads = new ArrayList<String>();
        List<String> preloaded = new ArrayList<String>();
        List<String> preloadGlobals = new ArrayList<String>();
        preloads.addAll(p.get("platform.preload"));
        preloads.addAll(p.get("platform.link"));
        UnsatisfiedLinkError preloadError = null;
        for (String preload : preloads) {
            try {
                boolean loadGlobally = false;
                if (preload.endsWith("!")) {
                    // defer call to loadGlobal() until after we've loaded the JNI library that contains the function
                    loadGlobally = true;
                    preload = preload.substring(0, preload.length() - 1);
                }
                URL[] urls = foundLibraries.get(preload);
                if (urls == null) {
                    foundLibraries.put(preload, urls = findLibrary(cls, p, preload, pathsFirst));
                }
                String filename = loadLibrary(cls, urls, preload, preloaded.toArray(new String[preloaded.size()]));
                if (filename != null && new File(filename).exists()) {
                    preloaded.add(filename);
                    if (loadGlobally) {
                        preloadGlobals.add(filename);
                    }
                }
                if (cacheDir != null && filename != null && filename.startsWith(cacheDir)) {
                    createLibraryLink(filename, p, preload);
                }
            } catch (UnsatisfiedLinkError e) {
                preloadError = e;
            }
        }

        String executable = p.getProperty("platform.executable");
        if (executable != null && executable.length() > 0) {
            String platform = p.getProperty("platform");
            String[] extensions = p.get("platform.extension").toArray(new String[0]);
            String prefix = p.getProperty("platform.executable.prefix", "");
            String suffix = p.getProperty("platform.executable.suffix", "");
            String filename = prefix + executable + suffix;
            try {
                for (int i = extensions.length - 1; i >= -1; i--) {
                    // iterate extensions in reverse to be consistent with the overriding of properties
                    String extension = i >= 0 ? extensions[i] : "";
                    String subdir = platform + (extension == null ? "" : extension) + "/";
                    File f = cacheResource(cls, subdir + filename);
                    if (f != null) {
                        f.setExecutable(true);
                        return f.getAbsolutePath();
                    }
                }
            } catch (IOException e) {
                logger.error("Could not extract executable " + filename + ": " + e);
            }
            return null;
        }

        int librarySuffix = -1;
    tryAgain:
        while (true) {
            try {
                String library = p.getProperty("platform.library");
                if (librarySuffix >= 0) {
                    // try to load the JNI library using a different name
                    library += "#" + library + librarySuffix;
                }
                URL[] urls = foundLibraries.get(library);
                if (urls == null) {
                    foundLibraries.put(library, urls = findLibrary(cls, p, library, pathsFirst));
                }
                String filename = loadLibrary(cls, urls, library, preloaded.toArray(new String[preloaded.size()]));
                if (cacheDir != null && filename != null && filename.startsWith(cacheDir)) {
                    createLibraryLink(filename, p, library);
                }
                for (String preloadGlobal : preloadGlobals) {
                    loadGlobal(preloadGlobal);
                }
                return filename;
            } catch (UnsatisfiedLinkError e) {
                Throwable t = e;
                while (t != null) {
                    if (t instanceof UnsatisfiedLinkError &&
                            t.getMessage().contains("already loaded in another classloader")) {
                        librarySuffix++;
                        continue tryAgain;
                    }
                    t = t.getCause() != t ? t.getCause() : null;
                }
                if (preloadError != null && e.getCause() == null) {
                    e.initCause(preloadError);
                }
                throw e;
            }
        }
    }

    /** Returns {@code findLibrary(cls, properties, libnameversion, Loader.pathsFirst)}. */
    public static URL[] findLibrary(Class cls, ClassProperties properties, String libnameversion) {
        return findLibrary(cls, properties, libnameversion, Loader.pathsFirst);
    }

    /**
     * Finds from where the library may be extracted and loaded among the {@link Class}
     * resources. But in case that fails, and depending on the value of {@code pathsFirst},
     * either as a fallback or in priority over bundled resources, also searches the paths
     * found in the "platform.preloadpath" and "platform.linkpath" class properties (as well as
     * the "java.library.path" system property if {@code pathsFirst || !loadLibraries}), in that order.
     *
     * @param cls the Class whose package name and {@link ClassLoader} are used to extract from resources
     * @param properties contains the directories to scan for if we fail to extract the library from resources
     * @param libnameversion the name of the library + ":" + optional exact path to library + "@" + optional version tag
     *                       + "#" + a second optional name used at extraction (or empty to prevent it, unless it is a second "#")
     *                       + "!" to load all symbols globally
     * @param pathsFirst search the paths first before bundled resources
     * @return URLs that point to potential locations of the library
     */
    public static URL[] findLibrary(Class cls, ClassProperties properties, String libnameversion, boolean pathsFirst) {
        boolean nostyle = false;
        if (libnameversion.startsWith(":")) {
            nostyle = true;
            libnameversion = libnameversion.substring(1);
        } else if (libnameversion.contains(":")) {
            nostyle = true;
            libnameversion = libnameversion.substring(libnameversion.indexOf(":") + 1);
        }
        if (libnameversion.endsWith("!")) {
            libnameversion = libnameversion.substring(0, libnameversion.length() - 1);
        }
        if (libnameversion.trim().endsWith("#") && !libnameversion.trim().endsWith("##")) {
            return new URL[0];
        }
        String[] split = libnameversion.split("#");
        boolean reference = split.length > 1 && split[1].length() > 0;
        String[] s = split[0].split("@");
        String[] s2 = (reference ? split[1] : split[0]).split("@");
        String libname = s[0];
        String libname2 = s2[0];
        String version = s.length > 1 ? s[s.length-1] : "";
        String version2 = s2.length > 1 ? s2[s2.length-1] : "";

        // If we do not already have the native library file ...
        String platform = properties.getProperty("platform");
        String[] extensions = properties.get("platform.extension").toArray(new String[0]);
        String prefix = properties.getProperty("platform.library.prefix", "");
        String suffix = properties.getProperty("platform.library.suffix", "");
        String[] styles = {
            prefix + libname + suffix + version, // Linux style
            prefix + libname + version + suffix, // Mac OS X style
            prefix + libname + suffix            // without version
        };
        String[] styles2 = {
            prefix + libname2 + suffix + version2, // Linux style
            prefix + libname2 + version2 + suffix, // Mac OS X style
            prefix + libname2 + suffix             // without version
        };

        String[] suffixes = properties.get("platform.library.suffix").toArray(new String[0]);
        if (suffixes.length > 1) {
            styles = new String[3 * suffixes.length];
            styles2 = new String[3 * suffixes.length];
            for (int i = 0; i < suffixes.length; i++) {
                styles[3 * i    ] = prefix + libname + suffixes[i] + version; // Linux style
                styles[3 * i + 1] = prefix + libname + version + suffixes[i]; // Mac OS X style
                styles[3 * i + 2] = prefix + libname + suffixes[i];           // without version
                styles2[3 * i    ] = prefix + libname2 + suffixes[i] + version2; // Linux style
                styles2[3 * i + 1] = prefix + libname2 + version2 + suffixes[i]; // Mac OS X style
                styles2[3 * i + 2] = prefix + libname2 + suffixes[i];            // without version
            }
        }
        if (nostyle) {
            styles = new String[] {libname};
            styles2 = new String[] {libname2};
        }

        List<String> paths = new ArrayList<String>();
        paths.addAll(properties.get("platform.linkpath"));
        paths.addAll(properties.get("platform.preloadpath"));
        String[] resources = properties.get("platform.preloadresource").toArray(new String[0]);
        String libpath = System.getProperty("java.library.path", "");
        if (libpath.length() > 0 && (pathsFirst || !isLoadLibraries() || reference)) {
            // leave loading from "java.library.path" to System.loadLibrary() as fallback,
            // which works better on Android, unless the user wants to rename a library
            paths.addAll(Arrays.asList(libpath.split(File.pathSeparator)));
        }
        ArrayList<URL> urls = new ArrayList<URL>(styles.length * (1 + paths.size()));
        for (int i = 0; cls != null && i < styles.length; i++) {
            // ... then find it from in our resources ...
            for (int j = extensions.length - 1; j >= -1; j--) {
                // iterate extensions in reverse to be consistent with the overriding of properties
                String extension = j >= 0 ? extensions[j] : "";
                for (String resource : Arrays.copyOf(resources, resources.length + 1)) {
                    if (resource != null && !resource.endsWith("/")) {
                        resource += "/";
                    }
                    String subdir = (resource == null ? "" : "/" + resource) + platform
                                  + (extension == null ? "" : extension) + "/";
                    try {
                        URL u = findResource(cls, subdir + styles[i]);
                        if (u != null) {
                            if (reference) {
                                u = new URL(u + "#" + styles2[i]);
                            }
                            if (!urls.contains(u)) {
                                urls.add(u);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        // ... and in case of bad resources search the paths last, or first on user request.
        int k = pathsFirst ? 0 : urls.size();
        for (int i = 0; paths.size() > 0 && i < styles.length; i++) {
            for (String path : paths) {
                File file = new File(path, styles[i]);
                if (file.exists()) {
                    try {
                        URL u = file.toURI().toURL();
                        if (reference) {
                            u = new URL(u + "#" + styles2[i]);
                        }
                        if (!urls.contains(u)) {
                            urls.add(k++, u);
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }

    /** Returns {@code loadLibrary(getCallerClass(2), libnameversion, preloaded)}. */
    public static String loadLibrary(String libnameversion, String ... preloaded) {
        return loadLibrary(getCallerClass(2), libnameversion, preloaded);
    }

    /** Returns {@code loadLibrary(findResources(cls, libnameversion), libnameversion, preloaded)}. */
    public static String loadLibrary(Class<?> cls, String libnameversion, String ... preloaded) {
        try {
            return loadLibrary(findResources(cls, libnameversion), libnameversion, preloaded);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns {@code loadLibrary(null, urls, libnameversion, preloaded)}. */
    public static String loadLibrary(URL[] urls, String libnameversion, String ... preloaded) {
        return loadLibrary(null, urls, libnameversion, preloaded);
    }

    /**
     * Tries to load the library from the URLs in order, extracting resources as necessary.
     * Finally, if all fails, falls back on {@link System#loadLibrary(String)}.
     *
     * @param cls the Class whose {@link ClassLoader} is used to load the library, may be null
     * @param urls the URLs to try loading the library from
     * @param libnameversion the name of the library + ":" + optional exact path to library + "@" + optional version tag
     *                       + "#" + a second optional name used at extraction (or empty to prevent it, unless it is a second "#")
     *                       + "!" to load all symbols globally
     * @param preloaded libraries for which to create symbolic links in same cache directory
     * @return the full path of the file loaded, or the library name if unknown
     *         (but {@code if (!isLoadLibraries) { return null; }})
     * @throws UnsatisfiedLinkError on failure or when interrupted
     */
    public static synchronized String loadLibrary(Class<?> cls, URL[] urls, String libnameversion, String ... preloaded) {
        if (!isLoadLibraries()) {
            return null;
        }
        if (libnameversion.startsWith(":")) {
            libnameversion = libnameversion.substring(1);
        } else if (libnameversion.contains(":")) {
            libnameversion = libnameversion.substring(0, libnameversion.indexOf(":"));
        }
        boolean loadGlobally = false;
        if (libnameversion.endsWith("!")) {
            loadGlobally = true;
            libnameversion = libnameversion.substring(0, libnameversion.length() - 1);
        }
        String[] split = libnameversion.split("#");
        String libnameversion2 = split[0];
        if (split.length > 1 && split[1].length() > 0) {
            libnameversion2 = split[1];
        }

        // If we do not already have the native library file ...
        String filename = loadedLibraries.get(libnameversion2);
        UnsatisfiedLinkError loadError = null;
        classStack.get().push(cls);
        try {
            for (URL url : urls) {
                URI uri = url.toURI();
                File file = null;
                try {
                    // ... and if the URL is not already a file without fragments, etc ...
                    file = new File(uri);
                } catch (Exception exc) {
                    // ... extract it from resources into the cache, if necessary ...
                    File f = cacheResource(url, filename);
                    try {
                        if (f != null) {
                            file = f;
                        } else {
                            // ... else try to load directly as some libraries do not like being renamed ...
                            try {
                                file = new File(new URI(uri.toString().split("#")[0]));
                            } catch (IllegalArgumentException | URISyntaxException e) {
                                file = new File(uri.getPath());
                            }
                        }
                    } catch (Exception exc2) {
                        // ... (or give up) and ...
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failed to access " + uri + ": " + exc2);
                        }
                    }

                    // ... create symbolic links to previously loaded libraries as needed on Mac,
                    // at least, and some libraries like MKL on Linux too, ...
                    if (file != null && preloaded != null) {
                        File dir = file.getParentFile();
                        for (String s : preloaded) {
                            File file2 = new File(s);
                            File dir2 = file2.getParentFile();
                            if (dir2 != null && !dir2.equals(dir)) {
                                File linkFile = new File(dir, file2.getName());
                                try {
                                    Path linkPath = linkFile.toPath().normalize();
                                    Path targetPath = file2.toPath().normalize();
                                    if ((!linkFile.exists() || !Files.isSymbolicLink(linkPath) || !Files.readSymbolicLink(linkPath).equals(targetPath))
                                            && targetPath.isAbsolute() && !targetPath.equals(linkPath)) {
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("Creating symbolic link " + linkPath + " to " + targetPath);
                                        }
                                        linkFile.delete();
                                        Files.createSymbolicLink(linkPath, targetPath);
                                    }
                                } catch (IOException | RuntimeException e) {
                                    // ... (probably an unsupported operation on Windows, but DLLs never need links,
                                    // or other (filesystem?) exception: for example,
                                    // "sun.nio.fs.UnixException: No such file or directory" on File.toPath()) ...
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Failed to create symbolic link " + linkFile + " to " + file2 + ": " + e);
                                    }
                                }
                            }
                        }
                    }
                }
                if (filename != null) {
                    return filename;
                } else if (file != null && file.exists()) {
                    String filename2 = file.getAbsolutePath();
                    try {
                        // ... and load it!
                        if (logger.isDebugEnabled()) {
                            logger.debug("Loading " + filename2);
                        }
                        loadedLibraries.put(libnameversion2, filename2);

                        boolean loadedByLoad0 = false;
                        if (cls != null && Loader.class.getClassLoader() != cls.getClassLoader()) {
                            try {
                                Method load0 = Runtime.class.getDeclaredMethod("load0", Class.class, String.class);
                                load0.setAccessible(true);
                                load0.invoke(Runtime.getRuntime(), cls, filename2);
                                loadedByLoad0 = true;
                            } catch (IllegalAccessException | IllegalArgumentException
                                    | NoSuchMethodException | SecurityException cnfe) {
                                logger.warn("Unable to load the library " + libnameversion2 +
                                        " within the ClassLoader scope of " + cls.getName());
                            } catch (InvocationTargetException ite) {
                                Throwable target = ite.getTargetException();
                                if (target instanceof UnsatisfiedLinkError) {
                                    throw (UnsatisfiedLinkError) target;
                                } else {
                                    logger.warn("Unable to load the library " + libnameversion2
                                            + " within the ClassLoader scope of " + cls.getName()
                                            + " because: " + target.getMessage());
                                }
                            }
                        }
                        if (!loadedByLoad0) {
                            System.load(filename2);
                        }
                        if (loadGlobally) {
                            loadGlobal(filename2);
                        }
                        return filename2;
                    } catch (UnsatisfiedLinkError e) {
                        loadError = e;
                        loadedLibraries.remove(libnameversion2);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failed to load " + filename2 + ": " + e);
                        }
                    }
                }
            }
            if (filename != null) {
                return filename;
            } else if (!libnameversion.trim().endsWith("#")) {
                // ... or as last resort, try to load it via the system.
                String libname = libnameversion.split("#")[0].split("@")[0];
                if (libname.endsWith("!")) {
                    libname = libname.substring(0, libname.length() - 1);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Loading library " + libname);
                }
                loadedLibraries.put(libnameversion2, libname);
                boolean loadedByLoadLibrary0 = false;
                if (cls != null && Loader.class.getClassLoader() != cls.getClassLoader()) {
                    try {
                        Method load0 = Runtime.class.getDeclaredMethod("loadLibrary0", Class.class, String.class);
                        load0.setAccessible(true);
                        load0.invoke(Runtime.getRuntime(), cls, libname);
                        loadedByLoadLibrary0 = true;
                    } catch (IllegalAccessException | IllegalArgumentException
                            | NoSuchMethodException | SecurityException cnfe) {
                        logger.warn("Unable to load the library " + libname +
                                " within the ClassLoader scope of " + cls.getName());
                    } catch (InvocationTargetException ite) {
                        Throwable target = ite.getTargetException();
                        if (target instanceof UnsatisfiedLinkError) {
                            throw (UnsatisfiedLinkError) target;
                        } else {
                            logger.warn("Unable to load the library " + libname
                                    + " within the ClassLoader scope of " + cls.getName()
                                    + " because: " + target.getMessage());
                        }
                    }
                }
                if (!loadedByLoadLibrary0) {
                    System.loadLibrary(libname);
                }
                return libname;
            } else {
                // But do not load when tagged as a system library
                return null;
            }
        } catch (UnsatisfiedLinkError e) {
            loadedLibraries.remove(libnameversion2);
            if (loadError != null && e.getCause() == null) {
                e.initCause(loadError);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to load for " + libnameversion + ": " + e);
            }
            throw e;
        } catch (IOException | URISyntaxException ex) {
            loadedLibraries.remove(libnameversion2);
            if (loadError != null && ex.getCause() == null) {
                ex.initCause(loadError);
            }
            Error e = new UnsatisfiedLinkError(ex.toString());
            e.initCause(ex);
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to extract for " + libnameversion + ": " + e);
            }
            throw e;
        } finally {
            classStack.get().pop();
        }
    }

    /**
     * Creates a version-less symbolic link to a library file, if needed.
     * Also creates symbolic links in given paths, with and without version.
     *
     * @param filename of the probably versioned library
     * @param properties of the class associated with the library
     * @param libnameversion the library name and version as with {@link #loadLibrary(URL[], String, String...)} (can be null)
     * @param paths where to create links, in addition to the parent directory of filename
     * @return the version-less filename (or null on failure), a symbolic link only if needed
     */
    public static String createLibraryLink(String filename, ClassProperties properties, String libnameversion, String ... paths) {
        if (libnameversion != null && libnameversion.startsWith(":")) {
            libnameversion = libnameversion.substring(1);
        } else if (libnameversion != null && libnameversion.contains(":")) {
            libnameversion = libnameversion.substring(0, libnameversion.indexOf(":"));
        }
        if (libnameversion != null && libnameversion.endsWith("!")) {
            libnameversion = libnameversion.substring(0, libnameversion.length() - 1);
        }
        File file = new File(filename);
        String parent = file.getParent(), name = file.getName(), link = null;

        String[] split = libnameversion != null ? libnameversion.split("#") : new String[] {""};
        String[] s = (split.length > 1 && split[1].length() > 0 ? split[1] : split[0]).split("@");
        String libname = s[0];
        String version = s.length > 1 ? s[s.length-1] : "";

        if (!name.contains(libname)) {
            return filename;
        }
        for (String suffix : properties.get("platform.library.suffix")) {
            int n = name.lastIndexOf(suffix);
            int n2 = version.length() != 0 ? name.lastIndexOf(version)
                                           : name.indexOf(".");
            int n3 = name.lastIndexOf(".");
            if (n2 < n && n < n3) {
                link = name.substring(0, n) + suffix;
                break;
            } else if (n > 0 && n2 > 0) {
                link = name.substring(0, n < n2 ? n : n2) + suffix;
                break;
            }
        }
        if (link == null) {
            for (String suffix : properties.get("platform.library.suffix")) {
                if (name.endsWith(suffix)) {
                    link = name;
                    break;
                }
            }
        }
        if (link != null && link.length() > 0) {
            File linkFile = new File(parent, link);
            try {
                Path linkPath = linkFile.toPath();
                Path targetPath = Paths.get(name);
                if ((!linkFile.exists() || !Files.isSymbolicLink(linkPath) || !Files.readSymbolicLink(linkPath).equals(targetPath))
                        && !targetPath.isAbsolute() && !targetPath.equals(linkPath.getFileName())) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating symbolic link " + linkPath);
                    }
                    linkFile.delete();
                    Files.createSymbolicLink(linkPath, targetPath);
                }
                filename = linkFile.toString();

                for (String parent2 : paths) {
                    if (parent2 == null) {
                        continue;
                    }
                    for (String link2 : new String[] { link, name }) {
                        File linkFile2 = new File(parent2, link2);
                        Path linkPath2 = linkFile2.toPath();
                        Path relativeTarget = Paths.get(parent2).relativize(Paths.get(parent)).resolve(name);
                        if ((!linkFile2.exists() || !Files.isSymbolicLink(linkPath2) || !Files.readSymbolicLink(linkPath2).equals(relativeTarget))
                                && !relativeTarget.isAbsolute() && !relativeTarget.equals(linkPath2.getFileName())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Creating symbolic link " + linkPath2);
                            }
                            linkFile2.delete();
                            Files.createSymbolicLink(linkPath2, relativeTarget);
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                // ... (probably an unsupported operation on Windows, but DLLs never need links,
                // or other (filesystem?) exception: for example,
                // "sun.nio.fs.UnixException: No such file or directory" on File.toPath()) ...
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to create symbolic link " + linkFile + ": " + e);
                }
                return null;
            }
        }
        return filename;
    }

    /**
     * Contains {@code offsetof()} and {@code sizeof()} values of native types
     * of {@code struct}, {@code class}, and {@code union}. A {@link WeakHashMap}
     * is used to prevent the Loader from hanging onto Class objects the user may
     * be trying to unload.
     */
    static WeakHashMap<Class<? extends Pointer>,HashMap<String,Integer>> memberOffsets =
            new WeakHashMap<Class<? extends Pointer>,HashMap<String,Integer>>();

    static {
        try {
            Loader.load();
        } catch (Throwable t) {
            logger.warn("Could not load Loader: " + t);
        }
    }

    /**
     * Called by native libraries to put {@code offsetof()} and {@code sizeof()} values in {@link #memberOffsets}.
     * Tries to load the Class object for typeName using the {@link ClassLoader} of the Loader.
     *
     * @param typeName the name of the peer Class acting as interface to the native type
     * @param member the name of the native member variable (can be null to retrieve the Class object only)
     * @param offset the value of {@code offsetof()} (or {@code sizeof()} when {@code member.equals("sizeof")})
     * @return {@code Class.forName(typeName, false)}
     * @throws ClassNotFoundException on Class initialization failure
     */
    static Class putMemberOffset(String typeName, String member, int offset) throws ClassNotFoundException {
        try {
            Class<?> context = classStack.get().peek();
            Class<?> c = Class.forName(typeName.replace('/', '.'), false,
                    context != null ? context.getClassLoader() : Loader.class.getClassLoader());
            if (member != null) {
                putMemberOffset(c.asSubclass(Pointer.class), member, offset);
            }
            return c;
        } catch (ClassNotFoundException e) {
            logger.warn("Loader.putMemberOffset(): " + e);
            return null;
        }
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
        HashMap<String,Integer> offsets = memberOffsets.get(type);
        while (offsets == null && type.getSuperclass() != null) {
            type = type.getSuperclass().asSubclass(Pointer.class);
            offsets = memberOffsets.get(type);
        }
        return offsets.get(member);
    }

    /**
     * Gets {@code sizeof()} values from {@link #memberOffsets} filled by native libraries.
     *
     * @param type the peer Class acting as interface to the native type
     * @return {@code memberOffsets.get(type).get("sizeof")}
     */
    public static int sizeof(Class<? extends Pointer> type) {
        return offsetof(type, "sizeof");
    }


    /** Returns the number of processors configured according to the operating system, or 0 if unknown.
     * This value can be greater than {@link Runtime#availableProcessors()} and {@link #totalCores()}. */
    @Name("JavaCPP_totalProcessors") public static native int totalProcessors();

    /** Returns the number of CPU cores usable according to the operating system, or 0 if unknown.
     * For SMT-capable systems, this value may be less than {@link #totalProcessors()}. */
    @Name("JavaCPP_totalCores") public static native int totalCores();

    /** Returns the number of CPU chips installed according to the operating system, or 0 if unknown.
     * For multi-core processors, this value may be less than {@link #totalCores()}. */
    @Name("JavaCPP_totalChips") public static native int totalChips();

    /** Returns the address found under the given name in the "dynamic symbol tables" (Linux, Mac OS X, etc)
     * or the "export tables" (Windows) of all libraries loaded, or null if not found. */
    @Name("JavaCPP_addressof") public static native Pointer addressof(String symbol);

    /** Loads all symbols from a library globally, that is {@code dlopen(filename, RTLD_LAZY | RTLD_GLOBAL)},
     * or simply by default with {@code LoadLibrary(filename)} on Windows. If the library name passed to
     * one of the other load functions in this class ends with "!", this function will get called on them. */
    @Name("JavaCPP_loadGlobal") @Raw(withEnv = true) public static native void loadGlobal(String filename);

    /** Returns the JavaVM JNI object, as required by some APIs for initialization. */
    @Name("JavaCPP_getJavaVM") public static native @Cast("JavaVM*") Pointer getJavaVM();
}
