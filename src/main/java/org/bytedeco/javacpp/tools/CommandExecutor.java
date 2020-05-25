/*
 * Copyright (C) 2020 Samuel Audet
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
import java.util.List;
import java.util.Map;
import org.bytedeco.javacpp.Loader;

/**
 * A wrapper for ProcessBuilder that can be overridden easily for frameworks like Gradle that don't support it well.
 *
 * @author Samuel Audet
 */
public class CommandExecutor {
    final Logger logger;

    public CommandExecutor(Logger logger) {
        this.logger = logger;
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
    public int executeCommand(List<String> command, File workingDirectory,
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
}
