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
