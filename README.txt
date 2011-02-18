=JavaCPP=

==Introduction==
JavaCPP provides efficient access to native C++ inside Java, not unlike the way some C/C++ compilers interact with assembly language. No need to invent [http://www.ecma-international.org/publications/standards/Ecma-372.htm a whole new language], whatever Microsoft may opine about it. Under the hood, it uses JNI, so it works with all Java implementations, including Android. It naturally supports many features of the C++ language often considered problematic, including overloaded operators, templates, function pointers, callback functions, complex struct definitions, variable length arguments, namespaces, large data structures containing arbitrary cycles, multiple inheritance, passing/returning by value/reference, anonymous unions, bit fields, exceptions, destructors and garbage collection, etc. Obviously, neatly supporting the whole of C++ would require more work (although one could argue about the intrinsic neatness of C++), but I am releasing it here as a proof of concept. I have already used it to produce complete interfaces to OpenCV, FFmpeg, libdc1394, PGR FlyCapture, and ARToolKitPlus as part of [http://code.google.com/p/javacv/ JavaCV].


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
 * Android NDK r5b  http://developer.android.com/sdk/ndk/

To modify the source code, note that the project files were created with:
 * NetBeans 6.9  http://www.netbeans.org/downloads/


==Key Use Cases==
To demonstrate its ease of use, imagine we had a C++ function that took a `vector<void*>` as argument. To get the job done with JavaCPP, we could easily define a bare-bones class such as this one (although having an IDE generate that code for us would be even better):

{{{
import com.googlecode.javacpp.*;
import com.googlecode.javacpp.annotation.*;

@Platform(include="<vector>")
public class VectorTest {
    static { Loader.load(); }

    @Namespace("std") @Name("vector<void*>")
    public static class PointerVector extends Pointer {
        public PointerVector()       { allocate();  }
        public PointerVector(long n) { allocate(n); }
        public PointerVector(Pointer p) { super(p); } // i.e.: `(vector<void*>*)p`
        private native void allocate();       // calls `new std::vector<void*>()`
        private native void allocate(long n); // calls `new std::vector<void*>(n)`
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
42  com.googlecode.javacpp.Pointer[address=0xdeadbeef,position=0,deallocator=null]
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


==Acknowledgments==
I am currently an active member of the Okutomi & Tanaka Laboratory, Tokyo Institute of Technology, supported by a scholarship from the Ministry of Education, Culture, Sports, Science and Technology (MEXT) of the Japanese Government.


==Changes==
===February 18, 2011===
Initial release


----
Copyright (C) 2011 Samuel Audet <samuel.audet@gmail.com>
Project site: http://code.google.com/p/javacpp/

Licensed under the GNU General Public License version 2 (GPLv2) with Classpath exception.
Please refer to LICENSE.txt or http://www.gnu.org/licenses/ for details.

