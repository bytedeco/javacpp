/*
 * Copyright (C) 2014-2015 Samuel Audet
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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;

/**
 * Caches libraries and their headers in a local repository using maven coordinates. Allows
 * compiling native libraries without installing their dependencies, and improves performance as
 * libraries are only expanded once and their memory footprint can be shared between processes.
 */
public class Repository {
    static final Logger _log = Logger.create(Repository.class);
    static final Path _home = Paths.get(System.getProperty("user.home"));
    static final Path _repo = _home.resolve(".javacpp").resolve("repository");

    private Repository() {
    }

    public static Path getPath(Coordinates coordinates) {
        Path dst = _repo
            .resolve(coordinates.group.replace('.', '/'))
            .resolve(coordinates.id)
            .resolve(coordinates.version)
            .resolve(coordinates.classifier);
        if (!Files.exists(dst)) {
            try {
                final URI uri = URI.create("jar:file:" + coordinates.jar.toUri().getPath());
                FileSystem jar = null;
                try {
                    jar = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException ex) {
                    // Ignore
                }
                if (jar == null)
                    jar = FileSystems.newFileSystem(uri, new HashMap<String, String>());
                final Path src = jar.getPath("/", "org", "bytedeco", "javacpp", coordinates.classifier);
                _log.debug("Expanding " + coordinates.jar);
                _log.debug("To " + dst.toString());
                expand(src, dst, coordinates.classifier);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return dst;
    }

    // Jars seem to store links as files containing a target. This heuristic figures if a file is
    // actually a link and reads it's target. TODO find proper portable way to zip and unzip links.
    public static String getLinkTarget(Path path, String platform) throws IOException {
        if (platform.equals("linux-x86_64") && Files.size(path) < 1000) {
            String content = Files.readAllLines(path).get(0);
            if (content.contains(".so"))
                return content;
        }
        return null;
    }

    private static void expand(final Path src, final Path dst, final String classifier) throws IOException {
        Files.createDirectories(dst);
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) throws IOException {
                Path rel = src.relativize(dir);
                rel = dst.resolve(rel.toString());
                if (!Files.exists(rel))
                    Files.createDirectories(rel);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                if (!src.equals(file)) {
                    Path rel = src.relativize(file);
                    rel = dst.resolve(rel.toString());
                    String link = getLinkTarget(file, classifier);
                    if (link != null)
                        Files.createSymbolicLink(dst.resolve(rel.toString()), Paths.get(link));
                    else {
                        Files.copy(file, rel, StandardCopyOption.REPLACE_EXISTING);
                        try {
                            Files.setPosixFilePermissions(rel,
                                PosixFilePermissions.fromString("rwxr-xr-x"));
                        } catch (UnsupportedOperationException _) {
                            // Ignore on platforms that do not support it
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
