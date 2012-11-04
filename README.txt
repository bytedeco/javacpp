=JavaCPP=

==Introduction==
JavaCPP provides efficient access to native C++ inside Java, not unlike the way some C/C++ compilers interact with assembly language. No need to invent new languages such as [http://www.ecma-international.org/publications/standards/Ecma-372.htm C++/CLI] or [http://www.cython.org/ Cython]. Under the hood, it uses JNI, so it works with all Java implementations, [#Instructions_for_Android including Android]. In contrast to other approaches ([http://www.swig.org/ SWIG], [http://www.itk.org/ITK/resources/CableSwig.html CableSwig], [http://www.eclipse.org/swt/jnigen.php JNIGeneratorApp], [http://www.teamdev.com/jniwrapper/ JNIWrapper], [http://msdn.microsoft.com/en-us/library/0h9e9t7d.aspx Platform Invoke], [http://jogamp.org/gluegen/www/ GlueGen], [http://homepage.mac.com/pcbeard/JNIDirect/ JNIDirect], [https://github.com/twall/jna JNA], [http://www.innowhere.com/ JNIEasy], [http://flinflon.brandonu.ca/Dueck/SystemsProgramming/JniMarshall/ JniMarshall], [http://jnative.free.fr/ JNative], [http://www.jinvoke.com/ J/Invoke], [http://hawtjni.fusesource.org/ HawtJNI], [http://code.google.com/p/bridj/ BridJ], etc.), it supports naturally and efficiently many features of the C++ language often considered problematic, including overloaded operators, template classes and functions, member function pointers, callback functions, functors, nested struct definitions, variable length arguments, nested namespaces, large data structures containing arbitrary cycles, multiple inheritance, passing/returning by value/reference/vector, anonymous unions, bit fields, exceptions, destructors and garbage collection. Obviously, neatly supporting the whole of C++ would require more work (although one could argue about the intrinsic neatness of C++), but I am releasing it here as a proof of concept. I have already used it to produce complete interfaces to OpenCV, FFmpeg, libdc1394, PGR FlyCapture, OpenKinect, videoInput, and ARToolKitPlus as part of [http://code.google.com/p/javacv/ JavaCV].


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
    * [http://www.microsoft.com/en-us/download/details.aspx?id=8442  Microsoft Windows SDK 7.1]
    * [http://msdn.microsoft.com/en-us/library/ff660764.aspx  Building Applications that Use the Windows SDK]

To produce binary files for Android, you will also have to install:
 * Android NDK r8b  http://developer.android.com/tools/sdk/ndk/

To modify the source code, please note that the project files were created for:
 * NetBeans 6.9  http://netbeans.org/downloads/  or
 * Maven 2 or 3  http://maven.apache.org/download.html

Please feel free to ask questions on [http://groups.google.com/group/javacpp-project the mailing list] if you encounter any problems with the software! I am sure it is far from perfect...


==Key Use Cases==
To implement `native` methods, JavaCPP generates appropriate code for JNI, and passes it to the C++ compiler to build a native library. At no point do we need to get our hands dirty with JNI, makefiles, or other native tools. The important thing to realize here is that, while we do all customization inside the Java language using annotations, JavaCPP produces code that has *zero overhead* compared to manually coded JNI functions (verify the generated .cpp files to convince yourself). Moreover, at runtime, the `Loader.load()` method automatically loads the native libraries from Java resources, which were placed in the right directory by the building process. They can even be archived in a JAR file, it changes nothing. Users simply do not need to figure out how to make the system load the files. These characteristics make JavaCPP suitable for either
 * [#Accessing_Native_APIs accessing native APIs],
 * [#Using_Complex_C++_Types using complex C++ types],
 * [#Optimizing_Code_Performance optimizing code performance], or
 * [#Creating_Callback_Functions creating callback functions].

In addition to the few examples provided below, to learn more about how to use the features of this tool, since documentation currently lacks, please refer to [http://code.google.com/p/javacv/source/browse/src/main/java/com/googlecode/javacv/cpp/ the source code of JavaCV].

As a matter of course, this all works with the Scala language as well, but to make the process even smoother, I would imagine that it should not be too hard to add support for "native properties", such that declarations like `@native var` could generate native getter and setter methods...

===Accessing Native APIs===
The most common use case involves accessing some legacy library written for C++, for example, inside a file named `LegacyLibrary.h` containing this C++ class:
{{{
#include <string>

namespace LegacyLibrary {
    class LegacyClass {
        public:
            const std::string& get_property() { return property; }
            void set_property(const std::string& property) { this->property = property; }
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

        // to call the getter and setter functions 
        public native @StdString String get_property(); public native void set_property(String property);

        // to access the member variable directly
        public native @StdString String property();     public native void property(String property);
    }

    public static void main(String[] args) {
        // Pointer objects allocated in Java get deallocated once they become unreachable,
        // but C++ destructors can still be called in a timely fashion with Pointer.deallocate()
        LegacyClass l = new LegacyClass();
        l.set_property("Hello World!");
        System.out.println(l.property());
    }
}
}}}

After compiling the Java source code in the usual way, we also need to build using JavaCPP before executing it as follows:
{{{
$ javac -cp javacpp.jar LegacyLibrary.java 
$ java -jar javacpp.jar LegacyLibrary
$ java  -cp javacpp.jar LegacyLibrary
Hello World!
}}}


===Using Complex C++ Types===
To demonstrate its relative ease of use even in the face of complex data types, imagine we had a C++ function that took a `vector<vector<void*> >` as argument. To support that type, we could define a bare-bones class like this:
{{{
import com.googlecode.javacpp.*;
import com.googlecode.javacpp.annotation.*;

@Platform(include="<vector>")
public class VectorTest {

    @Name("std::vector<std::vector<void*> >")
    public static class PointerVectorVector extends Pointer {
        static { Loader.load(); }
        public PointerVectorVector()       { allocate();  }
        public PointerVectorVector(long n) { allocate(n); }
        public PointerVectorVector(Pointer p) { super(p); } // this = (vector<vector<void*> >*)p
        private native void allocate();                  // this = new vector<vector<void*> >()
        private native void allocate(long n);            // this = new vector<vector<void*> >(n)
        @Name("operator=")
        public native @ByRef PointerVectorVector put(@ByRef PointerVectorVector x);

        @Name("operator[]")
        public native @StdVector PointerPointer get(long n);
        public native @StdVector PointerPointer at(long n);

        public native long size();
        public native @Cast("bool") boolean empty();
        public native void resize(long n);
        public native @Index long size(long i);                   // return (*this)[i].size()
        public native @Index @Cast("bool") boolean empty(long i); // return (*this)[i].empty()
        public native @Index void resize(long i, long n);         // (*this)[i].resize(n)

        public native @Index Pointer get(long i, long j);  // return (*this)[i][j]
        public native void put(long i, long j, Pointer p); // (*this)[i][j] = p
    }

    public static void main(String[] args) {
        PointerVectorVector v = new PointerVectorVector(13);
        v.resize(0, 42); // v[0].resize(42)
        Pointer p = new Pointer() { { address = 0xDEADBEEFL; } };
        v.put(0, 0, p);  // v[0][0] = p

        PointerVectorVector v2 = new PointerVectorVector().put(v);
        Pointer p2 = v2.get(0).get(); // p2 = *(&v[0][0])
        System.out.println(v2.size() + " " + v2.size(0) + "  " + p2);

        v2.at(42);
    }
}
}}}

Executing that program on my machine using these commands produces the following output:
{{{
$ javac -cp javacpp.jar VectorTest.java
$ java -jar javacpp.jar VectorTest
$ java  -cp javacpp.jar VectorTest
13 42  com.googlecode.javacpp.Pointer[address=0xdeadbeef,position=0,limit=0,capacity=0,deallocator=null]
Exception in thread "main" java.lang.RuntimeException: vector::_M_range_check
	at VectorTest$PointerVectorVector.at(Native Method)
	at VectorTest.main(VectorTest.java:44)
}}}


===Optimizing Code Performance===
Other times, we may wish to code in C++ (including CUDA) for performance reasons. Suppose our profiler had identified that a method named `Processor.process()` took 90% of the program's execution time:
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
$ javac -cp javacpp.jar Processor.java
$ java -jar javacpp.jar Processor
$ java  -cp javacpp.jar Processor
Processing in C++...
}}}


==Creating Callback Functions==
Some applications also require a way to call back into the JVM from C/C++, so JavaCPP provides a simple way to define custom callback function (pointers). Although there exist frameworks, which are arguably harder to use, such as [http://code.google.com/p/jace/ Jace] and [JunC++ion http://codemesh.com/products/junction/] that can map complete Java APIs to C++, since invoking a Java method from native code takes at least an order of magnitude more time than the other way around, it does not make much sense in my opinion to export as is an API that was designed to be used in Java. Nevertheless, suppose we want to perform some operations in Java, planning to wrap that into a function named `foo()` that calls some method inside class `Foo`, we can write the following code in a file named `foo.cpp`, taking care to initialize the JVM if necessary with either `JavaCPP_init()` or by any other means:

{{{
#include <iostream>
#include "jniFoo.h"

int main() {
    JavaCPP_init(0, NULL);
    try {
        foo(6, 7);
    } catch (std::exception &e) {
        std::cout << e.what() << std::endl;
    }
    JavaCPP_uninit();
}
}}}

We may then declare that function to a `call()` or `apply()` method defined in a `FunctionPointer` as follows:

{{{
import com.googlecode.javacpp.*;
import com.googlecode.javacpp.annotation.*;

public class Foo {
    public static class Bar extends FunctionPointer {
        public @Name("foo") void call(int a, int b) throws Exception { 
            throw new Exception("bar " + a * b);
        }
    }
}
}}}

Since functions also have pointers, we can use `FunctionPointer` instances accordingly, in ways similar to the [http://hackage.haskell.org/packages/archive/base/4.6.0.0/doc/html/Foreign-Ptr.html#g:2 `FunPtr` type of Haskell FFI], but where any `java.lang.Throwable` object thrown gets translated to `std::exception`. Building and running this sample code with these commands under Linux x86_64 produces the following output:

{{{
$ javac -cp javacpp.jar Foo.java
$ java -jar javacpp.jar Foo -header
$ g++ -I/usr/lib/jvm/java/include/ -I/usr/lib/jvm/java/include/linux/ linux-x86_64/libjniFoo.so foo.cpp -o foo
$ ./foo
java.lang.Exception: bar 42
}}}

In this example, the `FunctionPointer` object gets created implicitly, but to call a native function pointer, we could define one that instead contains a `native call()/apply()` method, and create an instance explicitly. Such a class can also be extended in Java to create callbacks, and like any other normal `Pointer` object, must be allocated with a `native void allocate()` method, so please remember to hang on to references in Java, as those will get garbage collected. As a bonus, `FunctionPointer.call()/apply()` maps in fact to an overloaded `operator()` of a C++ function object that we can pass to other functions by annotating parameters with `@ByVal` or `@ByRef`.


==Instructions for Android==
Inside the directory of the Android project:
 # Copy the `javacpp.jar` file into the `libs/` subdirectory, and
 # Run this command to produce the `*.so` library files in `libs/armeabi/`:
{{{
java -jar libs/javacpp.jar -classpath bin/ -classpath bin/classes/ \
-properties android-arm -Dplatform.root=<path/to/android-ndk-r8b> \
-Dcompiler.path=<path/to/arm-linux-androideabi-g++> -d libs/armeabi/
}}}
To make everything automatic, we may also insert that command into, for example, the Ant `build.xml` file or the Eclipse `.project` file as a [http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.user/gettingStarted/qs-96_non_ant_pjs.htm Non-Ant project builder].


==Acknowledgments==
This project was conceived at the Okutomi & Tanaka Laboratory, Tokyo Institute of Technology, where I was supported for my doctoral research program by a generous scholarship from the Ministry of Education, Culture, Sports, Science and Technology (MEXT) of the Japanese Government. I extend my gratitude further to all who have reported bugs, donated code, or made suggestions for improvements!


==Changes==
===November 4, 2012 version 0.3===
 * Added `Pointer.withDeallocator(Pointer)` method to attach easily a custom `Deallocator` created out of a `static void deallocate(Pointer)` method in the subclass, including native ones such as `@Namespace @Name("delete") static void deallocate(Pointer)`
 * A name starting with "::", for example `@Name("::std::string")` or `@Namespace("::std")`, now drops the remaining enclosing scope
 * Removed confusing `cast` value of `@Adapter` instead relying on new `String[]` value of `@Cast` to order multiple casts
 * Renamed various variables in `Generator` to make the generated code more readable
 * Fixed memory corruption when using an adapter or `@ByRef` on a function that returns by value an `std::vector<>` or `std::string` (issue #26)
 * Added `Pointer.zero()` method that calls `memset(0)` on the range
 * For easier memory management, more than one `Pointer` now allowed to share the `deallocator` when "casting" them
 * Upgraded references of the Android NDK to version r8b
 * Fixed `JavaCPP_log()` not printing correctly (issue #27)
 * Added functionality to access easily `FunctionPointer` callbacks by their names from C/C++: We can annotate them with `@Name` and build with the new `-header` option to get their declarations in a header file, while the `Builder` links with the `jvm` library by default
 * `Loader` now displays an informative error message when trying to use an undefined `compiler.options` with `@Platform(options="")` (issue #24)
 * `Pointer.deallocator()` would needlessly enqueue `Deallocator` objects pointing to the native `NULL` address
 * Added support for C++ "functors" based on the `operator()`, which gets used when annotating a `FunctionPointer` method parameter with `@ByRef` or `@ByVal`
 * For convenience in Scala, added `apply()` as an acceptable caller method name within a `FunctionPointer`, in addition to `call()`
 * Fixed `@Cast` not working along parameters with an `@Adapter` or when attempting to `return` the argument
 * `Generator` would ignore `Pointer.position()` in the case of `@ByPtrPtr` and `@ByPtrRef` parameters
 * Replaced hack to create a `Pointer` from a `Buffer` object with something more standard
 * Fixed `Loader.sizeof(Pointer.class)` to return the `sizeof(void*)`
 * In addition to methods and parameters, we may now apply `@Adapter` to annotation types as well, allowing us to shorten expressions like `@Adapter("VectorAdapter<int>") int[]` to just `@StdVector int[]`, for `std::vector<int>` support, and similarly for `@StdString` and `std::string`
 * Fixed callback parameter casting of primitive and `String` types
 * An empty `@Namespace` can now be used to let `Generator` know of entities that are not part of any scope, such as macros and operators
 * Turned `FunctionPointer` into an `abstract class` with `protected` constructors, but if users still try to use it as function parameters, `Generator` now logs a warning indicating that a subclass should be used (issue #23)
 * Removed the `out` value of the `@Adapter` annotation: All adapters are now "out" by default, unless `@Const` also appears on the same element
 * Fixed `Pointer.equals(null)` throwing `NullPointerException` (issue #22)
 * `@NoOffset` would erroneously prevent `sizeof()` operations from getting generated

===July 21, 2012 version 0.2===
 * Fixed problems when trying to map `java.lang.String` to other native types than `char*`, such as `unsigned char*`
 * JavaCPP now uses the `new (std::nothrow)` operator for allocation, which guarantees that allocation of primitive native arrays won't throw exceptions, making it possible to build C++ exception free JNI libraries
 * Added new `Pointer.limit` property, mainly useful to get the `size` of an output parameter, as returned by an adapter specified with the `@Adapter` annotation
 * Renamed the `capacity` field of an adapter to `size` as it now maps to both `Pointer.limit` and `Pointer.capacity` (the latter only for new allocations)
 * Added `Pointer.put(Pointer)` method, the counterpart of `Buffer.put(Buffer)`, to call the native `memcpy()` function on two `Pointer` objects
 * New `@NoException` annotation to reduce the size of generated code and optimize runtime performance of functions that are guaranteed not to throw exceptions, or for cases when we do not mind that the JVM may crash and burn
 * Trying to generate code for non-static native methods inside a class not extending `Pointer` now generates proper warning (issue #19)
 * Fixed regression where the `@Adapter` notation generates incorrect code for types other than `Pointer` (issue #20)

===May 27, 2012 version 0.1===
 * Started using version numbers, friendly to tools like Maven, and placing packages in a sort of [http://maven2.javacpp.googlecode.com/git/ Maven repository] (issue #10)
 * Before loading a JNI library, the `Loader` now also tries to extract and load libraries listed in the `@Platform(link={...}, preload={...})` annotation values, and to support library names with version numbers, each value has to follow the format "libname@version" (or "libname@@version" to have `Builder` use it for the compiler as well), where "version" is the version number found in the filename as required by the native dynamic linker, usually a short sequence of digits and dots, but it can be anything (e.g.: "mylib@.4.2" would map to "libmylib.so.4.2", "libmylib.4.2.dylib", and "mylib.4.2.dll" under Linux, Mac OS X, and Windows respectively)
 * All files now get extracted into a temporary subdirectory, and with the appropriate platform-dependent linker options, or with libraries patched up after the fact with tools such as `install_name_tool` of Mac OS X, most native dynamic linkers can load dependent libraries from there
 * Stopped using `java.net.URL` as hash key in `Loader` (very bad idea)
 * Changed the default value of the `@Index` annotation from 0 to 1, and fixed the `Generator` when it is used with member getters and setters
 * Renamed `mingw-*.properties` to `windows-*-mingw.properties` for consistency
 * Made the `Generator` allocate native heap memory for callback arguments passed `@ByVal` (in addition to `FunctionPointer`), rendering their behavior consistent with return `@ByVal` in the case of function calls (issue #16)
 * `Generator` now uses `std::runtime_error(std::string&)` instead of assuming that some nonstandard `std::exception(std::string&)` constructor exists (issue #17)
 * Fixed `Generator` producing incorrect code when applying invalid annotations such as `@ByVal` on a method that returns something else than a `Pointer` object (issue #18)

===May 12, 2012===
 * Added `pom.xml` file and `BuildMojo` plugin class for Maven support and changed the directory structure of the source code to match Maven's standard directory layout (issue #10) Many thanks to Adam Waldenberg and Arnaud Nauwynck for their ongoing support with that!
 * Moved the source code repository to Git
 * Created a new `@Raw` annotation to use Java object as raw `jobject` in C++, also passing `JNIEnv` and the enclosing `jclass` or the `jobject` corresponding to `this`, as the first two arguments of the function, when the `Generator` encounters any `@Raw(withEnv=true)` (issue #13)
 * The `Builder` now handles more cases when some prefix or suffix property starts or ends with a space (issue #14)
 * Fixed syntax error in `VectorAdapter`, which GCC and Visual C++ would still happily compile
 * Added new `source.suffix` property to have the names of generated source files end with something else than `.cpp` and support frameworks like CUDA that require filenames with a `.cu` extension to compile properly, such as used by the new `*-cuda.properties`, and also changed the "-cpp" command line option to "-nocompile"
 * New `Loader.loadLibrary()` method similar to `System.loadLibrary()`, but before searching the library path, it tries to extract and load the library from Java resources
 * `Generator` now accepts `@Const` on `FunctionPointer` class declarations
 * Added new `@Adapter.cast()` value to cast explicitly the output of a C++ adapter object
 * Upgraded references of the Android NDK to version r8
 * Included new command line option "-Xcompiler" to pass options such as "-Wl,-static" directly to the compiler
 * Made other various minor changes and enhancements

===March 29, 2012===
 * Added new `compiler.framework` property and corresponding `@Platform.framework()` value to allow easier binding with Mac OS X frameworks
 * Changed most `Builder` errors into warnings, letting the building process complete successfully more often
 * We may now use the `@NoDeallocator` annotation on a class to disable deallocation for all allocation methods

===March 03, 2012===
 * Added new `@NoDeallocator` annotation to prevent `allocate()` and `allocateArray()` methods from registering a native deallocator to `Pointer` objects (issue #1)
 * `Generator` now properly skips as unsupported array parameters that do not have a primitive component type, and logs a warning (issue #7)
 * `Generator` and `Builder` would append the same include files, libraries, or options multiple times when not required: Fixed in `Loader.appendProperty()` (issue #8)
 * Moved the placement of the class-level @Index annotation to the getter and setter methods themselves
 * To process all classes in a package, we may now specify as argument to the `Builder` its name followed by ".*", in a similar fashion to the `import` statement of the Java language, or by ".**" to process recursively all packages, while omitting to specify any class or package results in JavaCPP processing all classes found under the directories or JAR files specified with the "-classpath" option (issue #12)
 * Equipped the `*Pointer` classes with new bulk `get()` and `put()` methods taking an array as argument, to compensate for direct NIO buffers lacking in performance on Android (issue #11)

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

