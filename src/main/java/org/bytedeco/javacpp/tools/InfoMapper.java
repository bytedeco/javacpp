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

import org.bytedeco.javacpp.annotation.Properties;

/**
 * An interface to define a kind of configuration file entirely written in Java.
 * A class implementing this interface can be passed to the {@link Parser}, which
 * will create an instance of the class before calling the {@link #map(InfoMap)}
 * method, to be filled in with {@link Info} objects defined by the user.
 * <p>
 * A class further annotated with {@link Properties#target()} gets detected by
 * the {@link Builder}, which then delegates it to the {@link Parser}.
 *
 * @author Samuel Audet
 */
public interface InfoMapper {
    void map(InfoMap infoMap);
}
