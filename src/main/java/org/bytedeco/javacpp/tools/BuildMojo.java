/*
 * Copyright (C) 2012-2014 Arnaud Nauwynck, Samuel Audet
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

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.bytedeco.javacpp.Loader;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResult;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * A Maven Mojo to call the {@link Builder} (C++ header file -> Java class -> C++ JNI -> native library).
 * Can also be considered as an example of how to use the Builder programmatically.
 *
 * @goal build
 * @phase process-classes
 * @author Arnaud Nauwynck
 * @author Samuel Audet
 */
public class BuildMojo extends AbstractMojo {

    /**
     * Load user classes from classPath
     * @parameter property="classPath" default-value="${project.build.outputDirectory}"
     */
    private String classPath = null;

    /**
     * Load user classes from classPaths
     * @parameter property="classPaths"
     */
    private String[] classPaths = null;

    /**
     * Add the path to the "platform.includepath" property.
     * @parameter property="includePath"
     */
    private String includePath = null;

    /**
     * Add the paths to the "platform.includepath" property.
     * @parameter property="includePaths"
     */
    private String[] includePaths = null;

    /**
     * Add the path to the "platform.linkpath" property.
     * @parameter property="linkPath"
     */
    private String linkPath = null;

    /**
     * Add the paths to the "platform.linkpath" property.
     * @parameter property="linkPaths"
     */
    private String[] linkPaths = null;

    /**
     * Add the path to the "platform.preloadpath" property.
     * @parameter property="preloadPath"
     */
    private String preloadPath = null;

    /**
     * Add the paths to the "platform.preloadpath" property.
     * @parameter property="preloadPaths"
     */
    private String[] preloadPaths = null;

    /**
     * Output all generated files to outputDirectory
     * @parameter property="outputDirectory"
     */
    private File outputDirectory = null;

    /**
     * Output everything in a file named after given outputName
     * @parameter property="outputName"
     */
    private String outputName = null;

    /**
     * Compile and delete the generated .cpp files
     * @parameter property="compile" default-value="true"
     */
    private boolean compile = true;

    /**
     * Generate header file with declarations of callbacks functions
     * @parameter property="header" default-value="false"
     */
    private boolean header = false;

    /**
     * Copy to output directory dependent libraries (link and preload)
     * @parameter property="copyLibs" default-value="false"
     */
    private boolean copyLibs = false;

    /**
     * Also create a JAR file named {@code <jarPrefix>-<platform>.jar}
     * @parameter property="jarPrefix"
     */
    private String jarPrefix = null;

    /**
     * Load all properties from resource
     * @parameter property="properties"
     */
    private String properties = null;

    /**
     * Load all properties from file
     * @parameter property="propertyFile"
     */
    private File propertyFile = null;

    /**
     * Set property keys to values
     * @parameter property="propertyKeysAndValues"
     */
    private Properties propertyKeysAndValues = null;

    /**
     * Process only this class or package (suffixed with .* or .**)
     * @parameter property="classOrPackageName"
     */
    private String classOrPackageName = null;

    /**
     * Process only these classes or packages (suffixed with .* or .**)
     * @parameter property="classOrPackageNames"
     */
    private String[] classOrPackageNames = null;

    /**
     * Environment variables added to the compiler subprocess
     * @parameter property="environmentVariables"
     */
    private Map<String,String> environmentVariables = null;

    /**
     * Pass compilerOptions directly to compiler
     * @parameter property="compilerOptions"
     */
    private String[] compilerOptions = null;

     /**
      * Skip the execution.
      * @parameter property="skip" default-value="false"
      */
    private boolean skip = false;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project = null;

    /**
     * @component
     */
    private RepositorySystem repoSystem = null;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession = null;

