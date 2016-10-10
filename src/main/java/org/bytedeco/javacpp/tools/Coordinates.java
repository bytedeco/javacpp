/*
 * Copyright (C) 2014-2016 Samuel Audet
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

import java.nio.file.Path;

/**
 * Uniquely identifies a library in Maven and the local JavaCPP repository.
 */
public class Coordinates {
    public final String group;
    public final String id;
    public final String version;
    public final String classifier;
    public final Path jar;

    public Coordinates(String group, String id, String version) {
        this(group, id, version, null, null);
    }

    public Coordinates(String group, String id, String version, String classifier, Path jar) {
        this.group = group;
        this.id = id;
        this.version = version;
        this.classifier = classifier;
        this.jar = jar;
    }

    public Coordinates(Coordinates coordinates, String classifier, Path jar) {
        this.group = coordinates.group;
        this.id = coordinates.id;
        this.version = coordinates.version;
        this.classifier = classifier;
        this.jar = jar;
    }

    public String canonical() {
        return group + ":" + id + ":jar:" + classifier + ":" + version;
    }
}
