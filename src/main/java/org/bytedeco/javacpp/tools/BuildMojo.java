/*
 * Copyright (C) 2012-2018 Arnaud Nauwynck, Samuel Audet
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
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.bytedeco.javacpp.Loader;

/**
 * A Maven Mojo to call the {@link Builder} (C++ header file -> Java class -> C++ JNI -> native library).
 * Can also be considered as an example of how to use the Builder programmatically.
 *
 * @author Arnaud Nauwynck
 * @author Samuel Audet
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public class BuildMojo extends AbstractMojo {

    /** Load user classes from classPath. */
    @Parameter(property = "javacpp.classPath", defaultValue = "${project.build.outputDirectory}")
    String classPath = null;

    /** Load user classes from classPaths. */
    @Parameter(property = "javacpp.classPaths")
    String[] classPaths = null;

    /** Add the path to the "platform.includepath" property. */
    @Parameter(property = "javacpp.includePath")
    String includePath = null;

    /** Add the paths to the "platform.includepath" property. */
    @Parameter(property = "javacpp.includePaths")
    String[] includePaths = null;

    /** Add the path to the "platform.includeresource" property. */
    @Parameter(property = "javacpp.includeResource")
    String includeResource = null;

    /** Add the paths to the "platform.includeresource" property. */
    @Parameter(property = "javacpp.includeResources")
    String[] includeResources = null;

    /** Add the path to the "platform.buildpath" property. */
    @Parameter(property = "javacpp.buildPath")
    String buildPath = null;

    /** Add the paths to the "platform.buildpath" property. */
    @Parameter(property = "javacpp.buildPaths")
    String[] buildPaths = null;

    /** Add the path to the "platform.buildresource" property. */
    @Parameter(property = "javacpp.buildResource")
    String buildResource = null;

    /** Add the paths to the "platform.buildresource" property. */
    @Parameter(property = "javacpp.buildResources")
    String[] buildResources = null;

    /** Add the path to the "platform.linkpath" property. */
    @Parameter(property = "javacpp.linkPath")
    String linkPath = null;

    /** Add the paths to the "platform.linkpath" property. */
    @Parameter(property = "javacpp.linkPaths")
    String[] linkPaths = null;

    /** Add the path to the "platform.linkresource" property. */
    @Parameter(property = "javacpp.linkResource")
    String linkResource = null;

    /** Add the paths to the "platform.linkresource" property. */
    @Parameter(property = "javacpp.linkResources")
    String[] linkResources = null;

    /** Add the path to the "platform.preloadpath" property. */
    @Parameter(property = "javacpp.preloadPath")
    String preloadPath = null;

    /** Add the paths to the "platform.preloadpath" property. */
    @Parameter(property = "javacpp.preloadPaths")
    String[] preloadPaths = null;

    /** Add the path to the "platform.preloadresource" property. */
    @Parameter(property = "javacpp.preloadResource")
    String preloadResource = null;

    /** Add the paths to the "platform.preloadresource" property. */
    @Parameter(property = "javacpp.preloadResources")
    String[] preloadResources = null;

    /** Add the path to the "platform.resourcepath" property. */
    @Parameter(property = "javacpp.resourcePath")
    String resourcePath = null;

    /** Add the paths to the "platform.resourcepath" property. */
    @Parameter(property = "javacpp.resourcePaths")
    String[] resourcePaths = null;

    /** Add the path to the "platform.executablepath" property. */
    @Parameter(property = "javacpp.executablePath")
    String executablePath = null;

    /** Add the paths to the "platform.executablepath" property. */
    @Parameter(property = "javacpp.executablePaths")
    String[] executablePaths = null;

    /** Specify the character encoding used for input and output. */
    @Parameter(property = "javacpp.encoding")
    String encoding = null;

    /** Output all generated files to outputDirectory. */
    @Parameter(property = "javacpp.outputDirectory")
    File outputDirectory = null;

    /** Output everything in a file named after given outputName. */
    @Parameter(property = "javacpp.outputName")
    String outputName = null;

    /** Delete all files from {@link #outputDirectory} before generating anything in it. */
    @Parameter(property = "javacpp.clean", defaultValue = "false")
    boolean clean = false;

    /** Generate .cpp files from Java interfaces if found, parsing from header files if not. */
    @Parameter(property = "javacpp.generate", defaultValue = "true")
    boolean generate = true;

    /** Compile and delete the generated .cpp files. */
    @Parameter(property = "javacpp.compile", defaultValue = "true")
    boolean compile = true;

    /** Delete generated C++ JNI files after compilation */
    @Parameter(property = "javacpp.deleteJniFiles", defaultValue = "true")
    boolean deleteJniFiles = true;

    /** Generate header file with declarations of callbacks functions. */
    @Parameter(property = "javacpp.header", defaultValue = "false")
    boolean header = false;

    /** Copy to output directory dependent libraries (link and preload). */
    @Parameter(property = "javacpp.copyLibs", defaultValue = "false")
    boolean copyLibs = false;

    /** Copy to output directory resources listed in properties. */
    @Parameter(property = "javacpp.copyResources", defaultValue = "false")
    boolean copyResources = false;

    /** Also create config files for GraalVM native-image in directory. */
    @Parameter(property = "javacpp.configDirectory")
    String configDirectory = null;

    /** Also create a JAR file named {@code <jarPrefix>-<platform>.jar}. */
    @Parameter(property = "javacpp.jarPrefix")
    String jarPrefix = null;

    /** Load all properties from resource. */
    @Parameter(property = "javacpp.properties")
    String properties = null;

    /** Load all properties from file. */
    @Parameter(property = "javacpp.propertyFile")
    File propertyFile = null;

    /** Set property keys to values. */
    @Parameter(property = "javacpp.propertyKeysAndValues")
    Properties propertyKeysAndValues = null;

    /** Process only this class or package (suffixed with .* or .**). */
    @Parameter(property = "javacpp.classOrPackageName")
    String classOrPackageName = null;

    /** Process only these classes or packages (suffixed with .* or .**). */
    @Parameter(property = "javacpp.classOrPackageNames")
    String[] classOrPackageNames = null;

    /** Execute a build command instead of JavaCPP itself, and return. */
    @Parameter(property = "javacpp.buildCommand")
    String[] buildCommand = null;

    /** Add to Maven project source directory of Java files generated by buildCommand. */
    @Parameter(property = "javacpp.targetDirectory")
    String targetDirectory = null;

    /** Add to Maven project source directory of Java files generated by buildCommand. */
    @Parameter(property = "javacpp.targetDirectories")
    String[] targetDirectories = null;

    /** Set the working directory of the build subprocess. */
    @Parameter(property = "javacpp.workingDirectory")
    File workingDirectory = null;

    /** Add environment variables to the compiler subprocess. */
    @Parameter(property = "javacpp.environmentVariables")
    Map<String,String> environmentVariables = null;

    /** Pass compilerOptions directly to compiler. */
    @Parameter(property = "javacpp.compilerOptions")
    String[] compilerOptions = null;

     /** Skip the execution. */
    @Parameter(property = "javacpp.skip", defaultValue = "false")
    boolean skip = false;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${plugin}", required = true, readonly = true)
    PluginDescriptor plugin;

    static String[] merge(String[] ss, String s) {
        if (ss != null && s != null) {
            ss = Arrays.copyOf(ss, ss.length + 1);
            ss[ss.length - 1] = s;
        } else if (s != null) {
            ss = new String[] { s };
        }
        return ss != null ? ss : new String[0];
    }

    @Override public void execute() throws MojoExecutionException {
        final Log log = getLog();
        try {
            if (log.isDebugEnabled()) {
                log.debug("classPath: " + classPath);
                log.debug("classPaths: " + Arrays.deepToString(classPaths));
                log.debug("buildPath: " + buildPath);
                log.debug("buildPaths: " + Arrays.deepToString(buildPaths));
                log.debug("buildResource: " + buildResource);
                log.debug("buildResources: " + Arrays.deepToString(buildResources));
                log.debug("includePath: " + includePath);
                log.debug("includePaths: " + Arrays.deepToString(includePaths));
                log.debug("includeResource: " + includeResource);
                log.debug("includeResources: " + Arrays.deepToString(includeResources));
                log.debug("linkPath: " + linkPath);
                log.debug("linkPaths: " + Arrays.deepToString(linkPaths));
                log.debug("linkResource: " + linkResource);
                log.debug("linkResources: " + Arrays.deepToString(linkResources));
                log.debug("preloadPath: " + preloadPath);
                log.debug("preloadPaths: " + Arrays.deepToString(preloadPaths));
                log.debug("preloadResource: " + preloadResource);
                log.debug("preloadResources: " + Arrays.deepToString(preloadResources));
                log.debug("resourcePath: " + resourcePath);
                log.debug("resourcePaths: " + Arrays.deepToString(resourcePaths));
                log.debug("executablePath: " + executablePath);
                log.debug("executablePaths: " + Arrays.deepToString(executablePaths));
                log.debug("encoding: " + encoding);
                log.debug("outputDirectory: " + outputDirectory);
                log.debug("outputName: " + outputName);
                log.debug("clean: " + clean);
                log.debug("generate: " + generate);
                log.debug("compile: " + compile);
                log.debug("deleteJniFiles: " + deleteJniFiles);
                log.debug("header: " + header);
                log.debug("copyLibs: " + copyLibs);
                log.debug("copyResources: " + copyResources);
                log.debug("configDirectory: " + configDirectory);
                log.debug("jarPrefix: " + jarPrefix);
                log.debug("properties: " + properties);
                log.debug("propertyFile: " + propertyFile);
                log.debug("propertyKeysAndValues: " + propertyKeysAndValues);
                log.debug("classOrPackageName: " + classOrPackageName);
                log.debug("classOrPackageNames: " + Arrays.deepToString(classOrPackageNames));
                log.debug("buildCommand: " + Arrays.deepToString(buildCommand));
                log.debug("targetDirectory: " + Arrays.deepToString(buildCommand));
                log.debug("workingDirectory: " + workingDirectory);
                log.debug("environmentVariables: " + environmentVariables);
                log.debug("compilerOptions: " + Arrays.deepToString(compilerOptions));
                log.debug("skip: " + skip);
            }
            if (targetDirectory != null) {
                project.addCompileSourceRoot(targetDirectory);
            }
            if (targetDirectories != null) {
                for (String targetDirectory : targetDirectories) {
                    project.addCompileSourceRoot(targetDirectory);
                }
            }

            if (skip) {
                log.info("Skipping execution of JavaCPP Builder");
                return;
            }

            classPaths = merge(classPaths, classPath);
            classOrPackageNames = merge(classOrPackageNames, classOrPackageName);

            Logger logger = new Logger() {
                @Override public void debug(String s) { log.debug(s); }
                @Override public void info (String s) { log.info(s);  }
                @Override public void warn (String s) { log.warn(s);  }
                @Override public void error(String s) { log.error(s); }
            };
            Builder builder = new Builder(logger)
                    .classPaths(classPaths)
                    .encoding(encoding)
                    .outputDirectory(outputDirectory)
                    .outputName(outputName)
                    .clean(clean)
                    .generate(generate)
                    .compile(compile)
                    .deleteJniFiles(deleteJniFiles)
                    .header(header)
                    .copyLibs(copyLibs)
                    .copyResources(copyResources)
                    .configDirectory(configDirectory)
                    .jarPrefix(jarPrefix)
                    .properties(properties)
                    .propertyFile(propertyFile)
                    .properties(propertyKeysAndValues)
                    .classesOrPackages(classOrPackageNames)
                    .buildCommand(buildCommand)
                    .workingDirectory(workingDirectory)
                    .environmentVariables(environmentVariables)
                    .compilerOptions(compilerOptions);
            Properties properties = builder.properties;
            String extension = properties.getProperty("platform.extension");
            log.info("Detected platform \"" + Loader.getPlatform() + "\"");
            log.info("Building platform \"" + properties.get("platform") + "\""
                    + (extension != null && extension.length() > 0 ? " with extension \"" + extension + "\"" : ""));
            properties.setProperty("platform.host", Loader.getPlatform());
            String module = properties.get("platform") + (extension != null ? extension : "");
            // make available a platform name that is JPMS friendly without hyphens
            properties.setProperty("platform.module", module.replace('-', '.'));
            String separator = properties.getProperty("platform.path.separator");
            for (String s : merge(buildPaths, buildPath)) {
                String v = properties.getProperty("platform.buildpath", "");
                properties.setProperty("platform.buildpath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(buildResources, buildResource)) {
                String v = properties.getProperty("platform.buildresource", "");
                properties.setProperty("platform.buildresource",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(includePaths, includePath)) {
                String v = properties.getProperty("platform.includepath", "");
                properties.setProperty("platform.includepath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(includeResources, includeResource)) {
                String v = properties.getProperty("platform.includeresource", "");
                properties.setProperty("platform.includeresource",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(linkPaths, linkPath)) {
                String v = properties.getProperty("platform.linkpath", "");
                properties.setProperty("platform.linkpath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(linkResources, linkResource)) {
                String v = properties.getProperty("platform.linkresource", "");
                properties.setProperty("platform.linkresource",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(preloadPaths, preloadPath)) {
                String v = properties.getProperty("platform.preloadpath", "");
                properties.setProperty("platform.preloadpath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(preloadResources, preloadResource)) {
                String v = properties.getProperty("platform.preloadresource", "");
                properties.setProperty("platform.preloadresource",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(resourcePaths, resourcePath)) {
                String v = properties.getProperty("platform.resourcepath", "");
                properties.setProperty("platform.resourcepath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(executablePaths, executablePath)) {
                String v = properties.getProperty("platform.executablepath", "");
                properties.setProperty("platform.executablepath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            properties.setProperty("platform.artifacts", project.getBuild().getOutputDirectory());
            for (Artifact a : plugin.getArtifacts()) {
                String s = Loader.getCanonicalPath(a.getFile());
                String v = properties.getProperty("platform.artifacts", "");
                properties.setProperty("platform.artifacts",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            Properties projectProperties = project.getProperties();
            for (String key : properties.stringPropertyNames()) {
                projectProperties.setProperty("javacpp." + key, properties.getProperty(key));
            }
            File[] outputFiles = builder.build();
            if (log.isDebugEnabled()) {
                log.debug("outputFiles: " + Arrays.deepToString(outputFiles));
            }
        } catch (IOException | ClassNotFoundException | NoClassDefFoundError | InterruptedException | ParserException e) {
            log.error("Failed to execute JavaCPP Builder: " + e.getMessage());
            throw new MojoExecutionException("Failed to execute JavaCPP Builder", e);
        }
    }
}
