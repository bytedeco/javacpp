/*
 * Copyright (C) 2011 Samuel Audet
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

package com.googlecode.javacpp;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.googlecode.javacpp.annotation.Allocator;
import com.googlecode.javacpp.annotation.ArrayAllocator;
import com.googlecode.javacpp.annotation.ByPtr;
import com.googlecode.javacpp.annotation.ByPtrPtr;
import com.googlecode.javacpp.annotation.ByPtrRef;
import com.googlecode.javacpp.annotation.ByRef;
import com.googlecode.javacpp.annotation.ByVal;
import com.googlecode.javacpp.annotation.Cast;
import com.googlecode.javacpp.annotation.Convention;
import com.googlecode.javacpp.annotation.Function;
import com.googlecode.javacpp.annotation.MemberGetter;
import com.googlecode.javacpp.annotation.MemberSetter;
import com.googlecode.javacpp.annotation.Name;
import com.googlecode.javacpp.annotation.Namespace;
import com.googlecode.javacpp.annotation.NoOffset;
import com.googlecode.javacpp.annotation.Opaque;
import com.googlecode.javacpp.annotation.Platform;
import com.googlecode.javacpp.annotation.ValueGetter;
import com.googlecode.javacpp.annotation.ValueSetter;

/**
 *
 * @author Samuel Audet
 */
public class Generator implements Closeable {

    public Generator(Properties properties, String filename) {
        this(properties, new File(filename));
    }
    public Generator(Properties properties, File file) {
        this.properties = properties;
        this.file = file;
        this.writer = null;
    }
    public Generator(Properties properties, PrintWriter writer) {
        this.properties = properties;
        this.file = null;
        this.writer = writer;
    }

    public static final String JNI_VERSION = "JNI_VERSION_1_4";

    private static final Logger logger = Logger.getLogger(Generator.class.getName());

    private Properties properties;
    private File file;
    private PrintWriter writer, out;
    private LinkedListRegister<String> functionDefinitions, functionPointers;
    private LinkedListRegister<Class> deallocators, arrayDeallocators, jclasses;
    private HashMap<Class,LinkedList<String>> members;
    private boolean generatedSomethingUseful;

    public static class LinkedListRegister<E> extends LinkedList<E> {
        public int register(E e) {
            int i = indexOf(e);
            if (i < 0) {
                add(e);
                i = size()-1;
            }
            return i;
        }
    }

    public boolean generate(Class<?> ... classes) throws FileNotFoundException {
        // first pass using a null writer to fill up the LinkedListRegister objects
        out = new PrintWriter(new Writer() {
            @Override public void close() { }
            @Override public void flush() { }
            @Override public void write(char[] cbuf, int off, int len) { }
        });
        functionDefinitions = new LinkedListRegister<String>();
        functionPointers    = new LinkedListRegister<String>();
        deallocators        = new LinkedListRegister<Class>();
        arrayDeallocators   = new LinkedListRegister<Class>();
        jclasses            = new LinkedListRegister<Class>();
        members             = new HashMap<Class,LinkedList<String>>();
        doClasses(classes);

        if (generatedSomethingUseful) {
            // second pass with the real writer
            out = writer != null ? writer : new PrintWriter(file);
            doClasses(classes);
            return true;
        }
        return false;
    }

    public void close() {
        if (out != null) {
            out.close();
        }
    }

