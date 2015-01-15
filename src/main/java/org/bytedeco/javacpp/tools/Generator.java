/*
 * Copyright (C) 2011,2012,2013,2014 Samuel Audet
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

import java.io.Closeable;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.bytedeco.javacpp.BoolPointer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.CLongPointer;
import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.ClassProperties;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.ShortPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.javacpp.annotation.Adapter;
import org.bytedeco.javacpp.annotation.Allocator;
import org.bytedeco.javacpp.annotation.ArrayAllocator;
import org.bytedeco.javacpp.annotation.ByPtr;
import org.bytedeco.javacpp.annotation.ByPtrPtr;
import org.bytedeco.javacpp.annotation.ByPtrRef;
import org.bytedeco.javacpp.annotation.ByRef;
import org.bytedeco.javacpp.annotation.ByVal;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Const;
import org.bytedeco.javacpp.annotation.Convention;
import org.bytedeco.javacpp.annotation.Function;
import org.bytedeco.javacpp.annotation.Index;
import org.bytedeco.javacpp.annotation.MemberGetter;
import org.bytedeco.javacpp.annotation.MemberSetter;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.annotation.Namespace;
import org.bytedeco.javacpp.annotation.NoDeallocator;
import org.bytedeco.javacpp.annotation.NoException;
import org.bytedeco.javacpp.annotation.NoOffset;
import org.bytedeco.javacpp.annotation.Opaque;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Raw;
import org.bytedeco.javacpp.annotation.ValueGetter;
import org.bytedeco.javacpp.annotation.ValueSetter;
import org.bytedeco.javacpp.annotation.Virtual;

/**
 * The Generator is where all the C++ source code that we need gets generated.
 * It has not been designed in any meaningful way since the requirements were
 * not well understood. It is basically a prototype and is really quite a mess.
 * Now that we understand better what we need, it could use some refactoring.
 * <p>
 * When attempting to understand what the Generator does, try to run experiments
 * and inspect the generated code: It is quite readable.
 * <p>
 * Moreover, although Generator is the one ultimately doing something with the
 * various annotations it relies on, it was easier to describe the behavior its
 * meant to have with them as part of the documentation of the annotations, so
 * we can refer to them to understand more about how Generator should work:
 *
 * @see Adapter
 * @see Allocator
 * @see ArrayAllocator
 * @see ByPtr
 * @see ByPtrPtr
 * @see ByPtrRef
 * @see ByRef
 * @see ByVal
 * @see Cast
 * @see Const
 * @see Convention
 * @see Function
 * @see Index
 * @see MemberGetter
 * @see MemberSetter
 * @see Name
 * @see Namespace
 * @see NoDeallocator
 * @see NoException
 * @see NoOffset
 * @see Opaque
 * @see Platform
 * @see Raw
 * @see ValueGetter
 * @see ValueSetter
 *
 * @author Samuel Audet
 */
public class Generator implements Closeable {

    public Generator(Logger logger, ClassProperties properties) {
        this.logger = logger;
        this.properties = properties;
    }

    static final String JNI_VERSION = "JNI_VERSION_1_4";
    static final List<Class> baseClasses = Arrays.asList(new Class[] {
            Pointer.class,
            //FunctionPointer.class,
            BytePointer.class,
            ShortPointer.class,
            IntPointer.class,
            LongPointer.class,
            FloatPointer.class,
            DoublePointer.class,
            CharPointer.class,
            PointerPointer.class,
            BoolPointer.class,
            CLongPointer.class,
            SizeTPointer.class });

    final Logger logger;
    final ClassProperties properties;
    PrintWriter out, out2;
    IndexedSet<String> callbacks;
    IndexedSet<Class> functions, deallocators, arrayDeallocators, jclasses, jclassesInit;
    HashMap<Class,LinkedList<String>> members, virtualFunctions, virtualMembers;
    boolean mayThrowExceptions, usesAdapters;

    public boolean generate(String sourceFilename, String headerFilename,
            String classPath, Class<?> ... classes) throws FileNotFoundException {
        // first pass using a null writer to fill up the IndexedSet objects
        out = new PrintWriter(new Writer() {
            @Override public void write(char[] cbuf, int off, int len) { }
            @Override public void flush() { }
            @Override public void close() { }
        });
        out2 = null;
        callbacks           = new IndexedSet<String>();
        functions           = new IndexedSet<Class>();
        deallocators        = new IndexedSet<Class>();
        arrayDeallocators   = new IndexedSet<Class>();
        jclasses            = new IndexedSet<Class>();
        jclassesInit        = new IndexedSet<Class>();
        members             = new HashMap<Class,LinkedList<String>>();
        virtualFunctions    = new HashMap<Class,LinkedList<String>>();
        virtualMembers      = new HashMap<Class,LinkedList<String>>();
        mayThrowExceptions  = false;
        usesAdapters        = false;
        if (classes(true, true, classPath, classes)) {
            // second pass with a real writer
            out = new PrintWriter(sourceFilename);
            if (headerFilename != null) {
                out2 = new PrintWriter(headerFilename);
            }
            return classes(mayThrowExceptions, usesAdapters, classPath, classes);
        } else {
            return false;
        }
    }

    public void close() {
        if (out != null) {
            out.close();
        }
        if (out2 != null) {
            out2.close();
        }
    }

