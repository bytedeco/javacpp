/*
 * Copyright (C) 2019-2021 Samuel Audet
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.bytedeco.javacpp.Loader;

/**
 * A Maven Mojo to call the {@link Loader} on all classes found in the project,
 * as well as call all {@code cachePackage()} methods found on them. It displays
 * to the standard output the directories cached, the former on a line starting
 * with "PATH=" and the latter on another line starting with "PACKAGEPATH=".
 *
 * @author Samuel Audet
 */
@Mojo(name = "cache", defaultPhase = LifecyclePhase.NONE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CacheMojo extends AbstractMojo {

    /** Process only this class or package (suffixed with .* or .**). */
    @Parameter(property = "javacpp.classOrPackageName")
    String classOrPackageName = null;

    /** Process only these classes or packages (suffixed with .* or .**). */
    @Parameter(property = "javacpp.classOrPackageNames")
    String[] classOrPackageNames = null;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${plugin}", required = true, readonly = true)
    PluginDescriptor plugin;

    String join(String separator, Iterable<String> strings) {
        String string = "";
        for (String s : strings) {
            string += (string.length() > 0 ? separator : "") + s;
        }
        return string;
    }

    @Override public void execute() throws MojoExecutionException {
        final Log log = getLog();
        Logger logger = new Logger() {
            @Override public void debug(String s) { log.debug(s); }
            @Override public void info (String s) { log.info(s);  }
            @Override public void warn (String s) { log.warn(s);  }
            @Override public void error(String s) { log.error(s); }
        };
        // to filter out the classes that cannot contain the Properties annotation we need
        ClassFilter classFilter = new ClassFilter() {
            byte[] s = Generator.signature(org.bytedeco.javacpp.annotation.Properties.class).getBytes(StandardCharsets.UTF_8);
            @Override public boolean keep(String filename, byte[] data) {
                boolean found = false;
                next:
                for (int i = 0; i < data.length; i++) {
                    for (int j = 0; i + j < data.length && j < s.length; j++) {
                        if (data[i + j] != s[j]) {
                            continue next;
                        }
                    }
                    found = true;
                    break;
                }
                return found;
            }
        };
        try {
            Set<Artifact> artifacts = project.getArtifacts();
            ArrayList<Class> classes = new ArrayList<Class>();
            UserClassLoader classLoader = new UserClassLoader();
            ClassScanner classScanner = new ClassScanner(logger, classes, classLoader, classFilter);

            classLoader.addPaths(plugin.getPluginArtifact().getFile().getAbsolutePath());
            for (Artifact a : artifacts) {
                classLoader.addPaths(a.getFile().getAbsolutePath());
            }

            classOrPackageNames = BuildMojo.merge(classOrPackageNames, classOrPackageName);
            if (classOrPackageNames == null || classOrPackageNames.length == 0) {
                classScanner.addPackage(null, true);
            } else for (String s : classOrPackageNames) {
                classScanner.addClassOrPackage(s);
            }

            LinkedHashSet<String> packages = new LinkedHashSet<String>();
            for (Class c : classes) {
                try {
                    Method cachePackage = c.getMethod("cachePackage");
                    logger.info("Caching " + c);
                    File f = (File)cachePackage.invoke(c);
                    if (f != null) {
                        packages.add(Loader.getCanonicalPath(f));
                    }
                } catch (NoSuchMethodException e) {
                    // assume this class has no associated packages, skip
                }
            }

            Class<?> loader = Class.forName("org.bytedeco.javacpp.Loader", true, classLoader);
            Method load = loader.getMethod("load", Class[].class);
            String[] filenames = (String[])load.invoke(loader, new Object[] {classes.toArray(new Class[classes.size()])});

            LinkedHashSet<String> paths = new LinkedHashSet<String>();
            for (String filename : filenames) {
                if (filename != null) {
                    paths.add(new File(filename).getParent());
                }
            }

            System.out.println("PATH=" + join(File.pathSeparator, paths));
            System.out.println("PACKAGEPATH=" + join(File.pathSeparator, packages));
        } catch (IOException | ClassNotFoundException | NoClassDefFoundError | NoSuchMethodException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.error("Failed to execute JavaCPP Loader: " + e.getMessage());
            throw new MojoExecutionException("Failed to execute JavaCPP Loader", e);
        }
    }
}
