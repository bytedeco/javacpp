/*
 * Copyright (C) 2014 Samuel Audet
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
