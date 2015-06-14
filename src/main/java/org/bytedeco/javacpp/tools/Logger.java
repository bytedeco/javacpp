/*
 * Copyright (C) 2014 Samuel Audet
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

/**
 * A simple but extensible logging interface that dumps messages to the "standard" output streams by default.
 *
 * @author Samuel Audet
 */
public class Logger {
    public void debug(CharSequence cs) {
        System.out.println(cs);
    }

    public void info(CharSequence cs) {
        System.out.println(cs);
    }

    public void warn(CharSequence cs) {
        System.err.println("Warning: " + cs);
    }

    public void error(CharSequence cs) {
        System.err.println("Error: " + cs);
    }
}