    boolean classes(boolean handleExceptions, boolean defineAdapters, String classPath, Class<?> ... classes) {
        String version = Generator.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "unknown";
        }
        String warning = "// Generated by JavaCPP version " + version;
        out.println(warning);
        out.println();
        if (out2 != null) {
            out2.println(warning);
            out2.println();
        }
        for (String s : properties.get("platform.define")) {
            out.println("#define " + s);
        }
        out.println();
        out.println("#ifdef __APPLE__");
        out.println("    #define _JAVASOFT_JNI_MD_H_");
        out.println();
        out.println("    #define JNIEXPORT __attribute__((visibility(\"default\")))");
        out.println("    #define JNIIMPORT");
        out.println("    #define JNICALL");
        out.println();
        out.println("    typedef int jint;");
        out.println("    typedef long long jlong;");
        out.println("    typedef signed char jbyte;");
        out.println("#endif");
        out.println("#ifdef _WIN32");
        out.println("    #define _JAVASOFT_JNI_MD_H_");
        out.println();
        out.println("    #define JNIEXPORT __declspec(dllexport)");
        out.println("    #define JNIIMPORT __declspec(dllimport)");
        out.println("    #define JNICALL __stdcall");
        out.println();
        out.println("    typedef int jint;");
        out.println("    typedef long long jlong;");
        out.println("    typedef signed char jbyte;");
        out.println("#endif");
        out.println("#include <jni.h>");
        if (out2 != null) {
            out2.println("#include <jni.h>");
        }
        out.println("#ifdef ANDROID");
        out.println("    #include <android/log.h>");
        out.println("#elif defined(__APPLE__) && defined(__OBJC__)");
        out.println("    #include <TargetConditionals.h>");
        out.println("    #include <Foundation/Foundation.h>");
        out.println("#endif");
        out.println("#if defined(ANDROID) || TARGET_OS_IPHONE");
        out.println("    #define NewWeakGlobalRef(obj) NewGlobalRef(obj)");
        out.println("    #define DeleteWeakGlobalRef(obj) DeleteGlobalRef(obj)");
        out.println("#endif");
        out.println();
        out.println("#include <stddef.h>");
        out.println("#ifndef _WIN32");
        out.println("    #include <stdint.h>");
        out.println("#endif");
        out.println("#include <stdio.h>");
        out.println("#include <stdlib.h>");
        out.println("#include <string.h>");
        out.println("#include <exception>");
        out.println("#include <new>");
        out.println();
        out.println("#define jlong_to_ptr(a) ((void*)(uintptr_t)(a))");
        out.println("#define ptr_to_jlong(a) ((jlong)(uintptr_t)(a))");
        out.println();
        out.println("#if defined(_MSC_VER)");
        out.println("    #define JavaCPP_noinline __declspec(noinline)");
        out.println("    #define JavaCPP_hidden /* hidden by default */");
        out.println("#elif defined(__GNUC__)");
        out.println("    #define JavaCPP_noinline __attribute__((noinline))");
        out.println("    #define JavaCPP_hidden   __attribute__((visibility(\"hidden\")))");
        out.println("#else");
        out.println("    #define JavaCPP_noinline");
        out.println("    #define JavaCPP_hidden");
        out.println("#endif");
        out.println();
        List[] include = { properties.get("platform.include"),
                           properties.get("platform.cinclude") };
        for (int i = 0; i < include.length; i++) {
            if (include[i] != null && include[i].size() > 0) {
                if (i == 1) {
                    out.println("extern \"C\" {");
                    if (out2 != null) {
                        out2.println("#ifdef __cplusplus");
                        out2.println("extern \"C\" {");
                        out2.println("#endif");
                    }
                }
                for (String s : (List<String>)include[i]) {
                    String line = "#include ";
                    if (!s.startsWith("<") && !s.startsWith("\"")) {
                        line += '"';
                    }
                    line += s;
                    if (!s.endsWith(">") && !s.endsWith("\"")) {
                        line += '"';
                    }
                    out.println(line);
                    if (out2 != null) {
                        out2.println(line);
                    }
                }
                if (i == 1) {
                    out.println("}");
                    if (out2 != null) {
                        out2.println("#ifdef __cplusplus");
                        out2.println("}");
                        out2.println("#endif");
                    }
                }
                out.println();
            }
        }
        out.println("static JavaVM* JavaCPP_vm = NULL;");
        out.println("static bool JavaCPP_haveAllocObject = false;");
        out.println("static bool JavaCPP_haveNonvirtual = false;");
        out.println("static const char* JavaCPP_classNames[" + jclasses.size() + "] = {");
        Iterator<Class> classIterator = jclasses.iterator();
        int maxMemberSize = 0;
        while (classIterator.hasNext()) {
            Class c = classIterator.next();
            out.print("        \"" + c.getName().replace('.','/') + "\"");
            if (classIterator.hasNext()) {
                out.println(",");
            }
            LinkedList<String> m = members.get(c);
            if (m != null && m.size() > maxMemberSize) {
                maxMemberSize = m.size();
            }
        }
        out.println(" };");
        out.println("static jclass JavaCPP_classes[" + jclasses.size() + "] = { NULL };");
        out.println("static jfieldID JavaCPP_addressFID = NULL;");
        out.println("static jfieldID JavaCPP_positionFID = NULL;");
        out.println("static jfieldID JavaCPP_limitFID = NULL;");
        out.println("static jfieldID JavaCPP_capacityFID = NULL;");
        out.println("static jmethodID JavaCPP_initMID = NULL;");
        out.println("static jmethodID JavaCPP_toStringMID = NULL;");
        out.println();
        out.println("static inline void JavaCPP_log(const char* fmt, ...) {");
        out.println("    va_list ap;");
        out.println("    va_start(ap, fmt);");
        out.println("#ifdef ANDROID");
        out.println("    __android_log_vprint(ANDROID_LOG_ERROR, \"javacpp\", fmt, ap);");
        out.println("#elif defined(__APPLE__) && defined(__OBJC__)");
        out.println("    NSLogv([NSString stringWithUTF8String:fmt], ap);");
        out.println("#else");
        out.println("    vfprintf(stderr, fmt, ap);");
        out.println("    fprintf(stderr, \"\\n\");");
        out.println("#endif");
        out.println("    va_end(ap);");
        out.println("}");
        out.println();
        out.println("static JavaCPP_noinline jclass JavaCPP_getClass(JNIEnv* env, int i) {");
        out.println("    if (JavaCPP_classes[i] == NULL && env->PushLocalFrame(1) == 0) {");
        out.println("        jclass cls = env->FindClass(JavaCPP_classNames[i]);");
        out.println("        if (cls == NULL || env->ExceptionCheck()) {");
        out.println("            JavaCPP_log(\"Error loading class %s.\", JavaCPP_classNames[i]);");
        out.println("            return NULL;");
        out.println("        }");
        out.println("        JavaCPP_classes[i] = (jclass)env->NewWeakGlobalRef(cls);");
        out.println("        if (JavaCPP_classes[i] == NULL || env->ExceptionCheck()) {");
        out.println("            JavaCPP_log(\"Error creating global reference of class %s.\", JavaCPP_classNames[i]);");
        out.println("            return NULL;");
        out.println("        }");
        out.println("        env->PopLocalFrame(NULL);");
        out.println("    }");
        out.println("    return JavaCPP_classes[i];");
        out.println("}");
        out.println();
        out.println("static JavaCPP_noinline jfieldID JavaCPP_getFieldID(JNIEnv* env, int i, const char* name, const char* sig) {");
        out.println("    jclass cls = JavaCPP_getClass(env, i);");
        out.println("    if (cls == NULL) {");
        out.println("        return NULL;");
        out.println("    }");
        out.println("    jfieldID fid = env->GetFieldID(cls, name, sig);");
        out.println("    if (fid == NULL || env->ExceptionCheck()) {");
        out.println("        JavaCPP_log(\"Error getting field ID of %s/%s\", JavaCPP_classNames[i], name);");
        out.println("        return NULL;");
        out.println("    }");
        out.println("    return fid;");
        out.println("}");
        out.println();
        out.println("static JavaCPP_noinline jmethodID JavaCPP_getMethodID(JNIEnv* env, int i, const char* name, const char* sig) {");
        out.println("    jclass cls = JavaCPP_getClass(env, i);");
        out.println("    if (cls == NULL) {");
        out.println("        return NULL;");
        out.println("    }");
        out.println("    jmethodID mid = env->GetMethodID(cls, name, sig);");
        out.println("    if (mid == NULL || env->ExceptionCheck()) {");
        out.println("        JavaCPP_log(\"Error getting method ID of %s/%s\", JavaCPP_classNames[i], name);");
        out.println("        return NULL;");
        out.println("    }");
        out.println("    return mid;");
        out.println("}");
        out.println();
        out.println("static JavaCPP_noinline jmethodID JavaCPP_getStaticMethodID(JNIEnv* env, int i, const char* name, const char* sig) {");
        out.println("    jclass cls = JavaCPP_getClass(env, i);");
        out.println("    if (cls == NULL) {");
        out.println("        return NULL;");
        out.println("    }");
        out.println("    jmethodID mid = env->GetStaticMethodID(cls, name, sig);");
        out.println("    if (mid == NULL || env->ExceptionCheck()) {");
        out.println("        JavaCPP_log(\"Error getting static method ID of %s/%s\", JavaCPP_classNames[i], name);");
        out.println("        return NULL;");
        out.println("    }");
        out.println("    return mid;");
        out.println("}");
        out.println();
        out.println("static JavaCPP_noinline jobject JavaCPP_createPointer(JNIEnv* env, int i, jclass cls = NULL) {");
        out.println("    if (cls == NULL && (cls = JavaCPP_getClass(env, i)) == NULL) {");
        out.println("        return NULL;");
        out.println("    }");
        out.println("    if (JavaCPP_haveAllocObject) {");
        out.println("        return env->AllocObject(cls);");
        out.println("    } else {");
        out.println("        jmethodID mid = env->GetMethodID(cls, \"<init>\", \"()V\");");
        out.println("        if (mid == NULL || env->ExceptionCheck()) {");
        out.println("            JavaCPP_log(\"Error getting default constructor of %s, while VM does not support AllocObject()\", JavaCPP_classNames[i]);");
        out.println("            return NULL;");
        out.println("        }");
        out.println("        return env->NewObject(cls, mid);");
        out.println("    }");
        out.println("}");
        out.println();
        out.println("static JavaCPP_noinline void JavaCPP_initPointer(JNIEnv* env, jobject obj, const void* ptr, int size, void (*deallocator)(void*)) {");
        out.println("    if (deallocator != NULL) {");
        out.println("        jvalue args[3];");
        out.println("        args[0].j = ptr_to_jlong(ptr);");
        out.println("        args[1].i = size;");
        out.println("        args[2].j = ptr_to_jlong(deallocator);");
        out.println("        if (JavaCPP_haveNonvirtual) {");
        out.println("            env->CallNonvirtualVoidMethodA(obj, JavaCPP_getClass(env, "
                                     + jclasses.index(Pointer.class) + "), JavaCPP_initMID, args);");
        out.println("        } else {");
        out.println("            env->CallVoidMethodA(obj, JavaCPP_initMID, args);");
        out.println("        }");
        out.println("    } else {");
        out.println("        env->SetLongField(obj, JavaCPP_addressFID, ptr_to_jlong(ptr));");
        out.println("        env->SetIntField(obj, JavaCPP_limitFID, size);");
        out.println("        env->SetIntField(obj, JavaCPP_capacityFID, size);");
        out.println("    }");
        out.println("}");
        out.println();
        out.println("class JavaCPP_hidden JavaCPP_exception : public std::exception {");
        out.println("public:");
        out.println("    JavaCPP_exception(const char* str) throw() {");
        out.println("        if (str == NULL) {");
        out.println("            strcpy(msg, \"Unknown exception.\");");
        out.println("        } else {");
        out.println("            strncpy(msg, str, sizeof(msg));");
        out.println("            msg[sizeof(msg) - 1] = 0;");
        out.println("        }");
        out.println("    }");
        out.println("    virtual const char* what() const throw() { return msg; }");
        out.println("    char msg[1024];");
        out.println("};");
        out.println();
        if (handleExceptions) {
            out.println("static JavaCPP_noinline jthrowable JavaCPP_handleException(JNIEnv* env, int i) {");
            out.println("    jstring str = NULL;");
            out.println("    try {");
            out.println("        throw;");
            out.println("    } catch (std::exception& e) {");
            out.println("        str = env->NewStringUTF(e.what());");
            out.println("    } catch (...) {");
            out.println("        str = env->NewStringUTF(\"Unknown exception.\");");
            out.println("    }");
            out.println("    jmethodID mid = JavaCPP_getMethodID(env, i, \"<init>\", \"(Ljava/lang/String;)V\");");
            out.println("    if (mid == NULL) {");
            out.println("        return NULL;");
            out.println("    }");
            out.println("    return (jthrowable)env->NewObject(JavaCPP_getClass(env, i), mid, str);");
            out.println("}");
            out.println();
        }
        if (defineAdapters) {
            out.println("#include <vector>");
            out.println("template<typename P, typename T = P> class JavaCPP_hidden VectorAdapter {");
            out.println("public:");
            out.println("    VectorAdapter(const P* ptr, typename std::vector<T>::size_type size) : ptr((P*)ptr), size(size),");
            out.println("        vec2(ptr ? std::vector<T>((P*)ptr, (P*)ptr + size) : std::vector<T>()), vec(vec2) { }");
            out.println("    VectorAdapter(const std::vector<T>& vec) : ptr(0), size(0), vec2(vec), vec(vec2) { }");
            out.println("    VectorAdapter(      std::vector<T>& vec) : ptr(0), size(0), vec(vec) { }");
            out.println("    void assign(P* ptr, typename std::vector<T>::size_type size) {");
            out.println("        this->ptr = ptr;");
            out.println("        this->size = size;");
            out.println("        vec.assign(ptr, ptr + size);");
            out.println("    }");
            out.println("    static void deallocate(void* ptr) { delete[] (P*)ptr; }");
            out.println("    operator P*() {");
            out.println("        if (vec.size() > size) {");
            out.println("            ptr = new (std::nothrow) P[vec.size()];");
            out.println("        }");
            out.println("        if (ptr) {");
            out.println("            std::copy(vec.begin(), vec.end(), ptr);");
            out.println("        }");
            out.println("        size = vec.size();");
            out.println("        return ptr;");
            out.println("    }");
            out.println("    operator const P*()        { return &vec[0]; }");
            out.println("    operator std::vector<T>&() { return vec; }");
            out.println("    operator std::vector<T>*() { return ptr ? &vec : 0; }");
            out.println("    P* ptr;");
            out.println("    typename std::vector<T>::size_type size;");
            out.println("    std::vector<T> vec2;");
            out.println("    std::vector<T>& vec;");
            out.println("};");
            out.println();
            out.println("#include <string>");
            out.println("class JavaCPP_hidden StringAdapter {");
            out.println("public:");
            out.println("    StringAdapter(const          char* ptr, size_t size) : ptr((char*)ptr), size(size),");
            out.println("        str2(ptr ? (char*)ptr : \"\"), str(str2) { }");
            out.println("    StringAdapter(const signed   char* ptr, size_t size) : ptr((char*)ptr), size(size),");
            out.println("        str2(ptr ? (char*)ptr : \"\"), str(str2) { }");
            out.println("    StringAdapter(const unsigned char* ptr, size_t size) : ptr((char*)ptr), size(size),");
            out.println("        str2(ptr ? (char*)ptr : \"\"), str(str2) { }");
            out.println("    StringAdapter(const std::string& str) : ptr(0), size(0), str2(str), str(str2) { }");
            out.println("    StringAdapter(      std::string& str) : ptr(0), size(0), str(str) { }");
            out.println("    void assign(char* ptr, size_t size) {");
            out.println("        this->ptr = ptr;");
            out.println("        this->size = size;");
            out.println("        str.assign(ptr ? ptr : \"\");");
            out.println("    }");
            out.println("    static void deallocate(void* ptr) { free(ptr); }");
            out.println("    operator char*() {");
            out.println("        const char* c_str = str.c_str();");
            out.println("        if (ptr == NULL || strcmp(c_str, ptr) != 0) {");
            out.println("            ptr = strdup(c_str);");
            out.println("        }");
            out.println("        size = strlen(c_str) + 1;");
            out.println("        return ptr;");
            out.println("    }");
            out.println("    operator       signed   char*() { return (signed   char*)(operator char*)(); }");
            out.println("    operator       unsigned char*() { return (unsigned char*)(operator char*)(); }");
            out.println("    operator const          char*() { return                 str.c_str(); }");
            out.println("    operator const signed   char*() { return (signed   char*)str.c_str(); }");
            out.println("    operator const unsigned char*() { return (unsigned char*)str.c_str(); }");
            out.println("    operator         std::string&() { return str; }");
            out.println("    operator         std::string*() { return ptr ? &str : 0; }");
            out.println("    char* ptr;");
            out.println("    size_t size;");
            out.println("    std::string str2;");
            out.println("    std::string& str;");
            out.println("};");
            out.println();
        }
        if (!functions.isEmpty() || !virtualFunctions.isEmpty()) {
            out.println("static JavaCPP_noinline void JavaCPP_detach(bool detach) {");
            out.println("    if (detach && JavaCPP_vm->DetachCurrentThread() != JNI_OK) {");
            out.println("        JavaCPP_log(\"Could not detach the JavaVM from the current thread.\");");
            out.println("    }");
            out.println("}");
            out.println();
            out.println("static JavaCPP_noinline bool JavaCPP_getEnv(JNIEnv** env) {");
            out.println("    bool attached = false;");
            out.println("    JavaVM *vm = JavaCPP_vm;");
            out.println("    if (vm == NULL) {");
            if (out2 != null) {
                out.println("#if !defined(ANDROID) && !TARGET_OS_IPHONE");
                out.println("        int size = 1;");
                out.println("        if (JNI_GetCreatedJavaVMs(&vm, 1, &size) != JNI_OK || size == 0) {");
                out.println("#endif");
            }
            out.println("            JavaCPP_log(\"Could not get any created JavaVM.\");");
            out.println("            *env = NULL;");
            out.println("            return false;");
            if (out2 != null) {
                out.println("#if !defined(ANDROID) && !TARGET_OS_IPHONE");
                out.println("        }");
                out.println("#endif");
            }
            out.println("    }");
            out.println("    if (vm->GetEnv((void**)env, " + JNI_VERSION + ") != JNI_OK) {");
            out.println("        struct {");
            out.println("            JNIEnv **env;");
            out.println("            operator JNIEnv**() { return env; } // Android JNI");
            out.println("            operator void**() { return (void**)env; } // standard JNI");
            out.println("        } env2 = { env };");
            out.println("        if (vm->AttachCurrentThread(env2, NULL) != JNI_OK) {");
            out.println("            JavaCPP_log(\"Could not attach the JavaVM to the current thread.\");");
            out.println("            *env = NULL;");
            out.println("            return false;");
            out.println("        }");
            out.println("        attached = true;");
            out.println("    }");
            out.println("    if (JavaCPP_vm == NULL) {");
            out.println("        if (JNI_OnLoad(vm, NULL) < 0) {");
            out.println("            JavaCPP_detach(attached);");
            out.println("            *env = NULL;");
            out.println("            return false;");
            out.println("        }");
            out.println("    }");
            out.println("    return attached;");
            out.println("}");
            out.println();
        }
        for (Class c : functions) {
            String[] typeName = cppTypeName(c);
            String[] returnConvention = typeName[0].split("\\(");
            returnConvention[1] = constValueTypeName(returnConvention[1]);
            String parameterDeclaration = typeName[1].substring(1);
            String instanceTypeName = functionClassName(c);
            out.println("struct JavaCPP_hidden " + instanceTypeName + " {");
            out.println("    " + instanceTypeName + "() : ptr(NULL), obj(NULL) { }");
            out.println("    " + returnConvention[0] + "operator()" + parameterDeclaration + ";");
            out.println("    " + typeName[0] + "ptr" + typeName[1] + ";");
            out.println("    jobject obj; static jmethodID mid;");
            out.println("};");
            out.println("jmethodID " + instanceTypeName + "::mid = NULL;");
        }
        out.println();
        for (Class c : jclasses) {
            LinkedList<String> functionList = virtualFunctions.get(c);
            if (functionList == null) {
                continue;
            }
            LinkedList<String> memberList = virtualMembers.get(c);
            String[] typeName = cppTypeName(c);
            String valueTypeName = valueTypeName(typeName);
            String subType = "JavaCPP_" + mangle(valueTypeName);
            out.println("class JavaCPP_hidden " + subType + " : public " + valueTypeName + " {");
            out.println("public:");
            out.println("    jobject obj;");
            for (String s : functionList) {
                out.println("    static jmethodID " + s + ";");
            }
            out.println();
            for (String s : memberList) {
                out.println(s);
            }
            out.println("};");
            for (String s : functionList) {
                out.println("jmethodID " + subType + "::" + s + " = NULL;");
            }
        }
        out.println();
        for (String s : callbacks) {
            out.println(s);
        }
        out.println();
        for (Class c : deallocators) {
            String name = "JavaCPP_" + mangle(c.getName());
            out.print("static void " + name + "_deallocate(void *p) { ");
            if (FunctionPointer.class.isAssignableFrom(c)) {
                String typeName = functionClassName(c) + "*";
                out.println("JNIEnv *e; bool a = JavaCPP_getEnv(&e); if (e != NULL) e->DeleteWeakGlobalRef((("
                        + typeName + ")p)->obj); delete (" + typeName + ")p; JavaCPP_detach(a); }");
            } else if (virtualFunctions.containsKey(c)) {
                String[] typeName = cppTypeName(c);
                String valueTypeName = valueTypeName(typeName);
                String subType = "JavaCPP_" + mangle(valueTypeName);
                out.println("JNIEnv *e; bool a = JavaCPP_getEnv(&e); if (e != NULL) e->DeleteWeakGlobalRef((("
                        + subType + "*)p)->obj); delete (" + subType + "*)p; JavaCPP_detach(a); }");
            } else {
                String[] typeName = cppTypeName(c);
                out.println("delete (" + typeName[0] + typeName[1] + ")p; }");
            }
        }
        for (Class c : arrayDeallocators) {
            String name = "JavaCPP_" + mangle(c.getName());
            String[] typeName = cppTypeName(c);
            out.println("static void " + name + "_deallocateArray(void* p) { delete[] (" + typeName[0] + typeName[1] + ")p; }");
        }
        out.println();
        out.println("extern \"C\" {");
        if (out2 != null) {
            out2.println();
            out2.println("#ifdef __cplusplus");
            out2.println("extern \"C\" {");
            out2.println("#endif");
            out2.println("JNIIMPORT int JavaCPP_init(int argc, const char *argv[]);");
            out.println();
            out.println("JNIEXPORT int JavaCPP_init(int argc, const char *argv[]) {");
            out.println("#if defined(ANDROID) || TARGET_OS_IPHONE");
            out.println("    return JNI_OK;");
            out.println("#else");
            out.println("    if (JavaCPP_vm != NULL) {");
            out.println("        return JNI_OK;");
            out.println("    }");
            out.println("    int err;");
            out.println("    JavaVM *vm;");
            out.println("    JNIEnv *env;");
            out.println("    int nOptions = 1 + (argc > 255 ? 255 : argc);");
            out.println("    JavaVMOption options[256] = { { NULL } };");
            out.println("    options[0].optionString = (char*)\"-Djava.class.path=" + classPath.replace('\\', '/') + "\";");
            out.println("    for (int i = 1; i < nOptions && argv != NULL; i++) {");
            out.println("        options[i].optionString = (char*)argv[i - 1];");
            out.println("    }");
            out.println("    JavaVMInitArgs vm_args = { " + JNI_VERSION + ", nOptions, options };");
            out.println("    return (err = JNI_CreateJavaVM(&vm, (void**)&env, &vm_args)) == JNI_OK && vm != NULL && (err = JNI_OnLoad(vm, NULL)) >= 0 ? JNI_OK : err;");
            out.println("#endif");
            out.println("}");
        }
        out.println(); // XXX: JNI_OnLoad() should ideally be protected by some mutex
        out.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        out.println("    JNIEnv* env;");
        out.println("    if (vm->GetEnv((void**)&env, " + JNI_VERSION + ") != JNI_OK) {");
        out.println("        JavaCPP_log(\"Could not get JNIEnv for " + JNI_VERSION + " inside JNI_OnLoad().\");");
        out.println("        return JNI_ERR;");
        out.println("    }");
        out.println("    if (JavaCPP_vm == vm) {");
        out.println("        return env->GetVersion();");
        out.println("    }");
        out.println("    JavaCPP_vm = vm;");
        out.println("    JavaCPP_haveAllocObject = env->functions->AllocObject != NULL;");
        out.println("    JavaCPP_haveNonvirtual = env->functions->CallNonvirtualVoidMethodA != NULL;");
        out.println("    const char* members[" + jclasses.size() + "][" + maxMemberSize + "] = {");
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
                String[] typeName = cppTypeName(c);
                String valueTypeName = valueTypeName(typeName);
                String memberName = memberIterator.next();
                if ("sizeof".equals(memberName)) {
                    if ("void".equals(valueTypeName)) {
                        valueTypeName = "void*";
                    }
                    out.print("sizeof(" + valueTypeName + ")");
                } else {
                    out.print("offsetof(" + valueTypeName  + ", " + memberName + ")");
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
            out.print(m == null ? 0 : m.size());
            if (classIterator.hasNext()) {
                out.print(", ");
            }
        }
        out.println(" };");
        out.println("    jmethodID putMemberOffsetMID = JavaCPP_getStaticMethodID(env, " +
                jclasses.index(Loader.class) + ", \"putMemberOffset\", \"(Ljava/lang/String;Ljava/lang/String;I)V\");");
        out.println("    if (putMemberOffsetMID == NULL) {");
        out.println("        return JNI_ERR;");
        out.println("    }");
        out.println("    for (int i = 0; i < " + jclasses.size() + " && !env->ExceptionCheck(); i++) {");
        out.println("        for (int j = 0; j < memberOffsetSizes[i] && !env->ExceptionCheck(); j++) {");
        out.println("            if (env->PushLocalFrame(2) == 0) {");
        out.println("                jvalue args[3];");
        out.println("                args[0].l = env->NewStringUTF(JavaCPP_classNames[i]);");
        out.println("                args[1].l = env->NewStringUTF(members[i][j]);");
        out.println("                args[2].i = offsets[i][j];");
        out.println("                env->CallStaticVoidMethodA(JavaCPP_getClass(env, " +
                jclasses.index(Loader.class) + "), putMemberOffsetMID, args);");
        out.println("                env->PopLocalFrame(NULL);");
        out.println("            }");
        out.println("        }");
        out.println("    }");
        out.println("    JavaCPP_addressFID = JavaCPP_getFieldID(env, " +
                jclasses.index(Pointer.class) + ", \"address\", \"J\");");
        out.println("    if (JavaCPP_addressFID == NULL) {");
        out.println("        return JNI_ERR;");
        out.println("    }");
        out.println("    JavaCPP_positionFID = JavaCPP_getFieldID(env, " +
                jclasses.index(Pointer.class) + ", \"position\", \"I\");");
        out.println("    if (JavaCPP_positionFID == NULL) {");
        out.println("        return JNI_ERR;");
        out.println("    }");
        out.println("    JavaCPP_limitFID = JavaCPP_getFieldID(env, " +
                jclasses.index(Pointer.class) + ", \"limit\", \"I\");");
        out.println("    if (JavaCPP_limitFID == NULL) {");
        out.println("        return JNI_ERR;");
        out.println("    }");
        out.println("    JavaCPP_capacityFID = JavaCPP_getFieldID(env, " +
                jclasses.index(Pointer.class) + ", \"capacity\", \"I\");");
        out.println("    if (JavaCPP_capacityFID == NULL) {");
        out.println("        return JNI_ERR;");
        out.println("    }");
        out.println("    JavaCPP_initMID = JavaCPP_getMethodID(env, " +
                jclasses.index(Pointer.class) + ", \"init\", \"(JIJ)V\");");
        out.println("    if (JavaCPP_initMID == NULL) {");
        out.println("        return JNI_ERR;");
        out.println("    }");
        out.println("    JavaCPP_toStringMID = JavaCPP_getMethodID(env, " +
                jclasses.index(Object.class) + ", \"toString\", \"()Ljava/lang/String;\");");
        out.println("    if (JavaCPP_toStringMID == NULL) {");
        out.println("        return JNI_ERR;");
        out.println("    }");
        classIterator = jclassesInit.iterator();
        while (classIterator.hasNext()) {
            Class c = classIterator.next();
            if (c == Pointer.class) {
                continue;
            }
            out.println("    if (JavaCPP_getClass(env, " + jclasses.index(c) + ") == NULL) {");
            out.println("        return JNI_ERR;");
            out.println("    }");
        }
        out.println("    return env->GetVersion();");
        out.println("}");
        out.println();
        if (out2 != null) {
            out2.println("JNIIMPORT int JavaCPP_uninit();");
            out2.println();
            out.println("JNIEXPORT int JavaCPP_uninit() {");
            out.println("#if defined(ANDROID) || TARGET_OS_IPHONE");
            out.println("    return JNI_OK;");
            out.println("#else");
            out.println("    JavaVM *vm = JavaCPP_vm;");
            out.println("    JNI_OnUnload(JavaCPP_vm, NULL);");
            out.println("    return vm->DestroyJavaVM();");
            out.println("#endif");
            out.println("}");
        }
        out.println();
        out.println("JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {");
        out.println("    JNIEnv* env;");
        out.println("    if (vm->GetEnv((void**)&env, " + JNI_VERSION + ") != JNI_OK) {");
        out.println("        JavaCPP_log(\"Could not get JNIEnv for " + JNI_VERSION + " inside JNI_OnUnLoad().\");");
        out.println("        return;");
        out.println("    }");
        out.println("    for (int i = 0; i < " + jclasses.size() + "; i++) {");
        out.println("        env->DeleteWeakGlobalRef(JavaCPP_classes[i]);");
        out.println("        JavaCPP_classes[i] = NULL;");
        out.println("    }");
        out.println("    JavaCPP_vm = NULL;");
        out.println("}");
        out.println();

