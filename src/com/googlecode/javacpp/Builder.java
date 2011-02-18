/*
 * Copyright (C) 2011 Samuel Audet
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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
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
                }
            }
        }
        if (jnipath[0] != null && jnipath[0].equals(jnipath[1])) {
            jnipath[1] = null;
        }
        Loader.appendProperty(properties, "compiler.includepath", File.pathSeparator, jnipath);
    }

    private Properties properties;

    public String mapLibraryName(String libname) {
        return properties.getProperty("library.prefix", "") + libname +
               properties.getProperty("library.suffix", "");
    }

    public int build(String sourceFilename, String outputFilename)
            throws IOException, InterruptedException {
        LinkedList<String> command = new LinkedList<String>();
        command.add(properties.getProperty("compiler.path"));
        String includepath = properties.getProperty("compiler.includepath");
        if (includepath != null && includepath.length() > 0) {
            for (String s : includepath.split(File.pathSeparator)) {
                if (new File(s).isDirectory()) {
                    command.add(properties.getProperty("compiler.includepath.prefix", "") + s);
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
        if (outputPrefix == null || outputPrefix.length() == 0 ||
                outputPrefix.charAt(outputPrefix.length()-1) == ' ') {
            command.add(outputFilename);
        } else {
            command.add(command.removeLast() + outputFilename);
        }
        String linkpath = properties.getProperty("compiler.linkpath");
        if (linkpath != null && linkpath.length() > 0) {
            for (String s : linkpath.split(File.pathSeparator)) {
                if (new File(s).isDirectory()) {
                    command.add(properties.getProperty("compiler.linkpath.prefix", "") + s);
                }
            }
        }
        String link = properties.getProperty("compiler.link");
        if (link != null && link.length() > 0) {
            for (String s : link.split(File.pathSeparator)) {
                command.add(properties.getProperty("compiler.link.prefix", "") + s +
                            properties.getProperty("compiler.link.suffix", ""));
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


    static void printHelp() {
        System.err.println("Usage: java -jar javacpp.jar [options] <classes>");
        System.err.println();
        System.err.println("where options include:");
        System.err.println();
        System.err.println("    -classpath <path>      Load user classes from path");
        System.err.println("    -d <directory>         Dump all output files in directory");
        System.err.println("    -cpp                   Do not build or delete the generated .cpp files");
        System.err.println("    -o <name>              Output everything in a file named after given name");
        System.err.println("    -jarprefix <prefix>    Move everything to jar file named \"<prefix>-<platform.name>.jar\"");
        System.err.println("    -properties <resource> Load all properties from resource");
        System.err.println("    -propertyfile <file>   Load all properties from file");
        System.err.println("    -D<property>=<value>   Set property to value");
        System.err.println();
    }

    static void generateAndBuild(Class[] classes, Properties properties, File outputDirectory,
            String outputName, boolean build, LinkedList<File> outputFiles)
            throws IOException, InterruptedException, URISyntaxException {
        properties = (Properties)properties.clone();
        for (Class c : classes) {
            Loader.appendProperties(properties, c);
        }
        File sourceFile;
        if (outputDirectory == null) {
            if (classes.length == 1) {
                URL resourceURL = classes[0].getResource(classes[0].getSimpleName() + ".class");
                File packageDir = new File(resourceURL.toURI()).getParentFile();
                outputDirectory = new File(packageDir, properties.getProperty("platform.name"));
                sourceFile      = new File(packageDir, outputName + ".cpp");
            } else {
                outputDirectory = new File(properties.getProperty("platform.name"));
                sourceFile      = new File(outputName + ".cpp");
            }
        } else {
            sourceFile = new File(outputDirectory, outputName + ".cpp");
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
    }

    static void createJar(File jarFile, String[] classpath, LinkedList<File> files) throws IOException {
        System.out.println("Creating jar file: " + jarFile);
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
        for (File f : files) {
            String name = f.getPath();
            if (classpath != null) {
                // Store only the path relative to the classpath so that
                // our Loader may use the package name of the associated
                // class to get the file as a resource from the ClassLoader
                for (String path : classpath) {
                    path = new File(path).getCanonicalPath();
                    if (name.startsWith(path)) {
                        name = name.substring(path.length() + 1);
                    }
                }
            }
            ZipEntry e = new ZipEntry(name);
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
            f.delete();
            f.getParentFile().delete();
        }
        jos.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }

        String[] classpath = { System.getProperty("user.dir") };
        URL url = new File(classpath[0]).toURI().toURL();
        ClassLoader classLoader = new URLClassLoader(new URL[] { url });
        File outputDirectory = null;
        String outputName = null, jarPrefix = null;
        boolean build = true;
        Properties properties = Loader.getProperties();
        LinkedList<Class> classes = new LinkedList<Class>();

        for (int i = 0; i < args.length; i++) {
            if ("-help".equals(args[i]) || "--help".equals(args[i])) {
                printHelp();
                System.exit(0);
            } else if ("-classpath".equals(args[i]) || "-cp".equals(args[i]) || "-lib".equals(args[i])) {
                classpath = args[++i].split(File.pathSeparator);
                URL[] urls = new URL[classpath.length];
                for (int j = 0; j < classpath.length; j++) {
                    urls[j] = new File(classpath[j]).toURI().toURL();
                }
                classLoader = new URLClassLoader(urls);
            } else if ("-d".equals(args[i])) {
                outputDirectory = new File(args[++i]);
            } else if ("-cpp".equals(args[i])) {
                build = false;
            } else if ("-o".equals(args[i])) {
                outputName = args[++i];
            } else if ("-jarprefix".equals(args[i])) {
                jarPrefix = args[++i];
            } else if ("-properties".equals(args[i])) {
                properties = Loader.getProperties(args[++i]);
            } else if ("-propertyfile".equals(args[i])) {
                FileInputStream fis = new FileInputStream(args[++i]);
                properties = new Properties(properties);
                try {
                    properties.load(new InputStreamReader(fis));
                } catch (NoSuchMethodError e) {
                    properties.load(fis);
                }
                fis.close();
            } else if (args[i].startsWith("-D")) {
                int equalIndex = args[i].indexOf('=');
                if (equalIndex < 0) {
                    equalIndex = args[i].indexOf(':');
                }
                String key = args[i].substring(2, equalIndex);
                String value = args[i].substring(equalIndex+1);
                if (key.length() > 0 && value.length() > 0) {
                    properties.put(key, value);
                }
            } else if (args[i].startsWith("-")) {
                System.err.println("Invalid option: " + args[i]);
                printHelp();
                System.exit(1);
            } else {
                Loader.loadLibraries = false;
                String className = args[i].replace('/', '.');
                if (className.endsWith(".class")) {
                    className = className.substring(0, className.length()-6);
                }
                try {
                    Class c = Class.forName(className, true, classLoader);
                    classes.add(c);
                } catch (ClassNotFoundException e) {
                    System.err.println("Class not found: " + className);
                    System.exit(1);
                }
            }
        }

        LinkedList<File> outputFiles = new LinkedList<File>();
        if (outputName == null) {
            for (Class c : classes) {
                generateAndBuild(new Class[] { c }, properties,
                        outputDirectory, Loader.getLibraryName(c), build, outputFiles);
            }
        } else {
            generateAndBuild(classes.toArray(new Class[0]), properties,
                    outputDirectory, outputName, build, outputFiles);
        }

        if (jarPrefix != null && !outputFiles.isEmpty()) {
            File jarFile = new File(jarPrefix + "-" + properties.get("platform.name") + ".jar");
            File d = jarFile.getParentFile();
            if (!d.exists()) {
                d.mkdir();
            }
            createJar(jarFile, outputDirectory == null ? classpath : null, outputFiles);
        }
    }
}
