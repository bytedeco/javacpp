JavaCPP
=======

Introduction
------------
JavaCPP provides efficient access to native C++ inside Java, not unlike the way some C/C++ compilers interact with assembly language. No need to invent new languages such as with [SWIG](http://www.swig.org/), [SIP](http://riverbankcomputing.co.uk/software/sip/), [C++/CLI](http://www.ecma-international.org/publications/standards/Ecma-372.htm), [Cython](http://www.cython.org/), or [RPython](http://doc.pypy.org/en/latest/coding-guide.html#id1) as required by [cppyy](http://doc.pypy.org/en/latest/cppyy.html). Instead, it exploits the syntactic and semantic similarities between Java and C++. Under the hood, it uses JNI, so it works with all implementations of Java SE, in addition to [Android](http://www.android.com/), [Avian](http://oss.readytalk.com/avian/), and [RoboVM](http://www.robovm.org/) ([instructions](#instructions-for-android-avian-and-robovm)).

More specifically, when compared to the approaches above or elsewhere ([CableSwig](http://www.itk.org/ITK/resources/CableSwig.html), [JNIGeneratorApp](http://www.eclipse.org/swt/jnigen.php), [cxxwrap](http://cxxwrap.sourceforge.net/), [JNIWrapper](http://www.teamdev.com/jniwrapper/), [Platform Invoke](http://msdn.microsoft.com/en-us/library/0h9e9t7d.aspx), [GlueGen](http://jogamp.org/gluegen/www/), [JNIDirect](http://web.archive.org/web/20050329122501/http://homepage.mac.com/pcbeard/JNIDirect/), [ctypes](http://docs.python.org/library/ctypes.html), [JNA](https://github.com/twall/jna), [JNIEasy](http://www.innowhere.com/jnieasy/), [JniMarshall](http://flinflon.brandonu.ca/Dueck/SystemsProgramming/JniMarshall/), [JNative](http://jnative.free.fr/), [J/Invoke](http://web.archive.org/web/20110727133817/http://www.jinvoke.com/), [HawtJNI](http://hawtjni.fusesource.org/), [JNR](https://github.com/jnr/), [BridJ](http://code.google.com/p/bridj/), [fficxx](http://ianwookim.org/fficxx/), etc.), it maps naturally and efficiently many common features afforded by the C++ language and often considered problematic, including overloaded operators, class and function templates, callbacks through function pointers, function objects (aka functors), virtual functions and member function pointers, nested struct definitions, variable length arguments, nested namespaces, large data structures containing arbitrary cycles, virtual and multiple inheritance, passing/returning by value/reference/vector, anonymous unions, bit fields, exceptions, destructors with garbage collection, and documentation comments. Obviously, neatly supporting the whole of C++ would require more work (although one could argue about the intrinsic neatness of C++), but we are releasing it here as a proof of concept. 

As a case in point, we have already used it to produce complete interfaces to OpenCV, FFmpeg, libdc1394, PGR FlyCapture, OpenKinect, videoInput, ARToolKitPlus, and others as part of the [JavaCPP Presets](https://github.com/bytedeco/javacpp-presets) subproject, also demonstrating early parsing capabilities of C/C++ header files that show promising and useful results.

Please feel free to ask questions on [the mailing list](http://groups.google.com/group/javacpp-project) if you encounter any problems with the software! I am sure it is far from perfect...


Downloads
---------
 * JavaCPP 0.10 binary archive  [javacpp-0.10-bin.zip](http://search.maven.org/remotecontent?filepath=org/bytedeco/javacpp/0.10/javacpp-0.10-bin.zip) (227 KB)
 * JavaCPP 0.10 source archive  [javacpp-0.10-src.zip](http://search.maven.org/remotecontent?filepath=org/bytedeco/javacpp/0.10/javacpp-0.10-src.zip) (208 KB)

We can also have everything downloaded and installed automatically with:

 * Maven (inside the `pom.xml` file)
```xml
  <dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacpp</artifactId>
    <version>0.10</version>
  </dependency>
```

 * Gradle (inside the `build.gradle` file)
```groovy
  dependencies {
    compile group: 'org.bytedeco', name: 'javacpp', version: '0.10'
  }
```

 * SBT (inside the `build.sbt` file)
```scala
  classpathTypes += "maven-plugin"
  libraryDependencies += "org.bytedeco" % "javacpp" % "0.10"
```


Required Software
-----------------
To use JavaCPP, you will need to download and install the following software:

 * An implementation of Java SE 6 or newer
   * OpenJDK  http://openjdk.java.net/install/  or
   * Sun JDK  http://www.oracle.com/technetwork/java/javase/downloads/  or
   * IBM JDK  http://www.ibm.com/developerworks/java/jdk/  or
   * Java SE for Mac OS X  http://developer.apple.com/java/  etc.
 * A C++ compiler, out of which these have been tested
   * GNU C/C++ Compiler (Linux, Mac OS X, etc.)  http://gcc.gnu.org/
     * For Windows x86 and x64  http://mingw-w64.sourceforge.net/
   * Microsoft C/C++ Compiler  http://msdn.microsoft.com/
     * [Microsoft Windows SDK 7.1](http://www.microsoft.com/en-us/download/details.aspx?id=8442)
     * [Building Applications that Use the Windows SDK](http://msdn.microsoft.com/en-us/library/ff660764.aspx)

To produce binary files for Android, you will also have to install:

 * Android NDK r7 or newer  http://developer.android.com/tools/sdk/ndk/

And similarly to target iOS, you will need to install:

 * RoboVM 0.0.x or 1.0.x  http://download.robovm.org/

To modify the source code, please note that the project files were created for:

 * Maven 2 or 3  http://maven.apache.org/download.html

Finally, because we are dealing with native code, bugs can easily crash the virtual machine. Luckily, the HotSpot VM provides some tools to help us debug under those circumstances:

 * Troubleshooting Guide for Java SE with HotSpot VM
   * http://www.oracle.com/technetwork/java/javase/index-137495.html
   * http://www.oracle.com/technetwork/java/javase/tsg-vm-149989.pdf
   * http://docs.oracle.com/javase/7/docs/webnotes/tsg/TSG-VM/html/
   * http://docs.oracle.com/javase/8/docs/technotes/guides/tsgvm/


Key Use Cases
-------------
To implement `native` methods, JavaCPP generates appropriate code for JNI, and passes it to the C++ compiler to build a native library. At no point do we need to get our hands dirty with JNI, makefiles, or other native tools. The important thing to realize here is that, while we do all customization inside the Java language using annotations, JavaCPP produces code that has *zero overhead* compared to manually coded JNI functions (verify the generated .cpp files to convince yourself). Moreover, at runtime, the `Loader.load()` method automatically loads the native libraries from Java resources, which were placed in the right directory by the building process. They can even be archived in a JAR file, it changes nothing. Users simply do not need to figure out how to make the system load the files. These characteristics make JavaCPP suitable for either

 * [accessing native APIs](#accessing-native-apis),
 * [using complex C++ types](#using-complex-c-types),
 * [optimizing code performance](#optimizing-code-performance), or
 * [creating callback functions](#creating-callback-functions).

In addition to the few examples provided below, to learn more about how to use the features of this tool, please refer to [the source code of the JavaCPP Presets](https://github.com/bytedeco/javacpp-presets). For more information about the API itself, one may refer to [the documentation generated by Javadoc](http://bytedeco.org/javacpp/apidocs/).

As a matter of course, this all works with the Scala language as well, but to make the process even smoother, it should not be too hard to add support for "native properties", such that declarations like `@native var` could generate native getter and setter methods...


### Accessing Native APIs
The most common use case involves accessing some legacy library written for C++, for example, inside a file named `LegacyLibrary.h` containing this C++ class:
```cpp
#include <string>

namespace LegacyLibrary {
    class LegacyClass {
        public:
            const std::string& get_property() { return property; }
            void set_property(const std::string& property) { this->property = property; }
            std::string property;
    };
}
```

To get the job done with JavaCPP, we can easily define a Java class such as this one--although one could use the `Parser` to produce it from the header file as demonstrated by the [JavaCPP Presets](https://github.com/bytedeco/javacpp-presets) subproject:
```java
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

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
```

After compiling the Java source code in the usual way, we also need to build using JavaCPP before executing it as follows:
```bash
$ javac -cp javacpp.jar LegacyLibrary.java 
$ java -jar javacpp.jar LegacyLibrary
$ java  -cp javacpp.jar LegacyLibrary
Hello World!
```


### Using Complex C++ Types
To demonstrate its relative ease of use even in the face of complex data types, imagine we had a C++ function that took a `vector<vector<void*> >` as argument. To support that type, we could define a bare-bones class like this:
```java
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

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
```

Executing that program using these commands produces the following output:
```bash
$ javac -cp javacpp.jar VectorTest.java
$ java -jar javacpp.jar VectorTest
$ java  -cp javacpp.jar VectorTest
13 42  org.bytedeco.javacpp.Pointer[address=0xdeadbeef,position=0,limit=0,capacity=0,deallocator=null]
Exception in thread "main" java.lang.RuntimeException: vector::_M_range_check
	at VectorTest$PointerVectorVector.at(Native Method)
	at VectorTest.main(VectorTest.java:44)
```


### Optimizing Code Performance
Other times, we may wish to code in C++ (including CUDA) for performance reasons. Suppose our profiler had identified that a method named `Processor.process()` took 90% of the program's execution time:
```java
public class Processor {
    public static void process(java.nio.Buffer buffer, int size) {
        System.out.println("Processing in Java...");
        // ...
    }

    public static void main(String[] args) {
        process(null, 0);
    }
}
```

After many days of hard work and sweat, the engineers figured out some hacks and managed to make that ratio drop to 80%, but you know, the managers were still not satisfied. So, we could try to rewrite it in C++ (or even assembly language for that matter via the inline assembler) and place the resulting function in a file named say `Processor.h`:
```cpp
#include <iostream>

static inline void process(void *buffer, int size) {
    std::cout << "Processing in C++..." << std::endl;
    // ...
}
```

After adjusting the Java source code to something like this:
```java
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

@Platform(include="Processor.h")
public class Processor {
    static { Loader.load(); }

    public static native void process(java.nio.Buffer buffer, int size);

    public static void main(String[] args) {
        process(null, 0);
    }
}
```

It would then compile and execute like this:
```bash
$ javac -cp javacpp.jar Processor.java
$ java -jar javacpp.jar Processor
$ java  -cp javacpp.jar Processor
Processing in C++...
```


### Creating Callback Functions
Some applications also require a way to call back into the JVM from C/C++, so JavaCPP provides a simple way to define custom callbacks, either as function pointers, function objects, or virtual functions. Although there exist frameworks, which are arguably harder to use, such as [Jace](http://code.google.com/p/jace/) and [JunC++ion](http://codemesh.com/products/junction/) that can map complete Java APIs to C++, since invoking a Java method from native code takes at least an order of magnitude more time than the other way around, it does not make much sense in my opinion to export as is an API that was designed to be used in Java. Nevertheless, suppose we want to perform some operations in Java, planning to wrap that into a function named `foo()` that calls some method inside class `Foo`, we can write the following code in a file named `foo.cpp`, taking care to initialize the JVM if necessary with either `JavaCPP_init()` or by any other means:

```cpp
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
```

We may then declare that function to a `call()` or `apply()` method defined in a `FunctionPointer` as follows:

```java
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

@Platform(include="<algorithm>")
@Namespace("std")
public class Foo {
    static { Loader.load(); }

    public static class Callback extends FunctionPointer {
        // Loader.load() and allocate() are required only when explicitly creating an instance
        static { Loader.load(); }
        protected Callback() { allocate(); }
        private native void allocate();

        public @Name("foo") boolean call(int a, int b) throws Exception { 
            throw new Exception("bar " + a * b);
        }
    }

    // We can also pass (or get) a FunctionPointer as argument to (or return value from) other functions
    public static native void stable_sort(IntPointer first, IntPointer last, Callback compare);

    // And to pass (or get) it as a C++ function object, annotate with @ByVal or @ByRef
    public static native void sort(IntPointer first, IntPointer last, @ByVal Callback compare);
}
```

Since functions also have pointers, we can use `FunctionPointer` instances accordingly, in ways similar to the [`FunPtr` type of Haskell FFI](http://hackage.haskell.org/packages/archive/base/4.6.0.0/doc/html/Foreign-Ptr.html#g:2), but where any `java.lang.Throwable` object thrown gets translated to `std::exception`. Building and running this sample code with these commands under Linux x86_64 produces the expected output:

```bash
$ javac -cp javacpp.jar Foo.java
$ java -jar javacpp.jar Foo -header
$ g++ -I/usr/lib/jvm/java/include/ -I/usr/lib/jvm/java/include/linux/ linux-x86_64/libjniFoo.so foo.cpp -o foo
$ ./foo
java.lang.Exception: bar 42
```

In this example, the `FunctionPointer` object gets created implicitly, but to call a native function pointer, we could define one that instead contains a `native call()/apply()` method, and create an instance explicitly. Such a class can also be extended in Java to create callbacks, and like any other normal `Pointer` object, must be allocated with a `native void allocate()` method, **so please remember to hang on to references in Java**, as those will get garbage collected. As a bonus, `FunctionPointer.call()/apply()` maps in fact to an overloaded `operator()` of a C++ function object that we can pass to other functions by annotating parameters with `@ByVal` or `@ByRef`, as with the `sort()` function in the example above.

It is also possible to do the same thing with virtual functions, whether "pure" or not. Consider the following C++ class defined in a file named `Foo.h`:
```cpp
#include <stdio.h>

class Foo {
public:
    int n;
    Foo(int n) : n(n) { }
    virtual ~Foo() { }
    virtual void bar() {
        printf("Callback in C++ (n == %d)\n", n);
    }
};

void callback(Foo *foo) {
    foo->bar();
}

```

The function `Foo::bar()` can be overridden in Java if we declare the method in the peer class either as `native` or `abstract` and annotate it with `@Virtual`, for example:
```java
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

@Platform(include="Foo.h")
public class VirtualFoo {
    static { Loader.load(); }

    public static class Foo extends Pointer {
        static { Loader.load(); }
        public Foo(int n) { allocate(n); }
        private native void allocate(int n);

        @NoOffset public native int n(); public native Foo n(int n);
        @Virtual  public native void bar();
    }

    public static native void callback(Foo foo);

    public static void main(String[] args) {
        Foo foo = new Foo(13);
        Foo foo2 = new Foo(42) {
            public void bar() {
                System.out.println("Callback in Java (n == " + n() + ")");
            }
        };
        foo.bar();
        foo2.bar();
        callback(foo);
        callback(foo2);
    }
}
```

Which outputs what one would naturally assume:
```bash
$ javac -cp javacpp.jar VirtualFoo.java
$ java -jar javacpp.jar VirtualFoo
$ java  -cp javacpp.jar VirtualFoo
Callback in C++ (n == 13)
Callback in Java (n == 42)
Callback in C++ (n == 13)
Callback in Java (n == 42)
```


Instructions for Android, Avian, and RoboVM
-------------------------------------------
The easiest one to get working is Avian compiled with OpenJDK class libraries, so let's start with that. After creating and building a program as described above, without any further modifications, we can directly execute it with this command:
```bash
$ /path/to/avian-dynamic -Xbootclasspath/a:javacpp.jar <MainClass>
```

However, in the case of Android, we need to do a bit more work. For the command line build system based on Ant, inside the directory of the project:

 1. Copy the `javacpp.jar` file into the `libs/` subdirectory, and
 2. Run this command to produce the `*.so` library files in `libs/armeabi/`:
```bash
$ java -jar libs/javacpp.jar -classpath bin/ -classpath bin/classes/ \
> -properties android-arm -Dplatform.root=/path/to/android-ndk/ \
> -Dplatform.compiler=/path/to/arm-linux-androideabi-g++ -d libs/armeabi/
```
To make everything automatic, we may also insert that command into the `build.xml` file. Alternatively, for integration with Android Studio, we can use the [Maven plugin](http://bytedeco.org/javacpp/apidocs/org/bytedeco/javacpp/tools/BuildMojo.html) with the [build system based on Gradle](http://tools.android.com/tech-docs/new-build-system/user-guide).


Similarly for RoboVM, assuming that the compiled classes are in the `classes` subdirectory:

 1. Copy the `javacpp.jar` file into the project directory, and
 2. Run the following commands to produce the native binary file:
```bash
$ java -jar javacpp.jar -cp classes/ -properties ios-arm -Dplatform.sysroot=SDKs/iPhoneOS7.0.sdk/ -o lib
$ /path/to/robovm -arch thumbv7 -os ios -cp javacpp.jar:classes/ -libs ios-arm/lib.o
```
And instead of `Loader.load()`, the library should be loaded with `System.load("lib.o")`, in this case.


----
Project lead: Samuel Audet [samuel.audet `at` gmail.com](mailto:samuel.audet at gmail.com)  
Developer site: https://github.com/bytedeco/javacpp  
Discussion group: http://groups.google.com/group/javacpp-project

Licensed under the GNU General Public License version 2 (GPLv2) **with Classpath exception**.  
Please refer to LICENSE.txt or http://www.gnu.org/software/classpath/license.html for details.
