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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 *
 * @author Samuel Audet
 */
public class MethodInformation {
    Class<?> cls;
    Method method;
    Annotation[] annotations;
    int modifiers;
    Class<?> returnType;
    String name, memberName[];
    int dim;
    boolean[] parameterRaw;
    Class<?>[] parameterTypes;
    Annotation[][] parameterAnnotations;
    boolean returnRaw, withEnv, overloaded, noOffset, deallocator, allocator, arrayAllocator,
            bufferGetter, valueGetter, valueSetter, memberGetter, memberSetter, noReturnGetter;
    Method pairedMethod;
    Class<?> throwsException;
}
