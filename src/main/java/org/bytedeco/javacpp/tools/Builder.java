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

package org.bytedeco.javacpp.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.bytedeco.javacpp.ClassProperties;
import org.bytedeco.javacpp.Loader;

/**
 * The Builder is responsible for coordinating efforts between the Parser, the
 * Generator, and the native compiler. It contains the main() method, and basically
 * takes care of the tasks one would expect from a command line build tool, but
 * can also be used programmatically by setting its properties and calling build().
 *
 * @author Samuel Audet
 */
public class Builder {

    /**
     * Deletes {@link #outputDirectory} if {@link #clean} is true.
     * @throws IOException
     */
    void cleanOutputDirectory() throws IOException {
        if (outputDirectory != null && outputDirectory.isDirectory() && clean) {
            logger.info("Deleting " + outputDirectory);
            Files.walkFileTree(outputDirectory.toPath(), new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    if (e != null) {
                        throw e;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Calls {@link Parser#parse(File, String[], Class)} after creating an instance of the Class.
     *
     * @param classPath an array of paths to try to load header files from
     * @param cls The class annotated with {@link org.bytedeco.javacpp.annotation.Properties}
     *            and implementing {@link InfoMapper}
     * @return the target File produced
     * @throws IOException on Java target file writing error
     * @throws ParserException on C/C++ header file parsing error
     */
    File[] parse(String[] classPath, Class cls) throws IOException, ParserException {
        cleanOutputDirectory();
        return new Parser(logger, properties, encoding, null).parse(outputDirectory, classPath, cls);
    }

    /**
     * Tries to find automatically include paths for {@code jni.h} and {@code jni_md.h},
     * as well as the link and library paths for the {@code jvm} library.
     *
     * @param properties the Properties containing the paths to update
     * @param header to request support for exporting callbacks via generated header file
     */
    void includeJavaPaths(ClassProperties properties, boolean header) {
        if (properties.getProperty("platform", "").startsWith("android")) {
            // Android includes its own jni.h file and doesn't have a jvm library
            return;
        }
        String platform = Loader.getPlatform();
        final String jvmlink = properties.getProperty("platform.link.prefix", "") +
                       "jvm" + properties.getProperty("platform.link.suffix", "");
        final String jvmlib  = properties.getProperty("platform.library.prefix", "") +
                       "jvm" + properties.getProperty("platform.library.suffix", "");
        final String[] jnipath = new String[2];
        final String[] jvmpath = new String[2];
        FilenameFilter filter = new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                if (new File(dir, "jni.h").exists()) {
                    jnipath[0] = dir.getAbsolutePath();
                }
                if (new File(dir, "jni_md.h").exists()) {
                    jnipath[1] = dir.getAbsolutePath();
                }
                if (new File(dir, jvmlink).exists()) {
                    jvmpath[0] = dir.getAbsolutePath();
                }
                if (new File(dir, jvmlib).exists()) {
                    jvmpath[1] = dir.getAbsolutePath();
                }
                return new File(dir, name).isDirectory();
            }
        };
        File javaHome;
        try {
            javaHome = new File(System.getProperty("java.home")).getParentFile().getCanonicalFile();
        } catch (IOException | NullPointerException e) {
            logger.warn("Could not include header files from java.home:" + e);
            return;
        }
        ArrayList<File> dirs = new ArrayList<File>(Arrays.asList(javaHome.listFiles(filter)));
        while (!dirs.isEmpty()) {
            File d = dirs.remove(dirs.size() - 1);
            String dpath = d.getPath();
            File[] files = d.listFiles(filter);
            if (dpath == null || files == null) {
                continue;
            }
            for (File f : files) {
                try {
                    f = f.getCanonicalFile();
                } catch (IOException e) { }
                if (!dpath.startsWith(f.getPath())) {
                    dirs.add(f);
                }
            }
        }
        if (jnipath[0] != null && jnipath[0].equals(jnipath[1])) {
            jnipath[1] = null;
        } else if (jnipath[0] == null) {
            String macpath = "/System/Library/Frameworks/JavaVM.framework/Headers/";
            if (new File(macpath).isDirectory()) {
                jnipath[0] = macpath;
            }
        }
        if (jvmpath[0] != null && jvmpath[0].equals(jvmpath[1])) {
            jvmpath[1] = null;
        }
        properties.addAll("platform.includepath", jnipath);
        if (platform.equals(properties.getProperty("platform", platform))) {
            if (header) {
                // We only need libjvm for callbacks exported with the header file
                properties.get("platform.link").add(0, "jvm");
                properties.addAll("platform.linkpath", jvmpath);
            }
            if (platform.startsWith("macosx")) {
                properties.addAll("platform.framework", "JavaVM");
            }
        }
    }

    /**
     * Executes a command with {@link ProcessBuilder}, but also logs the call
     * and redirects its input and output to our process.
     *
     * @param command to have {@link ProcessBuilder} execute
     * @param workingDirectory to pass to {@link ProcessBuilder#directory()}
     * @param environmentVariables to put in {@link ProcessBuilder#environment()}
     * @return the exit value of the command
     * @throws IOException
     * @throws InterruptedException
     */
    int executeCommand(List<String> command, File workingDirectory,
            Map<String,String> environmentVariables) throws IOException, InterruptedException {
        String platform = Loader.getPlatform();
        boolean windows = platform.startsWith("windows");
        for (int i = 0; i < command.size(); i++) {
            String arg = command.get(i);
            if (arg == null) {
                arg = "";
            }
            if (arg.trim().isEmpty() && windows) {
                // seems to be the only way to pass empty arguments on Windows?
                arg = "\"\"";
            }
            command.set(i, arg);
        }

        String text = "";
        for (String s : command) {
            boolean hasSpaces = s.indexOf(" ") > 0 || s.isEmpty();
            if (hasSpaces) {
                text += windows ? "\"" : "'";
            }
            text += s;
            if (hasSpaces) {
                text += windows ? "\"" : "'";
            }
            text += " ";
        }
        logger.info(text);

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDirectory != null) {
            pb.directory(workingDirectory);
        }
        if (environmentVariables != null) {
            for (Map.Entry<String,String> e : environmentVariables.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    pb.environment().put(e.getKey(), e.getValue());
                }
            }
        }
        return pb.inheritIO().start().waitFor();
    }

    /**
     * Launches and waits for the native compiler to produce a native shared library.
     *
     * @param sourceFilenames the C++ source filenames
     * @param outputFilename the output filename of the shared library
     * @param properties the Properties detailing the compiler options to use
     * @return the result of {@link Process#waitFor()}
     * @throws IOException
     * @throws InterruptedException
     */
    int compile(String[] sourceFilenames, String outputFilename, ClassProperties properties, File workingDirectory)
            throws IOException, InterruptedException {
        ArrayList<String> command = new ArrayList<String>();

        includeJavaPaths(properties, header);

        String platform  = Loader.getPlatform();
        String compilerPath = properties.getProperty("platform.compiler");
        command.add(compilerPath);

        {
            String p = properties.getProperty("platform.sysroot.prefix", "");
            for (String s : properties.get("platform.sysroot")) {
                if (new File(s).isDirectory()) {
                    s = new File(s).getCanonicalPath();
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
        }

        {
            String p = properties.getProperty("platform.toolchain.prefix", "");
            for (String s : properties.get("platform.toolchain")) {
                if (new File(s).isDirectory()) {
                    s = new File(s).getCanonicalPath();
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
        }

        {
            String p = properties.getProperty("platform.includepath.prefix", "");
            for (String s : properties.get("platform.includepath")) {
                if (new File(s).isDirectory()) {
                    s = new File(s).getCanonicalPath();
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
            for (String s : properties.get("platform.includeresource")) {
                for (File f : Loader.cacheResources(s)) {
                    if (f.isDirectory()) {
                        if (p.endsWith(" ")) {
                            command.add(p.trim()); command.add(f.getCanonicalPath());
                        } else {
                            command.add(p + f.getCanonicalPath());
                        }
                    }
                }
            }
        }

        for (int i = sourceFilenames.length - 1; i >= 0; i--) {
            command.add(sourceFilenames[i]);
        }

        List<String> allOptions = properties.get("platform.compiler.*");
        if (!allOptions.contains("!default") && !allOptions.contains("default")) {
            allOptions.add(0, "default");
        }
        for (String s : allOptions) {
            if (s == null || s.length() == 0) {
                continue;
            }
            String p = "platform.compiler." + s;
            String options = properties.getProperty(p);
            if (options != null && options.length() > 0) {
                command.addAll(Arrays.asList(options.split(" ")));
            } else if (!"!default".equals(s) && !"default".equals(s)) {
                logger.warn("Could not get the property named \"" + p + "\"");
            }
        }

        command.addAll(compilerOptions);

        String output = properties.getProperty("platform.compiler.output");
        for (int i = 1; i < 2 || output != null; i++,
                output = properties.getProperty("platform.compiler.output" + i)) {
            if (output != null && output.length() > 0) {
                command.addAll(Arrays.asList(output.split(" ")));
            }

            if (output == null || output.length() == 0 || output.endsWith(" ")) {
                command.add(outputFilename);
            } else {
                command.add(command.remove(command.size() - 1) + outputFilename);
            }
        }

        {
            String p  = properties.getProperty("platform.linkpath.prefix", "");
            String p2 = properties.getProperty("platform.linkpath.prefix2");
            for (String s : properties.get("platform.linkpath")) {
                if (new File(s).isDirectory()) {
                    s = new File(s).getCanonicalPath();
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                    if (p2 != null) {
                        if (p2.endsWith(" ")) {
                            command.add(p2.trim()); command.add(s);
                        } else {
                            command.add(p2 + s);
                        }
                    }
                }
            }
            for (String s : properties.get("platform.linkresource")) {
                for (File f : Loader.cacheResources(s)) {
                    if (f.isDirectory()) {
                        if (p.endsWith(" ")) {
                            command.add(p.trim()); command.add(f.getCanonicalPath());
                        } else {
                            command.add(p + f.getCanonicalPath());
                        }
                        if (p2 != null) {
                            if (p2.endsWith(" ")) {
                                command.add(p2.trim()); command.add(f.getCanonicalPath());
                            } else {
                                command.add(p2 + f.getCanonicalPath());
                            }
                        }
                    }
                }
            }
        }

        {
            String p = properties.getProperty("platform.link.prefix", "");
            String x = properties.getProperty("platform.link.suffix", "");

            String linkPrefix = "";
            String linkSuffix = "";
            List<String> linkBeforeOptions = new ArrayList<>();
            List<String> linkAfterOptions  = new ArrayList<>();

            if (p.endsWith(" ")) {
                linkBeforeOptions.addAll(Arrays.asList(p.trim().split(" ")));
            } else {
                int lastSpaceIndex = p.lastIndexOf(" ");
                if (lastSpaceIndex != -1) {
                    linkBeforeOptions.addAll(Arrays.asList(p.substring(0, lastSpaceIndex).split(" ")));
                    linkPrefix = p.substring(lastSpaceIndex + 1);
                } else {
                    linkPrefix = p;
                }
            }

            if (x.startsWith(" ")) {
                linkAfterOptions.addAll(Arrays.asList(x.trim().split(" ")));
            } else {
                int firstSpaceIndex = x.indexOf(" ");
                if (firstSpaceIndex != -1) {
                    linkSuffix = x.substring(0, firstSpaceIndex);
                    linkAfterOptions.addAll(Arrays.asList(x.substring(firstSpaceIndex + 1).split(" ")));
                } else {
                    linkSuffix = x;
                }
            }

            int i = command.size(); // to inverse order and satisfy typical compilers
            for (String s : properties.get("platform.link")) {
                String[] libnameversion = s.split("#")[0].split("@");
                if (libnameversion.length == 3 && libnameversion[1].length() == 0) {
                    // Only use the version number when the user gave us a double @
                    s = libnameversion[0] + libnameversion[2];
                } else {
                    s = libnameversion[0];
                }
                List<String> l = new ArrayList<>();
                l.addAll(linkBeforeOptions);
                l.add(linkPrefix + (s.endsWith("!") ? s.substring(0, s.length() - 1) : s) + linkSuffix);
                l.addAll(linkAfterOptions);

                command.addAll(i, l);
            }
        }

        {
            String p = properties.getProperty("platform.frameworkpath.prefix", "");
            for (String s : properties.get("platform.frameworkpath")) {
                if (new File(s).isDirectory()) {
                    s = new File(s).getCanonicalPath();
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
        }

        {
            String p = properties.getProperty("platform.framework.prefix", "");
            String x = properties.getProperty("platform.framework.suffix", "");
            for (String s : properties.get("platform.framework")) {
                if (p.endsWith(" ") && x.startsWith(" ")) {
                    command.add(p.trim()); command.add(s); command.add(x.trim());
                } else if (p.endsWith(" ")) {
                    command.add(p.trim()); command.add(s + x);
                } else if (x.startsWith(" ")) {
                    command.add(p + s); command.add(x.trim());
                } else {
                    command.add(p + s + x);
                }
            }
        }

        boolean windows = platform.startsWith("windows");
        for (int i = 0; i < command.size(); i++) {
            String arg = command.get(i);
            if (arg == null) {
                arg = "";
            }
            if (arg.trim().isEmpty() && windows) {
                // seems to be the only way to pass empty arguments on Windows?
                arg = "\"\"";
            }
            command.set(i, arg);
        }

        // Use the library output path as the working directory so that all
        // build files, including intermediate ones from MSVC, are dumped there
        return executeCommand(command, workingDirectory, environmentVariables);
    }

    /**
     * Creates and returns the directory where output files should be placed.
     * Uses {@link #outputDirectory} as is when available, but falls back
     * on the shortest common path to the classes as well as the platform
     * specific library path when available, or the platform name itself
     * and the user provided extension when not.
     *
     * @param classes from which to derive the output path
     * @param sourcePrefixes returned, 2 strings without platform names, one from our class loader, the other from the common path of the classes
     * @return directory where binary files should be written to
     * @throws IOException
     */
    File getOutputPath(Class[] classes, String[] sourcePrefixes) throws IOException {
        cleanOutputDirectory();
        File outputPath = outputDirectory != null ? outputDirectory.getCanonicalFile() : null;
        ClassProperties p = Loader.loadProperties(classes, properties, true);
        String platform     = properties.getProperty("platform");
        String extension    = properties.getProperty("platform.extension");
        String sourcePrefix = outputPath != null ? outputPath.getPath() + File.separator : "";
        String libraryPath  = p.getProperty("platform.library.path", "");
        if (sourcePrefixes != null) {
            sourcePrefixes[0] = sourcePrefixes[1] = sourcePrefix;
        }
        if (outputPath == null) {
            URI uri = null;
            try {
                String resourceName = '/' + classes[0].getName().replace('.', '/')  + ".class";
                String resourceURL = Loader.findResource(classes[0], resourceName).toString();
                String packageURI = resourceURL.substring(0, resourceURL.lastIndexOf('/') + 1);
                for (int i = 1; i < classes.length; i++) {
                    // Use shortest common package name among all classes as default output path
                    String resourceName2 = '/' + classes[i].getName().replace('.', '/')  + ".class";
                    String resourceURL2 = Loader.findResource(classes[i], resourceName2).toString();
                    String packageURI2 = resourceURL2.substring(0, resourceURL2.lastIndexOf('/') + 1);

                    String longest = packageURI2.length() > packageURI.length() ? packageURI2 : packageURI;
                    String shortest = packageURI2.length() < packageURI.length() ? packageURI2 : packageURI;
                    while (!longest.startsWith(shortest) && shortest.lastIndexOf('/') > 0) {
                        shortest = shortest.substring(0, shortest.lastIndexOf('/'));
                    }
                    packageURI = shortest;
                }
                uri = new URI(packageURI);
                boolean isFile = "file".equals(uri.getScheme());
                File classPath = new File(classScanner.getClassLoader().getPaths()[0]).getCanonicalFile();
                // If our class is not a file, use first path of the user class loader as base for our output path
                File packageDir = isFile ? new File(uri)
                                         : new File(classPath, resourceName.substring(0, resourceName.lastIndexOf('/') + 1));
                // Output to the library path inside of the class path, if provided by the user
                uri = new URI(resourceURL.substring(0, resourceURL.length() - resourceName.length() + 1));
                File targetDir = libraryPath.length() > 0
                        ? (isFile ? new File(uri) : classPath)
                        : new File(packageDir, platform + (extension != null ? extension : ""));
                outputPath = new File(targetDir, libraryPath);
                sourcePrefix = packageDir.getPath() + File.separator;
                // make sure jnijavacpp.cpp ends up in the same directory for all classes in different packages
                if (sourcePrefixes != null) {
                    sourcePrefixes[0] = classPath.getPath() + File.separator;
                    sourcePrefixes[1] = sourcePrefix;
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("URI: " + uri, e);
            }
        }
        if (!outputPath.exists()) {
            outputPath.mkdirs();
        }
        return outputPath;
    }

    /**
     * Generates C++ source files for classes, and compiles everything in
     * one shared library when {@code compile == true}.
     *
     * @param classes the Class objects as input to Generator
     * @param outputName the output name of the shared library
     * @param first of the batch, so generate jnijavacpp.cpp
     * @param last of the batch, so delete jnijavacpp.cpp
     * @return the actual File generated, either the compiled library or its source
     * @throws IOException
     * @throws InterruptedException
     */
    File[] generateAndCompile(Class[] classes, String outputName, boolean first, boolean last) throws IOException, InterruptedException {
        String[] sourcePrefixes = new String[2];
        File outputPath = getOutputPath(classes, sourcePrefixes);
        ClassProperties p = Loader.loadProperties(classes, properties, true);
        String sourceSuffix = p.getProperty("platform.source.suffix", ".cpp");
        String libraryPrefix  = p.getProperty("platform.library.prefix", "") ;
        String librarySuffix  = p.getProperty("platform.library.suffix", "");
        Generator generator = new Generator(logger, properties, encoding);
        String[] sourceFilenames = {sourcePrefixes[0] + "jnijavacpp" + sourceSuffix,
                                    sourcePrefixes[1] + outputName + sourceSuffix};
        String[] headerFilenames = {null, header ? sourcePrefixes[1] + outputName +  ".h" : null};
        String[] loadSuffixes = {"_jnijavacpp", null};
        String[] baseLoadSuffixes = {null, "_jnijavacpp"};
        String classPath = System.getProperty("java.class.path");
        for (String s : classScanner.getClassLoader().getPaths()) {
            classPath += File.pathSeparator + s;
        }
        String[] classPaths = {null, classPath};
        Class[][] classesArray = {null, classes};
        String[] libraryNames  = {libraryPrefix + "jnijavacpp" + librarySuffix,
                                  libraryPrefix + outputName + librarySuffix};
        File[] outputFiles = null;

        if (outputName.equals("jnijavacpp")) {
            // generate a single file if the user only wants "jnijavacpp"
            sourceFilenames = new String[] {sourcePrefixes[0] + outputName + sourceSuffix};
            headerFilenames = new String[] {header ? sourcePrefixes[0] + outputName +  ".h" : null};
            loadSuffixes = new String[] {null};
            baseLoadSuffixes = new String[] {null};
            classPaths = new String[] {null};
            classesArray = new Class[][] {null};
            libraryNames  = new String[] {libraryPrefix + outputName + librarySuffix};
        }

        boolean generated = true;
        for (int i = 0; i < sourceFilenames.length; i++) {
            if (i == 0 && !first) {
                continue;
            }
            logger.info("Generating " + sourceFilenames[i]);
            if (!generator.generate(sourceFilenames[i], headerFilenames[i],
                    loadSuffixes[i], baseLoadSuffixes[i], classPaths[i], classesArray[i])) {
                logger.info("Nothing generated for " + sourceFilenames[i]);
                generated = false;
                break;
            }
        }
        if (generated) {
            if (compile) {
                int exitValue = 0;
                String s = properties.getProperty("platform.library.static", "false").toLowerCase();
                if (s.equals("true") || s.equals("t") || s.equals("")) {
                    outputFiles = new File[sourceFilenames.length];
                    for (int i = 0; exitValue == 0 && i < sourceFilenames.length; i++) {
                        if (i == 0 && !first) {
                            continue;
                        }
                        logger.info("Compiling " + outputPath.getPath() + File.separator + libraryNames[i]);
                        exitValue = compile(new String[] {sourceFilenames[i]}, libraryNames[i], p, outputPath);
                        outputFiles[i] = new File(outputPath, libraryNames[i]);
                    }
                } else {
                    String libraryName = libraryNames[libraryNames.length - 1];
                    logger.info("Compiling " + outputPath.getPath() + File.separator + libraryName);
                    exitValue = compile(sourceFilenames, libraryName, p, outputPath);
                    outputFiles = new File[] {new File(outputPath, libraryName)};
                }
                if (exitValue == 0) {
                    for (int i = sourceFilenames.length - 1; i >= 0; i--) {
                        if (i == 0 && !last) {
                            continue;
                        }
                        if (deleteJniFiles) {
                            logger.info("Deleting " + sourceFilenames[i]);
                            new File(sourceFilenames[i]).delete();
                        } else {
                            logger.info("Keeping " + sourceFilenames[i]);
                        }
                    }
                } else {
                    throw new RuntimeException("Process exited with an error: " + exitValue);
                }
            } else {
                outputFiles = new File[sourceFilenames.length];
                for (int i = 0; i < sourceFilenames.length; i++) {
                    outputFiles[i] = new File(sourceFilenames[i]);
                }
            }
        }
        return outputFiles;
    }

    /**
     * Stores all the files in the given JAR file. Also attempts to root the paths
     * of the filenames to each element of a list of classpaths.
     *
     * @param jarFile the JAR file to create
     * @param classPath an array of paths to try to use as root for classes
     * @param files a list of files to store in the JAR file
     * @throws IOException
     */
    void createJar(File jarFile, String[] classPath, File ... files) throws IOException {
        logger.info("Creating " + jarFile);
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
        for (File f : files) {
            String name = f.getPath();
            if (classPath != null) {
                // Store only the path relative to the classpath so that
                // our Loader may use the package name of the associated
                // class to get the file as a resource from the ClassLoader.
                String[] names = new String[classPath.length];
                for (int i = 0; i < classPath.length; i++) {
                    String path = new File(classPath[i]).getCanonicalPath();
                    if (name.startsWith(path)) {
                        names[i] = name.substring(path.length() + 1);
                    }
                }
                // Retain only the shortest relative name.
                for (int i = 0; i < names.length; i++) {
                    if (names[i] != null && names[i].length() < name.length()) {
                        name = names[i];
                    }
                }
            }
            ZipEntry e = new ZipEntry(name.replace(File.separatorChar, '/'));
            e.setTime(f.lastModified());
            jos.putNextEntry(e);
            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[64 * 1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                jos.write(buffer, 0, length);
            }
            fis.close();
            jos.closeEntry();
        }
        jos.close();
    }

    /**
     * Default constructor that simply initializes everything.
     */
    public Builder() {
        this(Logger.create(Builder.class));
    }
    /**
     * Constructor that simply initializes everything.
     * @param logger where to send messages
     */
    public Builder(Logger logger) {
        this.logger = logger;
        System.setProperty("org.bytedeco.javacpp.loadlibraries", "false");
        properties = Loader.loadProperties();
        classScanner = new ClassScanner(logger, new ArrayList<Class>(),
                new UserClassLoader(Thread.currentThread().getContextClassLoader()));
        compilerOptions = new ArrayList<String>();
    }

    /** Logger where to send debug, info, warning, and error messages. */
    final Logger logger;
    /** The name of the character encoding used for input files as well as output files. */
    String encoding = null;
    /** The directory where the generated files and compiled shared libraries get written to.
     *  By default they are placed in the same directory as the {@code .class} file. */
    File outputDirectory = null;
    /** The name of the output generated source file or shared library. This enables single-
     *  file output mode. By default, the top-level enclosing classes get one file each. */
    String outputName = null;
    /** The name of the JAR file to create, if not {@code null}. */
    String jarPrefix = null;
    /** If true, deletes all files from {@link #outputDirectory} before writing anything in it. */
    boolean clean = false;
    /** If true, attempts to generate C++ JNI files, but if false, only attempts to parse header files. */
    boolean generate = true;
    /** If true, compiles the generated source file to a shared library and deletes source. */
    boolean compile = true;
    /** If true, preserves the generated C++ JNI files after compilation. */
    boolean deleteJniFiles = true;
    /** If true, also generates C++ header files containing declarations of callback functions. */
    boolean header = false;
    /** If true, also copies to the output directory dependent shared libraries (link and preload). */
    boolean copyLibs = false;
    /** If true, also copies to the output directory resources listed in properties. */
    boolean copyResources = false;
    /** Accumulates the various properties loaded from resources, files, command line options, etc. */
    Properties properties = null;
    /** The instance of the {@link ClassScanner} that fills up a {@link Collection} of {@link Class} objects to process. */
    ClassScanner classScanner = null;
    /** A system command for {@link ProcessBuilder} to execute for the build, instead of JavaCPP itself. */
    String[] buildCommand = null;
    /** User specified working directory to execute build subprocesses under. */
    File workingDirectory = null;
    /** User specified environment variables to pass to the native compiler. */
    Map<String,String> environmentVariables = null;
    /** Contains additional command line options from the user for the native compiler. */
    Collection<String> compilerOptions = null;

    /** Splits argument with {@link File#pathSeparator} and appends result to paths of the {@link #classScanner}. */
    public Builder classPaths(String classPaths) {
        classPaths(classPaths == null ? null : classPaths.split(File.pathSeparator));
        return this;
    }
    /** Appends argument to the paths of the {@link #classScanner}. */
    public Builder classPaths(String ... classPaths) {
        classScanner.getClassLoader().addPaths(classPaths);
        return this;
    }
    /** Sets the {@link #encoding} field to the argument. */
    public Builder encoding(String encoding) {
        this.encoding = encoding;
        return this;
    }
    /** Sets the {@link #outputDirectory} field to the argument. */
    public Builder outputDirectory(String outputDirectory) {
        outputDirectory(outputDirectory == null ? null : new File(outputDirectory));
        return this;
    }
    /** Sets the {@link #outputDirectory} field to the argument. */
    public Builder outputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }
    /** Sets the {@link #clean} field to the argument. */
    public Builder clean(boolean clean) {
        this.clean = clean;
        return this;
    }
    /** Sets the {@link #generate} field to the argument. */
    public Builder generate(boolean generate) {
        this.generate = generate;
        return this;
    }
    /** Sets the {@link #compile} field to the argument. */
    public Builder compile(boolean compile) {
        this.compile = compile;
        return this;
    }
    /** Sets the {@link #deleteJniFiles} field to the argument. */
    public Builder deleteJniFiles(boolean deleteJniFiles) {
        this.deleteJniFiles = deleteJniFiles;
        return this;
    }
    /** Sets the {@link #header} field to the argument. */
    public Builder header(boolean header) {
        this.header = header;
        return this;
    }
    /** Sets the {@link #copyLibs} field to the argument. */
    public Builder copyLibs(boolean copyLibs) {
        this.copyLibs = copyLibs;
        return this;
    }
    /** Sets the {@link #copyResources} field to the argument. */
    public Builder copyResources(boolean copyResources) {
        this.copyResources = copyResources;
        return this;
    }
    /** Sets the {@link #outputName} field to the argument. */
    public Builder outputName(String outputName) {
        this.outputName = outputName;
        return this;
    }
    /** Sets the {@link #jarPrefix} field to the argument. */
    public Builder jarPrefix(String jarPrefix) {
        this.jarPrefix = jarPrefix;
        return this;
    }
    /** Sets the {@link #properties} field to the ones loaded from resources for the specified platform. */
    public Builder properties(String platform) {
        if (platform != null) {
            properties = Loader.loadProperties(platform, null);
        }
        return this;
    }
    /** Adds all the properties of the argument to the {@link #properties} field. */
    public Builder properties(Properties properties) {
        if (properties != null) {
            for (Map.Entry e : properties.entrySet()) {
                property((String)e.getKey(), (String)e.getValue());
            }
        }
        return this;
    }
    /** Sets the {@link #properties} field to the ones loaded from the specified file. */
    public Builder propertyFile(String filename) throws IOException {
        propertyFile(filename == null ? null : new File(filename));
        return this;
    }
    /** Sets the {@link #properties} field to the ones loaded from the specified file. */
    public Builder propertyFile(File propertyFile) throws IOException {
        if (propertyFile == null) {
            return this;
        }
        FileInputStream fis = new FileInputStream(propertyFile);
        properties = new Properties();
        try {
            properties.load(new InputStreamReader(fis));
        } catch (NoSuchMethodError e) {
            properties.load(fis);
        }
        fis.close();
        return this;
    }
    /** Sets a property of the {@link #properties} field, in either "key=value" or "key:value" format. */
    public Builder property(String keyValue) {
        int equalIndex = keyValue.indexOf('=');
        if (equalIndex < 0) {
            equalIndex = keyValue.indexOf(':');
        }
        property(keyValue.substring(2, equalIndex),
                 keyValue.substring(equalIndex+1));
        return this;
    }
    /** Sets a key/value pair property of the {@link #properties} field. */
    public Builder property(String key, String value) {
        if (key.length() > 0 && value.length() > 0) {
            properties.put(key, value);
        }
        return this;
    }
    /** Requests the {@link #classScanner} to add a class or all classes from a package.
     *  A {@code null} argument indicates the unnamed package. */
    public Builder classesOrPackages(String ... classesOrPackages) throws IOException, ClassNotFoundException, NoClassDefFoundError {
        if (classesOrPackages == null) {
            classScanner.addPackage(null, true);
        } else for (String s : classesOrPackages) {
            classScanner.addClassOrPackage(s);
        }
        return this;
    }
    /** Sets the {@link #buildCommand} field to the argument. */
    public Builder buildCommand(String[] buildCommand) {
        this.buildCommand = buildCommand;
        return this;
    }
    /** Sets the {@link #workingDirectory} field to the argument. */
    public Builder workingDirectory(String workingDirectory) {
        workingDirectory(workingDirectory == null ? null : new File(workingDirectory));
        return this;
    }
    /** Sets the {@link #workingDirectory} field to the argument. */
    public Builder workingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }
    /** Sets the {@link #environmentVariables} field to the argument. */
    public Builder environmentVariables(Map<String,String> environmentVariables) {
        this.environmentVariables = environmentVariables;
        return this;
    }
    /** Appends arguments to the {@link #compilerOptions} field. */
    public Builder compilerOptions(String ... options) {
        if (options != null) {
            compilerOptions.addAll(Arrays.asList(options));
        }
        return this;
    }

    /**
     * Starts the build process and returns an array of {@link File} produced.
     *
     * @return the array of File produced
     * @throws IOException
     * @throws InterruptedException
     * @throws ParserException
     */
    public File[] build() throws IOException, InterruptedException, ParserException {
        if (buildCommand != null && buildCommand.length > 0) {
            List<String> command = Arrays.asList(buildCommand);
            String paths = properties.getProperty("platform.buildpath", "");
            String links = properties.getProperty("platform.linkresource", "");
            String resources = properties.getProperty("platform.buildresource", "");
            String separator = properties.getProperty("platform.path.separator");

            // Get all native libraries for classes on the class path.
            List<String> libs = new ArrayList<String>();
            ClassProperties libProperties = null;
            for (Class c : classScanner.getClasses()) {
                if (Loader.getEnclosingClass(c) != c) {
                    continue;
                }
                libProperties = Loader.loadProperties(c, properties, true);
                if (!libProperties.isLoaded()) {
                    logger.warn("Could not load platform properties for " + c);
                    continue;
                }
                libs.addAll(libProperties.get("platform.preload"));
                libs.addAll(libProperties.get("platform.link"));
            }
            if (libProperties == null) {
                libProperties = new ClassProperties(properties);
            }
            includeJavaPaths(libProperties, header);
            for (Map.Entry<String, List<String>> entry : libProperties.entrySet()) {
                String key = entry.getKey();
                key = key.toUpperCase().replace('.', '_');

                List<String> values = entry.getValue();
                String value = "";
                for (String s : values) {
                    value += value.length() > 0 && !value.endsWith(separator) ? separator + s : s;
                }
                environmentVariables.put(key, value);
            }

            paths = paths.replace(separator, File.pathSeparator);
            if (paths.length() > 0 || resources.length() > 0) {
                // Extract the required resources.
                for (String s : resources.split(separator)) {
                    for (File f : Loader.cacheResources(s)) {
                        String path = f.getCanonicalPath();
                        if (paths.length() > 0 && !paths.endsWith(File.pathSeparator)) {
                            paths += File.pathSeparator;
                        }
                        paths += path;

                        // Also create symbolic links for native libraries found there.
                        List<String> linkPaths = new ArrayList<String>();
                        for (String s2 : links.split(separator)) {
                            for (File f2 : Loader.cacheResources(s2)) {
                                String path2 = f2.getCanonicalPath();
                                if (path2.startsWith(path) && !path2.equals(path)) {
                                    linkPaths.add(path2);
                                }
                            }
                        }
                        File[] files = f.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                Loader.createLibraryLink(file.getAbsolutePath(), libProperties, null,
                                        linkPaths.toArray(new String[linkPaths.size()]));
                            }
                        }
                    }
                }
                if (paths.length() > 0) {
                    if (environmentVariables == null) {
                        environmentVariables = new LinkedHashMap<String,String>();
                    }
                    environmentVariables.put("BUILD_PATH", paths);
                    environmentVariables.put("BUILD_PATH_SEPARATOR", File.pathSeparator);
                }
            }
            int exitValue = executeCommand(command, workingDirectory, environmentVariables);
            if (exitValue != 0) {
                throw new RuntimeException("Process exited with an error: " + exitValue);
            }
            return null;
        }

        List<File> outputFiles = new ArrayList<File>();
        List<String> allNames = new ArrayList<String>();
        if (classScanner.getClasses().isEmpty()) {
            if (outputName != null && outputName.equals("jnijavacpp")) {
                // the user only wants the "jnijavacpp" library
                File[] files = generateAndCompile(null, outputName, true, true);
                if (files != null && files.length > 0) {
                    outputFiles.addAll(Arrays.asList(files));
                }
            } else {
                return null;
            }
        }

        Map<String, LinkedHashSet<Class>> executableMap = new HashMap<String, LinkedHashSet<Class>>();
        Map<String, LinkedHashSet<Class>> libraryMap = new HashMap<String, LinkedHashSet<Class>>();
        for (Class c : classScanner.getClasses()) {
            if (Loader.getEnclosingClass(c) != c) {
                continue;
            }
            // Do not inherit properties when parsing because it generates annotations itself
            ClassProperties p = Loader.loadProperties(c, properties, false);
            if (p.isLoaded()) {
                try {
                    if (Arrays.asList(c.getInterfaces()).contains(BuildEnabled.class)) {
                        ((BuildEnabled)c.newInstance()).init(logger, properties, encoding);
                    }
                } catch (ClassCastException | InstantiationException | IllegalAccessException e) {
                    // fail silently as if the interface wasn't implemented
                }
                String target = p.getProperty("global");
                if (target != null && !c.getName().equals(target)) {
                    boolean found = false;
                    for (Class c2 : classScanner.getClasses()) {
                        // do not try to regenerate classes that are already scheduled for C++ compilation
                        found |= c2.getName().equals(target);
                    }
                    if (!generate || !found) {
                        File[] files = parse(classScanner.getClassLoader().getPaths(), c);
                        if (files != null) {
                            outputFiles.addAll(Arrays.asList(files));
                        }
                    }
                    continue;
                }
            }
            if (!p.isLoaded()) {
                // Now try to inherit to generate C++ source files
                p = Loader.loadProperties(c, properties, true);
            }
            if (!p.isLoaded()) {
                logger.warn("Could not load platform properties for " + c);
                continue;
            }

            String executableName = p.getProperty("platform.executable");
            if (executableName != null && executableName.length() > 0) {
                LinkedHashSet<Class> classList = executableMap.get(executableName);
                if (classList == null) {
                    allNames.add(executableName);
                    executableMap.put(executableName, classList = new LinkedHashSet<Class>());
                }
                classList.addAll(p.getEffectiveClasses());
                continue;
            }

            String libraryName = outputName != null ? outputName : p.getProperty("platform.library", "");
            if (!generate || libraryName.length() == 0) {
                continue;
            }
            LinkedHashSet<Class> classList = libraryMap.get(libraryName);
            if (classList == null) {
                allNames.add(libraryName);
                libraryMap.put(libraryName, classList = new LinkedHashSet<Class>());
            }
            classList.addAll(p.getEffectiveClasses());
        }
        int count = 0;
        for (String name : allNames) {
            LinkedHashSet<Class> executableClassSet = executableMap.get(name);
            LinkedHashSet<Class> libraryClassSet = libraryMap.get(name);
            Class[] classArray = null;
            File[] files = null;
            if (executableClassSet != null) {
                classArray = executableClassSet.toArray(new Class[executableClassSet.size()]);
                ClassProperties p = Loader.loadProperties(classArray, properties, true);
                String prefix = p.getProperty("platform.executable.prefix", "");
                String suffix = p.getProperty("platform.executable.suffix", "");
                String filename = prefix + name + suffix;
                for (String path : p.get("platform.executablepath")) {
                    Path in = Paths.get(path, filename);
                    if (Files.exists(in)) {
                        logger.info("Copying " + in);
                        File outputPath = getOutputPath(classArray, null);
                        Path out = new File(outputPath, filename).toPath();
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                        files = new File[] { out.toFile() };
                        break;
                    }
                }
            } else if (libraryClassSet != null) {
                classArray = libraryClassSet.toArray(new Class[libraryClassSet.size()]);
                files = generateAndCompile(classArray, name, count == 0, count == libraryMap.size() - 1);
                count++;
            } else {
                continue;
            }

            if (files != null && files.length > 0) {
                // files[0] might be null if "jnijavacpp" was not generated and compiled
                File directory = files[files.length - 1].getParentFile();
                outputFiles.addAll(Arrays.asList(files));
                if (copyLibs) {
                    // Do not copy library files from inherit properties ...
                    ClassProperties p = Loader.loadProperties(classArray, properties, false);
                    List<String> preloads = new ArrayList<String>();
                    preloads.addAll(p.get("platform.preload"));
                    preloads.addAll(p.get("platform.link"));
                    // ... but we should try to use all the inherited paths!
                    ClassProperties p2 = Loader.loadProperties(classArray, properties, true);

                    for (String s : preloads) {
                        if (s.trim().endsWith("#") || s.trim().length() == 0) {
                            // the user specified an empty destination to skip the copy
                            continue;
                        }
                        URL[] urls = Loader.findLibrary(null, p, s);
                        File fi;
                        try {
                            fi = new File(new URI(urls[0].toURI().toString().split("#")[0]));
                        } catch (Exception e) {
                            // try with inherited paths as well
                            urls = Loader.findLibrary(null, p2, s);
                            try {
                                fi = new File(new URI(urls[0].toURI().toString().split("#")[0]));
                            } catch (Exception e2) {
                                logger.warn("Could not find library " + s);
                                continue;
                            }
                        }
                        File fo = new File(directory, fi.getName());
                        if (fi.exists() && !outputFiles.contains(fo)) {
                            logger.info("Copying " + fi);
                            Files.copy(fi.toPath(), fo.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            outputFiles.add(fo);
                        }
                    }
                }
                if (copyResources) {
                    // Do not copy resources from inherit properties ...
                    ClassProperties p = Loader.loadProperties(classArray, properties, false);
                    List<String> resources = p.get("platform.resource");
                    // ... but we should use all the inherited paths!
                    p = Loader.loadProperties(classArray, properties, true);
                    List<String> paths =  p.get("platform.resourcepath");

                    Path directoryPath = directory.toPath();
                    for (String resource : resources) {
                        final Path target = directoryPath.resolve(resource);
                        if (!Files.exists(target)) {
                            Files.createDirectories(target);
                        }
                        for (String path : paths) {
                            final Path source = Paths.get(path, resource);
                            if (Files.exists(source)) {
                                logger.info("Copying " + source);
                                Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                                    @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                        Path targetdir = target.resolve(source.relativize(dir));
                                        try {
                                            Files.copy(dir, targetdir, StandardCopyOption.REPLACE_EXISTING);
                                        } catch (DirectoryNotEmptyException | FileAlreadyExistsException e) {
                                             if (!Files.isDirectory(targetdir)) {
                                                 throw e;
                                             }
                                        }
                                        return FileVisitResult.CONTINUE;
                                    }
                                    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                        Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                                        return FileVisitResult.CONTINUE;
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        File[] files = outputFiles.toArray(new File[outputFiles.size()]);
        if (jarPrefix != null && files.length > 0) {
            File jarFile = new File(jarPrefix + "-" + properties.getProperty("platform") + properties.getProperty("platform.extension", "") + ".jar");
            File d = jarFile.getParentFile();
            if (d != null && !d.exists()) {
                d.mkdir();
            }
            createJar(jarFile, outputDirectory == null ? classScanner.getClassLoader().getPaths() : null, files);
        }

        // reset the load flag to let users load compiled libraries
        System.setProperty("org.bytedeco.javacpp.loadlibraries", "true");
        return files;
    }

    /**
     * Simply prints out to the display the command line usage.
     */
    public static void printHelp() {
        String version = Builder.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "unknown";
        }
        System.out.println(
            "JavaCPP version " + version + "\n" +
            "Copyright (C) 2011-2020 Samuel Audet <samuel.audet@gmail.com>\n" +
            "Project site: https://github.com/bytedeco/javacpp");
        System.out.println();
        System.out.println("Usage: java -jar javacpp.jar [options] [class or package (suffixed with .* or .**)] [commands]");
        System.out.println();
        System.out.println("where options include:");
        System.out.println();
        System.out.println("    -classpath <path>      Load user classes from path");
        System.out.println("    -encoding <name>       Character encoding used for input and output files");
        System.out.println("    -d <directory>         Output all generated files to directory");
        System.out.println("    -o <name>              Output everything in a file named after given name");
        System.out.println("    -clean                 Delete the output directory before generating anything in it");
        System.out.println("    -nogenerate            Do not try to generate C++ source files, only try to parse header files");
        System.out.println("    -nocompile             Do not compile or delete the generated C++ source files");
        System.out.println("    -nodelete              Do not delete generated C++ JNI files after compilation");
        System.out.println("    -header                Generate header file with declarations of callbacks functions");
        System.out.println("    -copylibs              Copy to output directory dependent libraries (link and preload)");
        System.out.println("    -copyresources         Copy to output directory resources listed in properties");
        System.out.println("    -jarprefix <prefix>    Also create a JAR file named \"<prefix>-<platform>.jar\"");
        System.out.println("    -properties <resource> Load all platform properties from resource");
        System.out.println("    -propertyfile <file>   Load all platform properties from file");
        System.out.println("    -D<property>=<value>   Set platform property to value");
        System.out.println("    -Xcompiler <option>    Pass option directly to compiler");
        System.out.println();
        System.out.println("and where optional commands include:");
        System.out.println();
        System.out.println("    -mod <file>            Output a module-info.java file for native JAR where module name is the package of the first class");
        System.out.println("    -exec [args...]        After build, call java command on the first class");
        System.out.println("    -print <property>      Print the given platform property, for example, \"platform.includepath\", and exit");
        System.out.println("                           \"platform.includepath\" has jni.h, jni_md.h, etc, and \"platform.linkpath\", the jvm library");
        System.out.println();
    }

    /**
     * The terminal shell interface to the Builder.
     *
     * @param args an array of arguments as described by {@link #printHelp()}
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        boolean addedClasses = false;
        Builder builder = new Builder();
        String[] execArgs = null;
        String moduleFile = null;
        String printPath = null;
        for (int i = 0; i < args.length; i++) {
            if ("-help".equals(args[i]) || "--help".equals(args[i])) {
                printHelp();
                System.exit(0);
            } else if ("-classpath".equals(args[i]) || "-cp".equals(args[i]) || "-lib".equals(args[i])) {
                builder.classPaths(args[++i]);
            } else if ("-encoding".equals(args[i])) {
                builder.encoding(args[++i]);
            } else if ("-d".equals(args[i])) {
                builder.outputDirectory(args[++i]);
            } else if ("-o".equals(args[i])) {
                builder.outputName(args[++i]);
            } else if ("-clean".equals(args[i])) {
                builder.clean(true);
            } else if ("-nocpp".equals(args[i]) || "-nogenerate".equals(args[i])) {
                builder.generate(false);
            } else if ("-cpp".equals(args[i]) || "-nocompile".equals(args[i])) {
                builder.compile(false);
            } else if ("-nodelete".equals(args[i])) {
                builder.deleteJniFiles(false);
            } else if ("-header".equals(args[i])) {
                builder.header(true);
            } else if ("-copylibs".equals(args[i])) {
                builder.copyLibs(true);
            } else if ("-copyresources".equals(args[i])) {
                builder.copyResources(true);
            } else if ("-jarprefix".equals(args[i])) {
                builder.jarPrefix(args[++i]);
            } else if ("-properties".equals(args[i])) {
                builder.properties(args[++i]);
            } else if ("-propertyfile".equals(args[i])) {
                builder.propertyFile(args[++i]);
            } else if (args[i].startsWith("-D")) {
                builder.property(args[i]);
            } else if ("-Xcompiler".equals(args[i])) {
                builder.compilerOptions(args[++i]);
            } else if ("-mod".equals(args[i])) {
                moduleFile = args[++i];
            } else if ("-exec".equals(args[i])) {
                execArgs = Arrays.copyOfRange(args, i + 1, args.length);
                i = args.length;
            } else if ("-print".equals(args[i])) {
                printPath = args[++i];
            } else if (args[i].startsWith("-")) {
                builder.logger.error("Invalid option \"" + args[i] + "\"");
                printHelp();
                System.exit(1);
            } else {
                String arg = args[i];
                if (arg.endsWith(".java")) {
                    // We got a source file instead, let's try to compile it first
                    ArrayList<String> command = new ArrayList<String>(Arrays.asList("javac", "-cp"));
                    String paths = System.getProperty("java.class.path");
                    for (String path : builder.classScanner.getClassLoader().getPaths()) {
                        paths += File.pathSeparator + path;
                    }
                    command.add(paths);
                    command.add(arg);
                    int exitValue = builder.executeCommand(command, builder.workingDirectory, builder.environmentVariables);
                    if (exitValue != 0) {
                        throw new RuntimeException("Could not compile " + arg + ": " + exitValue);
                    }
                    arg = arg.replace(File.separatorChar, '.').replace('/', '.').substring(0, arg.length() - 5);
                }
                builder.classesOrPackages(arg);
                addedClasses = true;
            }
        }
        if (printPath != null) {
            Collection<Class> classes = builder.classScanner.getClasses();
            ClassProperties p = Loader.loadProperties(classes.toArray(new Class[classes.size()]), builder.properties, true);
            builder.includeJavaPaths(p, builder.header);
            for (String s : p.get(printPath)) {
                System.out.println(s);
            }
            System.exit(0);
        } else if (!addedClasses) {
            printHelp();
            System.exit(2);
        }
        File[] outputFiles = builder.build();
        Collection<Class> classes = builder.classScanner.getClasses();
        if (moduleFile != null) {
            Class c = classes.iterator().next();
            String pkg = c.getPackage().getName();
            String s = "open module " + pkg + "." + builder.properties.getProperty("platform").replace('-', '.') + " {\n"
                     + "  requires transitive " + pkg + ";\n"
                     + "}\n";
            Path f = Paths.get(moduleFile);
            Path d = f.getParent();
            if (d != null) {
                Files.createDirectories(d);
            }
            Files.write(f, s.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (outputFiles != null && outputFiles.length > 0 && !classes.isEmpty() && execArgs != null) {
            Class c = classes.iterator().next();
            ArrayList<String> command = new ArrayList<String>(Arrays.asList("java", "-cp"));
            String paths = System.getProperty("java.class.path");
            for (String path : builder.classScanner.getClassLoader().getPaths()) {
                paths += File.pathSeparator + path;
            }
            command.add(paths);
            command.add(c.getCanonicalName());
            command.addAll(Arrays.asList(execArgs));
            System.exit(builder.executeCommand(command, builder.workingDirectory, builder.environmentVariables));
        }
    }
}