        for (Class<?> cls : baseClasses) {
            methods(cls);
        }

        boolean didSomethingUseful = false;
        for (Class<?> cls : classes) {
            try {
                didSomethingUseful |= methods(cls);
            } catch (NoClassDefFoundError e) {
                logger.warn("Could not generate code for class " + cls.getCanonicalName() + ": " + e);
            }
        }

        out.println("}");
        out.println();
        if (out2 != null) {
            out2.println("#ifdef __cplusplus");
            out2.println("}");
            out2.println("#endif");
        }

        return didSomethingUseful;
    }

    boolean methods(Class<?> cls) {
        if (!checkPlatform(cls)) {
            return false;
        }

        LinkedList<String> memberList = members.get(cls);
        if (!cls.isAnnotationPresent(Opaque.class) &&
                !FunctionPointer.class.isAssignableFrom(cls)) {
            if (memberList == null) {
                members.put(cls, memberList = new LinkedList<String>());
            }
            if (!memberList.contains("sizeof")) {
                memberList.add("sizeof");
            }
        }

        boolean didSomething = false;
        for (Class<?> c : cls.getDeclaredClasses()) {
            if (Pointer.class.isAssignableFrom(c) ||
                    Pointer.class.isAssignableFrom(c.getEnclosingClass())) {
                didSomething |= methods(c);
            }
        }

        Method[] methods = cls.getDeclaredMethods();
        boolean[] callbackAllocators = new boolean[methods.length];
        Method functionMethod = functionMethod(cls, callbackAllocators);
        boolean firstCallback = true;
        for (int i = 0; i < methods.length; i++) {
            String nativeName = mangle(cls.getName()) + "_" + mangle(methods[i].getName());
            if (!checkPlatform(methods[i].getAnnotation(Platform.class))) {
                continue;
            }
            MethodInformation methodInfo = methodInformation(methods[i]);

            String callbackName = "JavaCPP_" + nativeName + "_callback";
            if (callbackAllocators[i] && functionMethod == null) {
                logger.warn("No callback method call() or apply() has been not declared in \"" +
                        cls.getCanonicalName() + "\". No code will be generated for callback allocator.");
                continue;
            } else if (callbackAllocators[i] || (methods[i].equals(functionMethod) && !Modifier.isNative(methods[i].getModifiers()))) {
                functions.index(cls);
                Name name = methods[i].getAnnotation(Name.class);
                if (name != null && name.value().length > 0 && name.value()[0].length() > 0) {
                    callbackName = name.value()[0];
                }
                callback(cls, functionMethod, callbackName, firstCallback, null);
                firstCallback = false;
                didSomething = true;
            }

            if ((Modifier.isNative(methods[i].getModifiers()) || Modifier.isAbstract(methods[i].getModifiers()))
                    && !methodInfo.valueGetter && !methodInfo.valueSetter && !methodInfo.memberGetter && !methodInfo.memberSetter
                    && !cls.isInterface() && (methods[i].isAnnotationPresent(Virtual.class) || methodInfo.allocator)) {
                callback(cls, methods[i], methodInfo.memberName[0], !methodInfo.allocator, methodInfo);
            }

            if (!Modifier.isNative(methods[i].getModifiers())) {
                continue;
            }

            if ((methodInfo.memberGetter || methodInfo.memberSetter) && !methodInfo.noOffset &&
                    memberList != null && !Modifier.isStatic(methodInfo.modifiers)) {
                if (!memberList.contains(methodInfo.memberName[0])) {
                    memberList.add(methodInfo.memberName[0]);
                }
            }

            didSomething = true;
            out.print("JNIEXPORT " + jniTypeName(methodInfo.returnType) + " JNICALL Java_" + nativeName);
            if (methodInfo.overloaded) {
                out.print("__" + mangle(signature(methodInfo.parameterTypes)));
            }
            if (Modifier.isStatic(methodInfo.modifiers)) {
                out.print("(JNIEnv* env, jclass cls");
            } else {
                out.print("(JNIEnv* env, jobject obj");
            }
            for (int j = 0; j < methodInfo.parameterTypes.length; j++) {
                out.print(", " + jniTypeName(methodInfo.parameterTypes[j]) + " arg" + j);
            }
            out.println(") {");

            if (callbackAllocators[i]) {
                callbackAllocator(cls, callbackName);
                continue;
            } else if (!Modifier.isStatic(methodInfo.modifiers) && !methodInfo.allocator &&
                    !methodInfo.arrayAllocator && !methodInfo.deallocator) {
                // get our "this" pointer
                String[] typeName = cppTypeName(cls);
                if ("void*".equals(typeName[0]) && !cls.isAnnotationPresent(Opaque.class)) {
                    typeName[0] = "char*";
                } else if (FunctionPointer.class.isAssignableFrom(cls)) {
                    functions.index(cls);
                    typeName[0] = functionClassName(cls) + "*";
                    typeName[1] = "";
                }
                out.println("    " + typeName[0] + " ptr" + typeName[1] + " = (" + typeName[0] +
                        typeName[1] + ")jlong_to_ptr(env->GetLongField(obj, JavaCPP_addressFID));");
                out.println("    if (ptr == NULL) {");
                out.println("        env->ThrowNew(JavaCPP_getClass(env, " +
                        jclasses.index(NullPointerException.class) + "), \"This pointer address is NULL.\");");
                out.println("        return" + (methodInfo.returnType == void.class ? ";" : " 0;"));
                out.println("    }");
                if (FunctionPointer.class.isAssignableFrom(cls)) {
                    out.println("    if (ptr->ptr == NULL) {");
                    out.println("        env->ThrowNew(JavaCPP_getClass(env, " +
                            jclasses.index(NullPointerException.class) + "), \"This function pointer address is NULL.\");");
                    out.println("        return" + (methodInfo.returnType == void.class ? ";" : " 0;"));
                    out.println("    }");
                }
                if (!cls.isAnnotationPresent(Opaque.class)) {
                    out.println("    jint position = env->GetIntField(obj, JavaCPP_positionFID);");
                    out.println("    ptr += position;");
                    if (methodInfo.bufferGetter) {
                        out.println("    jint size = env->GetIntField(obj, JavaCPP_limitFID);");
                        out.println("    size -= position;");
                    }
                }
            }

            parametersBefore(methodInfo);
            String returnPrefix = returnBefore(methodInfo);
            call(methodInfo, returnPrefix, false);
            returnAfter(methodInfo);
            parametersAfter(methodInfo);
            if (methodInfo.throwsException != null) {
                out.println("    if (exc != NULL) {");
                out.println("        env->Throw(exc);");
                out.println("    }");
            }
            if (methodInfo.returnType != void.class) {
                out.println("    return rarg;");
            }
            out.println("}");
        }
        out.println();
        return didSomething;
    }

    void parametersBefore(MethodInformation methodInfo) {
        String adapterLine  = "";
        AdapterInformation prevAdapterInfo = null;
        int skipParameters = methodInfo.parameterTypes.length > 0 && methodInfo.parameterTypes[0] == Class.class ? 1 : 0;
        for (int j = skipParameters; j < methodInfo.parameterTypes.length; j++) {
            if (!methodInfo.parameterTypes[j].isPrimitive()) {
                Annotation passBy = by(methodInfo, j);
                String cast = cast(methodInfo, j);
                String[] typeName = methodInfo.parameterRaw[j] ? new String[] { "" }
                        : cppTypeName(methodInfo.parameterTypes[j]);
                AdapterInformation adapterInfo = methodInfo.parameterRaw[j] ? null
                        : adapterInformation(false, methodInfo, j);

                if (FunctionPointer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                    functions.index(methodInfo.parameterTypes[j]);
                    if (methodInfo.parameterTypes[j] == FunctionPointer.class) {
                        logger.warn("Method \"" + methodInfo.method + "\" has an abstract FunctionPointer parameter, " +
                                "but a concrete subclass is required. Compilation will most likely fail.");
                    }
                    typeName[0] = functionClassName(methodInfo.parameterTypes[j]) + "*";
                    typeName[1] = "";
                }

                if (typeName[0].length() == 0 || methodInfo.parameterRaw[j]) {
                    methodInfo.parameterRaw[j] = true;
                    typeName[0] = jniTypeName(methodInfo.parameterTypes[j]);
                    out.println("    " + typeName[0] + " ptr" + j + " = arg" + j + ";");
                    continue;
                }

                if ("void*".equals(typeName[0]) && !methodInfo.parameterTypes[j].isAnnotationPresent(Opaque.class)) {
                    typeName[0] = "char*";
                }
                out.print("    " + typeName[0] + " ptr" + j + typeName[1] + " = ");
                if (Pointer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                    out.println("arg" + j + " == NULL ? NULL : (" + typeName[0] + typeName[1] +
                            ")jlong_to_ptr(env->GetLongField(arg" + j + ", JavaCPP_addressFID));");
                    if ((j == 0 && FunctionPointer.class.isAssignableFrom(methodInfo.cls) &&
                            methodInfo.cls.isAnnotationPresent(Namespace.class)) ||
                            passBy instanceof ByVal || passBy instanceof ByRef) {
                        // in the case of member ptr, ptr0 is our object pointer, which cannot be NULL
                        out.println("    if (ptr" + j + " == NULL) {");
                        out.println("        env->ThrowNew(JavaCPP_getClass(env, " +
                                jclasses.index(NullPointerException.class) + "), \"Pointer address of argument " + j + " is NULL.\");");
                        out.println("        return" + (methodInfo.returnType == void.class ? ";" : " 0;"));
                        out.println("    }");
                    }
                    if (adapterInfo != null || prevAdapterInfo != null) {
                        out.println("    jint size" + j + " = arg" + j + " == NULL ? 0 : env->GetIntField(arg" + j +
                                ", JavaCPP_limitFID);");
                    }
                    if (!methodInfo.parameterTypes[j].isAnnotationPresent(Opaque.class)) {
                        out.println("    jint position" + j + " = arg" + j + " == NULL ? 0 : env->GetIntField(arg" + j +
                                ", JavaCPP_positionFID);");
                        out.println("    ptr"  + j + " += position" + j + ";");
                        if (adapterInfo != null || prevAdapterInfo != null) {
                            out.println("    size" + j + " -= position" + j + ";");
                        }
                    }
                } else if (methodInfo.parameterTypes[j] == String.class) {
                    out.println("arg" + j + " == NULL ? NULL : env->GetStringUTFChars(arg" + j + ", NULL);");
                    if (adapterInfo != null || prevAdapterInfo != null) {
                        out.println("    jint size" + j + " = 0;");
                    }
                } else if (methodInfo.parameterTypes[j].isArray() &&
                        methodInfo.parameterTypes[j].getComponentType().isPrimitive()) {
                    out.print("arg" + j + " == NULL ? NULL : ");
                    String s = methodInfo.parameterTypes[j].getComponentType().getName();
                    if (methodInfo.valueGetter || methodInfo.valueSetter ||
                            methodInfo.memberGetter || methodInfo.memberSetter) {
                        out.println("(j" + s + "*)env->GetPrimitiveArrayCritical(arg" + j + ", NULL);");
                    } else {
                        s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
                        out.println("env->Get" + s + "ArrayElements(arg" + j + ", NULL);");
                    }
                    if (adapterInfo != null || prevAdapterInfo != null) {
                        out.println("    jint size" + j +
                                " = arg" + j + " == NULL ? 0 : env->GetArrayLength(arg" + j + ");");
                    }
                } else if (Buffer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                    out.println("arg" + j + " == NULL ? NULL : (" + typeName[0] + typeName[1] + ")env->GetDirectBufferAddress(arg" + j + ");");
                    if (adapterInfo != null || prevAdapterInfo != null) {
                        out.println("    jint size" + j +
                                " = arg" + j + " == NULL ? 0 : env->GetDirectBufferCapacity(arg" + j + ");");
                    }
                } else {
                    out.println("arg" + j + ";");
                    logger.warn("Method \"" + methodInfo.method + "\" has an unsupported parameter of type \"" +
                            methodInfo.parameterTypes[j].getCanonicalName() + "\". Compilation will most likely fail.");
                }

                if (adapterInfo != null) {
                    usesAdapters = true;
                    adapterLine = "    " + adapterInfo.name + " adapter" + j + "(";
                    prevAdapterInfo = adapterInfo;
                }
                if (prevAdapterInfo != null) {
                    if (!FunctionPointer.class.isAssignableFrom(methodInfo.cls)) {
                        // sometimes we need to use the Cast annotation for declaring functions only
                        adapterLine += cast;
                    }
                    adapterLine += "ptr" + j + ", size" + j;
                    if (--prevAdapterInfo.argc > 0) {
                        adapterLine += ", ";
                    }
                }
                if (prevAdapterInfo != null && prevAdapterInfo.argc <= 0) {
                    out.println(adapterLine + ");");
                    prevAdapterInfo = null;
                }
            }
        }
    }

    String returnBefore(MethodInformation methodInfo) {
        String returnPrefix = "";
        if (methodInfo.returnType == void.class) {
            if (methodInfo.allocator || methodInfo.arrayAllocator) {
                if (methodInfo.cls != Pointer.class && !virtualFunctions.containsKey(methodInfo.cls)) {
                    out.println("    if (!env->IsSameObject(env->GetObjectClass(obj), JavaCPP_getClass(env, " +
                            jclasses.index(methodInfo.cls) + "))) {");
                    out.println("        return;");
                    out.println("    }");
                }
                String[] typeName = cppTypeName(methodInfo.cls);
                returnPrefix = typeName[0] + " rptr" + typeName[1] + " = ";
            }
        } else {
            String cast = cast(methodInfo.returnType, methodInfo.annotations);
            String[] typeName = methodInfo.returnRaw ? new String[] { "" }
                    : cppCastTypeName(methodInfo.returnType, methodInfo.annotations);
            if (methodInfo.valueSetter || methodInfo.memberSetter || methodInfo.noReturnGetter) {
                out.println("    jobject rarg = obj;");
            } else if (methodInfo.returnType.isPrimitive()) {
                out.println("    " + jniTypeName(methodInfo.returnType) + " rarg = 0;");
                returnPrefix = typeName[0] + " rvalue" + typeName[1] + " = " + cast;
            } else {
                Annotation returnBy = by(methodInfo.annotations);
                String valueTypeName = valueTypeName(typeName);

                returnPrefix = "rptr = " + cast;
                if (typeName[0].length() == 0 || methodInfo.returnRaw) {
                    methodInfo.returnRaw = true;
                    typeName[0] = jniTypeName(methodInfo.returnType);
                    out.println("    " + typeName[0] + " rarg = NULL;");
                    out.println("    " + typeName[0] + " rptr;");
                } else if (Pointer.class.isAssignableFrom(methodInfo.returnType) ||
                        Buffer.class.isAssignableFrom(methodInfo.returnType) ||
                        (methodInfo.returnType.isArray() &&
                         methodInfo.returnType.getComponentType().isPrimitive())) {
                    if (FunctionPointer.class.isAssignableFrom(methodInfo.returnType)) {
                        functions.index(methodInfo.returnType);
                        returnPrefix = "if (rptr != NULL) rptr->ptr = ";
                        if (methodInfo.method.isAnnotationPresent(Virtual.class)) {
                            // cast from member function pointers declared in Java
                            returnPrefix += "(" + typeName[0] + typeName[1] + ")&";
                        }
                        typeName[0] = functionClassName(methodInfo.returnType) + "*";
                        typeName[1] = "";
                        valueTypeName = valueTypeName(typeName);
                    } else if (virtualFunctions.containsKey(methodInfo.cls)) {
                        String subType = "JavaCPP_" + mangle(valueTypeName);
                        valueTypeName = subType;
                    }
                    if (returnBy instanceof ByVal) {
                        returnPrefix += (noException(methodInfo.returnType, methodInfo.method) ?
                            "new (std::nothrow) " : "new ") + valueTypeName + typeName[1] + "(";
                    } else if (returnBy instanceof ByRef) {
                        returnPrefix += "&";
                    } else if (returnBy instanceof ByPtrPtr) {
                        if (cast.length() > 0) {
                            typeName[0] = typeName[0].substring(0, typeName[0].length()-1);
                        }
                        returnPrefix = "rptr = NULL; " + typeName[0] + "* rptrptr" + typeName[1] + " = " + cast;
                    } // else ByPtr || ByPtrRef
                    if (methodInfo.bufferGetter) {
                        out.println("    jobject rarg = NULL;");
                        out.println("    char* rptr;");
                    } else {
                        out.println("    " + jniTypeName(methodInfo.returnType) + " rarg = NULL;");
                        out.println("    " + typeName[0] + " rptr" + typeName[1] + ";");
                    }
                    if (FunctionPointer.class.isAssignableFrom(methodInfo.returnType)) {
                        out.println("    rptr = new (std::nothrow) " + valueTypeName + ";");
                    }
                } else if (methodInfo.returnType == String.class) {
                    out.println("    jstring rarg = NULL;");
                    out.println("    const char* rptr;");
                    if (returnBy instanceof ByRef) {
                        returnPrefix = "std::string rstr(";
                    } else {
                        returnPrefix += "(const char*)";
                    }
                } else {
                    logger.warn("Method \"" + methodInfo.method + "\" has unsupported return type \"" +
                            methodInfo.returnType.getCanonicalName() + "\". Compilation will most likely fail.");
                }

                AdapterInformation adapterInfo = adapterInformation(false, valueTypeName, methodInfo.annotations);
                if (adapterInfo != null) {
                    usesAdapters = true;
                    returnPrefix = adapterInfo.name + " radapter(";
                }
            }
        }
        if (methodInfo.throwsException != null) {
            out.println("    jthrowable exc = NULL;");
            out.println("    try {");
        }
        return returnPrefix;
    }

    void call(MethodInformation methodInfo, String returnPrefix, boolean secondCall) {
        boolean needSecondCall = false;
        String indent = secondCall ? "" : methodInfo.throwsException != null ? "        " : "    ";
        String prefix = "(";
        String suffix = ")";
        int skipParameters = methodInfo.parameterTypes.length > 0 && methodInfo.parameterTypes[0] == Class.class ? 1 : 0;
        boolean index = methodInfo.method.isAnnotationPresent(Index.class) ||
                 (methodInfo.pairedMethod != null &&
                  methodInfo.pairedMethod.isAnnotationPresent(Index.class));
        if (methodInfo.deallocator) {
            out.println(indent + "void* allocatedAddress = jlong_to_ptr(arg0);");
            out.println(indent + "void (*deallocatorAddress)(void*) = (void(*)(void*))jlong_to_ptr(arg1);");
            out.println(indent + "if (deallocatorAddress != NULL && allocatedAddress != NULL) {");
            out.println(indent + "    (*deallocatorAddress)(allocatedAddress);");
            out.println(indent + "}");
            return; // nothing else should be appended here for deallocator
        } else if (methodInfo.valueGetter || methodInfo.valueSetter ||
                methodInfo.memberGetter || methodInfo.memberSetter) {
            boolean wantsPointer = false;
            int k = methodInfo.parameterTypes.length-1;
            if ((methodInfo.valueSetter || methodInfo.memberSetter) &&
                    !(by(methodInfo, k) instanceof ByRef) &&
                    adapterInformation(false, methodInfo, k) == null &&
                    methodInfo.parameterTypes[k] == String.class) {
                // special considerations for char arrays as strings
                out.print(indent + "strcpy((char*)");
                wantsPointer = true;
                prefix = ", ";
            } else if (k >= 1 && methodInfo.parameterTypes[0].isArray() &&
                    methodInfo.parameterTypes[0].getComponentType().isPrimitive() &&
                    (methodInfo.parameterTypes[1] == int.class ||
                     methodInfo.parameterTypes[1] == long.class)) {
                // special considerations for primitive arrays
                out.print(indent + "memcpy(");
                wantsPointer = true;
                prefix = ", ";
                if (methodInfo.memberGetter || methodInfo.valueGetter) {
                    out.print("ptr0 + arg1, ");
                } else { // methodInfo.memberSetter || methodInfo.valueSetter
                    prefix += "ptr0 + arg1, ";
                }
                skipParameters = 2;
                suffix = " * sizeof(*ptr0)" + suffix;
            } else {
                out.print(indent + returnPrefix);
                prefix = methodInfo.valueGetter || methodInfo.memberGetter ? "" : " = ";
                suffix = "";
            }
            if (Modifier.isStatic(methodInfo.modifiers)) {
                out.print(cppScopeName(methodInfo));
            } else if (methodInfo.memberGetter || methodInfo.memberSetter) {
                if (index) {
                    out.print("(*ptr)");
                    prefix = "." + methodInfo.memberName[0] + prefix;
                } else {
                    out.print("ptr->" + methodInfo.memberName[0]);
                }
            } else { // methodInfo.valueGetter || methodInfo.valueSetter
                out.print(index ? "(*ptr)" : methodInfo.dim > 0 || wantsPointer ? "ptr" : "*ptr");
            }
        } else if (methodInfo.bufferGetter) {
            out.print(indent + returnPrefix + "ptr");
            prefix = "";
            suffix = "";
        } else { // function call
            out.print(indent + returnPrefix);
            if (FunctionPointer.class.isAssignableFrom(methodInfo.cls)) {
                if (methodInfo.cls.isAnnotationPresent(Namespace.class)) {
                    out.print("(ptr0->*(ptr->ptr))");
                    skipParameters = 1;
                } else {
                    out.print("(*ptr->ptr)");
                }
            } else if (methodInfo.allocator) {
                String[] typeName = cppTypeName(methodInfo.cls);
                String valueTypeName = valueTypeName(typeName);
                if (virtualFunctions.containsKey(methodInfo.cls)) {
                    String subType = "JavaCPP_" + mangle(valueTypeName);
                    valueTypeName = subType;
                }
                if (methodInfo.cls == Pointer.class) {
                    // can't allocate a "void", so simply assign the argument instead
                    prefix = "";
                    suffix = "";
                } else {
                    out.print((noException(methodInfo.cls, methodInfo.method) ?
                        "new (std::nothrow) " : "new ") + valueTypeName + typeName[1]);
                    if (methodInfo.arrayAllocator) {
                        prefix = "[";
                        suffix = "]";
                    }
                }
            } else if (Modifier.isStatic(methodInfo.modifiers)) {
                out.print(cppScopeName(methodInfo));
            } else {
                String name = methodInfo.memberName[0];
                String[] typeName = cppTypeName(methodInfo.cls);
                String valueTypeName = valueTypeName(typeName);
                if (virtualFunctions.containsKey(methodInfo.cls) && !secondCall) {
                    String subType = "JavaCPP_" + mangle(valueTypeName);
                    out.print("dynamic_cast<" + subType + "*>(ptr) != NULL ? ");
                    name = valueTypeName + "::" + name;
                    needSecondCall = true;
                }
                if (index) {
                    out.print("(*ptr)");
                    prefix = "." + name + prefix;
                } else {
                    out.print("ptr->" + name);
                }
            }
        }

        for (int j = skipParameters; j <= methodInfo.parameterTypes.length; j++) {
            if (j == skipParameters + methodInfo.dim) {
                if (methodInfo.memberName.length > 1) {
                    out.print(methodInfo.memberName[1]);
                }
                out.print(prefix);
                if (methodInfo.withEnv) {
                    out.print(Modifier.isStatic(methodInfo.modifiers) ? "env, cls" : "env, obj");
                    if (methodInfo.parameterTypes.length - skipParameters - methodInfo.dim > 0) {
                        out.print(", ");
                    }
                }
            }
            if (j == methodInfo.parameterTypes.length) {
                break;
            }
            if (j < skipParameters + methodInfo.dim) {
                // print array indices to access array members, or whatever
                // the C++ operator does with them when the Index annotation is present
                out.print("[");
            }
            Annotation passBy = by(methodInfo, j);
            String cast = cast(methodInfo, j);
            AdapterInformation adapterInfo = methodInfo.parameterRaw[j] ? null
                    : adapterInformation(false, methodInfo, j);

            if (("(void*)".equals(cast) || "(void *)".equals(cast)) &&
                    methodInfo.parameterTypes[j] == long.class) {
                out.print("jlong_to_ptr(arg" + j + ")");
            } else if (methodInfo.parameterTypes[j].isPrimitive()) {
                out.print(cast + "arg" + j);
            } else if (adapterInfo != null) {
                cast = adapterInfo.cast.trim();
                if (cast.length() > 0 && !cast.startsWith("(") && !cast.endsWith(")")) {
                    cast = "(" + cast + ")";
                }
                out.print(cast + "adapter" + j);
                j += adapterInfo.argc - 1;
            } else if (FunctionPointer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                out.print(cast + "(ptr" + j + " == NULL ? NULL : " + (passBy instanceof ByPtrPtr ? "&ptr" : "ptr") + j + "->ptr)");
            } else if (passBy instanceof ByVal || (passBy instanceof ByRef &&
                    methodInfo.parameterTypes[j] != String.class)) {
                out.print("*" + cast + "ptr" + j);
            } else if (passBy instanceof ByPtrPtr) {
                out.print(cast + "(arg" + j + " == NULL ? NULL : &ptr" + j + ")");
            } else { // ByPtr || ByPtrRef || (ByRef && std::string)
                out.print(cast + "ptr" + j);
            }

            if (j < skipParameters + methodInfo.dim) {
                out.print("]");
            } else if (j < methodInfo.parameterTypes.length - 1) {
                out.print(", ");
            }
        }
        out.print(suffix);
        if (methodInfo.memberName.length > 2) {
            out.print(methodInfo.memberName[2]);
        }
        if (by(methodInfo.annotations) instanceof ByRef &&
                methodInfo.returnType == String.class) {
            // special considerations for std::string
            out.print(");\n" + indent + "rptr = rstr.c_str()");
        }
        if (needSecondCall) {
            call(methodInfo, " : ", true);
        }
    }

    void returnAfter(MethodInformation methodInfo) {
        String indent = methodInfo.throwsException != null ? "        " : "    ";
        String[] typeName = methodInfo.returnRaw ? new String[] { "" }
                : cppCastTypeName(methodInfo.returnType, methodInfo.annotations);
        Annotation returnBy = by(methodInfo.annotations);
        String valueTypeName = valueTypeName(typeName);
        AdapterInformation adapterInfo = adapterInformation(false, valueTypeName, methodInfo.annotations);
        String suffix = methodInfo.deallocator ? "" : ";";
        if (!methodInfo.returnType.isPrimitive() && adapterInfo != null) {
            suffix = ")" + suffix;
        }
        if ((Pointer.class.isAssignableFrom(methodInfo.returnType) ||
                (methodInfo.returnType.isArray() &&
                 methodInfo.returnType.getComponentType().isPrimitive()) ||
                Buffer.class.isAssignableFrom(methodInfo.returnType))) {
            if (returnBy instanceof ByVal) {
                suffix = ")" + suffix;
            } else if (returnBy instanceof ByPtrPtr) {
                out.println(suffix);
                suffix = "";
                out.println(indent + "if (rptrptr == NULL) {");
                out.println(indent + "    env->ThrowNew(JavaCPP_getClass(env, " +
                        jclasses.index(NullPointerException.class) + "), \"Return pointer address is NULL.\");");
                out.println(indent + "} else {");
                out.println(indent + "    rptr = *rptrptr;");
                out.println(indent + "}");
            }
        }
        out.println(suffix);

        if (methodInfo.returnType == void.class) {
            if (methodInfo.allocator || methodInfo.arrayAllocator) {
                out.println(indent + "jint rcapacity = " + (methodInfo.arrayAllocator ? "arg0;" : "1;"));
                boolean noDeallocator = methodInfo.cls == Pointer.class ||
                        methodInfo.cls.isAnnotationPresent(NoDeallocator.class) ||
                        methodInfo.method.isAnnotationPresent(NoDeallocator.class);
                out.print(indent + "JavaCPP_initPointer(env, obj, rptr, rcapacity, ");
                if (noDeallocator) {
                    out.println("NULL);");
                } else if (methodInfo.arrayAllocator) {
                    out.println("&JavaCPP_" + mangle(methodInfo.cls.getName()) + "_deallocateArray);");
                    arrayDeallocators.index(methodInfo.cls);
                } else {
                    out.println("&JavaCPP_" + mangle(methodInfo.cls.getName()) + "_deallocate);");
                    deallocators.index(methodInfo.cls);
                }
                if (virtualFunctions.containsKey(methodInfo.cls)) {
                    typeName = cppTypeName(methodInfo.cls);
                    valueTypeName = valueTypeName(typeName);
                    String subType = "JavaCPP_" + mangle(valueTypeName);
                    out.println(indent + "((" + subType + "*)rptr)->obj = env->NewWeakGlobalRef(obj);");
                }
            }
        } else {
            if (methodInfo.valueSetter || methodInfo.memberSetter || methodInfo.noReturnGetter) {
                // nothing
            } else if (methodInfo.returnType.isPrimitive()) {
                out.println(indent + "rarg = (" + jniTypeName(methodInfo.returnType) + ")rvalue;");
            } else if (methodInfo.returnRaw) {
                out.println(indent + "rarg = rptr;");
            } else {
                boolean needInit = false;
                if (adapterInfo != null) {
                    out.println(indent + "rptr = radapter;");
                    if (methodInfo.returnType != String.class) {
                        out.println(indent + "jint rcapacity = (jint)radapter.size;");
                        out.println(indent + "void (*deallocator)(void*) = " +
                                (adapterInfo.constant ? "NULL;" : "&" + adapterInfo.name + "::deallocate;"));
                    }
                    needInit = true;
                } else if (returnBy instanceof ByVal ||
                        FunctionPointer.class.isAssignableFrom(methodInfo.returnType)) {
                    out.println(indent + "jint rcapacity = 1;");
                    out.println(indent + "void (*deallocator)(void*) = &JavaCPP_" +
                            mangle(methodInfo.returnType.getName()) + "_deallocate;");
                    deallocators.index(methodInfo.returnType);
                    needInit = true;
                }

                if (Pointer.class.isAssignableFrom(methodInfo.returnType)) {
                    out.print(indent);
                    if (!(returnBy instanceof ByVal)) {
                        // check if we can reuse one of the Pointer objects from the arguments
                        if (Modifier.isStatic(methodInfo.modifiers) && methodInfo.parameterTypes.length > 0) {
                            for (int i = 0; i < methodInfo.parameterTypes.length; i++) {
                                String cast = cast(methodInfo, i);
                                if (Arrays.equals(methodInfo.parameterAnnotations[i], methodInfo.annotations)
                                        && methodInfo.parameterTypes[i] == methodInfo.returnType) {
                                    out.println(         "if (rptr == " + cast + "ptr" + i + ") {");
                                    out.println(indent + "    rarg = arg" + i + ";");
                                    out.print(indent + "} else ");
                                }
                            }
                        } else if (!Modifier.isStatic(methodInfo.modifiers) && methodInfo.cls == methodInfo.returnType) {
                            out.println(         "if (rptr == ptr) {");
                            out.println(indent + "    rarg = obj;");
                            out.print(indent + "} else ");
                        }
                    }
                    out.println(         "if (rptr != NULL) {");
                    out.println(indent + "    rarg = JavaCPP_createPointer(env, " + jclasses.index(methodInfo.returnType) +
                            (methodInfo.parameterTypes.length > 0 && methodInfo.parameterTypes[0] == Class.class ? ", arg0);" : ");"));
                    if (needInit) {
                        out.println(indent + "    JavaCPP_initPointer(env, rarg, rptr, rcapacity, deallocator);");
                    } else {
                        out.println(indent + "    env->SetLongField(rarg, JavaCPP_addressFID, ptr_to_jlong(rptr));");
                    }
                    if (returnBy instanceof ByVal && virtualFunctions.containsKey(methodInfo.cls)) {
                        String subType = "JavaCPP_" + mangle(valueTypeName);
                        out.println(indent + "    ((" + subType + "*))rptr->obj = env->NewWeakGlobalRef(rarg);");
                    }
                    out.println(indent + "}");
                } else if (methodInfo.returnType == String.class) {
                    out.println(indent + "if (rptr != NULL) {");
                    out.println(indent + "    rarg = env->NewStringUTF(rptr);");
                    out.println(indent + "}");
                } else if (methodInfo.returnType.isArray() &&
                        methodInfo.returnType.getComponentType().isPrimitive()) {
                    if (adapterInfo == null && !(returnBy instanceof ByVal)) {
                        out.println(indent + "jint rcapacity = rptr != NULL ? 1 : 0;");
                    }
                    String s = methodInfo.returnType.getComponentType().getName();
                    String S = Character.toUpperCase(s.charAt(0)) + s.substring(1);
                    out.println(indent + "if (rptr != NULL) {");
                    out.println(indent + "    rarg = env->New" + S + "Array(rcapacity);");
                    out.println(indent + "    env->Set" + S + "ArrayRegion(rarg, 0, rcapacity, (j" + s + "*)rptr);");
                    out.println(indent + "}");
                    if (adapterInfo != null) {
                        out.println(indent + "if (deallocator != 0 && rptr != NULL) {");
                        out.println(indent + "    (*(void(*)(void*))jlong_to_ptr(deallocator))((void*)rptr);");
                        out.println(indent + "}");
                    }
                } else if (Buffer.class.isAssignableFrom(methodInfo.returnType)) {
                    if (methodInfo.bufferGetter) {
                        out.println(indent + "jint rcapacity = size;");
                    } else if (adapterInfo == null && !(returnBy instanceof ByVal)) {
                        out.println(indent + "jint rcapacity = rptr != NULL ? 1 : 0;");
                    }
                    out.println(indent + "if (rptr != NULL) {");
                    out.println(indent + "    rarg = env->NewDirectByteBuffer((void*)rptr, rcapacity);");
                    out.println(indent + "}");
                }
            }
        }
    }

    void parametersAfter(MethodInformation methodInfo) {
        if (methodInfo.throwsException != null) {
            mayThrowExceptions = true;
            out.println("    } catch (...) {");
            out.println("        exc = JavaCPP_handleException(env, " + jclasses.index(methodInfo.throwsException) + ");");
            out.println("    }");
            out.println();
        }
        int skipParameters = methodInfo.parameterTypes.length > 0 && methodInfo.parameterTypes[0] == Class.class ? 1 : 0;
        for (int j = skipParameters; j < methodInfo.parameterTypes.length; j++) {
            if (methodInfo.parameterRaw[j]) {
                continue;
            }
            Annotation passBy = by(methodInfo, j);
            String cast = cast(methodInfo, j);
            String[] typeName = cppCastTypeName(methodInfo.parameterTypes[j], methodInfo.parameterAnnotations[j]);
            AdapterInformation adapterInfo = adapterInformation(true, methodInfo, j);
            if ("void*".equals(typeName[0]) && !methodInfo.parameterTypes[j].isAnnotationPresent(Opaque.class)) {
                typeName[0] = "char*";
            }
            if (Pointer.class.isAssignableFrom(methodInfo.parameterTypes[j])) {
                if (adapterInfo != null) {
                    for (int k = 0; k < adapterInfo.argc; k++) {
                        out.println("    " + typeName[0] + " rptr" + (j+k) + typeName[1] + " = " + cast + "adapter" + j + ";");
                        out.println("    jint rsize" + (j+k) + " = (jint)adapter" + j + ".size" + (k > 0 ? (k+1) + ";" : ";"));
                        out.println("    if (rptr" + (j+k) + " != " + cast + "ptr" + (j+k) + ") {");
                        out.println("        JavaCPP_initPointer(env, arg" + j + ", rptr" + (j+k) + ", rsize" + (j+k) + ", &" + adapterInfo.name + "::deallocate);");
                        out.println("    } else {");
                        out.println("        env->SetIntField(arg" + j + ", JavaCPP_limitFID, rsize" + (j+k) + " + position" + (j+k) + ");");
                        out.println("    }");
                    }
                } else if ((passBy instanceof ByPtrPtr || passBy instanceof ByPtrRef) &&
                        !methodInfo.valueSetter && !methodInfo.memberSetter) {
                    if (!methodInfo.parameterTypes[j].isAnnotationPresent(Opaque.class)) {
                        out.println("    ptr" + j + " -= position" + j + ";");
                    }
                    out.println("    if (arg" + j + " != NULL) env->SetLongField(arg" + j +
                            ", JavaCPP_addressFID, ptr_to_jlong(ptr" + j + "));");
                }
            } else if (methodInfo.parameterTypes[j] == String.class) {
                out.println("    if (arg" + j + " != NULL) env->ReleaseStringUTFChars(arg" + j + ", ptr" + j + ");");
            } else if (methodInfo.parameterTypes[j].isArray() &&
                    methodInfo.parameterTypes[j].getComponentType().isPrimitive()) {
                out.print("    if (arg" + j + " != NULL) ");
                if (methodInfo.valueGetter || methodInfo.valueSetter ||
                        methodInfo.memberGetter || methodInfo.memberSetter) {
                    out.println("env->ReleasePrimitiveArrayCritical(arg" + j + ", ptr" + j + ", 0);");
                } else {
                    String s = methodInfo.parameterTypes[j].getComponentType().getName();
                    String S = Character.toUpperCase(s.charAt(0)) + s.substring(1);
                    out.println("env->Release" + S + "ArrayElements(arg" + j + ", (j" + s + "*)ptr" + j + ", 0);");
                }
            }
        }
    }

    void callback(Class<?> cls, Method callbackMethod, String callbackName, boolean needDefinition, MethodInformation methodInfo) {
        Class<?> callbackReturnType = callbackMethod.getReturnType();
        Class<?>[] callbackParameterTypes = callbackMethod.getParameterTypes();
        Annotation[] callbackAnnotations = callbackMethod.getAnnotations();
        Annotation[][] callbackParameterAnnotations = callbackMethod.getParameterAnnotations();

        String instanceTypeName = functionClassName(cls);
        String[] callbackTypeName = cppFunctionTypeName(callbackMethod);
        String[] returnConvention = callbackTypeName[0].split("\\(");
        returnConvention[1] = constValueTypeName(returnConvention[1]);
        String parameterDeclaration = callbackTypeName[1].substring(1);
        String fieldName = mangle(callbackMethod.getName()) + "__" + mangle(signature(callbackMethod.getParameterTypes()));

        String firstLine = "";
        if (methodInfo != null) {
            String[] typeName = methodInfo.returnRaw ? new String[] { "" }
                    : cppTypeName(methodInfo.cls);
            String valueTypeName = valueTypeName(typeName);
            String subType = "JavaCPP_" + mangle(valueTypeName);
            LinkedList<String> memberList = virtualMembers.get(cls);
            if (memberList == null) {
                virtualMembers.put(cls, memberList = new LinkedList<String>());
            }
            String member = "    ";
            if (methodInfo.allocator) {
                member += subType + parameterDeclaration + " : " + valueTypeName + "(";
                for (int j = 0; j < callbackParameterTypes.length; j++) {
                    member += "arg" + j;
                    if (j < callbackParameterTypes.length - 1) {
                        member += ", ";
                    }
                }
                member += "), obj(NULL) { }";
            } else {
                LinkedList<String> functionList = virtualFunctions.get(cls);
                if (functionList == null) {
                    virtualFunctions.put(cls, functionList = new LinkedList<String>());
                }
                member += "virtual " + returnConvention[0] + (returnConvention.length > 1 ? returnConvention[1] : "")
                        + methodInfo.memberName[0] + parameterDeclaration + ";";
                firstLine = returnConvention[0] + (returnConvention.length > 1 ? returnConvention[1] : "")
                        + subType + "::" + methodInfo.memberName[0] + parameterDeclaration + " {";
                functionList.add(fieldName);
                jclassesInit.index(cls); // for custom class loaders
            }
            memberList.add(member);
        } else {
            callbacks.index("static " + instanceTypeName + " " + callbackName + "_instance;");
            jclassesInit.index(cls); // for custom class loaders
            if (out2 != null) {
                out2.println("JNIIMPORT " + returnConvention[0] + (returnConvention.length > 1 ?
                        returnConvention[1] : "") + callbackName + parameterDeclaration + ";");
            }
            out.println("JNIEXPORT " + returnConvention[0] + (returnConvention.length > 1 ?
                    returnConvention[1] : "") + callbackName + parameterDeclaration + " {");
            out.print((callbackReturnType != void.class ? "    return " : "    ") + callbackName + "_instance(");
            for (int j = 0; j < callbackParameterTypes.length; j++) {
                out.print("arg" + j);
                if (j < callbackParameterTypes.length - 1) {
                    out.print(", ");
                }
            }
            out.println(");");
            out.println("}");

            firstLine = returnConvention[0] + instanceTypeName + "::operator()" + parameterDeclaration + " {";
        }

        if (!needDefinition) {
            return;
        }
        out.println(firstLine);

        String returnPrefix = "";
        if (callbackReturnType != void.class) {
            out.println("    " + jniTypeName(callbackReturnType) + " rarg = 0;");
            returnPrefix = "rarg = ";
            if (callbackReturnType == String.class) {
                returnPrefix += "(jstring)";
            }
        }
        String callbackReturnCast = cast(callbackReturnType, callbackAnnotations);
        Annotation returnBy = by(callbackAnnotations);
        String[] returnTypeName = cppTypeName(callbackReturnType);
        String returnValueTypeName = valueTypeName(returnTypeName);
        AdapterInformation returnAdapterInfo = adapterInformation(false, returnValueTypeName, callbackAnnotations);

        out.println("    jthrowable exc = NULL;");
        out.println("    JNIEnv* env;");
        out.println("    bool attached = JavaCPP_getEnv(&env);");
        out.println("    if (env == NULL) {");
        out.println("        goto end;");
        out.println("    }");
        out.println("{");
        if (callbackParameterTypes.length > 0) {
            out.println("    jvalue args[" + callbackParameterTypes.length + "];");
            for (int j = 0; j < callbackParameterTypes.length; j++) {
                if (callbackParameterTypes[j].isPrimitive()) {
                    out.println("    args[" + j + "]." +
                            signature(callbackParameterTypes[j]).toLowerCase() + " = (" +
                            jniTypeName(callbackParameterTypes[j]) + ")arg" + j + ";");
                } else {
                    Annotation passBy = by(callbackParameterAnnotations[j]);
                    String[] typeName = cppTypeName(callbackParameterTypes[j]);
                    String valueTypeName = valueTypeName(typeName);
                    AdapterInformation adapterInfo = adapterInformation(false, valueTypeName, callbackParameterAnnotations[j]);

                    boolean needInit = false;
                    if (adapterInfo != null) {
                        usesAdapters = true;
                        out.println("    " + adapterInfo.name + " adapter" + j + "(arg" + j + ");");
                        if (callbackParameterTypes[j] != String.class) {
                            out.println("    jint size" + j + " = (jint)adapter" + j + ".size;");
                            out.println("    void (*deallocator" + j + ")(void*) = &" + adapterInfo.name + "::deallocate;");
                        }
                        needInit = true;
                    } else if ((passBy instanceof ByVal && callbackParameterTypes[j] != Pointer.class) ||
                            FunctionPointer.class.isAssignableFrom(callbackParameterTypes[j])) {
                        out.println("    jint size" + j + " = 1;");
                        out.println("    void (*deallocator" + j + ")(void*) = &JavaCPP_" +
                                mangle(callbackParameterTypes[j].getName()) + "_deallocate;");
                        deallocators.index(callbackParameterTypes[j]);
                        needInit = true;
                    }

                    if (Pointer.class.isAssignableFrom(callbackParameterTypes[j]) ||
                            Buffer.class.isAssignableFrom(callbackParameterTypes[j]) ||
                            (callbackParameterTypes[j].isArray() &&
                             callbackParameterTypes[j].getComponentType().isPrimitive())) {
                        String cast = "(" + typeName[0] + typeName[1] + ")";
                        if (FunctionPointer.class.isAssignableFrom(callbackParameterTypes[j])) {
                            functions.index(callbackParameterTypes[j]);
                            typeName[0] = functionClassName(callbackParameterTypes[j]) + "*";
                            typeName[1] = "";
                            valueTypeName = valueTypeName(typeName);
                        } else if (virtualFunctions.containsKey(callbackParameterTypes[j])) {
                            String subType = "JavaCPP_" + mangle(valueTypeName);
                            valueTypeName = subType;
                        }
                        out.println("    " + jniTypeName(callbackParameterTypes[j]) + " obj" + j + " = NULL;");
                        out.println("    " + typeName[0] + " ptr" + j + typeName[1] + " = NULL;");
                        if (FunctionPointer.class.isAssignableFrom(callbackParameterTypes[j])) {
                            out.println("    ptr" + j + " = new (std::nothrow) " + valueTypeName + ";");
                            out.println("    if (ptr" + j + " != NULL) {");
                            out.println("        ptr" + j + "->ptr = " + cast + "&arg" + j + ";");
                            out.println("    }");
                        } else if (adapterInfo != null) {
                            out.println("    ptr" + j + " = adapter" + j + ";");
                        } else if (passBy instanceof ByVal && callbackParameterTypes[j] != Pointer.class) {
                            out.println("    ptr" + j + (noException(callbackParameterTypes[j], callbackMethod) ?
                                " = new (std::nothrow) " : " = new ") + valueTypeName + typeName[1] +
                                "(*" + cast + "&arg" + j + ");");
                        } else if (passBy instanceof ByVal || passBy instanceof ByRef) {
                            out.println("    ptr" + j + " = " + cast + "&arg" + j + ";");
                        } else if (passBy instanceof ByPtrPtr) {
                            out.println("    if (arg" + j + " == NULL) {");
                            out.println("        JavaCPP_log(\"Pointer address of argument " + j + " is NULL in callback for " + cls.getCanonicalName() + ".\");");
                            out.println("    } else {");
                            out.println("        ptr" + j + " = " + cast + "*arg" + j + ";");
                            out.println("    }");
                        } else { // ByPtr || ByPtrRef
                            out.println("    ptr" + j + " = " + cast + "arg" + j + ";");
                        }
                    }

                    if (Pointer.class.isAssignableFrom(callbackParameterTypes[j])) {
                        String s = "    obj" + j + " = JavaCPP_createPointer(env, " + jclasses.index(callbackParameterTypes[j]) + ");";
                        jclassesInit.index(callbackParameterTypes[j]); // for custom class loaders
                        adapterInfo = adapterInformation(true, valueTypeName, callbackParameterAnnotations[j]);
                        if (adapterInfo != null || passBy instanceof ByPtrPtr || passBy instanceof ByPtrRef) {
                            out.println(s);
                        } else {
                            out.println("    if (ptr" + j + " != NULL) { ");
                            out.println("    " + s);
                            out.println("    }");
                        }
                        out.println("    if (obj" + j + " != NULL) { ");
                        if (needInit) {
                            out.println("        JavaCPP_initPointer(env, obj" + j + ", ptr" + j + ", size" + j + ", deallocator" + j + ");");
                        } else {
                            out.println("        env->SetLongField(obj" + j + ", JavaCPP_addressFID, ptr_to_jlong(ptr" + j + "));");
                        }
                        out.println("    }");
                        out.println("    args[" + j + "].l = obj" + j + ";");
                    } else if (callbackParameterTypes[j] == String.class) {
                        out.println("    jstring obj" + j + " = (const char*)" + (adapterInfo != null ? "adapter" : "arg") + j +
                                " == NULL ? NULL : env->NewStringUTF((const char*)" + (adapterInfo != null ? "adapter" : "arg") + j + ");");
                        out.println("    args[" + j + "].l = obj" + j + ";");
                    } else if (callbackParameterTypes[j].isArray() &&
                            callbackParameterTypes[j].getComponentType().isPrimitive()) {
                        if (adapterInfo == null) {
                            out.println("    jint size" + j + " = ptr" + j + " != NULL ? 1 : 0;");
                        }
                        String s = callbackParameterTypes[j].getComponentType().getName();
                        String S = Character.toUpperCase(s.charAt(0)) + s.substring(1);
                        out.println("    if (ptr" + j + " != NULL) {");
                        out.println("        obj" + j + " = env->New" + S + "Array(size"+ j + ");");
                        out.println("        env->Set" + S + "ArrayRegion(obj" + j + ", 0, size" + j + ", (j" + s + "*)ptr" + j + ");");
                        out.println("    }");
                        if (adapterInfo != null) {
                            out.println("    if (deallocator" + j + " != 0 && ptr" + j + " != NULL) {");
                            out.println("        (*(void(*)(void*))jlong_to_ptr(deallocator" + j + "))((void*)ptr" + j + ");");
                            out.println("    }");
                        }
                    } else if (Buffer.class.isAssignableFrom(callbackParameterTypes[j])) {
                        if (adapterInfo == null) {
                            out.println("    jint size" + j + " = ptr" + j + " != NULL ? 1 : 0;");
                        }
                        out.println("    if (ptr" + j + " != NULL) {");
                        out.println("        obj" + j + " = env->NewDirectByteBuffer((void*)ptr" + j + ", size" + j + ");");
                        out.println("    }");
                    } else {
                        logger.warn("Callback \"" + callbackMethod + "\" has unsupported parameter type \"" +
                                callbackParameterTypes[j].getCanonicalName() + "\". Compilation will most likely fail.");
                    }
                }
            }
        }

        if (methodInfo != null) {
            out.println("    if (" + fieldName + " == NULL) {");
            out.println("        " + fieldName + " = JavaCPP_getMethodID(env, " + jclasses.index(cls) + ", \"" + methodInfo.method.getName() + "\", \"(" +
                    signature(methodInfo.method.getParameterTypes()) + ")" + signature(methodInfo.method.getReturnType()) + "\");");
            out.println("    }");
            out.println("    jmethodID mid = " + fieldName + ";");
        } else {
            out.println("    if (obj == NULL) {");
            out.println("        obj = env->NewGlobalRef(JavaCPP_createPointer(env, " + jclasses.index(cls) + "));");
            out.println("        if (obj == NULL) {");
            out.println("            JavaCPP_log(\"Error creating global reference of " + cls.getCanonicalName() + " instance for callback.\");");
            out.println("        } else {");
            out.println("            env->SetLongField(obj, JavaCPP_addressFID, ptr_to_jlong(this));");
            out.println("        }");
            out.println("        ptr = &" + callbackName + ";");
            out.println("    }");
            out.println("    if (mid == NULL) {");
            out.println("        mid = JavaCPP_getMethodID(env, " + jclasses.index(cls) + ", \"" + callbackMethod.getName() + "\", \"(" +
                    signature(callbackMethod.getParameterTypes()) + ")" + signature(callbackMethod.getReturnType()) + "\");");
            out.println("    }");
        }
        out.println("    if (env->IsSameObject(obj, NULL)) {");
        out.println("        JavaCPP_log(\"Function pointer object is NULL in callback for " + cls.getCanonicalName() + ".\");");
        out.println("    } else if (mid == NULL) {");
        out.println("        JavaCPP_log(\"Error getting method ID of function caller \\\"" + callbackMethod + "\\\" for callback.\");");
        out.println("    } else {");
        String s = "Object";
        if (callbackReturnType.isPrimitive()) {
            s = callbackReturnType.getName();
            s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        out.println("        " + returnPrefix + "env->Call" + s + "MethodA(obj, mid, " + (callbackParameterTypes.length == 0 ? "NULL);" : "args);"));
        out.println("        if ((exc = env->ExceptionOccurred()) != NULL) {");
        out.println("            env->ExceptionClear();");
        out.println("        }");
        out.println("    }");

        for (int j = 0; j < callbackParameterTypes.length; j++) {
            if (Pointer.class.isAssignableFrom(callbackParameterTypes[j])) {
                String[] typeName = cppTypeName(callbackParameterTypes[j]);
                Annotation passBy = by(callbackParameterAnnotations[j]);
                String cast = cast(callbackParameterTypes[j], callbackParameterAnnotations[j]);
                String valueTypeName = valueTypeName(typeName);
                AdapterInformation adapterInfo = adapterInformation(true, valueTypeName, callbackParameterAnnotations[j]);

                if ("void*".equals(typeName[0]) && !callbackParameterTypes[j].isAnnotationPresent(Opaque.class)) {
                    typeName[0] = "char*";
                }
                if (adapterInfo != null || passBy instanceof ByPtrPtr || passBy instanceof ByPtrRef) {
                    out.println("    " + typeName[0] + " rptr" + j + typeName[1] + " = (" +
                            typeName[0] + typeName[1] + ")jlong_to_ptr(env->GetLongField(obj" + j + ", JavaCPP_addressFID));");
                    if (adapterInfo != null) {
                        out.println("    jint rsize" + j + " = env->GetIntField(obj" + j + ", JavaCPP_limitFID);");
                    }
                    if (!callbackParameterTypes[j].isAnnotationPresent(Opaque.class)) {
                        out.println("    jint rposition" + j + " = env->GetIntField(obj" + j + ", JavaCPP_positionFID);");
                        out.println("    rptr" + j + " += rposition" + j + ";");
                        if (adapterInfo != null) {
                            out.println("    rsize" + j + " -= rposition" + j + ";");
                        }
                    }
                    if (adapterInfo != null) {
                        out.println("    adapter" + j + ".assign(rptr" + j + ", rsize" + j + ");");
                    } else if (passBy instanceof ByPtrPtr) {
                        out.println("    if (arg" + j + " != NULL) {");
                        out.println("        *arg" + j + " = *" + cast + "&rptr" + j + ";");
                        out.println("    }");
                    } else if (passBy instanceof ByPtrRef) {
                        out.println("    arg"  + j + " = " + cast + "rptr" + j + ";");
                    }
                }
            }
            if (!callbackParameterTypes[j].isPrimitive()) {
                out.println("    env->DeleteLocalRef(obj" + j + ");");
            }
        }
        out.println("}");
        out.println("end:");

        if (callbackReturnType != void.class) {
            if ("void*".equals(returnTypeName[0]) && !callbackReturnType.isAnnotationPresent(Opaque.class)) {
                returnTypeName[0] = "char*";
            }
            if (Pointer.class.isAssignableFrom(callbackReturnType)) {
                out.println("    " + returnTypeName[0] + " rptr" + returnTypeName[1] + " = rarg == NULL ? NULL : (" +
                        returnTypeName[0] + returnTypeName[1] + ")jlong_to_ptr(env->GetLongField(rarg, JavaCPP_addressFID));");
                if (returnAdapterInfo != null) {
                    out.println("    jint rsize = rarg == NULL ? 0 : env->GetIntField(rarg, JavaCPP_limitFID);");
                }
                if (!callbackReturnType.isAnnotationPresent(Opaque.class)) {
                    out.println("    jint rposition = rarg == NULL ? 0 : env->GetIntField(rarg, JavaCPP_positionFID);");
                    out.println("    rptr += rposition;");
                    if (returnAdapterInfo != null) {
                        out.println("    rsize -= rposition;");
                    }
                }
            } else if (callbackReturnType == String.class) {
                out.println("    " + returnTypeName[0] + " rptr" + returnTypeName[1] + " = rarg == NULL ? NULL : env->GetStringUTFChars(rarg, NULL);");
                if (returnAdapterInfo != null) {
                    out.println("    jint rsize = 0;");
                }
            } else if (Buffer.class.isAssignableFrom(callbackReturnType)) {
                out.println("    " + returnTypeName[0] + " rptr" + returnTypeName[1] + " = rarg == NULL ? NULL : env->GetDirectBufferAddress(rarg);");
                if (returnAdapterInfo != null) {
                    out.println("    jint rsize = rarg == NULL ? 0 : env->GetDirectBufferCapacity(rarg);");
                }
            } else if (!callbackReturnType.isPrimitive()) {
                logger.warn("Callback \"" + callbackMethod + "\" has unsupported return type \"" +
                        callbackReturnType.getCanonicalName() + "\". Compilation will most likely fail.");
            }
        }

        out.println("    if (exc != NULL) {");
        out.println("        jstring str = (jstring)env->CallObjectMethod(exc, JavaCPP_toStringMID);");
        out.println("        env->DeleteLocalRef(exc);");
        out.println("        const char *msg = env->GetStringUTFChars(str, NULL);");
        out.println("        JavaCPP_exception e(msg);");
        out.println("        env->ReleaseStringUTFChars(str, msg);");
        out.println("        env->DeleteLocalRef(str);");
        out.println("        JavaCPP_detach(attached);");
        out.println("        throw e;");
        out.println("    } else {");
        out.println("        JavaCPP_detach(attached);");
        out.println("    }");

        if (callbackReturnType != void.class) {
            if (callbackReturnType.isPrimitive()) {
                out.println("    return " + callbackReturnCast + "rarg;");
            } else if (returnAdapterInfo != null) {
                usesAdapters = true;
                out.println("    return " + returnAdapterInfo.name + "(" + callbackReturnCast + "rptr, rsize);");
            } else if (FunctionPointer.class.isAssignableFrom(callbackReturnType)) {
                functions.index(callbackReturnType);
                out.println("    return " + callbackReturnCast + "(rptr == NULL ? NULL : rptr->ptr);");
            } else if (returnBy instanceof ByVal || returnBy instanceof ByRef) {
                out.println("    if (rptr == NULL) {");
                out.println("        JavaCPP_log(\"Return pointer address is NULL in callback for " + cls.getCanonicalName() + ".\");");
                out.println("        static " + returnValueTypeName + " empty" + returnTypeName[1] + ";");
                out.println("        return empty;");
                out.println("    } else {");
                out.println("        return *" + callbackReturnCast + "rptr;");
                out.println("    }");
            } else if (returnBy instanceof ByPtrPtr) {
                out.println("    return " + callbackReturnCast + "&rptr;");
            } else { // ByPtr || ByPtrRef
                out.println("    return " + callbackReturnCast + "rptr;");
            }
        }
        out.println("}");
    }

    void callbackAllocator(Class cls, String callbackName) {
        // XXX: Here, we should actually allocate new trampolines on the heap somehow...
        // For now it just bumps out from the global variable the last object that called this method
        String instanceTypeName = functionClassName(cls);
        out.println("    obj = env->NewWeakGlobalRef(obj);");
        out.println("    if (obj == NULL) {");
        out.println("        JavaCPP_log(\"Error creating global reference of " + cls.getCanonicalName() + " instance for callback.\");");
        out.println("        return;");
        out.println("    }");
        out.println("    " + instanceTypeName + "* rptr = new (std::nothrow) " + instanceTypeName + ";");
        out.println("    if (rptr != NULL) {");
        out.println("        rptr->ptr = &" + callbackName + ";");
        out.println("        rptr->obj = obj;");
        out.println("        JavaCPP_initPointer(env, obj, rptr, 1, &JavaCPP_" + mangle(cls.getName()) + "_deallocate);");
        deallocators.index(cls);
        out.println("        " + callbackName + "_instance = *rptr;");
        out.println("    }");
        out.println("}");
    }

    boolean checkPlatform(Class<?> cls) {
        while (!cls.isAnnotationPresent(org.bytedeco.javacpp.annotation.Properties.class)
                && !cls.isAnnotationPresent(Platform.class) && cls.getSuperclass() != null) {
            cls = cls.getSuperclass();
        }
        org.bytedeco.javacpp.annotation.Properties classProperties =
            cls.getAnnotation(org.bytedeco.javacpp.annotation.Properties.class);
        if (classProperties != null) {
            Class[] classes = classProperties.inherit();
            if (classes != null) {
                for (Class c : classes) {
                    if (checkPlatform(c)) {
                        return true;
                    }
                }
            }
            Platform[] platforms = classProperties.value();
            if (platforms != null) {
                for (Platform p : platforms) {
                    if (checkPlatform(p)) {
                        return true;
                    }
                }
            }
        } else if (checkPlatform(cls.getAnnotation(Platform.class))) {
            return true;
        }
        return false;
    }

    boolean checkPlatform(Platform platform) {
        if (platform == null) {
            return true;
        }
        String platform2 = properties.getProperty("platform");
        String[][] names = { platform.value(), platform.not() };
        boolean[] matches = { false, false };
        for (int i = 0; i < names.length; i++) {
            for (String s : names[i]) {
                if (platform2.startsWith(s)) {
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

    static String functionClassName(Class<?> cls) {
        Name name = cls.getAnnotation(Name.class);
        return name != null ? name.value()[0] : "JavaCPP_" + mangle(cls.getName());
    }

    static Method functionMethod(Class<?> cls, boolean[] callbackAllocators) {
        if (!FunctionPointer.class.isAssignableFrom(cls)) {
            return null;
        }
        Method[] methods = cls.getDeclaredMethods();
        Method functionMethod = null;
        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            int modifiers = methods[i].getModifiers();
            Class[] parameterTypes = methods[i].getParameterTypes();
            Class returnType = methods[i].getReturnType();
            if (Modifier.isStatic(modifiers)) {
                continue;
            }
            if (callbackAllocators != null && methodName.startsWith("allocate") &&
                    Modifier.isNative(modifiers) && returnType == void.class &&
                    parameterTypes.length == 0) {
                // found a callback allocator method
                callbackAllocators[i] = true;
            } else if (methodName.startsWith("call") || methodName.startsWith("apply")) {
                // found a function caller method and/or callback method
                functionMethod = methods[i];
            }
        }
        return functionMethod;
    }

    MethodInformation methodInformation(Method method) {
        MethodInformation info = new MethodInformation();
        info.cls         = method.getDeclaringClass();
        info.method      = method;
        info.annotations = method.getAnnotations();
        info.modifiers   = method.getModifiers();
        info.returnType  = method.getReturnType();
        info.name = method.getName();
        Name name = method.getAnnotation(Name.class);
        info.memberName = name != null ? name.value() : new String[] { info.name };
        Index index = method.getAnnotation(Index.class);
        info.dim    = index != null ? index.value() : 0;
        info.parameterTypes       = method.getParameterTypes();
        info.parameterAnnotations = method.getParameterAnnotations();
        info.returnRaw = method.isAnnotationPresent(Raw.class);
        info.withEnv = info.returnRaw ? method.getAnnotation(Raw.class).withEnv() : false;
        info.parameterRaw = new boolean[info.parameterAnnotations.length];
        for (int i = 0; i < info.parameterAnnotations.length; i++) {
            for (int j = 0; j < info.parameterAnnotations[i].length; j++) {
                if (info.parameterAnnotations[i][j] instanceof Raw) {
                    info.parameterRaw[i] = true;
                    info.withEnv |= ((Raw)info.parameterAnnotations[i][j]).withEnv();
                }
            }
        }

        boolean canBeGetter =  info.returnType != void.class || (info.parameterTypes.length > 0 &&
                info.parameterTypes[0].isArray() && info.parameterTypes[0].getComponentType().isPrimitive());
        boolean canBeSetter = (info.returnType == void.class ||
                info.returnType == info.cls) && info.parameterTypes.length > 0;
        boolean canBeAllocator = !Modifier.isStatic(info.modifiers) && info.returnType == void.class;
        boolean canBeArrayAllocator = canBeAllocator && info.parameterTypes.length == 1 &&
                (info.parameterTypes[0] == int.class || info.parameterTypes[0] == long.class);

        boolean valueGetter = false;
        boolean valueSetter = false;
        boolean memberGetter = false;
        boolean memberSetter = false;
        boolean noReturnGetter = false;
        Method pairedMethod = null;
        for (Method method2 : info.cls.getDeclaredMethods()) {
            int modifiers2          = method2.getModifiers();
            Class returnType2       = method2.getReturnType();
            String methodName2      = method2.getName();
            Class[] parameterTypes2 = method2.getParameterTypes();
            Annotation[] annotations2 = method2.getAnnotations();
            Annotation[][] parameterAnnotations2 = method2.getParameterAnnotations();
            int skipParameters = info.parameterTypes.length > 0 && info.parameterTypes[0] == Class.class ? 1 : 0;
            int skipParameters2 = parameterTypes2.length > 0 && parameterTypes2[0] == Class.class ? 1 : 0;

            if (method.equals(method2) || !Modifier.isNative(modifiers2)) {
                continue;
            }

            boolean canBeValueGetter = false;
            boolean canBeValueSetter = false;
            boolean canBeMemberGetter = false;
            boolean canBeMemberSetter = false;
            if (canBeGetter && "get".equals(info.name) && "put".equals(methodName2)) {
                canBeValueGetter = true;
            } else if (canBeSetter && "put".equals(info.name) && "get".equals(methodName2)) {
                canBeValueSetter = true;
            } else if (methodName2.equals(info.name)) {
                info.overloaded = true;
                canBeMemberGetter = canBeGetter;
                canBeMemberSetter = canBeSetter;
                for (int j = skipParameters; j < info.parameterTypes.length; j++) {
                    if (info.parameterTypes[j] != int.class && info.parameterTypes[j] != long.class) {
                        canBeMemberGetter = false;
                        if (j < info.parameterTypes.length - 1) {
                            canBeMemberSetter = false;
                        }
                    }
                }
            } else {
                continue;
            }

            boolean sameIndexParameters = true;
            for (int j = 0; j < info.parameterTypes.length - skipParameters && j < parameterTypes2.length - skipParameters2; j++) {
                if (info.parameterTypes[j + skipParameters] != parameterTypes2[j + skipParameters2]) {
                    sameIndexParameters = false;
                }
            }
            if (!sameIndexParameters) {
                continue;
            }

            boolean parameterAsReturn = canBeValueGetter && info.parameterTypes.length > 0 &&
                    info.parameterTypes[0].isArray() && info.parameterTypes[0].getComponentType().isPrimitive();
            boolean parameterAsReturn2 = canBeValueSetter && parameterTypes2.length > 0 &&
                    parameterTypes2[0].isArray() && parameterTypes2[0].getComponentType().isPrimitive();

            if (canBeGetter && parameterTypes2.length - (parameterAsReturn ? 0 : 1) == info.parameterTypes.length - skipParameters
                    && (parameterAsReturn ? info.parameterTypes[info.parameterTypes.length - 1] : info.returnType) ==
                        parameterTypes2[parameterTypes2.length - 1] && (returnType2 == void.class || returnType2 == info.cls)
                    && (parameterAnnotations2[parameterAnnotations2.length - 1].length == 0
                        || (Arrays.equals(parameterAnnotations2[parameterAnnotations2.length - 1], info.annotations)))) {
                pairedMethod = method2;
                valueGetter  = canBeValueGetter;
                memberGetter = canBeMemberGetter;
                noReturnGetter = parameterAsReturn;
            } else if (canBeSetter && info.parameterTypes.length - (parameterAsReturn2 ? 0 : 1) == parameterTypes2.length - skipParameters2
                    && (parameterAsReturn2 ? parameterTypes2[parameterTypes2.length - 1] : returnType2) ==
                        info.parameterTypes[info.parameterTypes.length - 1] && (info.returnType == void.class || info.returnType == info.cls)
                    && (info.parameterAnnotations[info.parameterAnnotations.length - 1].length == 0
                        || (Arrays.equals(info.parameterAnnotations[info.parameterAnnotations.length - 1], annotations2)))) {
                pairedMethod = method2;
                valueSetter  = canBeValueSetter;
                memberSetter = canBeMemberSetter;
            }
        }

        Annotation behavior = behavior(info.annotations);
        if (canBeGetter && behavior instanceof ValueGetter) {
            info.valueGetter = true;
            info.noReturnGetter = noReturnGetter;
        } else if (canBeSetter && behavior instanceof ValueSetter) {
            info.valueSetter = true;
        } else if (canBeGetter && behavior instanceof MemberGetter) {
            info.memberGetter = true;
            info.noReturnGetter = noReturnGetter;
        } else if (canBeSetter && behavior instanceof MemberSetter) {
            info.memberSetter = true;
        } else if (canBeAllocator && behavior instanceof Allocator) {
            info.allocator = true;
        } else if (canBeArrayAllocator && behavior instanceof ArrayAllocator) {
            info.allocator = info.arrayAllocator = true;
        } else if (behavior == null) {
            // try to guess the behavior of the method
            if (info.returnType == void.class && "deallocate".equals(info.name) &&
                    !Modifier.isStatic(info.modifiers) && info.parameterTypes.length == 2 &&
                    info.parameterTypes[0] == long.class && info.parameterTypes[1] == long.class) {
                info.deallocator = true;
            } else if (canBeAllocator && "allocate".equals(info.name)) {
                info.allocator = true;
            } else if (canBeArrayAllocator && "allocateArray".equals(info.name)) {
                info.allocator = info.arrayAllocator = true;
            } else if (info.returnType.isAssignableFrom(ByteBuffer.class) && "asDirectBuffer".equals(info.name) &&
                    !Modifier.isStatic(info.modifiers) && info.parameterTypes.length == 0) {
                info.bufferGetter = true;
            } else if (valueGetter) {
                info.valueGetter = true;
                info.noReturnGetter = noReturnGetter;
                info.pairedMethod = pairedMethod;
            } else if (valueSetter) {
                info.valueSetter = true;
                info.pairedMethod = pairedMethod;
            } else if (memberGetter) {
                info.memberGetter = true;
                info.noReturnGetter = noReturnGetter;
                info.pairedMethod = pairedMethod;
            } else if (memberSetter) {
                info.memberSetter = true;
                info.pairedMethod = pairedMethod;
            }
        } else if (!(behavior instanceof Function)) {
            logger.warn("Method \"" + method + "\" cannot behave like a \"" +
                    behavior.annotationType().getSimpleName() + "\". No code will be generated.");
            return null;
        }

        if (name == null && info.pairedMethod != null) {
            name = info.pairedMethod.getAnnotation(Name.class);
            if (name != null) {
                info.memberName = name.value();
            }
        }

        info.noOffset = info.cls.isAnnotationPresent(NoOffset.class) ||
                          method.isAnnotationPresent(NoOffset.class) ||
                          method.isAnnotationPresent(Index.class);
        if (!info.noOffset && info.pairedMethod != null) {
            info.noOffset = info.pairedMethod.isAnnotationPresent(NoOffset.class) ||
                            info.pairedMethod.isAnnotationPresent(Index.class);
        }

        if (info.parameterTypes.length == 0 || !info.parameterTypes[0].isArray()) {
            if (info.valueGetter || info.memberGetter) {
                info.dim = info.parameterTypes.length;
            } else if (info.memberSetter || info.valueSetter) {
                info.dim = info.parameterTypes.length-1;
            }
        }

        info.throwsException = null;
        if (!noException(info.cls, method)) {
            if ((by(info.annotations) instanceof ByVal && !noException(info.returnType, method)) ||
                    !info.deallocator && !info.valueGetter && !info.valueSetter &&
                    !info.memberGetter && !info.memberSetter && !info.bufferGetter) {
                Class<?>[] exceptions = method.getExceptionTypes();
                info.throwsException = exceptions.length > 0 ? exceptions[0] : RuntimeException.class;
            }
        }
        return info;
    }

    static boolean noException(Class<?> cls, Method method) {
        boolean noException = baseClasses.contains(cls) ||
                method.isAnnotationPresent(NoException.class);
        while (!noException && cls != null) {
            if (noException = cls.isAnnotationPresent(NoException.class)) {
                break;
            }
            cls = cls.getDeclaringClass();
        }
        return noException;
    }

    AdapterInformation adapterInformation(boolean out, MethodInformation methodInfo, int j) {
        if (out && (methodInfo.parameterTypes[j] == String.class || methodInfo.valueSetter || methodInfo.memberSetter)) {
            return null;
        }
        String typeName = cast(methodInfo, j);
        if (typeName != null && typeName.startsWith("(") && typeName.endsWith(")")) {
            typeName = typeName.substring(1, typeName.length()-1);
        }
        if (typeName == null || typeName.length() == 0) {
            typeName = cppCastTypeName(methodInfo.parameterTypes[j], methodInfo.parameterAnnotations[j])[0];
        }
        String valueTypeName = valueTypeName(typeName);
        AdapterInformation adapter = adapterInformation(out, valueTypeName, methodInfo.parameterAnnotations[j]);
        if (adapter == null && methodInfo.pairedMethod != null && j == methodInfo.parameterTypes.length - 1 &&
                (methodInfo.valueSetter || methodInfo.memberSetter)) {
            adapter = adapterInformation(out, valueTypeName, methodInfo.pairedMethod.getAnnotations());
        }
        return adapter;
    }

    AdapterInformation adapterInformation(boolean out, String valueTypeName, Annotation ... annotations) {
        AdapterInformation adapterInfo = null;
        boolean constant = false;
        String cast = "";
        for (Annotation a : annotations) {
            Adapter adapter = a instanceof Adapter ? (Adapter)a : a.annotationType().getAnnotation(Adapter.class);
            if (adapter != null) {
                adapterInfo = new AdapterInformation();
                adapterInfo.name = adapter.value();
                adapterInfo.argc = adapter.argc();
                if (a != adapter) {
                    try {
                        Class cls = a.annotationType();
                        if (cls.isAnnotationPresent(Const.class)) {
                            constant = true;
                        }
                        try {
                            String value = cls.getDeclaredMethod("value").invoke(a).toString();
                            if (value != null && value.length() > 0) {
                                valueTypeName = value;
                            }
                            // else use inferred type
                        } catch (NoSuchMethodException e) {
                            // this adapter does not support a template type
                            valueTypeName = null;
                        }
                        Cast c = (Cast)cls.getAnnotation(Cast.class);
                        if (c != null && cast.length() == 0) {
                            cast = c.value()[0];
                            if (valueTypeName != null) {
                                cast += "< " + valueTypeName + " >";
                            }
                            if (c.value().length > 1) {
                                cast += c.value()[1];
                            }
                        }
                    } catch (Exception ex) { 
                        logger.warn("Could not invoke the value() method on annotation \"" + a + "\": " + ex);
                    }
                    if (valueTypeName != null && valueTypeName.length() > 0) {
                        adapterInfo.name += "< " + valueTypeName + " >";
                    }
                }
            } else if (a instanceof Const) {
                constant = true;
            } else if (a instanceof Cast) {
                Cast c = ((Cast)a);
                if (c.value().length > 1) {
                    cast = c.value()[1];
                }
            }
        }
        if (adapterInfo != null) {
            adapterInfo.cast = cast;
            adapterInfo.constant = constant;
        }
        return out && constant ? null : adapterInfo;
    }

    String cast(MethodInformation methodInfo, int j) {
        String cast = cast(methodInfo.parameterTypes[j], methodInfo.parameterAnnotations[j]);
        if ((cast == null || cast.length() == 0) && j == methodInfo.parameterTypes.length-1 &&
                (methodInfo.valueSetter || methodInfo.memberSetter) && methodInfo.pairedMethod != null) {
            cast = cast(methodInfo.pairedMethod.getReturnType(), methodInfo.pairedMethod.getAnnotations());
        }
        return cast;
    }

    String cast(Class<?> type, Annotation ... annotations) {
        String[] typeName = null;
        for (Annotation a : annotations) {
            if ((a instanceof Cast && ((Cast)a).value()[0].length() > 0) || a instanceof Const) {
                typeName = cppCastTypeName(type, annotations);
                break;
            }
        }
        return typeName != null && typeName.length > 0 ? "(" + typeName[0] + typeName[1] + ")" : "";
    }

    Annotation by(MethodInformation methodInfo, int j) {
        Annotation passBy = by(methodInfo.parameterAnnotations[j]);
        if (passBy == null && methodInfo.pairedMethod != null &&
                (methodInfo.valueSetter || methodInfo.memberSetter)) {
            passBy = by(methodInfo.pairedMethod.getAnnotations());
        }
        return passBy;
    }

    Annotation by(Annotation ... annotations) {
        Annotation byAnnotation = null;
        for (Annotation a : annotations) {
            if (a instanceof ByPtr || a instanceof ByPtrPtr || a instanceof ByPtrRef ||
                    a instanceof ByRef || a instanceof ByVal) {
                if (byAnnotation != null) {
                    logger.warn("\"By\" annotation \"" + byAnnotation +
                            "\" already found. Ignoring superfluous annotation \"" + a + "\".");
                } else {
                    byAnnotation = a;
                }
            }
        }
        return byAnnotation;
    }

    Annotation behavior(Annotation ... annotations) {
        Annotation behaviorAnnotation = null;
        for (Annotation a : annotations) {
            if (a instanceof Function || a instanceof Allocator || a instanceof ArrayAllocator ||
                    a instanceof ValueSetter || a instanceof ValueGetter ||
                    a instanceof MemberGetter || a instanceof MemberSetter) {
                if (behaviorAnnotation != null) {
                    logger.warn("Behavior annotation \"" + behaviorAnnotation +
                            "\" already found. Ignoring superfluous annotation \"" + a + "\".");
                } else {
                    behaviorAnnotation = a;
                }
            }
        }
        return behaviorAnnotation;
    }

    static String constValueTypeName(String ... typeName) {
        String type = typeName[0];
        if (type.endsWith("*") || type.endsWith("&")) {
            type = type.substring(0, type.length()-1);
        }
        return type;
    }

    static String valueTypeName(String ... typeName) {
        String type = typeName[0];
        if (type.startsWith("const ")) {
            type = type.substring(6, type.length()-1);
        } else if (type.endsWith("*") || type.endsWith("&")) {
            type = type.substring(0, type.length()-1);
        }
        return type;
    }

    String[] cppAnnotationTypeName(Class<?> type, Annotation ... annotations) {
        String[] typeName = cppCastTypeName(type, annotations);
        String prefix = typeName[0];
        String suffix = typeName[1];

        boolean casted = false;
        for (Annotation a : annotations) {
            if ((a instanceof Cast && ((Cast)a).value()[0].length() > 0) || a instanceof Const) {
                casted = true;
                break;
            }
        }

        Annotation by = by(annotations);
        if (by instanceof ByVal) {
            prefix = constValueTypeName(typeName);
        } else if (by instanceof ByRef) {
            prefix = constValueTypeName(typeName) + "&";
        } else if (by instanceof ByPtrPtr && !casted) {
            prefix = prefix + "*";
        } else if (by instanceof ByPtrRef) {
            prefix = prefix + "&";
        } // else ByPtr

        typeName[0] = prefix;
        typeName[1] = suffix;
        return typeName;
    }

    String[] cppCastTypeName(Class<?> type, Annotation ... annotations) {
        String[] typeName = null;
        boolean warning = false, adapter = false;
        for (Annotation a : annotations) {
            if (a instanceof Cast) {
                warning = typeName != null;
                String prefix = ((Cast)a).value()[0], suffix = "";
                int parenthesis = prefix.indexOf(')');
                if (parenthesis > 0) {
                    suffix = prefix.substring(parenthesis).trim();
                    prefix = prefix.substring(0, parenthesis).trim();
                }
                typeName = prefix.length() > 0 ? new String[] { prefix, suffix } : null;
            } else if (a instanceof Const) {
                if (warning = typeName != null) {
                    // prioritize @Cast
                    continue;
                }
                typeName = cppTypeName(type);
                boolean[] b = ((Const)a).value();
                if (b.length > 1 && b[1]) {
                    typeName[0] = valueTypeName(typeName) + " const *";
                }
                if (b.length > 0 && b[0]) {
                    typeName[0] = "const " + typeName[0];
                }
                Annotation by = by(annotations);
                if (by instanceof ByPtrPtr) {
                    typeName[0] += "*";
                } else if (by instanceof ByPtrRef) {
                    typeName[0] += "&";
                }
            } else if (a instanceof Adapter || a.annotationType().isAnnotationPresent(Adapter.class)) {
                adapter = true;
            }
        }
        if (warning && !adapter) {
            logger.warn("Without \"Adapter\", \"Cast\" and \"Const\" annotations are mutually exclusive.");
        }
        if (typeName == null) {
            typeName = cppTypeName(type);
        }
        return typeName;
    }

    String[] cppTypeName(Class<?> type) {
        String prefix = "", suffix = "";
        if (type == Buffer.class || type == Pointer.class) {
            prefix = "void*";
        } else if (type == byte[].class || type == ByteBuffer.class || type == BytePointer.class) {
            prefix = "signed char*";
        } else if (type == short[].class || type == ShortBuffer.class || type == ShortPointer.class) {
            prefix = "short*";
        } else if (type == int[].class || type == IntBuffer.class || type == IntPointer.class) {
            prefix = "int*";
        } else if (type == long[].class || type == LongBuffer.class || type == LongPointer.class) {
            prefix = "jlong*";
        } else if (type == float[].class || type == FloatBuffer.class || type == FloatPointer.class) {
            prefix = "float*";
        } else if (type == double[].class || type == DoubleBuffer.class || type == DoublePointer.class) {
            prefix = "double*";
        } else if (type == char[].class || type == CharBuffer.class || type == CharPointer.class) {
            prefix = "unsigned short*";
        } else if (type == boolean[].class) {
            prefix = "unsigned char*";
        } else if (type == PointerPointer.class) {
            prefix = "void**";
        } else if (type == String.class) {
            prefix = "const char*";
        } else if (type == byte.class) {
            prefix = "signed char";
        } else if (type == long.class) {
            prefix = "jlong";
        } else if (type == char.class) {
            prefix = "unsigned short";
        } else if (type == boolean.class) {
            prefix = "unsigned char";
        } else if (type.isPrimitive()) {
            prefix = type.getName();
        } else if (FunctionPointer.class.isAssignableFrom(type)) {
            Method functionMethod = functionMethod(type, null);
            if (functionMethod != null) {
                return cppFunctionTypeName(functionMethod);
            }
        } else {
            String scopedType = cppScopeName(type);
            if (scopedType.length() > 0) {
                prefix = scopedType + "*";
            } else {
                logger.warn("The class " + type.getCanonicalName() +
                        " does not map to any C++ type. Compilation will most likely fail.");
            }
        }
        return new String[] { prefix, suffix };
    }

    String[] cppFunctionTypeName(Method functionMethod) {
        String prefix = "", suffix = "";
        Class<?> type = functionMethod.getDeclaringClass();
        Convention convention = type.getAnnotation(Convention.class);
        String callingConvention = convention == null ? "" : convention.value() + " ";
        // for virtual functions, the namespace is managed by the enclosing class
        Namespace namespace = FunctionPointer.class.isAssignableFrom(type) ? type.getAnnotation(Namespace.class) : null;
        String spaceName = namespace == null ? "" : namespace.value();
        if (spaceName.length() > 0 && !spaceName.endsWith("::")) {
            spaceName += "::";
        }
        Class returnType = functionMethod.getReturnType();
        Class[] parameterTypes = functionMethod.getParameterTypes();
        Annotation[] annotations = functionMethod.getAnnotations();
        Annotation[][] parameterAnnotations = functionMethod.getParameterAnnotations();
        String[] returnTypeName = cppAnnotationTypeName(returnType, annotations);
        AdapterInformation returnAdapterInfo = adapterInformation(false, valueTypeName(returnTypeName), annotations);
        if (returnAdapterInfo != null && returnAdapterInfo.cast.length() > 0) {
            prefix = returnAdapterInfo.cast;
        } else {
            prefix = returnTypeName[0] + returnTypeName[1];
        }
        prefix += " (" + callingConvention + spaceName + "*";
        suffix = ")(";
        if (FunctionPointer.class.isAssignableFrom(type) && namespace != null
                && (parameterTypes.length == 0 || !Pointer.class.isAssignableFrom(parameterTypes[0]))) {
            logger.warn("First parameter of caller method call() or apply() for member function pointer " +
                    type.getCanonicalName() + " is not a Pointer. Compilation will most likely fail.");
        }
        for (int j = namespace == null ? 0 : 1; j < parameterTypes.length; j++) {
            String[] paramTypeName = cppAnnotationTypeName(parameterTypes[j], parameterAnnotations[j]);
            AdapterInformation paramAdapterInfo = adapterInformation(false, valueTypeName(paramTypeName), parameterAnnotations[j]);
            if (paramAdapterInfo != null && paramAdapterInfo.cast.length() > 0) {
                suffix += paramAdapterInfo.cast + " arg" + j;
            } else {
                suffix += paramTypeName[0] + " arg" + j + paramTypeName[1];
            }
            if (j < parameterTypes.length - 1) {
                suffix += ", ";
            }
        }
        suffix += ")";
        if (type.isAnnotationPresent(Const.class)) {
            suffix += " const";
        }
        return new String[] { prefix, suffix };
    }

    static String cppScopeName(MethodInformation methodInfo) {
        String scopeName = cppScopeName(methodInfo.cls);
        if (methodInfo.method.isAnnotationPresent(Virtual.class)) {
            String subType = "JavaCPP_" + mangle(scopeName);
            scopeName = subType;
        }
        Namespace namespace = methodInfo.method.getAnnotation(Namespace.class);
        String spaceName = namespace == null ? "" : namespace.value();
        if ((namespace != null && namespace.value().length() == 0) || spaceName.startsWith("::")) {
            scopeName = ""; // user wants to reset namespace here
        }
        if (scopeName.length() > 0 && !scopeName.endsWith("::")) {
            scopeName += "::";
        }
        scopeName += spaceName;
        if (spaceName.length() > 0 && !spaceName.endsWith("::")) {
            scopeName += "::";
        }
        return scopeName + methodInfo.memberName[0];
    }

    static String cppScopeName(Class<?> type) {
        String scopeName = "";
        while (type != null) {
            Namespace namespace = type.getAnnotation(Namespace.class);
            String spaceName = namespace == null ? "" : namespace.value();
            if (Pointer.class.isAssignableFrom(type) && type != Pointer.class) {
                Name name = type.getAnnotation(Name.class);
                String s;
                if (name == null) {
                    s = type.getName();
                    int i = s.lastIndexOf("$");
                    if (i < 0) {
                        i = s.lastIndexOf(".");
                    }
                    s = s.substring(i+1);
                } else {
                    s = name.value()[0];
                }
                if (spaceName.length() > 0 && !spaceName.endsWith("::")) {
                    spaceName += "::";
                }
                spaceName += s;
            }
            if (scopeName.length() > 0 && !spaceName.endsWith("::")) {
                spaceName += "::";
            }
            scopeName = spaceName + scopeName;
            if ((namespace != null && namespace.value().length() == 0) || spaceName.startsWith("::")) {
                // user wants to reset namespace here
                break;
            }
            type = type.getDeclaringClass();
        }
        return scopeName;
    }

    static String jniTypeName(Class type) {
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
        } else if (type == char.class) {
            return "jchar";
        } else if (type == boolean.class) {
            return "jboolean";
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
        } else if (type == char[].class) {
            return "jcharArray";
        } else if (type == boolean[].class) {
            return "jbooleanArray";
        } else if (type.isArray()) {
            return "jobjectArray";
        } else if (type == String.class) {
            return "jstring";
        } else if (type == Class.class) {
            return "jclass";
        } else if (type == void.class) {
            return "void";
        } else {
            return "jobject";
        }
    }

    static String signature(Class ... types) {
        StringBuilder signature = new StringBuilder(2*types.length);
        for (Class type : types) {
            if (type == byte.class) {
                signature.append("B");
            } else if (type == short.class) {
                signature.append("S");
            } else if (type == int.class) {
                signature.append("I");
            } else if (type == long.class) {
                signature.append("J");
            } else if (type == float.class) {
                signature.append("F");
            } else if (type == double.class) {
                signature.append("D");
            } else if (type == boolean.class) {
                signature.append("Z");
            } else if (type == char.class) {
                signature.append("C");
            } else if (type == void.class) {
                signature.append("V");
            } else if (type.isArray()) {
                signature.append(type.getName().replace('.', '/'));
            } else {
                signature.append("L").append(type.getName().replace('.', '/')).append(";");
            }
        }
        return signature.toString();
    }

    static String mangle(String name) {
        StringBuilder mangledName = new StringBuilder(2*name.length());
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
