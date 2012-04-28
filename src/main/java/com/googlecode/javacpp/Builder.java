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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 *
 * @author Samuel Audet
 */
public class Builder {
    public Builder(Properties properties) {
        this.properties = properties;

        // try to find include paths for jni.h and jni_md.h automatically
        final String[] jnipath = new String[2];
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (new File(dir, "jni.h").exists()) {
                    jnipath[0] = dir.getAbsolutePath();
                }
                if (new File(dir, "jni_md.h").exists()) {
                    jnipath[1] = dir.getAbsolutePath();
                }
                return new File(dir, name).isDirectory();
            }
        };
        File javaHome = new File(System.getProperty("java.home"));
        for (File f : javaHome.getParentFile().listFiles(filter)) {
            for (File f2 : f.listFiles(filter)) {
                for (File f3 : f2.listFiles(filter)) {
                    for (File f4 : f3.listFiles(filter)) {
                    }
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
        Loader.appendProperty(properties, "compiler.includepath", 
                properties.getProperty("path.separator"), jnipath);
    }

    private Properties properties;

    public String mapLibraryName(String libname) {
        return properties.getProperty("library.prefix", "") + libname +
               properties.getProperty("library.suffix", "");
    }

    public int build(String sourceFilename, String outputFilename)
            throws IOException, InterruptedException {
        LinkedList<String> command = new LinkedList<String>();

        String pathSeparator = properties.getProperty("path.separator");
        String platformRoot  = properties.getProperty("platform.root");
        if (platformRoot == null || platformRoot.length() == 0) {
            platformRoot = ".";
        }
        if (!platformRoot.endsWith(File.separator)) {
            platformRoot += File.separator;
        }

        String compilerPath = properties.getProperty("compiler.path");
        if (platformRoot != null && !new File(compilerPath).isAbsolute() &&
                new File(platformRoot + compilerPath).exists()) {
            compilerPath = platformRoot + compilerPath;
        }
        command.add(compilerPath);

        String sysroot = properties.getProperty("compiler.sysroot");
        if (sysroot != null && sysroot.length() > 0) {
            String p = properties.getProperty("compiler.sysroot.prefix", "");
            for (String s : sysroot.split(pathSeparator)) {
                if (platformRoot != null && !new File(s).isAbsolute()) {
                    s = platformRoot + s;
                }
                if (new File(s).isDirectory()) {
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
        }

        String includepath = properties.getProperty("compiler.includepath");
        if (includepath != null && includepath.length() > 0) {
            String p = properties.getProperty("compiler.includepath.prefix", "");
            for (String s : includepath.split(pathSeparator)) {
                if (platformRoot != null && !new File(s).isAbsolute()) {
                    s = platformRoot + s;
                }
                if (new File(s).isDirectory()) {
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
        }

        command.add(sourceFilename);

        String options = properties.getProperty("compiler.options");
        if (options != null && options.length() > 0) {
            command.addAll(Arrays.asList(options.split(" ")));
        }

        String outputPrefix = properties.getProperty("compiler.output.prefix");
        if (outputPrefix != null && outputPrefix.length() > 0) {
            command.addAll(Arrays.asList(outputPrefix.split(" ")));
        }

        if (outputPrefix == null || outputPrefix.length() == 0 || outputPrefix.endsWith(" ")) {
            command.add(outputFilename);
        } else {
            command.add(command.removeLast() + outputFilename);
        }

        String linkpath = properties.getProperty("compiler.linkpath");
        if (linkpath != null && linkpath.length() > 0) {
            String p  = properties.getProperty("compiler.linkpath.prefix", "");
            String p2 = properties.getProperty("compiler.linkpath.prefix2");
            for (String s : linkpath.split(pathSeparator)) {
                if (platformRoot != null && !new File(s).isAbsolute()) {
                    s = platformRoot + s;
                }
                if (new File(s).isDirectory()) {
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
        }

        String link = properties.getProperty("compiler.link");
        if (link != null && link.length() > 0) {
            String p = properties.getProperty("compiler.link.prefix", "");
            String x = properties.getProperty("compiler.link.suffix", "");
            for (String s : link.split(pathSeparator)) {
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

        String framework = properties.getProperty("compiler.framework");
        if (framework != null && framework.length() > 0) {
            String p = properties.getProperty("compiler.framework.prefix", "");
            String x = properties.getProperty("compiler.framework.suffix", "");
            for (String s : framework.split(pathSeparator)) {
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

        for (String s : command) {
            boolean hasSpaces = s.indexOf(" ") > 0;
            if (hasSpaces) {
                System.out.print("\"");
            }
            System.out.print(s);
            if (hasSpaces) {
                System.out.print("\"");
            }
            System.out.print(" ");
        }
        System.out.println();

        Process p = new ProcessBuilder(command).start();
        new Piper(p.getErrorStream(), System.err).start();
        new Piper(p.getInputStream(), System.out).start();
        return p.waitFor();
    }

    public static class UserClassLoader extends URLClassLoader {
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
                this.paths.add(path);
                try {
                    addURL(new File(path).toURI().toURL());
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

    public static class ClassScanner {
        public ClassScanner(Collection<Class> classes, UserClassLoader loader) {
            this.classes = classes;
            this.loader  = loader;
        }

        private Collection<Class> classes;
        private UserClassLoader loader;

        public void addClass(String className) {
            if (className == null || className.indexOf('$') > 0) {
                // skip nested classes
                return;
            } else if (className.endsWith(".class")) {
                className = className.substring(0, className.length()-6);
            }
            try {
                Class c = Class.forName(className, true, loader);
                if (!classes.contains(c)) {
                    classes.add(c);
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Warning: Could not find class " + className + ": " + e);
            } catch (NoClassDefFoundError e) {
                System.err.println("Warning: Could not load class " + className + ": " + e);
            }
        }

        public void addMatchingFile(String filename, String packagePath, boolean recursive) {
            if (filename != null && filename.endsWith(".class") && filename.indexOf('$') < 0 &&
                    (packagePath == null || (recursive && filename.startsWith(packagePath)) ||
                    filename.regionMatches(0, packagePath, 0, Math.max(filename.lastIndexOf('/'), packagePath.lastIndexOf('/'))))) {
                addClass(filename.replace('/', '.'));
            }
        }

        public void addMatchingDir(String parentName, File dir, String packagePath, boolean recursive) {
            for (File f : dir.listFiles()) {
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
            if (prevSize == classes.size()) {
                if (packageName == null) {
                    System.err.println("Warning: No classes found in the unnamed package");
                    printHelp();
                } else {
                    System.err.println("Warning: No classes found in package " + packageName);
                }
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

    public static class Main {
        public Main() {
            Loader.loadLibraries = false;
            this.classLoader = new UserClassLoader(Thread.currentThread().getContextClassLoader());
            this.properties = Loader.getProperties();
            this.classes = new LinkedList<Class>();
            this.classScanner = new ClassScanner(classes, classLoader);
        }

        UserClassLoader classLoader = null;
        File outputDirectory = null;
        String outputName = null, jarPrefix = null;
        boolean compile = true;
        Properties properties = null;
        LinkedList<Class> classes = null;
        ClassScanner classScanner = null;

        public void setClassPaths(String classPaths) {
            setClassPaths(classPaths == null ? null : classPaths.split(File.pathSeparator));
        }
        public void setClassPaths(String ... classPaths) {
            classLoader.addPaths(classPaths);
        }
        public void setOutputDirectory(String outputDirectory) {
            setOutputDirectory(outputDirectory == null ? null : new File(outputDirectory));
        }
        public void setOutputDirectory(File outputDirectory) {
            this.outputDirectory = outputDirectory;
        }
        public void setCompile(boolean compile) {
            this.compile = compile;
        }
        public void setOutputName(String outputName) {
            this.outputName = outputName;
        }
        public void setJarPrefix(String jarPrefix) {
            this.jarPrefix = jarPrefix;
        }
        public void setProperties(String properties) {
            setProperties(properties == null ? null : Loader.getProperties(properties));
        }
        public void setProperties(Properties properties) {
            if (properties != null) {
                this.properties.putAll(properties);
            }
        }
        public void setPropertyFile(String propertyFile) throws IOException {
            setPropertyFile(propertyFile == null ? null : new File(propertyFile));
        }
        public void setPropertyFile(File propertyFile) throws IOException {
            if (propertyFile == null) {
                return;
            }
            FileInputStream fis = new FileInputStream(propertyFile);
            properties = new Properties(properties);
            try {
                properties.load(new InputStreamReader(fis));
            } catch (NoSuchMethodError e) {
                properties.load(fis);
            }
            fis.close();
        }
        public void setProperty(String keyValue) {
            int equalIndex = keyValue.indexOf('=');
            if (equalIndex < 0) {
                equalIndex = keyValue.indexOf(':');
            }
            setProperty(keyValue.substring(2, equalIndex),
                        keyValue.substring(equalIndex+1));
        }
        public void setProperty(String key, String value) {
            if (key.length() > 0 && value.length() > 0) {
                properties.put(key, value);
            }
        }
        public void setClassesOrPackages(String ... classesOrPackages) throws IOException {
            for (String s : classesOrPackages) {
                classScanner.addClassOrPackage(s);
            }
        }

        public static LinkedList<File> generateAndBuild(Class[] classes, Properties properties, File outputDirectory,
                String outputName, boolean build) throws IOException, InterruptedException {
            LinkedList<File> outputFiles = new LinkedList<File>();
            properties = (Properties)properties.clone();
            for (Class c : classes) {
                Loader.appendProperties(properties, c);
            }
            File sourceFile;
            if (outputDirectory == null) {
                if (classes.length == 1) {
                    try {
                        URL resourceURL = classes[0].getResource(classes[0].getSimpleName() + ".class");
                        File packageDir = new File(resourceURL.toURI()).getParentFile();
                        outputDirectory = new File(packageDir, properties.getProperty("platform.name"));
                        sourceFile      = new File(packageDir, outputName + properties.getProperty("source.suffix", ".cpp"));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    outputDirectory = new File(properties.getProperty("platform.name"));
                    sourceFile      = new File(outputName + properties.getProperty("source.suffix", ".cpp"));
                }
            } else {
                sourceFile = new File(outputDirectory, outputName + properties.getProperty("source.suffix", ".cpp"));
            }
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }
            System.out.println("Generating source file: " + sourceFile);
            Generator generator = new Generator(properties, sourceFile);
            boolean generatedSomething = generator.generate(classes);
            generator.close();

            if (generatedSomething) {
                if (build) {
                    Builder builder = new Builder(properties);
                    File libraryFile = new File(outputDirectory, builder.mapLibraryName(outputName));
                    System.out.println("Building library file: " + libraryFile);
                    int exitValue = builder.build(sourceFile.getPath(), libraryFile.getPath());
                    if (exitValue == 0) {
                        sourceFile.delete();
                        outputFiles.add(libraryFile);
                    } else {
                        System.exit(exitValue);
                    }
                } else {
                    outputFiles.add(sourceFile);
                }
            } else {
                System.out.println("No need to generate source file: " + sourceFile);
            }
            return outputFiles;
        }

        public static void createJar(File jarFile, String[] classpath, LinkedList<File> files) throws IOException {
            System.out.println("Creating JAR file: " + jarFile);
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
            for (File f : files) {
                String name = f.getPath();
                if (classpath != null) {
                    // Store only the path relative to the classpath so that
                    // our Loader may use the package name of the associated
                    // class to get the file as a resource from the ClassLoader.
                    String[] names = new String[classpath.length];
                    for (int i = 0; i < classpath.length; i++) {
                        String path = new File(classpath[i]).getCanonicalPath();
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
                byte[] data = new byte[fis.available()];
                int n;
                while ((n = fis.read(data)) > 0) {
                    jos.write(data, 0, n);
                }
                fis.close();
                jos.closeEntry();
    //            f.delete();
    //            f.getParentFile().delete();
            }
            jos.close();
        }

        public void build() throws IOException, InterruptedException {
            if (classes.isEmpty()) {
                classScanner.addPackage(null, true);
            }

            LinkedList<File> outputFiles;
            if (outputName == null) {
                outputFiles = new LinkedList<File>();
                for (Class c : classes) {
                    outputFiles.addAll(generateAndBuild(new Class[] { c }, properties,
                            outputDirectory, Loader.getLibraryName(c), compile));
                }
            } else {
                outputFiles = generateAndBuild(classes.toArray(new Class[classes.size()]),
                        properties, outputDirectory, outputName, compile);
            }

            if (jarPrefix != null && !outputFiles.isEmpty()) {
                File jarFile = new File(jarPrefix + "-" + properties.get("platform.name") + ".jar");
                File d = jarFile.getParentFile();
                if (d != null && !d.exists()) {
                    d.mkdir();
                }
                createJar(jarFile, outputDirectory == null ? classLoader.getPaths() : null, outputFiles);
            }
        }
    }

    public static void printHelp() {
        String timestamp = Builder.class.getPackage().getImplementationVersion();
        if (timestamp == null) {
            timestamp = "unknown";
        }
        System.out.println(
            "JavaCPP build timestamp " + timestamp + "\n" +
            "Copyright (C) 2011-2012 Samuel Audet <samuel.audet@gmail.com>\n" +
            "Project site: http://code.google.com/p/javacpp/\n\n" +

            "Licensed under the GNU General Public License version 2 (GPLv2) with Classpath exception.\n" +
            "Please refer to LICENSE.txt or http://www.gnu.org/licenses/ for details.");
        System.out.println();
        System.out.println("Usage: java -jar javacpp.jar [options] [class or package names]");
        System.out.println();
        System.out.println("where options include:");
        System.out.println();
        System.out.println("    -classpath <path>      Load user classes from path");
        System.out.println("    -d <directory>         Output all generated files to directory");
        System.out.println("    -o <name>              Output everything in a file named after given name");
        System.out.println("    -nocompile             Do not compile or delete the generated source files");
        System.out.println("    -jarprefix <prefix>    Also create a JAR file named \"<prefix>-<platform.name>.jar\"");
        System.out.println("    -properties <resource> Load all properties from resource");
        System.out.println("    -propertyfile <file>   Load all properties from file");
        System.out.println("    -D<property>=<value>   Set property to value");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        for (int i = 0; i < args.length; i++) {
            if ("-help".equals(args[i]) || "--help".equals(args[i])) {
                printHelp();
                System.exit(0);
            } else if ("-classpath".equals(args[i]) || "-cp".equals(args[i]) || "-lib".equals(args[i])) {
                main.setClassPaths(args[++i]);
            } else if ("-d".equals(args[i])) {
                main.setOutputDirectory(args[++i]);
            } else if ("-o".equals(args[i])) {
                main.setOutputName(args[++i]);
            } else if ("-cpp".equals(args[i]) || "-nocompile".equals(args[i])) {
                main.setCompile(false);
            } else if ("-jarprefix".equals(args[i])) {
                main.setJarPrefix(args[++i]);
            } else if ("-properties".equals(args[i])) {
                main.setProperties(args[++i]);
            } else if ("-propertyfile".equals(args[i])) {
                main.setPropertyFile(args[++i]);
            } else if (args[i].startsWith("-D")) {
                main.setProperty(args[i]);
            } else if (args[i].startsWith("-")) {
                System.err.println("Error: Invalid option \"" + args[i] + "\"");
                printHelp();
                System.exit(1);
            } else {
                main.setClassesOrPackages(args[i]);
            }
        }
        main.build();
    }
}
