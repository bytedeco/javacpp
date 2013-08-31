/*
 * Copyright (C) 2012,2013 Arnaud Nauwynck, Samuel Audet
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
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * A Maven Mojo to call the {@link Builder} (Java class -> C++ JNI -> native library).
 * Can also be seen as an example of how to use the Builder programmatically.
 *
 * @goal build
 * @phase process-classes
 * @author Arnaud Nauwynck
 * @author Samuel Audet
 */
public class BuildMojo extends AbstractMojo {

    /**
     * Load user classes from classPath
     * @parameter expression="${classPath}" default-value="${project.build.outputDirectory}"
     */
    private String classPath = null;

    /**
     * Load user classes from classPaths
     * @parameter expression="${classPaths}"
     */
    private String[] classPaths = null;

    /**
     * Output all generated files to outputDirectory
     * @parameter expression="${outputDirectory}"
     */
    private File outputDirectory = null;

    /**
     * Output everything in a file named after given outputName
     * @parameter expression="${outputName}"
     */
    private String outputName = null;

    /**
     * Compile and delete the generated .cpp files
     * @parameter expression="${compile}" default-value="true"
     */
    private boolean compile = true;

    /**
     * Generate header file with declarations of callbacks functions
     * @parameter expression="${header}" default-value="false"
     */
    private boolean header = false;

    /**
     * Copy to output directory dependent libraries (link and preload)
     * @parameter expression="${copylibs}" default-value="false"
     */
    private boolean copyLibs = false;

    /**
     * Also create a JAR file named {@code <jarPrefix>-<platform.name>.jar}
     * @parameter expression="${jarPrefix}"
     */
    private String jarPrefix = null;

    /**
     * Load all properties from resource
     * @parameter expression="${properties}"
     */
    private String properties = null;

    /**
     * Load all properties from file
     * @parameter expression="${propertyFile}"
     */
    private File propertyFile = null;

    /**
     * Set property keys to values
     * @parameter expression="${propertyKeysAndValues}"
     */
    private Properties propertyKeysAndValues = null;

    /**
     * Process only this class or package (suffixed with .* or .**)
     * @parameter expression="${classOrPackageName}"
     */
    private String classOrPackageName = null;

    /**
     * Process only these classes or packages (suffixed with .* or .**)
     * @parameter expression="${classOrPackageNames}"
     */
    private String[] classOrPackageNames = null;

    /**
     * Environment variables added to the compiler subprocess
     * @parameter expression="${environmentVariables}"
     */
    private Map<String,String> environmentVariables = null;

    /**
     * Pass compilerOptions directly to compiler
     * @parameter expression="${compilerOptions}"
     */
    private String[] compilerOptions = null;

    @Override public void execute() throws MojoExecutionException {
        try {
            getLog().info("Executing JavaCPP Builder");
            if (getLog().isDebugEnabled()) {
                getLog().debug("classPath: " + classPath);
                getLog().debug("classPaths: " + Arrays.deepToString(classPaths));
                getLog().debug("outputDirectory: " + outputDirectory);
                getLog().debug("outputName: " + outputName);
                getLog().debug("compile: " + compile);
                getLog().debug("header: " + header);
                getLog().debug("copyLibs: " + copyLibs);
                getLog().debug("jarPrefix: " + jarPrefix);
                getLog().debug("properties: " + properties);
                getLog().debug("propertyFile: " + propertyFile);
                getLog().debug("propertyKeysAndValues: " + propertyKeysAndValues);
                getLog().debug("classOrPackageName: " + classOrPackageName);
                getLog().debug("classOrPackageNames: " + Arrays.deepToString(classOrPackageNames));
                getLog().debug("environmentVariables: " + environmentVariables);
                getLog().debug("compilerOptions: " + Arrays.deepToString(compilerOptions));
            }

            if (classPaths != null && classPath != null) {
                classPaths = Arrays.copyOf(classPaths, classPaths.length + 1);
                classPaths[classPaths.length - 1] = classPath;
            } else if (classPath != null) {
                classPaths = new String[] { classPath };
            }

            if (classOrPackageNames != null && classOrPackageName != null) {
                classOrPackageNames = Arrays.copyOf(classOrPackageNames, classOrPackageNames.length + 1);
                classOrPackageNames[classOrPackageNames.length - 1] = classOrPackageName;
            } else if (classOrPackageName != null) {
                classOrPackageNames = new String[] { classOrPackageName };
            }

            File[] outputFiles = new Builder()
                    .classPaths(classPaths)
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
                    .compilerOptions(compilerOptions).build();
            getLog().info("Successfully executed JavaCPP Builder");
            if (getLog().isDebugEnabled()) {
                getLog().debug("outputFiles: " + Arrays.deepToString(outputFiles));
            }
	} catch (Exception e) {
            getLog().error("Failed to execute JavaCPP Builder: " + e.getMessage());
            throw new MojoExecutionException("Failed to execute JavaCPP Builder", e);
        }
    }
}
