=JavaCPP=

==Introduction==
JavaCPP provides efficient access to native C++ inside Java, not unlike the way some C/C++ compilers interact with assembly language. No need to invent [http://www.ecma-international.org/publications/standards/Ecma-372.htm a whole new language], whatever Microsoft may opine about it. Under the hood, it uses JNI, so it works with all Java implementations, [#Instructions_for_Android including Android]. In contrast to other approaches ([http://www.swig.org/ SWIG], [http://www.teamdev.com/jniwrapper/ JNIWrapper], [http://msdn.microsoft.com/en-us/library/0h9e9t7d.aspx Platform Invoke], [http://jogamp.org/gluegen/www/ GlueGen], [http://homepage.mac.com/pcbeard/JNIDirect/ JNIDirect], [https://github.com/twall/jna JNA], [http://flinflon.brandonu.ca/Dueck/SystemsProgramming/JniMarshall/ JniMarshall], [http://jnative.free.fr/ JNative], [http://www.jinvoke.com/ J/Invoke], [http://hawtjni.fusesource.org/ HawtJNI], [http://code.google.com/p/bridj/ BridJ], etc.), it supports naturally and efficiently many features of the C++ language often considered problematic, including overloaded operators, template classes and functions, member function pointers, callback functions, nested struct definitions, variable length arguments, nested namespaces, large data structures containing arbitrary cycles, multiple inheritance, passing/returning by value/reference/vector, anonymous unions, bit fields, exceptions, destructors and garbage collection. Obviously, neatly supporting the whole of C++ would require more work (although one could argue about the intrinsic neatness of C++), but I am releasing it here as a proof of concept. I have already used it to produce complete interfaces to OpenCV, FFmpeg, libdc1394, PGR FlyCapture, OpenKinect, videoInput, and ARToolKitPlus as part of [http://code.google.com/p/javacv/ JavaCV].


==Required Software==
To use JavaCPP, you will need to download and install the following software:
 * An implementation of Java SE 6 or 7
  * OpenJDK  http://openjdk.java.net/install/  or
  * Sun JDK  http://www.oracle.com/technetwork/java/javase/downloads/  or
  * IBM JDK  http://www.ibm.com/developerworks/java/jdk/  or
  * Java SE for Mac OS X  http://developer.apple.com/java/  etc.
 * A C++ compiler, out of which these have been tested
  * GNU C/C++ Compiler (Linux, Mac OS X, etc.)  http://gcc.gnu.org/
  * Microsoft C/C++ Compiler  http://msdn.microsoft.com/
    * [http://msdn.microsoft.com/en-us/library/ms235639.aspx  Walkthrough: Compiling a Native C++ Program on the Command Line]

To produce binary files for Android, you will also have to install:
 * Android NDK r7b  http://developer.android.com/sdk/ndk/

To modify the source code, please note that the project files were created for:
 * NetBeans 6.9  http://www.netbeans.org/downloads/

Please feel free to ask questions on [http://groups.google.com/group/javacpp-project the mailing list] if you encounter any problems with the software! I am sure it is far from perfect...


==Key Use Cases==
The most common use case involves accessing some legacy library written for C++, for example, inside a file named `LegacyLibrary.h` containing this C++ class:
{{{
#include <string>

namespace LegacyLibrary {
    class LegacyClass {
        public:
            const std::string& getProperty() { return property; }
            void setProperty(const std::string& property) { this->property = property; }
        private:
            std::string property;
    };
}
}}}

To get the job done with JavaCPP, we can easily define a Java class such as this one (although having an IDE generate that code for us would be even better):
{{{
import com.googlecode.javacpp.*;
import com.googlecode.javacpp.annotation.*;

@Platform(include="LegacyLibrary.h")
@Namespace("LegacyLibrary")
public class LegacyLibrary {
    public static class LegacyClass extends Pointer {
        static { Loader.load(); }
        public LegacyClass() { allocate(); }
        private native void allocate();

        public native @ByRef String getProperty();
        public native void setProperty(String property);
    }

    public static void main(String[] args) {
        LegacyClass l = new LegacyClass();
        l.setProperty("Hello World!");
        System.out.println(l.getProperty());
    }
}
}}}

After compiling the Java source code in the usual way, we also need to build using JavaCPP as follows:
{{{
[saudet@nemesis workspace]$ javac -cp javacpp.jar:. LegacyLibrary.java 

[saudet@nemesis workspace]$ java -jar javacpp.jar LegacyLibrary
Generating source file: /home/saudet/workspace/jniLegacyLibrary.cpp
Building library file: /home/saudet/workspace/linux-x86_64/libjniLegacyLibrary.so
g++ -I/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/include
-I/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/include/linux
/home/saudet/workspace/jniLegacyLibrary.cpp -march=x86-64 -m64 -Wall -O3 -fPIC
-shared -s -o /home/saudet/workspace/linux-x86_64/libjniLegacyLibrary.so 

[saudet@nemesis workspace]$ java -cp javacpp.jar:. LegacyLibrary
Hello World!
}}}
<br>


To demonstrate its relative ease of use even in the face of complex data types, imagine we had a C++ function that took a `vector<vector<void*> >` as argument. To support that type, we could define a bare-bones class like this:
{{{
import com.googlecode.javacpp.*;
import com.googlecode.javacpp.annotation.*;

@Platform(include="<vector>")
public class VectorTest {

    @Name("std::vector<std::vector<void*> >") @Index
    public static class PointerVectorVector extends Pointer {
        static { Loader.load(); }
        public PointerVectorVector()       { allocate();  }
        public PointerVectorVector(long n) { allocate(n); }
        public PointerVectorVector(Pointer p) { super(p); } // this = (vector<vector<void*> >*)p
        private native void allocate();                  // this = new vector<vector<void*> >()
        private native void allocate(long n);            // this = new vector<vector<void*> >(n)
        @Name("operator=")
        public native @ByRef PointerVectorVector copy(@ByRef PointerVectorVector x);

        @Name("operator[]")
        public native @Adapter("VectorAdapter<void*>") PointerPointer get(long n);
        public native @Adapter("VectorAdapter<void*>") PointerPointer at(long n);

        public native long size();
        public native @Cast("bool") boolean empty();
        public native void resize(long n);
        public native @Index(1) long size(long i);
        public native @Index(1) @Cast("bool") boolean empty(long i);
        public native @Index(1) void resize(long i, long n);

        // These two depend on the class-level @Index annotation.
        public native Pointer get(long i, long j);         // return this[i][j]
        public native void put(long i, long j, Pointer p); // this[i][j] = p
    }

    public static void main(String[] args) {
        PointerVectorVector v = new PointerVectorVector(13);
        v.resize(0, 42); // v[0].resize(42)
        Pointer p = new Pointer() { { address = 0xDEADBEEFL; } };
        v.put(0, 0, p);  // v[0][0] = p

        PointerVectorVector v2 = new PointerVectorVector().copy(v);
        Pointer p2 = v2.get(0).get(); // p2 = *(&v[0][0])
        System.out.println(v2.size() + " " + v2.size(0) + "  " + p2);

        v2.at(42);
    }
}
}}}

Executing that program on my machine using these commands produces the following output:
{{{
[saudet@nemesis workspace]$ javac -cp javacpp.jar:. VectorTest.java

[saudet@nemesis workspace]$ java -jar javacpp.jar VectorTest
Generating source file: /home/saudet/workspace/jniVectorTest.cpp
Building library file: /home/saudet/workspace/linux-x86_64/libjniVectorTest.so
g++ -I/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/include
-I/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/include/linux
/home/saudet/workspace/jniVectorTest.cpp -march=x86-64 -m64 -Wall -O3 -fPIC
-shared -s -o /home/saudet/workspace/linux-x86_64/libjniVectorTest.so

[saudet@nemesis workspace]$ java -cp javacpp.jar:. VectorTest
13 42  com.googlecode.javacpp.Pointer[address=0xdeadbeef,position=0,capacity=0,deallocator=null]
Exception in thread "main" java.lang.RuntimeException: vector::_M_range_check
	at VectorTest$PointerVectorVector.at(Native Method)
	at VectorTest.main(VectorTest.java:44)
}}}
<br>


Other times, we may wish to code in C++ for performance reasons. Suppose our profiler had identified that a method named `Processor.process()` took 90% of the program's execution time:
{{{
public class Processor {
    public static void process(java.nio.Buffer buffer, int size) {
        System.out.println("Processing in Java...");
        // ...
    }

    public static void main(String[] args) {
        process(null, 0);
    }
}
}}}

After many days of hard work and sweat, the engineers figured out some hacks and managed to make that ratio drop to 80%, but you know, the managers were still not satisfied. So, we could try to rewrite it in C++ (or even assembly language for that matter via the inline assembler) and place the resulting function in a file named say `Processor.h`:
{{{
#include <iostream>

static inline void process(void *buffer, int size) {
    std::cout << "Processing in C++..." << std::endl;
    // ...
}
}}}

After adjusting the Java source code to something like this:
{{{
import com.googlecode.javacpp.*;
import com.googlecode.javacpp.annotation.*;

@Platform(include="Processor.h")
public class Processor {
    static { Loader.load(); }

    public static native void process(java.nio.Buffer buffer, int size);

    public static void main(String[] args) {
        process(null, 0);
    }
}
}}}

It would then compile and execute like this:
{{{
[saudet@nemesis workspace]$ javac -cp javacpp.jar:. Processor.java

[saudet@nemesis workspace]$ java -jar javacpp.jar Processor
Generating source file: /home/saudet/workspace/jniProcessor.cpp
Building library file: /home/saudet/workspace/linux-x86_64/libjniProcessor.so
g++ -I/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/include
-I/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/include/linux
/home/saudet/workspace/jniProcessor.cpp -march=x86-64 -m64 -Wall -O3 -fPIC
-shared -s -o /home/saudet/workspace/linux-x86_64/libjniProcessor.so

[saudet@nemesis workspace]$ java -cp javacpp.jar:. Processor
Processing in C++...
}}}
<br>


To implement `native` methods, JavaCPP generates appropriate code for JNI, and passes it to the C++ compiler to build a native library. At no point do we need to get our hands dirty with JNI, makefiles, or other native tools. The important thing to realize here is that, while we do all customization inside the Java language using annotations, JavaCPP produces code that has *zero overhead* compared to manually coded JNI functions (verify the generated .cpp files to convince yourself). Moreover, at runtime, the `Loader.load()` method automatically loads the native libraries from Java resources, which were placed in the right directory by the building process. They can even be archived in a JAR file, it changes nothing. Users simply do not need to figure out how to make the system load the files. 

To learn more about how to use the features of this tool, since documentation currently lacks, please refer to [http://code.google.com/p/javacv/source/browse/trunk/javacv/src/com/googlecode/javacv/cpp/ the source code of JavaCV].

As a matter of course, this all works with the Scala language as well, but to make the process even smoother, I would imagine that it should not be too hard to add support for "native properties", such that declarations like `@native var` could generate native getter and setter methods...


==Instructions for Android==
Inside the directory of the Android project:
 # Copy the `javacpp.jar` file into the `libs/` subdirectory, and
 # Run this command to produce the `*.so` library files in `libs/armeabi/`:
{{{
java -jar libs/javacpp.jar -classpath bin/ -classpath bin/classes/ -d libs/armeabi/ \
-properties android-arm -Dplatform.root=<path to android-ndk-r7b> \
-Dcompiler.path=<path to arm-linux-androideabi-g++> <class names>
}}}
And to make everything automatic, we may insert that command into, for example, the Ant `build.xml` file or the Eclipse `.project` file as a [http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.user/gettingStarted/qs-96_non_ant_pjs.htm Non-Ant project builder].


==Acknowledgments==
I am currently an active member of the Okutomi & Tanaka Laboratory, Tokyo Institute of Technology, supported by a scholarship from the Ministry of Education, Culture, Sports, Science and Technology (MEXT) of the Japanese Government.


==Changes==
===February 18, 2012===
 * Cleaned up a few minor `Exception` blocks
 * New `Pointer.deallocateReferences()` static method to force immediate deallocation of all native memory allocated by `Pointer` objects that since have been garbage collected
 * Updated `android-arm.properties` to reflect the fact that, starting from Android NDK r7, `libstdc++.a` has been surreptitiously renamed to `libgnustl_static.a`, such that JavaCPP was instead linking to a new bogus `libstdc++.so` library, causing runtime linking errors
 * Included new `android-x86.properties` to compile binaries for that platform as well
 * Added new `compiler.sysroot.prefix` and `compiler.sysroot` platform properties to pass options such as `--sysroot` to satisfy new rituals of the Android NDK starting from r7b
 * Upgraded references of the Android NDK to version r7b

===January 8, 2012===
 * Added new `compiler.linkpath.prefix2` platform property to pass options such as `-Wl,-rpath,` to linkers that support them
 * Fixed `Loader.load()` on Android 4.0, where `SecurityManager.getClassContext()` returns `null`
 * Upgraded references of the Android NDK to version r7

===October 29, 2011===
 * Changed the following to make MinGW work: `Generator` now maps `jlong` to the more standard `long long` instead of `__int64` type and also includes `stdint.h`, and added `-D_JNI_IMPLEMENTATION_ -Wl,--kill-at` to the compiler options, as recommended by MinGW's documentation for building DLLs compatible with JNI 
 * Added hack for `AttachCurrentThread()`, whose signature differs under Android, and `DetachCurrentThread()` now gets called as appropriate after returning from a callback function, to prevent memory leaks (and also crashes on platforms such as Android) (issue #3)
 * `Generator` now generates correct code for the annotation pairs `@Const @ByRef` and `@Const @ByVal` (issue #4)
 * Worked around callback functions crashing on Android, which is unable to load user classes from native threads (issue #5)
 * Fixed a few potential pitfalls inside `Generator` and `Loader`
 * Removed compiler warnings due to the type of the `capacity` member variable of `VectorAdapter`
 * Callback `FunctionPointer` objects may now return `@ByVal` or `@ByRef`
 * On Android, changed the output of runtime error messages from `stderr` (equivalent to `/dev/null` on Android) to the log

===October 1, 2011===
 * Changed default option flag "/MT" to "/MD" (and a few others that Visual Studio uses by default) inside `windows-x86.properties` and `windows-x86_64.properties` because `std::vector`, `VectorAdapter` and C++ memory allocation in general does not work well with static runtime libraries across DLLs under Windows Vista and Windows 7 for some reason, and because Microsoft fixed the manifest file insanity starting with Visual C++ 2010
 * `Builder` now searches for `jni.h` and `jni_md.h` inside `/System/Library/Frameworks/JavaVM.framework/Headers/` if not found inside `java.home`, as with Mac OS X Lion (issue #2)
 * Upgraded references of the Android NDK to version r6b
 * Fixed a few potential pitfalls inside `Generator`
 * Added hack to let `*Pointer` classes with a corresponding `*Buffer` class have constructors for them

===July 5, 2011===
 * `Generator` now lets `get()/put()` (or the `ValueGetter/ValueSetter` annotated) methods use non-integer indices for the `Index` annotation
 * Removed calls to `Arrays.copyOf()` inside `getString*()` methods so they may work on Android as well
 * Fixed race condition that could occur in the deallocation code of `Pointer` due to incorrect synchronization
 * `platform.root` now defaults to the current directory

===June 10, 2011===
 * New `Adapter` annotation that uses C++ classes such as `VectorAdapter`, which can let us use Java arrays or `Pointer` objects in place of C++ vector objects by mapping types such as `vector<int>` to `@Adapter("VectorAdapter<int>") int[]` or `@Adapter("VectorAdapter<int>") IntPointer`
 * Added new `Pointer.capacity` field to keep track of allocated size for arrays, needed by the `Adapter` annotation
 * Removed the `capacity` parameter from the `Pointer.asByteBuffer()` and `Pointer.asBuffer()` methods, which now rely instead on the value of the new `capacity` field
 * `ValueGetter` and `ValueSetter`, defaulting to the `get()` and `put()` methods, now accept indices as arguments, and `*Pointer` classes have been updated accordingly
 * New `Index` annotation to indicate that a C++ class, such as `vector<T>` implements the `operator[]` that can be mapped to the `ValueGetter` and `ValueSetter` as well as to arbitrary function calls, taking the first n arguments as indices, where n is the value placed in the annotation
 * The `Name` annotation now accepts as value a `String` array to indicate names before and after these indices
 * New `Const` annotation for convenience
 * Fixed scoping of static members inside namespaces and classes
 * Added new `BoolPointer` class
 * Improved support of function pointers to generate more standard C++ and to make it work with things like member function pointers
 * Inserted hack to call `std::string.c_str()` when returned as `@ByRef java.lang.String`
 * Multiplied checks for invalid `NULL` pointers
 * Upgraded references of the Android NDK to version r5c, which now also works on Android 2.1 or older ([http://code.google.com/p/android/issues/detail?id=16008 android issue #16008])
 * `Loader.load()` no longer requires a JVM that supports annotations to function properly

===April 22, 2011===
 * `Generator` now outputs `#include <stdio.h>`, the lack of which prevents Android NDK under Windows from compiling

===April 7, 2011===
 * Replaced arrays from constructors with variable-length argument lists for convenience
 * Fixed a few small potential pitfalls previously overlooked

===March 1, 2011===
 * Fixed directory search for `jni_md.h`, which did not search deep enough in some cases
 * Added new `path.separator` property to set the path separator of the target platform, regardless of the build platform
 * Added hack to make sure the temporarily extracted library files get properly deleted under Windows
 * Now loads classes more lazily
 * Changed the paths for libstdc++ inside `android-arm.properties` to the non "v7a" versions
 * Added new `platform.root` property to let users specify the path to their toolchains more easily

===February 18, 2011===
Initial release


----
Copyright (C) 2011,2012 Samuel Audet <samuel.audet@gmail.com>
Project site: http://code.google.com/p/javacpp/

Licensed under the GNU General Public License version 2 (GPLv2) with Classpath exception.
Please refer to LICENSE.txt or http://www.gnu.org/licenses/ for details.

