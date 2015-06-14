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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.bytedeco.javacpp.ClassProperties;

/**
 * A simple {@link Thread} that reads data as fast as possible from an {@link InputStream} and
 * writes to the {@link OutputStream}. Used by {@link Builder#compile(String, String, ClassProperties)}
 * to flush the streams of a {@link Process}.
 */
class Piper extends Thread {
    public Piper(Logger logger, InputStream is, OutputStream os) {
        this.logger = logger;
        this.is = is;
        this.os = os;
    }

    Logger logger;
    InputStream is;
    OutputStream os;

    @Override public void run() {
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            logger.error("Could not pipe from the InputStream to the OutputStream: " + e.getMessage());
        }
    }
}