    /**
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    private List<RemoteRepository> remoteRepos = null;

    String[] merge(String[] ss, String s) {
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
            log.info("Executing JavaCPP Builder");
            if (log.isDebugEnabled()) {
                log.debug("classPath: " + classPath);
                log.debug("classPaths: " + Arrays.deepToString(classPaths));
                log.debug("includePath: " + includePath);
                log.debug("includePaths: " + Arrays.deepToString(includePaths));
                log.debug("linkPath: " + linkPath);
                log.debug("linkPaths: " + Arrays.deepToString(linkPaths));
                log.debug("preloadPath: " + preloadPath);
                log.debug("preloadPaths: " + Arrays.deepToString(preloadPaths));
                log.debug("outputDirectory: " + outputDirectory);
                log.debug("outputName: " + outputName);
                log.debug("compile: " + compile);
                log.debug("header: " + header);
                log.debug("copyLibs: " + copyLibs);
                log.debug("jarPrefix: " + jarPrefix);
                log.debug("properties: " + properties);
                log.debug("propertyFile: " + propertyFile);
                log.debug("propertyKeysAndValues: " + propertyKeysAndValues);
                log.debug("classOrPackageName: " + classOrPackageName);
                log.debug("classOrPackageNames: " + Arrays.deepToString(classOrPackageNames));
                log.debug("environmentVariables: " + environmentVariables);
                log.debug("compilerOptions: " + Arrays.deepToString(compilerOptions));
                log.debug("skip: " + skip);
            }

            if (skip) {
                log.info("Skipped execution of JavaCPP Builder");
                return;
            }

            for (Dependency dependency : project.getModel().getDependencies()) {
                if (dependency.getGroupId().equals("org.bytedeco.javacpp-presets")) {
                    dependency = dependency.clone(); // Not sure if they copy the output
                    Coordinates coord = resolve(dependency);
                    classPaths = merge(classPaths, coord.jar.toAbsolutePath().toString());

                    try {
                        dependency.setClassifier("include");
                        coord = resolve(dependency);
                        Path path = Repository.getPath(coord);
                        includePaths = merge(includePaths, path.toString());
                    } catch (Exception _) {
                        // Ignore, include might not exist for legacy builds
                    }

                    dependency.setClassifier(Loader.getPlatform());
                    coord = resolve(dependency);
                    Path path = Repository.getPath(coord);
                    linkPaths = merge(linkPaths, path.toString());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("includePaths with dependencies: " + Arrays.deepToString(includePaths));
                log.debug("linkPaths with dependencies: " + Arrays.deepToString(linkPaths));
            }

            classPaths = merge(classPaths, classPath);
            classOrPackageNames = merge(classOrPackageNames, classOrPackageName);

            Logger logger = new Logger() {
                @Override public void debug(String s) { log.debug(s); }
                @Override public void info (String s) { log.info(s);  }
                @Override public void warn (String s) { log.warn(s);  }
                @Override public void error(String s) { log.error(s); }
            };

            Coordinates coordinates = new Coordinates(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion()
            );
            Builder builder = new Builder(logger)
                    .classPaths(classPaths)
                    .includePath(includePath)
                    .outputDirectory(outputDirectory)
                    .outputName(outputName)
                    .compile(compile)
                    .header(header)
                    .copyLibs(copyLibs)
                    .jarPrefix(jarPrefix)
                    .properties(properties)
                    .propertyFile(propertyFile)
                    .properties(propertyKeysAndValues)
                    .classesOrPackages(classOrPackageNames)
                    .environmentVariables(environmentVariables)
                    .compilerOptions(compilerOptions)
                    .coordinates(coordinates);
            Properties properties = builder.properties;
            String separator = properties.getProperty("platform.path.separator");
            for (String s : merge(includePaths, includePath)) {
                String v = properties.getProperty("platform.includepath", "");
                properties.setProperty("platform.includepath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(linkPaths, linkPath)) {
                String v = properties.getProperty("platform.linkpath", "");
                properties.setProperty("platform.linkpath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            for (String s : merge(preloadPaths, preloadPath)) {
                String v = properties.getProperty("platform.preloadpath", "");
                properties.setProperty("platform.preloadpath",
                        v.length() == 0 || v.endsWith(separator) ? v + s : v + separator + s);
            }
            project.getProperties().putAll(properties);
            File[] outputFiles = builder.build();
            log.info("Successfully executed JavaCPP Builder");
            if (log.isDebugEnabled()) {
                log.debug("outputFiles: " + Arrays.deepToString(outputFiles));
            }
        } catch (Exception e) {
            log.error("Failed to execute JavaCPP Builder: " + e.getMessage());
            throw new MojoExecutionException("Failed to execute JavaCPP Builder", e);
        }
    }

    private Coordinates resolve(Dependency dependency) throws Exception {
        Artifact artifact = new DefaultArtifact(
            dependency.getGroupId(),
            dependency.getArtifactId(),
            dependency.getClassifier(),
            "jar",
            dependency.getVersion());

        VersionRequest versionRequest = new VersionRequest();
        versionRequest.setArtifact(artifact);
        versionRequest.setRepositories(remoteRepos);
        VersionResult versionResult = repoSystem.resolveVersion(repoSession, versionRequest);
        artifact.setVersion(versionResult.getVersion());

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);
        ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
        return new Coordinates(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            artifact.getBaseVersion(),
            dependency.getClassifier(),
            result.getArtifact().getFile().toPath()
        );
    }
}