    private void doClasses(Class<?> ... classes) {
        out.println("/* DO NOT EDIT THIS FILE - IT IS MACHINE GENERATED */");
        out.println();
        String define = properties.getProperty("generator.define");
        if (define != null && define.length() > 0) {
            for (String s : define.split("\u0000")) {
                out.println("#define " + s);
            }
            out.println();
        }
        String[] include = { properties.getProperty("generator.include"),
                             properties.getProperty("generator.cinclude") };
        for (int i = 0; i < include.length; i++) {
            if (include[i] != null && include[i].length() > 0) {
                if (i == 1) {
                    out.println("extern \"C\" {");
                }
                for (String s : include[i].split("\u0000")) {
                    char[] chars = s.toCharArray();
                    out.print("#include ");
                    char c = chars[0];
                    if (c != '<' && c != '"') {
                        out.print('"');
                    }
                    out.print(chars);
                    c = chars[chars.length-1];
                    if (c != '>' && c != '"') {
                        out.print('"');
                    }
                    out.println();
                }
                if (i == 1) {
                    out.println("}");
                }
                out.println();
            }
        }
        out.println("#ifdef _WIN32");
        out.println("    #define _JAVASOFT_JNI_MD_H_");
        out.println();
        out.println("    #define JNIEXPORT __declspec(dllexport)");
        out.println("    #define JNIIMPORT __declspec(dllimport)");
        out.println("    #define JNICALL __stdcall");
        out.println();
        out.println("    typedef int jint;");
        out.println("    typedef __int64 jlong;");
        out.println("    typedef signed char jbyte;");
        out.println("#endif");
        out.println("#include <jni.h>");
        out.println("#ifndef _WIN32");
        out.println("    #include <stdint.h>");
        out.println("#endif");
        out.println("#include <stdlib.h>");
        out.println("#include <stddef.h>");
        out.println("#include <exception>");
        out.println();
        out.println("#define jlong_to_ptr(a) ((void*)(uintptr_t)(a))");
        out.println("#define ptr_to_jlong(a) ((jlong)(uintptr_t)(a))");
        out.println("#ifdef ANDROID");
        out.println("    #define NewWeakGlobalRef(o) NewGlobalRef(o)");
        out.println("    #define DeleteWeakGlobalRef(o) DeleteGlobalRef(o)");
        out.println("#endif");
        out.println();
        out.println("extern \"C\" {");
        out.println();
        for (String s : functionDefinitions) {
            out.println(s);
        }
        out.println();
        for (String s : functionPointers) {
            out.println("static jobject " + s + " = NULL;");
        }
        out.println();
        for (Class c : deallocators) {
            String mangledName = mangle(c.getName());
            String typeName = getCPPTypeName(c);
            out.println("static void JavaCPP_" + mangledName + "_deallocate(" + typeName + " address) {");
            out.println("    delete address;");
            out.println("}");
        }
        for (Class c : arrayDeallocators) {
            String mangledName = mangle(c.getName());
            String typeName = getCPPTypeName(c);
            out.println("static void JavaCPP_" + mangledName + "_deallocateArray(" + typeName + " address) {");
            out.println("    delete[] address;");
            out.println("}");
        }
        out.println();
        out.println("static JavaVM *JavaCPP_vm = NULL;");
        out.println("static jclass JavaCPP_classes[" + jclasses.size() + "] = { NULL };");
        out.println("static jmethodID JavaCPP_initMethodID = NULL;");
        out.println("static jfieldID JavaCPP_addressFieldID = NULL;");
        out.println("static jfieldID JavaCPP_positionFieldID = NULL;");
        out.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {");
        out.println("    JavaCPP_vm = vm;");
        out.println("    JNIEnv* e;");
        out.println("    if (vm->GetEnv((void**)&e, " + JNI_VERSION + ") != JNI_OK) {");
        out.println("        fprintf(stderr, \"Could not get JNIEnv for " + JNI_VERSION + " inside JNI_OnLoad().\");");
        out.println("        return 0;");
        out.println("    }");
        out.println("    const char *classNames[" + jclasses.size() + "] = {");
        Iterator<Class> classIterator = jclasses.iterator();
        int maxMemberSize = 0;
        while (classIterator.hasNext()) {
            Class c = classIterator.next();
            out.print("            \"" + c.getName().replace('.','/') + "\"");
            if (classIterator.hasNext()) {
                out.println(",");
            }
            LinkedList<String> m = members.get(c);
            if (m != null && m.size() > maxMemberSize) {
                maxMemberSize = m.size();
            }
        }
        out.println(" };");
        out.println("    const char *members[" + jclasses.size() + "][" + maxMemberSize + "] = {");
        classIterator = jclasses.iterator();
        while (classIterator.hasNext()) {
            out.print("            { ");
            LinkedList<String> m = members.get(classIterator.next());
            Iterator<String> memberIterator = m == null ? null : m.iterator();
            while (memberIterator != null && memberIterator.hasNext()) {
                out.print("\"" + memberIterator.next() + "\"");
                if (memberIterator.hasNext()) {
                    out.print(", ");
                }
            }
            out.print(" }");
            if (classIterator.hasNext()) {
                out.println(",");
            }
        }
        out.println(" };");
        out.println("    int offsets[" + jclasses.size() + "][" + maxMemberSize + "] = {");
        classIterator = jclasses.iterator();
        while (classIterator.hasNext()) {
            out.print("            { ");
            Class c = classIterator.next();
            LinkedList<String> m = members.get(c);
            Iterator<String> memberIterator = m == null ? null : m.iterator();
            while (memberIterator != null && memberIterator.hasNext()) {
                String typeName = getCPPTypeName(c);
                String valueTypeName = typeName.substring(0, typeName.length()-1);
                String memberName = memberIterator.next();
                if ("sizeof".equals(memberName)) {
                    if ("void".equals(valueTypeName)) {
                        valueTypeName = "void*";
                    }
                    out.print("sizeof(" + valueTypeName + ")");
                } else {
                    out.print("offsetof(" + valueTypeName  + "," + memberName + ")");
                }
                if (memberIterator.hasNext()) {
                    out.print(", ");
                }
            }
            out.print(" }");
            if (classIterator.hasNext()) {
                out.println(",");
            }
        }
        out.println(" };");
        out.print("    int memberOffsetSizes[" + jclasses.size() + "] = { ");
        classIterator = jclasses.iterator();
        while (classIterator.hasNext()) {
            LinkedList<String> m = members.get(classIterator.next());
            out.print(m == null ? "0" : m.size());
            if (classIterator.hasNext()) {
                out.print(", ");
            }
        }
        out.println(" };");
        out.println("    for (int i = 0; i < " + jclasses.size() + "; i++) {");
        out.println("        jclass c = e->FindClass(classNames[i]);");
        out.println("        if (c == NULL) {");
        out.println("            fprintf(stderr, \"Error loading class %s.\", classNames[i]);");
        out.println("            return 0;");
        out.println("        }");
        out.println("        JavaCPP_classes[i] = (jclass)e->NewWeakGlobalRef(c);");
        out.println("        if (JavaCPP_classes[i] == NULL) {");
        out.println("            fprintf(stderr, \"Error creating global reference of class %s.\", classNames[i]);");
        out.println("            return 0;");
        out.println("        }");
        out.println("    }");
        out.println("    jmethodID putMemberOffsetMethodID = e->GetStaticMethodID(JavaCPP_classes[" +
                jclasses.register(Loader.class) + "], \"putMemberOffset\", \"(Ljava/lang/Class;Ljava/lang/String;I)V\");");
        out.println("    if (putMemberOffsetMethodID == NULL) {");
        out.println("        fprintf(stderr, \"Error getting putMemberOffset method ID of Loader class.\");");
        out.println("        return 0;");
        out.println("    }");
        out.println("    for (int i = 0; i < " + jclasses.size() + "; i++) {");
        out.println("        for (int j = 0; j < memberOffsetSizes[i]; j++) {");
        out.println("            jvalue args[3];");
        out.println("            args[0].l = JavaCPP_classes[i];");
        out.println("            args[1].l = e->NewStringUTF(members[i][j]);");
        out.println("            args[2].i = offsets[i][j];");
        out.println("            e->CallStaticVoidMethodA(JavaCPP_classes[" +
                jclasses.register(Loader.class) + "], putMemberOffsetMethodID, args);");
        out.println("        }");
        out.println("    }");
        out.println("    JavaCPP_initMethodID = e->GetMethodID(JavaCPP_classes[" +
                jclasses.register(Pointer.class) + "], \"init\", \"(JJ)V\");");
        out.println("    if (JavaCPP_initMethodID == NULL) {");
        out.println("        fprintf(stderr, \"Error getting init method ID of Pointer class.\");");
        out.println("        return 0;");
        out.println("    }");
        out.println("    JavaCPP_addressFieldID = e->GetFieldID(JavaCPP_classes[" +
                jclasses.register(Pointer.class) + "], \"address\", \"J\");");
        out.println("    if (JavaCPP_addressFieldID == NULL) {");
        out.println("        fprintf(stderr, \"Error getting address field ID of Pointer class.\");");
        out.println("        return 0;");
        out.println("    }");
        out.println("    JavaCPP_positionFieldID = e->GetFieldID(JavaCPP_classes[" +
                jclasses.register(Pointer.class) + "], \"position\", \"I\");");
        out.println("    if (JavaCPP_positionFieldID == NULL) {");
        out.println("        fprintf(stderr, \"Error getting position field ID of Pointer class.\");");
        out.println("        return 0;");
        out.println("    }");
        out.println("    return e->GetVersion();");
        out.println("}");
        out.println();
        out.println("JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {");
        out.println("    JNIEnv* e;");
        out.println("    if (vm->GetEnv((void**)&e, " + JNI_VERSION + ") != JNI_OK) {");
        out.println("        fprintf(stderr, \"Could not get JNIEnv for " + JNI_VERSION + " inside JNI_OnUnLoad().\");");
        out.println("        return;");
        out.println("    }");
        for (String s : functionPointers) {
            out.println("    e->DeleteGlobalRef(" + s + ");");
        }
        out.println("    for (int i = 0; i < " + jclasses.size() + "; i++) {");
        out.println("        e->DeleteWeakGlobalRef(JavaCPP_classes[i]);");
        out.println("    }");
        out.println("}");
        out.println();
        out.println("static void JavaCPP_handleException(JNIEnv *e) { ");
        out.println("    try {");
        out.println("        throw;");
        out.println("    } catch (std::exception& ex) {");
        out.println("        e->ThrowNew(JavaCPP_classes[" + 
                jclasses.register(RuntimeException.class) + "], ex.what());");
        out.println("    } catch (...) {");
        out.println("        e->ThrowNew(JavaCPP_classes[" + 
                jclasses.register(RuntimeException.class) + "], \"Unknown exception.\");");
        out.println("    }");
        out.println("}");
        out.println();

        doMethods(Pointer.class);
        doMethods(BytePointer.class);
        doMethods(ShortPointer.class);
        doMethods(IntPointer.class);
        doMethods(LongPointer.class);
        doMethods(FloatPointer.class);
        doMethods(DoublePointer.class);
        doMethods(CharPointer.class);
        doMethods(PointerPointer.class);
        doMethods(CLongPointer.class);
        doMethods(SizeTPointer.class);
//        doMethods(FunctionPointer.class);
        generatedSomethingUseful = false;
        for (Class<?> cls : classes) {
            doMethods(cls);
        }

        out.println("}");
        out.println();
    }

    private void doMethods(Class<?> cls) {
        com.googlecode.javacpp.annotation.Properties classProperties =
            cls.getAnnotation(com.googlecode.javacpp.annotation.Properties.class);
        boolean platformMatches = false;
        if (classProperties != null) {
            for (Platform p : classProperties.value()) {
                if (checkPlatform(p)) {
                    platformMatches = true;
                }
            }
        } else if (checkPlatform(cls.getAnnotation(Platform.class))) {
            platformMatches = true;
        }
        if (!platformMatches) {
            return;
        }

        generatedSomethingUseful = true;

        LinkedList<String> memberList = members.get(cls);
        if ((!cls.isAnnotationPresent(Opaque.class) || cls == Pointer.class) &&
                !FunctionPointer.class.isAssignableFrom(cls) &&
                !cls.isAnnotationPresent(NoOffset.class)) {
            if (memberList == null) {
                members.put(cls, memberList = new LinkedList<String>());
            }
            if (!memberList.contains("sizeof")) {
                memberList.add("sizeof");
            }
        }

        Class[] classes = cls.getDeclaredClasses();
        for (int i = 0; i < classes.length; i++) {
            if (Pointer.class.isAssignableFrom(classes[i]) ||
                    Pointer.Deallocator.class.isAssignableFrom(classes[i])) {
                doMethods(classes[i]);
            }
        }

        Method[] methods = cls.getDeclaredMethods();
        boolean[] callbackAllocators = new boolean[methods.length];
        Method functionMethod = doFunctionDefinitions(cls, callbackAllocators);
        for (int i = 0; i < methods.length; i++) {
            if (!checkPlatform(methods[i].getAnnotation(Platform.class))) {
                continue;
            }
            MethodInformation methodInfo = getMethodInformation(methods[i]);
            if (methodInfo == null) {
                continue;
            }
            String baseFunctionName = mangle(cls.getName()) + "_" + mangle(methodInfo.name);

            if ((methodInfo.memberGetter || methodInfo.memberSetter) && !methodInfo.noOffset &&
                    memberList != null && !Modifier.isStatic(methodInfo.modifiers)) {
                if (!memberList.contains(methodInfo.memberName)) {
                    memberList.add(methodInfo.memberName);
                }
            }

            if (callbackAllocators[i]) {
                doCallback(cls, functionMethod, baseFunctionName);
            }

            out.print("JNIEXPORT " + getJNITypeName(methodInfo.returnType) + 
                    " JNICALL Java_" + baseFunctionName);
            if (methodInfo.overloaded) {
                out.print("__" + mangle(getSignature(methodInfo.parameterTypes)));
            }
            if (Modifier.isStatic(methodInfo.modifiers)) {
                out.print("(JNIEnv *e, jclass c");
            } else {
                out.print("(JNIEnv *e, jobject o");
            }
            for (int j = 0; j < methodInfo.parameterTypes.length; j++) {
                out.print(", " + getJNITypeName(methodInfo.parameterTypes[j]) + " p" + j);
            }
            out.println(") {");

            if (callbackAllocators[i]) {
                doCallbackAllocator(cls, functionMethod, baseFunctionName);
                continue;
            } else if (!Modifier.isStatic(methodInfo.modifiers) && !methodInfo.allocator &&
                    !methodInfo.arrayAllocator && !methodInfo.deallocator) {
                // get our "this" pointer
                String typeName = getCPPTypeName(cls);
                String thisType = methodInfo.bufferGetter && "void*".equals(typeName) ?
                        "char*" : typeName;
                out.println("    " + thisType + " pointer = (" + thisType + ")" +
                        "jlong_to_ptr(e->GetLongField(o, JavaCPP_addressFieldID));");
                out.println("    if (pointer == NULL) {");
                out.println("        e->ThrowNew(JavaCPP_classes[" + 
                        jclasses.register(NullPointerException.class) + "], \"Pointer address is NULL.\");");
                out.println("        return" + (methodInfo.returnType == void.class ? ";" : " 0;"));
                out.println("    }");
                if (!cls.isAnnotationPresent(Opaque.class) || methodInfo.bufferGetter) {
                    out.println("    jint position = e->GetIntField(o, JavaCPP_positionFieldID);");
                    if (FunctionPointer.class.isAssignableFrom(cls)) {
                        out.println("    pointer = position == 0 ? pointer : (" + thisType + ")((void**)pointer + position);");
                    } else {
                        out.println("    pointer += position;");
                    }
                }
            }

            boolean[] hasPointer = new boolean[methodInfo.parameterTypes.length];
            boolean[] mayChange  = new boolean[methodInfo.parameterTypes.length];
            doParametersBefore(methodInfo, hasPointer, mayChange);
            String returnVariable = doReturnBefore(cls, methodInfo);
            boolean needsCatchBlock = doMainOperation(cls, methodInfo, returnVariable, hasPointer);
            doReturnAfter(cls, methodInfo, needsCatchBlock);
            doParametersAfter(methodInfo, needsCatchBlock, mayChange);
            if (methodInfo.returnType != void.class) {
                out.println("    return r;");
            }
            out.println("}");
        }
        out.println();
    }

    private void doParametersBefore(MethodInformation methodInfo,
            boolean[] hasPointer, boolean[] mayChange) {
        for (int j = 0; j < methodInfo.parameterTypes.length; j++) {
            Class passBy = getBy(methodInfo.parameterAnnotations[j]);
            if (passBy == null && methodInfo.pairedMethod != null &&
                    (methodInfo.valueSetter || methodInfo.memberSetter)) {
                passBy = getBy(methodInfo.pairedMethod.getAnnotations());
            }
            hasPointer[j] = true;
            String typeName = getCPPTypeName(methodInfo.parameterTypes[j]);

            if (Pointer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                out.println("    " + typeName + " pointer" + j + " = p" + j + " == NULL ? NULL : (" + 
                        typeName + ")jlong_to_ptr(e->GetLongField(p" + j + ", JavaCPP_addressFieldID));");
                if (!methodInfo.parameterTypes[j].isAnnotationPresent(Opaque.class) &&
                        passBy != ByPtrPtr.class && passBy != ByPtrRef.class) {
                    out.println("    jint position" + j + " = p" + j + " == NULL ? 0 : e->GetIntField(p" + j + 
                            ", JavaCPP_positionFieldID);");
                    if (FunctionPointer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                        out.println("    pointer" + j + " = position" + j + " == 0 ? pointer" + j +
                                " : (" + typeName + ")((void**)pointer" + j + " + position" + j + ");");
                    } else {
                        out.println("    pointer" + j + " += position" + j + ";");
                    }
                } else if (passBy == ByPtrPtr.class || passBy == ByPtrRef.class) {
                    mayChange[j] = true;
                }
            } else if (methodInfo.parameterTypes[j] == String.class) {
                out.println("    const char *pointer" + j +
                        " = p" + j + " == NULL ? NULL : e->GetStringUTFChars(p" + j + ", NULL);");
            } else if (methodInfo.parameterTypes[j].isArray()) {
                Class t = methodInfo.parameterTypes[j].getComponentType();
                String s = t.getName();
                s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
                out.println("    " + getJNITypeName(t) + " *pointer" + j +
                        " = p" + j + " == NULL ? NULL : e->Get" + s + "ArrayElements(p" + j + ", NULL);");
            } else if (Buffer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                out.println("    " + typeName + " pointer" + j +
                        " = p" + j + " == NULL ? NULL : (" + typeName + ")e->GetDirectBufferAddress(p" + j + ");");
            } else if (methodInfo.parameterTypes[j].isPrimitive()) {
                hasPointer[j] = false;
            } else {
                logger.log(Level.WARNING, "Method \"" + methodInfo.method + "\" has unsupported parameter type \"" +
                        methodInfo.parameterTypes[j].getCanonicalName() + "\". Compilation will most likely fail.");
            }
        }
    }

    private String doReturnBefore(Class cls, MethodInformation methodInfo) {
        String returnVariable = "";
        if (methodInfo.returnType == void.class) {
            String typeName = getCPPTypeName(cls);
            if (methodInfo.allocator || methodInfo.arrayAllocator) {
                out.println("    if (!e->IsSameObject(e->GetObjectClass(o), JavaCPP_classes[" +
                        jclasses.register(cls) + "])) {");
                out.println("        return;");
                out.println("    }");
                out.println("    " + typeName + " rpointer;");
                returnVariable = "rpointer = ";
            }
        } else {
            String[] typeName = getAnnotatedCPPTypeName(methodInfo.annotations, methodInfo.returnType);
            if (methodInfo.valueSetter || methodInfo.memberSetter) {
                out.println("    jobject r = o;");
            } else if (methodInfo.returnType == String.class) {
                out.println("    jstring r = NULL;");
                out.println("    const char *rpointer;");
                returnVariable = "rpointer = ";
            } else if (methodInfo.bufferGetter) {
                out.println("    jobject r = NULL;");
                out.println("    char *rpointer;");
                returnVariable = "rpointer = ";
            } else if (Pointer.class.isAssignableFrom(methodInfo.returnType)) {
                Class returnBy = getBy(methodInfo.annotations);
                out.println("    jobject r = NULL;");
                if (returnBy == ByVal.class) {
                    out.println("    " + typeName[0] + "* rpointer" + typeName[1] + ";");
                    if (methodInfo.valueGetter || methodInfo.memberGetter) {
                        returnVariable = "rpointer = &";
                    } else {
                        returnVariable = "rpointer = new " + typeName[0] + typeName[1] + "(";
                    }
                } else if (returnBy == ByRef.class) {
                    String valueTypeName = typeName[0].substring(0, typeName[0].length()-1);
                    out.println("    " + valueTypeName + "* rpointer" + typeName[1] + ";");
                    returnVariable = "rpointer = &";
                } else if (returnBy == ByPtrPtr.class) {
                    String pointerTypeName = typeName[0].substring(0, typeName[0].length()-1);
                    out.println("    " + pointerTypeName + " rpointer" + typeName[1] + ";");
                    returnVariable = "rpointer = *";
                } else if (returnBy == ByPtrRef.class) {
                    String pointerTypeName = typeName[0].substring(0, typeName[0].length()-1);
                    out.println("    " + pointerTypeName + " rpointer" + typeName[1] + ";");
                    returnVariable = "rpointer = ";
                } else { // default ByPtr
                    out.println("    " + typeName[0] + " rpointer" + typeName[1] + ";");
                    returnVariable = "rpointer = ";
                }
            } else if (methodInfo.returnType.isPrimitive()) {
                out.println("    " + getJNITypeName(methodInfo.returnType) + " r = 0;");
                out.println("    " + typeName[0] + " rvalue" + typeName[1] + ";");
                returnVariable = "rvalue = ";
            } else {
                logger.log(Level.WARNING, "Method \"" + methodInfo.method + "\" has unsupported return type \"" +
                        methodInfo.returnType.getCanonicalName() + "\". Compilation will most likely fail.");
            }
        }
        return returnVariable;
    }

    private boolean doMainOperation(Class<?> cls, MethodInformation methodInfo,
            String returnVariable, boolean[] hasPointer) {
        boolean needsCatchBlock = false;
        String typeName = getCPPTypeName(cls);
        String valueTypeName = typeName.substring(0, typeName.length()-1);
        String suffix = ");";
        if (methodInfo.deallocator) {
            out.println("    void *allocatedAddress = jlong_to_ptr(p0);");
            out.println("    void (*deallocatorAddress)(void *) = (void(*)(void*))jlong_to_ptr(p1);");
            out.println("    if (deallocatorAddress != NULL && allocatedAddress != NULL) {");
            out.println("        (*deallocatorAddress)(allocatedAddress);");
            out.println("    }");
            return false; // nothing else should be appended here for deallocator
        } else if (methodInfo.valueGetter || methodInfo.valueSetter ||
                methodInfo.memberGetter || methodInfo.memberSetter) {
            out.print("    " + returnVariable);
            if ((methodInfo.valueSetter || methodInfo.memberSetter) && String.class ==
                    methodInfo.parameterTypes[methodInfo.parameterTypes.length-1]) {
                out.print("strcpy(");
                suffix = ");";
            } else {
                suffix = ";";
            }
            if (Modifier.isStatic(methodInfo.modifiers)) {
                out.print(methodInfo.memberName);
            } else if (methodInfo.memberGetter || methodInfo.memberSetter) {
                out.print("pointer->" + methodInfo.memberName);
            } else { // methodInfo.valueGetter || methodInfo.valueSetter
                out.print("*pointer");
            }
        } else if (methodInfo.bufferGetter) {
            out.println("    " + returnVariable + "pointer;");
            out.print("    jlong capacity = ");
            suffix = ";";
        } else { // function call
            out.println("    try {");
            needsCatchBlock = true;
            out.print  ("        " + returnVariable);
            if (FunctionPointer.class.isAssignableFrom(cls)) {
                out.print("(*pointer)(");
            } else if (methodInfo.allocator) {
                out.print("new " + valueTypeName + (methodInfo.arrayAllocator ? "[" : "("));
                suffix = methodInfo.arrayAllocator ? "];" : ");";
            } else if (Modifier.isStatic(methodInfo.modifiers)) {
                if (valueTypeName.length() > 0) {
                    valueTypeName += "::";
                }
                out.print(valueTypeName + methodInfo.memberName + "(");
            } else {
                out.print("pointer->" + methodInfo.memberName + "(");
            }
        }

        for (int j = 0; j < methodInfo.parameterTypes.length; j++) {
            if ((methodInfo.memberSetter || methodInfo.valueSetter) &&
                    j >= methodInfo.parameterTypes.length-1) {
                if (methodInfo.memberNameSuffix != null) {
                    out.print(methodInfo.memberNameSuffix);
                }
                if (String.class == methodInfo.parameterTypes[methodInfo.parameterTypes.length-1]) {
                    out.print(", "); // for strcpy
                } else {
                    out.print(" = ");
                }
            } else if (methodInfo.valueGetter || methodInfo.valueSetter ||
                    methodInfo.memberGetter || methodInfo.memberSetter) {
                // print array indices to access array members
                out.print("[p" + j + "]");
                continue;
            }

            String cast = getCast(methodInfo.parameterAnnotations[j]);
            if ((cast == null || cast.length() == 0) && methodInfo.pairedMethod != null &&
                    (methodInfo.valueSetter || methodInfo.memberSetter)) {
                cast = getCast(methodInfo.pairedMethod.getAnnotations());
            }
            if (("(void*)".equals(cast) || "(void *)".equals(cast)) &&
                    methodInfo.parameterTypes[j] == long.class) {
                out.print("jlong_to_ptr(p" + j + ")");
            } else if (hasPointer[j]) {
                Class passBy = getBy(methodInfo.parameterAnnotations[j]);
                if (passBy == null && methodInfo.pairedMethod != null &&
                        (methodInfo.valueSetter || methodInfo.memberSetter)) {
                    passBy = getBy(methodInfo.pairedMethod.getAnnotations());
                }
                if (passBy == ByVal.class || passBy == ByRef.class) {
                    // XXX: add check for null pointer here somehow
                    out.print("*" + cast + "pointer" + j);
                } else if (passBy == ByPtrPtr.class) {
                    out.print(cast + "&pointer" + j);
                } else { // ByPtr.class || ByPtrRef.class
                    out.print(cast + "pointer" + j);
                }
            } else {
                out.print(cast + "p" + j);
            }
            if (j < methodInfo.parameterTypes.length - 1) {
                out.print(", ");
            }
        }

        if (!methodInfo.memberSetter && !methodInfo.valueSetter && methodInfo.memberNameSuffix != null) {
            out.print(methodInfo.memberNameSuffix);
        }
        Class returnBy = getBy(methodInfo.annotations);
        if (returnBy == ByVal.class && !methodInfo.valueGetter &&
                !methodInfo.memberGetter && !methodInfo.bufferGetter) {
            out.print(")");
        }
        out.println(suffix);
        return needsCatchBlock;
    }

    private void doReturnAfter(Class cls, MethodInformation methodInfo, boolean needsCatchBlock) {
        String indent = needsCatchBlock ? "        " : "    ";
        if (methodInfo.returnType == void.class) {
            if (methodInfo.allocator || methodInfo.arrayAllocator) {
                    out.println(indent + "jvalue args[2];");
                    out.println(indent + "args[0].j = ptr_to_jlong(rpointer);");
                    out.print  (indent + "args[1].j = ptr_to_jlong(&JavaCPP_" + mangle(cls.getName()));
                if (methodInfo.arrayAllocator) {
                    out.println("_deallocateArray);");
                    arrayDeallocators.register(cls);
                } else {
                    out.println("_deallocate);");
                    deallocators.register(cls);
                }
                out.println(indent + "e->CallNonvirtualVoidMethodA(o, JavaCPP_classes[" +
                        jclasses.register(Pointer.class) + "], JavaCPP_initMethodID, args);");
            }
        } else {
            if (methodInfo.valueSetter || methodInfo.memberSetter) {
                // nothing
            } else if (methodInfo.returnType == String.class) {
                out.println(indent + "if (rpointer != NULL) {");
                out.println(indent + "    r = e->NewStringUTF(rpointer);");
                out.println(indent + "}");
            } else if (methodInfo.bufferGetter) {
                out.println(indent + "if (rpointer != NULL) {");
                out.println(indent + "    r = e->NewDirectByteBuffer(rpointer, capacity);");
                out.println(indent + "}");
            } else if (Pointer.class.isAssignableFrom(methodInfo.returnType)) {
                Class returnBy = getBy(methodInfo.annotations);
                if (!Modifier.isStatic(methodInfo.modifiers) && 
                        cls == methodInfo.returnType && returnBy != ByVal.class) {
                    out.println(indent + "if (rpointer == pointer) {");
                    out.println(indent + "    r = o;");
                    out.println(indent + "} else if (rpointer != NULL) {");
                } else {
                    out.println(indent + "if (rpointer != NULL) {");
                }
                out.println(indent + "    r = e->AllocObject(JavaCPP_classes[" +
                        jclasses.register(methodInfo.returnType) + "]);");
                if (returnBy == ByVal.class && !methodInfo.valueGetter &&
                        !methodInfo.memberGetter && !methodInfo.bufferGetter) {
                    out.println(indent + "    jvalue args[2];");
                    out.println(indent + "    args[0].j = ptr_to_jlong(rpointer);");
                    out.println(indent + "    args[1].j = ptr_to_jlong(&JavaCPP_" +
                            mangle(methodInfo.returnType.getName()) +"_deallocate);");
                    out.println(indent + "    e->CallNonvirtualVoidMethodA(r, JavaCPP_classes[" +
                            jclasses.register(Pointer.class) + "], JavaCPP_initMethodID, args);");
                    deallocators.register(methodInfo.returnType);
                } else {
                    out.println(indent + "    e->SetLongField(r, JavaCPP_addressFieldID, ptr_to_jlong(rpointer));");
                }
                out.println(indent + "}");
            } else if (methodInfo.returnType.isPrimitive()) {
                out.println(indent + "r = (" + getJNITypeName(methodInfo.returnType) + ")rvalue;");
            }
        }
    }

    private void doParametersAfter(MethodInformation methodInfo, boolean needsCatchBlock, boolean[] mayChange) {
        String indent = needsCatchBlock ? "    " : "";
        for (int j = 0; j < methodInfo.parameterTypes.length; j++) {
            if (mayChange[j] && !methodInfo.valueSetter && !methodInfo.memberSetter) {
                out.println(indent + "    if (p" + j + " != NULL) e->SetLongField(p" + j +
                        ", JavaCPP_addressFieldID, ptr_to_jlong(pointer" + j + "));");
            }
        }

        if (needsCatchBlock) {
            out.println("    } catch (...) {");
            out.println("        JavaCPP_handleException(e);");
            out.println("    }");
        }

        for (int j = 0; j < methodInfo.parameterTypes.length; j++) {
            if (methodInfo.parameterTypes[j] == String.class) {
                out.println("    if (p" + j + " != NULL) e->ReleaseStringUTFChars(p" + j + ", pointer" + j + ");");
            } else if (methodInfo.parameterTypes[j].isArray()) {
                String s = methodInfo.parameterTypes[j].getComponentType().getName();
                s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
                out.println("    if (p" + j + " != NULL) e->Release" + s + "ArrayElements(p" + j + ", pointer" + j + ", 0);");
            }
        }
    }

    private Method doFunctionDefinitions(Class<?> cls, boolean[] callbackAllocators) {
        if (!FunctionPointer.class.isAssignableFrom(cls)) {
            return null;
        }
        Method[] methods = cls.getDeclaredMethods();
        Convention convention = cls.getAnnotation(Convention.class);
        String callingConvention = convention == null ? "" : convention.value() + " ";

        Method functionMethod = null;
        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            int modifiers = methods[i].getModifiers();
            Class[] parameterTypes = methods[i].getParameterTypes();
            Class returnType = methods[i].getReturnType();
            if (!Modifier.isNative(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            if (methodName.startsWith("allocate") && returnType == void.class && parameterTypes.length == 0) {
                // found a callback allocator method
                callbackAllocators[i] = true;
            } else if (methodName.startsWith("call")) {
                // found a function caller method
                functionMethod = methods[i];
            }
        }
        if (functionMethod != null) {
            Class returnType = functionMethod.getReturnType();
            Class[] parameterTypes = functionMethod.getParameterTypes();
            Annotation[] annotations = functionMethod.getAnnotations();
            Annotation[][] parameterAnnotations = functionMethod.getParameterAnnotations();
            String[] typeName = getAnnotatedCPPTypeName(annotations, returnType);
            String s = "typedef " + typeName[0] + typeName[1] + " (" + callingConvention +
                    "JavaCPP_" + mangle(cls.getName()) + ")(";
            for (int j = 0; j < parameterTypes.length; j++) {
                typeName = getAnnotatedCPPTypeName(parameterAnnotations[j], parameterTypes[j]);
                s += typeName[0] + typeName[1] + " p" + j;
                if (j < parameterTypes.length - 1) {
                    s += ", ";
                }
            }
            functionDefinitions.register(s + ");");
        }
        return functionMethod;
    }

    private void doCallback(Class<?> cls, Method callbackMethod, String callbackName) {
        Class callbackReturnType = callbackMethod.getReturnType();
        Class[] callbackParameterTypes = callbackMethod.getParameterTypes();
        Annotation[] callbackAnnotations = callbackMethod.getAnnotations();
        Annotation[][] callbackParameterAnnotations = callbackMethod.getParameterAnnotations();
        Convention convention = cls.getAnnotation(Convention.class);
        String callingConvention = convention == null ? "" : convention.value() + " ";

        functionPointers.register("JavaCPP_" + callbackName + "_instance");
        out.println("static jmethodID JavaCPP_" + callbackName + "_callMethodID = NULL;");
        String[] typeName = getAnnotatedCPPTypeName(callbackAnnotations, callbackReturnType);
        out.print("static " + typeName[0] + typeName[1] + " " + callingConvention + "JavaCPP_" + callbackName + "_callback(");
        for (int j = 0; j < callbackParameterTypes.length; j++) {
            typeName = getAnnotatedCPPTypeName(callbackParameterAnnotations[j], callbackParameterTypes[j]);
            out.print(typeName[0] + typeName[1] + " p" + j);
            if (j < callbackParameterTypes.length - 1) {
                out.print(", ");
            }
        }
        out.println(") {");
        String returnVariable = "";
        if (callbackReturnType != void.class) {
            out.println("    " + getJNITypeName(callbackReturnType) + " r = 0;");
            returnVariable = "r";
        }
        out.println("    JNIEnv *e;");
        out.println("    if (JavaCPP_vm->AttachCurrentThread((void **)&e, NULL) != JNI_OK) {");
        out.println("        fprintf(stderr, \"Could not attach the JavaVM to the current thread in callback for " + cls.getName() + ".\");");
        out.println("        return" + (callbackReturnType == void.class ? ";" : " " + getCast(callbackAnnotations) + "0;"));
        out.println("    }");
        if (callbackParameterTypes.length > 0) {
            out.println("    jvalue args[" + callbackParameterTypes.length + "];");
            for (int j = 0; j < callbackParameterTypes.length; j++) {
                if (Pointer.class.isAssignableFrom(callbackParameterTypes[j])) {
                    Class passBy = getBy(callbackParameterAnnotations[j]);
                    out.println("    jobject o" + j + " = NULL;");
                    out.print  ("    void *pointer" + j + " = (void*)");
                    if (passBy == ByVal.class || passBy == ByRef.class) {
                        out.println("&p" + j + ";");
                    } else if (passBy == ByPtrPtr.class) {
                        // XXX: add check for null pointer here somehow
                        out.println("*p" + j + ";");
                    } else { // ByPtr.class || ByPtrRef.class
                        out.println("p" + j + ";");
                    }
                    String s = "    o" + j + " = e->AllocObject(JavaCPP_classes[" +
                            jclasses.register(callbackParameterTypes[j]) + "]);";
                    if (passBy == ByPtrPtr.class || passBy == ByPtrRef.class) {
                        out.println(s);
                    } else {
                        out.println("    if (pointer" + j + " != NULL) { ");
                        out.println("    " + s);
                        out.println("    }");
                    }
                    out.println("    if (o" + j + " != NULL) { ");
                    out.println("        e->SetLongField(o" + j + ", JavaCPP_addressFieldID, ptr_to_jlong(pointer" + j + "));");
                    out.println("    }");
                    out.println("    args[" + j + "].l = o" + j + ";");
                } else if (callbackParameterTypes[j] == String.class) {
                    out.println("    jstring o" + j + " = p" + j + " == NULL ? NULL : e->NewStringUTF(p" + j + ");");
                    out.println("    args[" + j + "].l = o" + j + ";");
                } else if (callbackParameterTypes[j].isPrimitive()) {
                    out.println("    args[" + j + "]." +
                            getSignature(callbackParameterTypes[j]).toLowerCase() + " = p" + j + ";");
                } else {
                    logger.log(Level.WARNING, "Callback \"" + callbackMethod + "\" has unsupported parameter type \"" +
                            callbackParameterTypes[j].getCanonicalName() + "\". Compilation will most likely fail.");
                }
            }
        }

        if (returnVariable.length() > 0) {
            returnVariable += " = ";
        }
        String s = "Object";
        if (callbackReturnType.isPrimitive()) {
            s = callbackReturnType.getName();
            s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        out.println("    " + returnVariable + "e->Call" + s + "MethodA(JavaCPP_" + callbackName +
                "_instance, JavaCPP_" + callbackName + "_callMethodID, " + 
                (callbackParameterTypes.length == 0 ? "NULL);" : "args);"));

        for (int j = 0; j < callbackParameterTypes.length; j++) {
            if (Pointer.class.isAssignableFrom(callbackParameterTypes[j])) {
                Class passBy = getBy(callbackParameterAnnotations[j]);
                if (passBy == ByPtrPtr.class || passBy == ByPtrRef.class) {
                    out.println("    pointer" + j + " = jlong_to_ptr(e->GetLongField(o" + j + ", JavaCPP_addressFieldID);");
                    if (passBy == ByPtrPtr.class) {
                        out.print("    *p" + j);
                    } else {
                        out.print("    p" + j);
                    }
                    typeName = getAnnotatedCPPTypeName(callbackParameterAnnotations[j], callbackParameterTypes[j]);
                    String pointerTypeName = typeName[0].substring(0, typeName[0].length()-1);
                    out.println(" = (" + pointerTypeName + typeName[1] + ")pointer" + j + ";");
                }
            }
            if (!callbackParameterTypes[j].isPrimitive()) {
                out.println("    e->DeleteLocalRef(o" + j + ");");
            }
        }

        if (callbackReturnType != void.class) {
            if (Pointer.class.isAssignableFrom(callbackReturnType)) {
                typeName = getAnnotatedCPPTypeName(callbackAnnotations, callbackReturnType);
                out.println("    return r == NULL ? NULL : (" + typeName[0] + typeName[1] +
                        ")jlong_to_ptr(e->GetLongField(r, JavaCPP_addressFieldID));");
//            } else if (callbackReturnType == String.class) {
//                out.println("    return r == NULL ? NULL : e->GetStringUTFChars(r, NULL);");
            } else if (Buffer.class.isAssignableFrom(callbackReturnType)) {
                typeName = getAnnotatedCPPTypeName(callbackAnnotations, callbackReturnType);
                out.println("    return r == NULL ? NULL : (" + typeName[0] + typeName[1] +
                        ")e->GetDirectBufferAddress(r);");
            } else if (callbackReturnType.isPrimitive()) {
                out.println("    return " + getCast(callbackAnnotations) + "r;");
            } else {
                logger.log(Level.WARNING, "Callback \"" + callbackMethod + "\" has unsupported return type \"" +
                        callbackReturnType.getCanonicalName() + "\". Compilation will most likely fail.");
            }
        }
        out.println("}");
    }

    private void doCallbackAllocator(Class cls, Method callbackMethod, String callbackName) {
        // XXX: Here, we should actually allocate new trampolines on the heap somehow...
        // For now it just bumps out from the global variable the last object that called this method
        out.println("    e->DeleteGlobalRef(JavaCPP_" + callbackName + "_instance);");
        out.println("    JavaCPP_" + callbackName + "_instance = e->NewGlobalRef(o);");
        out.println("    if (JavaCPP_" + callbackName + "_instance == NULL) {");
        out.println("        fprintf(stderr, \"Error creating global reference of " + cls.getName() + " instance for callback.\");");
        out.println("        return;");
        out.println("    }");
        out.println("    JavaCPP_" + callbackName + "_callMethodID = e->GetMethodID(e->GetObjectClass(o), \"" +
                callbackMethod.getName() + "\", \"(" + getSignature(callbackMethod.getParameterTypes()) + ")" +
                getSignature(callbackMethod.getReturnType()) + "\");");
        out.println("    if (JavaCPP_" + callbackName + "_callMethodID == NULL) {");
        out.println("        fprintf(stderr, \"Error getting method ID of function caller \\\"" + callbackMethod + "\\\" for callback.\");");
        out.println("        return;");
        out.println("    }");
        out.println("    e->SetLongField(o, JavaCPP_addressFieldID, ptr_to_jlong(&JavaCPP_" + callbackName + "_callback));");
        out.println("}");
    }

    public boolean checkPlatform(Platform platform) {
        if (platform == null) {
            return true;
        }
        String platformName = properties.getProperty("platform.name");
        String[][] names = { platform.value(), platform.not() };
        boolean[] matches = { false, false };
        for (int i = 0; i < names.length; i++) {
            for (String s : names[i]) {
                if (platformName.startsWith(s)) {
                    matches[i] = true;
                    break;
                }
            }
        }
        if ((names[0].length == 0 || matches[0]) && (names[1].length == 0 || !matches[1])) {
            return true;
        }
        return false;
    }

    public static class MethodInformation {
        public Method method;
        public Annotation[] annotations;
        public int modifiers;
        public Class<?> returnType;
        public String name, memberName, memberNameSuffix;
        public Class<?>[] parameterTypes;
        public Annotation[][] parameterAnnotations;
        public boolean overloaded, noOffset, deallocator, allocator, arrayAllocator,
                bufferGetter, valueGetter, valueSetter, memberGetter, memberSetter;
        public Method pairedMethod;
    }
    public static MethodInformation getMethodInformation(Method method) {
        if (!Modifier.isNative(method.getModifiers())) {
            return null;
        }
        MethodInformation info = new MethodInformation();
        info.method      = method;
        info.annotations = method.getAnnotations();
        info.modifiers   = method.getModifiers();
        info.returnType  = method.getReturnType();
        info.name        = method.getName();
        info.memberName  = info.name;
        Name memberName  = method.getAnnotation(Name.class);
        if (memberName != null) {
            info.memberName       = memberName.value();
            info.memberNameSuffix = memberName.suffix();
        }
        info.parameterTypes       = method.getParameterTypes();
        info.parameterAnnotations = method.getParameterAnnotations();
        Class role = getMethodRole(method);

        boolean canBeGetter = info.returnType != void.class;
        boolean canBeSetter = true;
        for (int j = 0; j < info.parameterTypes.length; j++) {
            if (info.parameterTypes[j] != int.class && info.parameterTypes[j] != long.class) {
                canBeGetter = false;
                if (j < info.parameterTypes.length-1) {
                    canBeSetter = false;
                }
            }
        }
        boolean canBeAllocator = !Modifier.isStatic(info.modifiers) &&
                info.returnType == void.class;
        boolean canBeArrayAllocator = canBeAllocator && info.parameterTypes.length == 1 &&
                (info.parameterTypes[0] == int.class || info.parameterTypes[0] == long.class);

        boolean valueGetter = false;
        boolean valueSetter = false;
        boolean memberGetter = false;
        boolean memberSetter = false;
        Method pairedMethod = null;
        Method[] methods = method.getDeclaringClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method2 = methods[i];
            int modifiers2          = method2.getModifiers();
            Class returnType2       = method2.getReturnType();
            String methodName2      = method2.getName();
            Class[] parameterTypes2 = method2.getParameterTypes();

            if (method.equals(method2) || !Modifier.isNative(modifiers2)) {
                continue;
            }

            if ("get".equals(info.name) && "put".equals(methodName2) && info.parameterTypes.length == 0 &&
                    parameterTypes2.length == 1 && parameterTypes2[0] == info.returnType && canBeGetter) {
                valueGetter = true;
                pairedMethod = method2;
            } else if ("put".equals(info.name) && "get".equals(methodName2) && info.parameterTypes.length == 1 &&
                    parameterTypes2.length == 0 && info.parameterTypes[0] == returnType2 && canBeSetter) {
                valueSetter = true;
                pairedMethod = method2;
            } else if (methodName2.equals(info.name)) {
                info.overloaded = true;

                boolean sameIndexParameters = true;
                for (int j = 0; j < info.parameterTypes.length && j < parameterTypes2.length; j++) {
                    if (info.parameterTypes[j] != parameterTypes2[j]) {
                        sameIndexParameters = false;
                    }
                }
                if (sameIndexParameters && parameterTypes2.length-1 == info.parameterTypes.length &&
                        info.returnType == parameterTypes2[parameterTypes2.length-1] && canBeGetter) {
                    memberGetter = true;
                    pairedMethod = method2;
                } else if (sameIndexParameters && info.parameterTypes.length-1 == parameterTypes2.length &&
                        returnType2 == info.parameterTypes[info.parameterTypes.length-1] && canBeSetter) {
                    memberSetter = true;
                    pairedMethod = method2;
                }
            }
        }

        if (canBeGetter && role == ValueGetter.class) {
            info.valueGetter = true;
        } else if (canBeSetter && role == ValueSetter.class) {
            info.valueSetter = true;
        } else if (canBeGetter && role == MemberGetter.class) {
            info.memberGetter = true;
        } else if (canBeSetter && role == MemberSetter.class) {
            info.memberSetter = true;
        } else if (canBeAllocator && role == Allocator.class) {
            info.allocator = true;
        } else if (canBeArrayAllocator && role == ArrayAllocator.class) {
            info.allocator = info.arrayAllocator = true;
        } else if (role == null) {
            // try to guess the role of the method
            if (info.returnType == void.class && "deallocate".equals(info.name) &&
                    !Modifier.isStatic(info.modifiers) && info.parameterTypes.length == 2 &&
                    info.parameterTypes[0] == long.class && info.parameterTypes[1] == long.class) {
                info.deallocator = true;
            } else if (canBeAllocator && "allocate".equals(info.name)) {
                info.allocator = true;
            } else if (canBeArrayAllocator && "allocateArray".equals(info.name)) {
                info.allocator = info.arrayAllocator = true;
            } else if (info.returnType.isAssignableFrom(ByteBuffer.class) && "asNativeBuffer".equals(info.name) &&
                    !Modifier.isStatic(info.modifiers) && info.parameterTypes.length == 1 &&
                    (info.parameterTypes[0] == int.class || info.parameterTypes[0] == long.class)) {
                info.bufferGetter = true;
            } else if (valueGetter) {
                info.valueGetter = true;
                info.pairedMethod = pairedMethod;
            } else if (valueSetter) {
                info.valueSetter = true;
                info.pairedMethod = pairedMethod;
            } else if (memberGetter) {
                info.memberGetter = true;
                info.pairedMethod = pairedMethod;
            } else if (memberSetter) {
                info.memberSetter = true;
                info.pairedMethod = pairedMethod;
            }
        } else {
            logger.log(Level.WARNING, "Method \"" + method + "\" cannot take on the role of \"" +
                    role + "\". No code will be generated.");
            return null;
        }

        if (memberName == null && info.pairedMethod != null) {
            memberName = info.pairedMethod.getAnnotation(Name.class);
            if (memberName != null) {
                info.memberName       = memberName.value();
                info.memberNameSuffix = memberName.suffix();
            }
        }
        info.noOffset = method.isAnnotationPresent(NoOffset.class);
        if (!info.noOffset && info.pairedMethod != null) {
            info.noOffset = info.pairedMethod.isAnnotationPresent(NoOffset.class);
        }
        return info;
    }

    public static String getCast(Annotation ... annotations) {
        for (Annotation a: annotations) {
            if (a instanceof Cast) {
                return "(" + ((Cast)a).value() + ")";
            }
        }
        return "";
    }

    public static Class<? extends Annotation> getBy(Annotation ... annotations) {
        Annotation byAnnotation = null;
        for (Annotation a: annotations) {
            if (a instanceof ByPtr || a instanceof ByPtrPtr || a instanceof ByPtrRef ||
                    a instanceof ByRef || a instanceof ByVal) {
                if (byAnnotation != null) {
                    logger.log(Level.WARNING, "\"By\" annotation \"" + byAnnotation +
                            "\" already found. Ignoring superfluous annotation \"" + a + "\".");
                } else {
                    byAnnotation = a;
                }
            }
        }
        return byAnnotation == null ? null : byAnnotation.annotationType();
    }

    public static Class<? extends Annotation> getMethodRole(Method method) {
        Annotation[] annotations = method.getAnnotations();
        Annotation roleAnnotation = null;
        for (Annotation a: annotations) {
            if (a instanceof Function || a instanceof Allocator || a instanceof ArrayAllocator ||
                    a instanceof ValueSetter || a instanceof ValueGetter ||
                    a instanceof MemberGetter || a instanceof MemberSetter) {
                if (roleAnnotation != null) {
                    logger.log(Level.WARNING, "Role annotation \"" + roleAnnotation +
                            "\" already found. Ignoring superfluous annotation \"" + a + "\".");
                } else {
                    roleAnnotation = a;
                }
            }
        }
        return roleAnnotation == null ? null : roleAnnotation.annotationType();
    }

    public static String[] getAnnotatedCPPTypeName(Annotation[] annotations, Class<?> type) {
        String name = getCPPTypeName(type);
        for (Annotation a: annotations) {
            if (a instanceof Cast) {
                name = ((Cast)a).value();
            }
        }
        String prefix = name, suffix = "";
        int parenthesis = name.indexOf(')');
        if (parenthesis > 0) {
            prefix = name.substring(0, parenthesis).trim();
            suffix = name.substring(parenthesis).trim();
        }

        Class by = getBy(annotations);
        if (by == ByVal.class) {
            prefix = prefix.substring(0, prefix.length()-1);
        } else if (by == ByRef.class) {
            prefix = prefix.substring(0, prefix.length()-1) + "&";
        } else if (by == ByPtrPtr.class) {
            prefix = prefix + "*";
        } else if (by == ByPtrRef.class) {
            prefix = prefix + "&";
        } // else default to ByPtr.class
        return new String[] { prefix, suffix };
    }

    public static String getCPPTypeName(Class<?> type) {
        if (type == Buffer.class || type == Pointer.class) {
            return "void*";
        } else if (type == byte[].class || type == ByteBuffer.class || type == BytePointer.class) {
            return "signed char*";
        } else if (type == short[].class || type == ShortBuffer.class || type == ShortPointer.class) {
            return "short*";
        } else if (type == int[].class || type == IntBuffer.class || type == IntPointer.class) {
            return "int*";
        } else if (type == long[].class || type == LongBuffer.class || type == LongPointer.class) {
            return "jlong*";
        } else if (type == float[].class || type == FloatBuffer.class || type == FloatPointer.class) {
            return "float*";
        } else if (type == double[].class || type == DoubleBuffer.class || type == DoublePointer.class) {
            return "double*";
        } else if (type == char[].class || type == CharBuffer.class || type == CharPointer.class) {
            return "unsigned short*";
        } else if (type == PointerPointer.class) {
            return "void**";
        } else if (type == String.class) {
            return "const char*";
        } else if (type == byte.class) {
            return "signed char";
        } else if (type == char.class) {
            return "unsigned short";
        } else if (type == long.class) {
            return "jlong";
        } else if (type == boolean.class) {
            return "unsigned char";
        } else if (type.isPrimitive()) {
            return type.getName();
        } else if (FunctionPointer.class.isAssignableFrom(type)) {
            return "JavaCPP_" + mangle(type.getName()) + "*";
        } else {
            String spacedType = "";
            while (type != null) {
                Namespace namespace = type.getAnnotation(Namespace.class);
                String spaceName = namespace != null ? namespace.value() : "";
                if (Pointer.class.isAssignableFrom(type)) {
                    Name name = type.getAnnotation(Name.class);
                    String s;
                    if (name == null) {
                        s = type.getName();
                        s = s.substring(s.lastIndexOf("$")+1);
                    } else {
                        s = name.value();
                    }
                    if (spaceName.length() == 0) {
                        spaceName = s;
                    } else {
                        spaceName = spaceName + "::" + s;
                    }
                }
                if (spacedType.length() == 0) {
                    spacedType = spaceName;
                } else if (spaceName.length() > 0) {
                    spacedType = spaceName + "::" + spacedType;
                }
                type = type.getDeclaringClass();
            }
            return spacedType + "*";
        }
    }

    public static String getJNITypeName(Class type) {
        if (type == byte.class) {
            return "jbyte";
        } else if (type == short.class) {
            return "jshort";
        } else if (type == int.class) {
            return "jint";
        } else if (type == long.class) {
            return "jlong";
        } else if (type == float.class) {
            return "jfloat";
        } else if (type == double.class) {
            return "jdouble";
        } else if (type == boolean.class) {
            return "jboolean";
        } else if (type == char.class) {
            return "jchar";
        } else if (type == byte[].class) {
            return "jbyteArray";
        } else if (type == short[].class) {
            return "jshortArray";
        } else if (type == int[].class) {
            return "jintArray";
        } else if (type == long[].class) {
            return "jlongArray";
        } else if (type == float[].class) {
            return "jfloatArray";
        } else if (type == double[].class) {
            return "jdoubleArray";
        } else if (type == boolean[].class) {
            return "jbooleanArray";
        } else if (type == char[].class) {
            return "jcharArray";
        } else if (type == String.class) {
            return "jstring";
        } else if (type == void.class) {
            return "void";
        } else {
            return "jobject";
        }
    }

    public static String getSignature(Class ... types) {
        StringBuilder signature = new StringBuilder(2*types.length);
        for (int i = 0; i < types.length; i++) {
            signature.append(getSignature(types[i]));
        }
        return signature.toString();
    }
    public static String getSignature(Class type) {
        if (type == byte.class) {
            return "B";
        } else if (type == short.class) {
            return "S";
        } else if (type == int.class) {
            return "I";
        } else if (type == long.class) {
            return "J";
        } else if (type == float.class) {
            return "F";
        } else if (type == double.class) {
            return "D";
        } else if (type == boolean.class) {
            return "Z";
        } else if (type == char.class) {
            return "C";
        } else if (type == void.class) {
            return "V";
        } else if (type.isArray()) {
            return type.getName().replace(".", "/");
        } else {
            return "L" + type.getName().replace(".", "/") + ";";
        }
    }

    public static String mangle(String name) {
        StringBuilder mangledName = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                mangledName.append(c);
            } else if (c == '_') {
                mangledName.append("_1");
            } else if (c == ';') {
                mangledName.append("_2");
            } else if (c == '[') {
                mangledName.append("_3");
            } else if (c == '.' || c == '/') {
                mangledName.append("_");
            } else {
                String code = Integer.toHexString(c);
                mangledName.append("_0");
                switch (code.length()) {
                    case 1:  mangledName.append("0");
                    case 2:  mangledName.append("0");
                    case 3:  mangledName.append("0");
                    default: mangledName.append(code);
                }
            }
        }
        return mangledName.toString();
    }
}
