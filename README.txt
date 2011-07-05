=JavaCPP=

==Introduction==
JavaCPP provides efficient access to native C++ inside Java, not unlike the way some C/C++ compilers interact with assembly language. No need to invent [http://www.ecma-international.org/publications/standards/Ecma-372.htm a whole new language], whatever Microsoft may opine about it. Under the hood, it uses JNI, so it works with all Java implementations, [#Instructions_for_Android including Android]. In contrast to other approaches ([http://www.swig.org/ SWIG], [http://www.teamdev.com/jniwrapper/ JNIWrapper], [http://msdn.microsoft.com/en-us/library/fzhhdwae.aspx Platform Invoke], [http://homepage.mac.com/pcbeard/JNIDirect/ JNI Direct], [http://jna.java.net/ JNA], [http://flinflon.brandonu.ca/Dueck/SystemsProgramming/JniMarshall/ JniMarshall], [http://www.jinvoke.com/ J/Invoke], [http://hawtjni.fusesource.org/ HawtJNI], [http://code.google.com/p/bridj/ BridJ], etc.), it supports naturally many features of the C++ language often considered problematic, including overloaded operators, template classes and functions, member function pointers, callback functions, nested struct definitions, variable length arguments, nested namespaces, large data structures containing arbitrary cycles, multiple inheritance, passing/returning by value/reference/vector, anonymous unions, bit fields, exceptions, destructors and garbage collection. Obviously, neatly supporting the whole of C++ would require more work (although one could argue about the intrinsic neatness of C++), but I am releasing it here as a proof of concept. I have already used it to produce complete interfaces to OpenCV, FFmpeg, libdc1394, PGR FlyCapture, OpenKinect, videoInput, and ARToolKitPlus as part of [http://code.google.com/p/javacv/ JavaCV].


==Required Software==
To use JavaCPP, you will need to download and install the following software:
 * An implementation of Java SE 6
  * OpenJDK 6  http://openjdk.java.net/install/  or
  * Sun JDK 6  http://www.oracle.com/technetwork/java/javase/downloads/  or
  * IBM JDK 6  http://www.ibm.com/developerworks/java/jdk/  or
  * Java SE 6 for Mac OS X  http://developer.apple.com/java/  etc.
 * A C++ compiler, out of which these have been tested
  * GNU C/C++ Compiler (Linux, Mac OS X, etc.)  http://gcc.gnu.org/
  * Microsoft C/C++ Compiler  http://msdn.microsoft.com/

To produce binary files for Android, you will also have to install:
 * Android NDK r5c  http://developer.android.com/sdk/ndk/

To modify the source code, please note that the project files were created for:
 * NetBeans 6.9  http://www.netbeans.org/downloads/

Please feel free to ask questions on [http://groups.google.com/group/javacpp-project the mailing list] if you encounter any problems with the software! I am sure it is far from perfect...


==Key Use Cases==
To demonstrate its ease of use, imagine we had a C++ function that took a `vector<void*>` as argument. To get the job done with JavaCPP, we could easily define a bare-bones class such as this one (although having an IDE generate that code for us would be even better):

{{{
import com.googlecode.javacpp.*;
import com.googlecode.javacpp.annotation.*;

@Platform(include="<vector>")
@Namespace("std")
public class VectorTest {
    static { Loader.load(); }

    @Name("vector<void*>")
    public static class PointerVector extends Pointer {
        public PointerVector()       { allocate();  }
        public PointerVector(long n) { allocate(n); }
        public PointerVector(Pointer p) { super(p); } // this = (vector<void*>*)p
        private native void allocate();       // this = new std::vector<void*>()
        private native void allocate(long n); // this = new std::vector<void*>(n)
        @Name("operator=")
        public native @ByRef PointerVector copy(@ByRef PointerVector x);

        public native long size();
        public native @Cast("bool") boolean empty();

        @Name("operator[]")
        public native @ByRef PointerPointer get(long n);
        public native @ByRef PointerPointer at(long n);
    }

    public static void main(String[] args) {
        PointerVector v = new PointerVector(42);
        Pointer p = new Pointer() { { address = 0xDEADBEEFL; } };
        v.get(0).put(p);

        PointerVector v2 = new PointerVector().copy(v);
        Pointer p2 = v2.at(0).get();
        System.out.println(v2.size() + "  " + p2);

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
42  com.googlecode.javacpp.Pointer[address=0xdeadbeef,position=0,capacity=0,deallocator=null]
Exception in thread "main" java.lang.RuntimeException: vector::_M_range_check
	at VectorTest$PointerVector.at(Native Method)
	at VectorTest.main(VectorTest.java:35)
}}}

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

To implement `native` methods, JavaCPP generates appropriate code for JNI, and passes it to the C++ compiler to build a native library. At no point do we need to get our hands dirty with JNI, makefiles, or other native tools. The important thing to realize here is that, while we do all customization inside the Java language using annotations, JavaCPP produces code that has *zero overhead* compared to manually coded JNI functions (verify the generated .cpp files to convince yourself). Moreover, at runtime, the `Loader.load()` method automatically loads the native libraries from Java resources, which were placed in the right directory by the building process. They can even be archived in a JAR file, it changes nothing. Users simply do not need to figure out how to make the system load the files. 

To learn more about how to use the features of this tool, since documentation currently lacks, please refer to [http://code.google.com/p/javacv/source/browse/trunk/javacv/src/com/googlecode/javacv/cpp/ the source code of JavaCV].

As a matter of course, this all works with the Scala language as well, but to make the process even smoother, I would imagine that it should not be too hard to add support for "native properties", such that declarations like `@native var` could generate native getter and setter methods...


==Instructions for Android==
Inside the directory of the Android project:
 # Copy the `javacpp.jar` file into the `libs/` subdirectory, and
 # Run this command to produce the `*.so` library files in `libs/armeabi/`:
{{{
java -jar libs/javacpp.jar -classpath bin/ -classpath bin/classes/ -d libs/armeabi/ \
-properties android-arm -Dplatform.root=<path to android-ndk-r5c> \
-Dcompiler.path=<path to arm-linux-androideabi-g++> <class names>
}}}
And to make everything automatic, we may insert that command into, for example, the Ant `build.xml` file or the Eclipse `.project` file as a [http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.user/gettingStarted/qs-96_non_ant_pjs.htm Non-Ant project builder].


==Acknowledgments==
I am currently an active member of the Okutomi & Tanaka Laboratory, Tokyo Institute of Technology, supported by a scholarship from the Ministry of Education, Culture, Sports, Science and Technology (MEXT) of the Japanese Government.


==Changes==
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
Copyright (C) 2011 Samuel Audet <samuel.audet@gmail.com>
Project site: http://code.google.com/p/javacpp/

Licensed under the GNU General Public License version 2 (GPLv2) with Classpath exception.
Please refer to LICENSE.txt or http://www.gnu.org/licenses/ for details.

