
 * Fix `Parser` not considering empty `class`, `struct`, or `union` declarations as opaque forward declarations
 * Provide `ByteIndexer` and `BytePointer` with value getters and setters for primitive types other than `byte` to facilitate unaligned memory accesses
 * Add a `BuildMojo.buildCommand` parameter that lets users execute arbitrary system commands easily with `ProcessBuilder`

### March 11, 2017 version 1.3.2
 * Add new "org.bytedeco.javacpp.cachedir.nosubdir" system property to restore old behavior ([issue #167](https://github.com/bytedeco/javacpp/issues/167))
 * Prevent `Pointer` from copying array data from NIO buffers that are also direct ([issue bytedeco/javacpp-presets#380](https://github.com/bytedeco/javacpp-presets/issues/380))
 * Fix `SharedPtrAdapter` and `UniquePtrAdapter` of the `Generator` for `const` types ([issue #166](https://github.com/bytedeco/javacpp/issues/166))
 * Prevent `Loader` from loading system libraries, which causes problems on Android 7.x ([issue bytedeco/javacv#617](https://github.com/bytedeco/javacv/issues/617))
 * Make `Parser` strip return type annotations when naming `FunctionPointer` ([issue #162](https://github.com/bytedeco/javacpp/issues/162))
 * Let `Pointer` log debug messages when forced to call `System.gc()`
 * Fix `Parser` handling of `std::map` and of documentation comments containing the "*/" sequence
 * Add portable and efficient `totalPhysicalBytes()`, `availablePhysicalBytes()`, `totalProcessors()`, `totalCores()`, `totalChips()` methods
 * Avoid `Loader` issues with spaces, etc in paths to library files ([issue deeplearning4j/nd4j#1564](https://github.com/deeplearning4j/nd4j/issues/1564))
 * Prevent `Generator` from creating duplicate `using` statements ([pull #158](https://github.com/bytedeco/javacpp/pull/158))
 * Make `Pointer.asBuffer()` thread-safe ([issue #155](https://github.com/bytedeco/javacpp/issues/155))

### December 24, 2016 version 1.3.1
 * Fix broken `outputDirectory` property and corresponding `-d` command line option ([issue #153](https://github.com/bytedeco/javacpp/issues/153))
 * Add `Loader.extractResources()` and `cacheResources()` methods to extract or cache all resources with given name
 * Fix potential issues with `Parser` repeating the `@ByPtrPtr` or `@ByPtrRef` annotations on parameters
 * To support Scala singleton objects better, consider as `static` methods from objects that are not `Pointer`
 * Allow `Loader.extractResource()` and `cacheResource()` to extract or cache all files from a directory in a JAR file
 * Create version-less symbolic links to libraries in cache on those platforms where it is useful to link easily
 * Use `java.io.tmpdir` as fallback in `Loader.getCacheDir()`, and throw a clear exception on failure

### December 7, 2016 version 1.3
 * Print memory sizes in a human-readable format with `Pointer.formatBytes()`
 * Map standard `malloc()`, `calloc()`, `realloc()`, and `free()` functions ([issue #136](https://github.com/bytedeco/javacpp/issues/136))

### November 29, 2016 version 1.2.7
 * Fix `Loader` errors that could occur due to recent changes

### November 28, 2016 version 1.2.6
 * Improve `Loader` handling of duplicate libraries found in different JAR files using symbolic links (useful for MKL, etc)
 * Prevent `Loader` from overwriting previously extracted and renamed libraries ([issue deeplearning4j/nd4j#1460](https://github.com/deeplearning4j/nd4j/issues/1460))
 * Allow users to define `NO_JNI_DETACH_THREAD` to prevent callbacks from reinitializing threads ([issue #143](https://github.com/bytedeco/javacpp/issues/143))

### November 13, 2016 version 1.2.5
 * Add support for `decltype()` declarations to the `Parser` ([issue #135](https://github.com/bytedeco/javacpp/issues/135))
 * Fix `Generator` when a `FunctionPointer` contains methods that start with "get" or "put" ([issue #137](https://github.com/bytedeco/javacpp/issues/137))
 * Enhance `Parser` to let users skip the default values of arguments, as well as classes when one base class is skipped
 * Fix `Parser` not properly mapping the type of `long` anonymous enums
 * Take into account `const` on function parameters when looking up in `InfoMap`, and fix some incorrectly translated macros into variables
 * Add to `InfoMap.defaults` more names that are reserved in Java, but not in C++
 * Add via `@ByPtrRef` support for function pointers passed by reference, as well as support for `Info.javaText` with `typedef`
 * Make sure `Parser` exhausts all combinations of method parameter types even with duplicates ([issue bytedeco/javacv#518](https://github.com/bytedeco/javacv/issues/518))
 * Make `Loader` cache libraries (in `~/.javacpp/cache/` by default) instead of using temporary files ([pull #120](https://github.com/bytedeco/javacpp/pull/120))
 * Have `Parser` annotate the `allocate()` functions and not the actual constructors ([issue bytedeco/javacpp-presets#297](https://github.com/bytedeco/javacpp-presets/issues/297))
 * Fix `Parser` handling of `class`, `struct`, or `union` types with variables declared in the same statement
 * Add missing `platform.link` to `psapi` required by some versions of Visual Studio ([issue bytedeco/javacpp-presets#298](https://github.com/bytedeco/javacpp-presets/issues/298))
 * Make sure default values placed in `nullValue` by the `Parser` have the right type ([issue bytedeco/javacv#518](https://github.com/bytedeco/javacv/issues/518))
 * Accelerate call to `Pointer.physicalBytes()` on Linux ([issue #133](https://github.com/bytedeco/javacpp/issues/133))
 * Fix `Parser` incorrectly skipping over some template function declarations
 * Allow C++ types to be prefixed by `class`, `struct`, or `union` to work around name clashes ([pull bytedeco/javacpp-presets#266](https://github.com/bytedeco/javacpp-presets/pull/266))
 * Properly expand the special predefined `__COUNTER__` macro ([pull bytedeco/javacpp-presets#266](https://github.com/bytedeco/javacpp-presets/pull/266))
 * Create all missing directories in the paths to the source files created by `Generator`

### September 16, 2016 version 1.2.4
 * Insure `Parser` properly ignores the `auto`, `mutable`, `register`, `thread_local`, and `volatile` C++ keywords for storage classes
 * Fix `Generator` and `Parser` for types like `std::unordered_map<std::string,std::pair<int,int> >` ([pull bytedeco/javacpp-presets#266](https://github.com/bytedeco/javacpp-presets/pull/266))
 * Add `std::forward_list`, `std::priority_queue`, `std::unordered_map`, and `std::unordered_set` to the list of "basic/containers" in `InfoMap`
 * Work around `linux-armhf` not being properly detected with OpenJDK ([issue #105](https://github.com/bytedeco/javacpp/issues/105))
 * Fix `Parser` not accepting namespace aliases with `::` tokens in them ([issue bytedeco/javacpp-presets#265](https://github.com/bytedeco/javacpp-presets/issues/265))
 * Add "org.bytedeco.javacpp.maxphysicalbytes" system property to force calls to `System.gc()` based on `Pointer.physicalBytes()`
 * Allow strings ending with "t", "g", "m", etc to specify the number of bytes in system properties ([issue #125](https://github.com/bytedeco/javacpp/issues/125))
 * Add `Info.linePatterns` to limit the lines from header files that the `Parser` has to process
 * Introduce "platform.compiler.hardfpu" option inside `android-arm.properties` to target `armeabi-v7a-hard`
 * Add `UniquePtrAdapter` and corresponding `@UniquePtr` annotation to support `unique_ptr` containers ([pull bytedeco/javacpp-presets#266](https://github.com/bytedeco/javacpp-presets/pull/266))
 * Fix `Parser` not expecting `friend class` declarations that start with `::` ([pull #122](https://github.com/bytedeco/javacpp/pull/122))
 * Synchronize memory allocation in `Pointer` to avoid `OutOfMemoryError` when low on memory
 * Make it clear that `Indexable.createIndexer()` can throw a `NullPointerException` ([issue bytedeco/javacv#437](https://github.com/bytedeco/javacv/issues/437))
 * Escape quotes when parsing default value for the `nullValue` of `@ByRef` or `@ByVal` ([pull #119](https://github.com/bytedeco/javacpp/pull/119))
 * Let `Parser` accept identifiers in addition to integer literals for bit fields ([issue #118](https://github.com/bytedeco/javacpp/issues/118))
 * Fix `Loader.load()` not renaming a library when previously loaded under a different name

### August 1, 2016 version 1.2.3
 * Add support for data member pointers as pseudo-`FunctionPointer` ([issue #114](https://github.com/bytedeco/javacpp/issues/114))
 * Change the packaging type to `jar` since `maven-plugin` causes issues with sbt and Ivy ([issue #113](https://github.com/bytedeco/javacpp/issues/113))
 * Include new `platform.compiler.debug` options inside the default properties file ([pull #90](https://github.com/bytedeco/javacpp/pull/90))
 * Always use the `platform.compiler.default` options unless `@Platform(compiler="!default", ...)` is specified
 * Move optimization options from `platform.compiler.output` to `platform.compiler.default`, allowing users to override
 * Create all missing directories in the path to the target file of `Parser`
 * Parse properly custom `enum` types, found after the ':' token in C++11
 * Output compiled libraries to user specified class path by default for input classes inside JAR files, etc
 * Add `HalfIndexer` to access `short` arrays as half-precision floating point numbers

### July 8, 2016 version 1.2.2
 * Prevent creating unnecessary garbage when using `Indexer` on simple arrays with a stride of 1 ([issue deeplearning4j/nd4j#1063](https://github.com/deeplearning4j/nd4j/issues/1063))
 * Add "org.bytedeco.javacpp.maxretries" system property, the number times to call `System.gc()` before giving up (defaults to 10)
 * Deallocate native memory in a dedicated thread to reduce lock contention ([issue #103](https://github.com/bytedeco/javacpp/issues/103))
 * Fix Javadoc links for externally referenced classes
 * Prevent Android system libraries from getting copied or extracted
 * Insert in `Indexer` an `indexable` field optionally set by the user for convenience
 * Fix potential `ParserException` on comments found after annotations before function declarations
 * Fix `IndexerTest` potentially failing with `OutOfMemoryError` ([issue bytedeco/javacpp-presets#234](https://github.com/bytedeco/javacpp-presets/issues/234))
 * Preload libraries to work around some cases when they refuse to load once renamed ([issue deeplearning4j/libnd4j#235](https://github.com/deeplearning4j/libnd4j/issues/235))
 * Fix compilation error on some `linux-ppc64le` platforms ([issue deeplearning4j/libnd4j#232](https://github.com/deeplearning4j/libnd4j/issues/232))
 * Make sure `Generator` defines `JavaCPP_getStringBytes()` to handle exception messages when using callbacks

### May 26, 2016 version 1.2.1
 * Fix `Loader` crashing on Android ([issue bytedeco/javacv#412](https://github.com/bytedeco/javacv/issues/412))
 * Fix `NullPointerException` on "generic" platforms
 * Throw `OutOfMemoryError` on `allocateArray()` for `Pointer` of primitive types with `size > 0 && address == 0` ([issue deeplearning4j/nd4j#960](https://github.com/deeplearning4j/nd4j/issues/960))
 * Add the ability the specify, after a `#` character, the output filename of libraries extracted by `Loader.load()`
 * Consider `FunctionPointer` annotated with empty `@Namespace` as non-member function pointers ([issue #99](https://github.com/bytedeco/javacpp/issues/99))

### May 15, 2016 version 1.2
 * Fix `NullPointerException` in `Builder` on invalid `java.home` system property or inaccessible directories
 * Add parameters to `Loader.load()` offering more flexibility over the platform properties and library paths
 * Treat all `String` with `Charset.defaultCharset()` (or define `MODIFIED_UTF8_STRING` for old behavior) ([issue #70](https://github.com/bytedeco/javacpp/issues/70))
 * Fix `NullPointerException` in `Parser` on variadic templates ([issue #81](https://github.com/bytedeco/javacpp/issues/81))
 * Fix `Loader.load()` error when called right after `Builder.build()` within the same process
 * Lower Maven prerequisite in the `pom.xml` file to 3.0 ([issue #93](https://github.com/bytedeco/javacpp/issues/93))
 * Use `Info.cppTypes` for all `Parser` type substitutions, in addition to macros and templates ([issue bytedeco/javacpp-presets#192](https://github.com/bytedeco/javacpp-presets/issues/192))
 * Make `Parser` take into account Java keywords not reserved in C++, casting issues with `int64_t`, and `const` value types in basic containers
 * Let users define `NATIVE_ALLOCATOR` and `NATIVE_DEALLOCATOR` macros to overload global `new` and `delete` operators
 * Map `jint` to `int` and `jlong` to `long long` on Windows as well as all platforms with GCC (or Clang)
 * Fix corner cases when checking for the platform in `Generator` and `Parser`
 * Link libraries with "-z noexecstack" on Linux as recommended by HotSpot ([pull #90](https://github.com/bytedeco/javacpp/pull/90))
 * Set the internal DT_SONAME field in libraries created for Android ([issue bytedeco/javacpp-presets#188](https://github.com/bytedeco/javacpp-presets/issues/188))
 * Add "org.bytedeco.javacpp.maxbytes" system property, forcing a call to `System.gc()` when this amount of memory tracked with deallocators is reached
 * Let `Parser` pick up `Info.annotations` in the case of function pointers as well
 * Add `@Convention(extern=...)` value to have `Generator` produce `FunctionPointer` with other language linkage than "C"
 * Enhance the `indexer` package with `long` indexing, initially via the `sun.misc.Unsafe`, for now
 * Lengthen the `position`, `limit`, and `capacity` fields of `Pointer` using `long`
 * Prevent creating text relocations for shared libraries on Android, which are rejected by recent versions of the SDK
 * Use the `outputDirectory` as the compiler's working directory ([pull #89](https://github.com/bytedeco/javacpp/pull/89))
 * Comment with first dimension of multidimensional array inside `@Cast` ([pull #87](https://github.com/bytedeco/javacpp/pull/87))
 * Remove `throws Exception` from `Pointer.close()` since destructors do not typically throw exceptions ([pull #86](https://github.com/bytedeco/javacpp/pull/86))
 * Add `-nodelete` flag to keep generated C++ files after compilation ([pull #85](https://github.com/bytedeco/javacpp/pull/85))
 * Annotate functions originating from `cinclude` with `@NoException` ([pull #84](https://github.com/bytedeco/javacpp/pull/84))
 * Call `JNI_ABORT` on release of `const` arrays ([pull #83](https://github.com/bytedeco/javacpp/pull/83))
 * Add support for C++11 `default` and `delete` on function declarations ([issue #80](https://github.com/bytedeco/javacpp/issues/80))
 * Add support for C++11 typed `enum`, with or without enumerator list ([issue #78](https://github.com/bytedeco/javacpp/issues/78))
 * Add missing space for `const` types when normalizing template arguments in `Parser` ([issue bytedeco/javacpp-presets#165](https://github.com/bytedeco/javacpp-presets/issues/165))
 * Make `Builder` fail on `ClassNotFoundException` or `NoClassDefFoundError` instead of logging warnings
 * Allow `Builder` to generate native libraries with empty `@Platform` annotation even without user defined `native` methods
 * Enhance `Parser` to support a bit better `&&` tokens and C++11 rvalue references ([issue bytedeco/javacpp-presets#160](https://github.com/bytedeco/javacpp-presets/issues/160))
 * Add properties for the `linux-armhf`, `linux-ppc64`, and `linux-ppc64le` platforms, and pick up `macosx` when `os.name` is `darwin`
 * Fix `NullPointerException` in `Parser` on unexpected forms of function pointers ([issue #70](https://github.com/bytedeco/javacpp/issues/70))
 * Make sure `Generator` produces calls to `sizeof()` and `offsetof()` for all `Pointer` classes with allocators
 * Let `Parser` use adapters in the case of `FunctionPointer` as well ([issue bytedeco/javacpp-presets#145](https://github.com/bytedeco/javacpp-presets/issues/145))
 * Prepend "javacpp." to all properties associated with Maven in `BuildMojo` to avoid name clashes
 * Let users define the `GENERIC_EXCEPTION_CLASS` macro (default of `std::exception`) to indicate the base exception thrown by native methods
 * Split type names at `::` delimiters before mapping them against templates in `Parser`
 * Fix swallowed `InterruptedException` ([issue bytedeco/javacv#315](https://github.com/bytedeco/javacv/issues/315))
 * Adjust a few things in `Generator` preventing `@Virtual` from working properly in some cases ([issue bytedeco/javacpp-presets#143](https://github.com/bytedeco/javacpp-presets/issues/143))
 * Fix `TokenIndexer` inserting an invalid token while expanding macros ending with a backslash ([issue #63](https://github.com/bytedeco/javacpp/issues/63))
 * Make `Parser` take `Info.skip` into account for `enum` declarations as well
 * Improve the performance of `BytePointer.getString()` by using `strlen()`
 * Prevent `Generator` from initializing classes when preloading them, which can cause problems ([issue bytedeco/javacpp-presets#126](https://github.com/bytedeco/javacpp-presets/issues/126))
 * Add `Info.flatten` to duplicate class declarations into their subclasses, useful when a subclass pointer cannot be used for the base class as well
 * Prevent `Loader` from extracting libraries more than once, which can cause problems ([issue bytedeco/javacpp-presets#126](https://github.com/bytedeco/javacpp-presets/issues/126))
 * Make `Indexer implements AutoCloseable` to let us try-with-resources
 * Add missing calls to `close()` for `InputStream` and `OutputStream` in `Loader` ([issue #53](https://github.com/bytedeco/javacpp/issues/53))
 * Remove `Piper` class no longer needed with Java SE 7
 * Let `Parser` place appropriate `Info.javaText()` provided by users in the case of destructors as well
 * Fix the `Parser` skipping over some declarations by mistake and producing invalid comments for macros
 * To let users specify `...` varargs as `Info.pointerTypes()`, have the `Parser` replace them with array `[]` when not found on the last parameter
 * Enhance basic support for containers of the style `std::vector<std::pair< ... > >` with user-friendly array-based setter methods
 * Fix `Generator` not passing function objects even when annotating `FunctionPointer` parameters with `@ByVal` or `@ByRef`
 * Map `bool*` to `boolean[]` tentatively in `Parser` since `sizeof(bool) == sizeof(jboolean)` on most platforms
 * Allow `Parser` to generate `@Cast()` annotations and overloaded `put()` methods in basic containers too
 * Move list of basic containers and types to `Info.cppTypes` of the "basic/containers" and "basic/types" `InfoMap` entries, letting users change them at build time
 * Fix some `Parser` issues with `typedef` and forward declarations inside `class` definitions
 * Insure `Parser` maps 64-bit values in C++ `enum` to `long` variables ([issue #48](https://github.com/bytedeco/javacpp/issues/48))
 * Fix `Generator` trying to cast improperly objects on return from  `@Virtual` functions
 * Make `Parser` take `constexpr`, `nullptr`, and `namespace` aliases into account, and fix a couple of preprocessing issues with `TokenIndexer`
 * Fix primitive arrays and NIO buffers not getting updated on return when used as arguments with adapters ([issue bytedeco/javacpp-presets#109](https://github.com/bytedeco/javacpp-presets/issues/109))
 * Enhance a bit the conversion from Doxygen-style documentation comments to Javadoc-style
 * Remove class check in allocators, which prevented peer classes from being extended in Java, instead relying on `super((Pointer)null)` in child peer classes, and remove confusing and now unnecessary empty constructors

### October 25, 2015 version 1.1
 * Make `Generator` use actual C++ operators for commonly overloaded ones instead of calling `operator??()` functions, for better portability
 * Fix potential race condition when deallocating `Pointer` objects from multiple threads
 * Add logging to `Loader.loadLibrary()` to help diagnose loading problems ([issue #41](https://github.com/bytedeco/javacpp/issues/41))
 * Provide new `@Platform(pragma=...)` value to have `Generator` output `#pragma` directives
 * Upgrade all Maven dependencies and plugins to latest versions, thus bumping minimum requirements to Maven 3.0
 * Add new "org.bytedeco.javacpp.cachedir" system property to specify where to extract and leave native libraries to share across multiple JVM instances
 * Provide `@Virtual(true)` to specify pure virtual functions and prevent `Generator` from making undefined calls
 * Update properties for Android to detect undefined symbols at compile time, instead of getting errors only at runtime
 * Log when `Pointer.deallocator` gets registered, garbage collected, or deallocated manually, if `Logger.isDebugEnabled()` (redirectable to SLF4J)
 * Make `Pointer implements AutoCloseable` to let us try-with-resources, thus bumping minimum requirements to Java SE 7 and Android 4.0
 * Introduce the concept of "owner address" to integrate `Pointer` transparently with `std::shared_ptr`, etc (Thanks to Cyprien Noel for the idea!)
 * Add new "cpp11" option for the `@Platform(compiler=...)` value to build against the C++11 standard
 * Fix `Parser` support for the `interface` keyword of the Microsoft C/C++ Compiler
 * Let `Parser` pick up names from `Info.pointerTypes` in the case of function pointers as well
 * Add new "org.bytedeco.javacpp.nopointergc" system property to prevent `Pointer` from registering deallocators with the garbage collector
 * Add `@Properties(names=...)` value to specify a list of default platform names that can be inherited by other classes
 * Fix a couple of `Parser` issues on complex template types ([issue #37](https://github.com/bytedeco/javacpp/issues/37))
 * Take into account `Buffer.arrayOffset()` when creating a `Pointer` from a buffer backed by an array ([issue bytedeco/javacv#190](https://github.com/bytedeco/javacv/issues/190))
 * Fix some incorrectly translated comments in `Parser` ([issue #32](https://github.com/bytedeco/javacpp/issues/32))
 * Add `Parser` support for the `std::bitset` "container", and a bug involving simple types and skipped identifiers
 * Properly parse overloaded `new` and `delete` operators, `friend` declarations, and default constructors with an explicit `void` parameter ([issue #31](https://github.com/bytedeco/javacpp/issues/31))
 * Fix a couple of potential `NullPointerException` in `Parser` ([issue #30](https://github.com/bytedeco/javacpp/issues/30))
 * Have the `Parser` wrap the `iterator` of some standard C++ containers when useful
 * Use Clang as the default compiler for Mac OS X and iOS (via RoboVM)
 * Adjust `BytePointer`, `CharPointer`, `IntPointer`, and `StringAdapter` to work with data strings that are not null-terminated ([issue #24](https://github.com/bytedeco/javacpp/issues/24))
 * Forbid `Parser` from producing `abstract` classes, preventing C++ factory methods and such from working properly ([issue #25](https://github.com/bytedeco/javacpp/issues/25))
 * Fix crash when trying to create objects from abstract classes, to let the exception be thrown on return ([issue #26](https://github.com/bytedeco/javacpp/issues/26))
 * Switch to GCC 4.9 by default on Android, probably dropping support for Android 2.2, because GCC 4.6 has been dropped from the NDK since r10e
 * Insure `Generator` casts properly to `jweak` when calling `DeleteWeakGlobalRef()` ([issue #23](https://github.com/bytedeco/javacpp/issues/23))

### July 11, 2015 version 1.0
 * Add `-undefined dynamic_lookup` option to Mac OS X compiler, making its native linker behave a bit better, plus search for libraries suffixed with ".so" too
 * Add missing `@Platform(frameworkpath=...)` value and corresponding property to set custom framework paths for the linker
 * Add `Parser` support for the `interface` keyword of the Microsoft C/C++ Compiler
 * Fix `Generator` performance issue on classes with a lot of methods ([issue bytedeco/javacpp-presets#36](https://github.com/bytedeco/javacpp-presets/issues/36))
 * Offer the Apache License, Version 2.0, as a new choice of license, in addition to the GPLv2 with Classpath exception
 * Fix `NullPointerException` when trying to process an `interface` class
 * Fix `Parser` errors on unnamed `namespace` blocks, preprocessor directives with comments, and empty macros
 * Introduce a `nullValue` to `@ByRef` and `@ByVal` annotations to let us specify what to do when passed `null`
 * Add properties for `android-arm64`, `android-x86_64`, and `linux-arm64` platforms
 * Add slow but generic `Indexer.putDouble()` to complement existing `Indexer.getDouble()` (useful for testing)
 * Fix and enhance in various ways the support of `Parser` and `Generator` for function pointers, virtual functions, and abstract classes
 * Improve `Parser` check for `const` references and pointers required to output appropriate `@Const` annotation
 * Add `Info.purify` to force the `Parser` in producing abstract classes
 * Let `StringAdapter` (via the `@StdString` annotation) support `std::string*`, that is to say, pointers to strings
 * Fix `Tokenizer` failing on some character and string literals
 * Fix `Parser` errors caused by constructors in `typedef struct` constructs, included nested ones, and skip over pointer names too ([issue bytedeco/javacpp-presets#62](https://github.com/bytedeco/javacpp-presets/issues/62))
 * Generalize `Info.virtualize` to let non-pure virtual functions get annotated with `@Virtual native`
 * Make `VectorAdapter` work even with elements that have no default constructor
 * Add `Parser` support for `std::pair` as a sort of zero-dimensional container type
 * Fix `Parser` crash on empty comments ([issue #14](https://github.com/bytedeco/javacpp/issues/14))

### April 4, 2015 version 0.11
 * Clarify with documentation comments various constructors produced by the `Parser`
 * Add `SharedPtrAdapter` and corresponding `@SharedPtr` annotation to support `shared_ptr` containers
 * Fix a few small issues and work around a few additional corner cases with the `Parser` and the `Generator`
 * Provide `UByteIndexer` and `UShortIndexer`, treating array and buffer data as unsigned integers, for ease of use
 * Clean up Windows `java.io.tmpdir` even when program messes with `java.class.path` ([issue #12](https://github.com/bytedeco/javacpp/issues/12))
 * In addition to direct NIO buffers, also accept as function arguments non-direct ones backed by arrays ([issue bytedeco/javacpp-presets#36](https://github.com/bytedeco/javacpp-presets/issues/36))
 * Fix `@Virtual` callback functions defined inside a `@Namespace`
 * Adjust `TokenIndexer` and `Parser` to handle `#if`, `#ifdef`, `#ifndef`, `#elif`, `#else`, and `#endif` preprocessor directives more appropriately, even when placed in the middle of declarations
 * Append `@Documented` to annotation types to have them picked up by Javadoc
 * Fix `friend` functions not getting skipped by the `Parser`
 * Add `Info` for `__int8`, `__int16`, `__int32`, and `__int64` to `InfoMap.defaults`

### December 23, 2014 version 0.10
 * Fix multiple "platform.preload" or "platform.preloadpath" properties not getting considered by the `Loader`
 * Fix some `Parser` exceptions on valid declarations with template arguments, macro expansions, or overloaded cast operators, and make `Info.javaName` usable in the case of `enum`
 * Disable DocLint, which prevents the build from succeeding on Java 8 ([issue #5](https://github.com/bytedeco/javacpp/issues/5))
 * Add new `indexer` package containing a set of `Indexer` for easy and efficient multidimensional access of arrays and buffers ([issue javacv:317](http://code.google.com/p/javacv/issues/detail?id=317))
 * Use `Long.decode()` inside the `Tokenizer` to test more precisely when integer values are larger than 32 bits
 * Have the `Parser` produce `@Name("operator=") ... put(... )` methods for standard C++ containers, avoiding mistaken calls to `Pointer.put(Pointer)` ([issue bytedeco/javacv#34](https://github.com/bytedeco/javacv/issues/34))
 * Let the `Parser` apply `Info.skip` in the case of macros as well
 * Remove warning log messages when using the `@Raw` annotation
 * Let `@Virtual @MemberGetter` annotated methods return member function pointers of functions defined with `@Virtual`, useful for frameworks like Cocos2d-x
 * Fix `NullPointerException` when leaving the `includePath`, `linkPath`, or `preloadPath` Mojo parameter empty
 * Add `Info.virtualize` to have the `Parser` generate `@Virtual abstract` for pure virtual functions in the given classes
 * Add `@Virtual` annotation and update `Generator` to support callback by overriding such annotated `native` or `abstract` methods
 * Add hack for `typedef void*` definitions and parameters with a double indirection to them

### July 27, 2014 version 0.9
 * Fix `Generator.checkPlatform()` not checking super classes
 * Add `includePath`, `linkPath`, and `preloadPath` parameters to `BuildMojo` to let Maven users append paths to the properties easily
 * In consequence, remove too arbitrary "local" paths from the default platform properties (issue #43)
 * Fix a few other more or less minor issues in the `Parser` with the `long double`, `ptrdiff_t`, `intptr_t`, `uintptr_t`, `off_t` types, floating-point numbers, macro redefinitions, access specifiers, casting of const values by reference, optional parentheses, const data types in templates, declarator names equal to a type name, friend functions, inline constructors, `typedef void` declarations within namespaces, pointers to function pointers
 * Allow users to instruct the `Parser` to skip the expansion of specific macro invocations
 * Let `Parser` concatenate tokens when expanding macros containing the `##` operator
 * Add some documentation for `Info`, `InfoMap`, `InfoMapper`, and `Parser`
 * Fix the `Parser` not filtering and expanding properly some preprocessor directives, as well as producing wrong code for `typedef struct *`
 * Skip Java path search when building for Android to prevent including some random `jni.h` file ([issue #3](https://github.com/bytedeco/javacpp/issues/3))
 * Fix the `Parser` losing some keywords like `static` on methods annotated with an `@Adapter` ([issue #2](https://github.com/bytedeco/javacpp/issues/2))
 * Fix `Loader.load()` not properly force loading all inherited target classes ([issue #1](https://github.com/bytedeco/javacpp/issues/1))

### April 28, 2014 version 0.8
 * Move from Google Code to GitHub as main source code repository
 * Place build-time classes in the `org.bytedeco.javacpp.tools` package and bring out static nested classes, in an effort to avoid conflicts and ease development
 * Rename the `com.googlecode.javacpp` package to `org.bytedeco.javacpp`
 * Added `public long Pointer.address()` getter method, useful when one needs to subtract two pointers
 * Removed old NetBeans project files that cause a conflict when trying to open as a Maven project (issue javacv:210)
 * Fixed compilation error on `FunctionPointer` classes containing no native callback methods
 * Added a `platform.library.path` property, such as "lib/armeabi/" in the case of the "android-arm" platform, to be used instead of "package/platform" (issue javacv:427)
 * Generalized references to the path of the Android NDK
 * Improved a few small things in the set of `Pointer` classes
 * Introduced a simple `Logger` class and unified the logging output calls around it
 * Unified the property names with the `@Properties` and `@Platform` annotations into a consistent naming scheme
 * Continued to clean up the `Parser` and improve the support of, for the most part, comments, enumerations, functions pointers, anonymous `struct` or `union`, templates, overloaded operators, namespaces, standard containers, default parameter arguments, multiple inheritance, custom names of wrapped declarators, and helper classes written in Java
 * Annotations such as `@Adapter` or `@ByVal` are no longer ignored on parameters of getters or setters annotated with `@Index`
 * Fixed some other corner cases in `Generator` and a few potential issues with the hacks in `Loader`
 * Added for convenience to `PointerPointer` a generic parameter `<P extends Pointer>` and the associated `get(Class<P> ...)` getters, as well as `String` getters and setters
 * Passing a `Class` object as first argument to a native method that returns a `Pointer` now determines the runtime type of that returned object
 * Generalized somewhat more the compiler options used inside `linux-arm.properties` (issue javacv:418)
 * Unified the function pointer type of native deallocators to `void (*)(void*)`
 * Removed dependency on (efficient) `AllocObject()` and `CallNonvirtualVoidMethodA()` JNI functions, which are not supported by Avian
 * Cleaned up and optimized `Generator` a bit, also fixing a crash that could occur when `FindClass()` returns `NULL`

### January 6, 2014 version 0.7
 * Tweaked a few things to support RoboVM and target iOS, but `JNI_OnLoad()` does not appear to get called...
 * Upgraded references of the Android NDK to version r9c
 * Made `Loader.load()` work, within reason, even when all annotations and resources have been removed, for example, by ProGuard
 * Fixed compile error when using a `FunctionPointer` as parameter from outside its top-level enclosing class
 * Added new `Pointer.deallocate(false)` call to disable garbage collection on a per object basis, allowing users to deal with memory leaks in other ways
 * Changed the default compiler option `-mfpu=vfpv` for ARM to `-mfpu=vfpv3-d16`, because the former is not supported by Tegra 2 (issue javacv:366)
 * Removed call to `Arrays.copyOf()` in `Loader.findLibrary()`, which would prevent it from working on Android 2.2 (issue #39)
 * Fixed invalid code generated for `FunctionPointer` parameters annotated with `@Const @ByRef`
 * Fixed `NullPointerException` in `Loader.load()` when no `@Platform` annotation is provided (issue #38)
 * Parsing for anonymous `struct` or `union` and for `typedef void` (mapped to `@Opaque Pointer`) now outputs something
 * The `Parser` now expands preprocessor macros, filters tokens appropriately, and outputs all unprocessed directives as comments
 * Improved the C++ support of the `Parser` for namespaces, derived classes, access specifiers, custom constructors, vector types, macros, templates, overloaded operators, etc
 * Fixed `typedef` of function pointers and a few code formatting issues with `Parser`
 * Supplied checks to prevent `Loader.load()` from throwing `java.lang.IllegalStateException: Can't overwrite cause`

### September 15, 2013 version 0.6
 * Added new very preliminary `Parser` to produce Java interface files almost automatically from C/C++ header files; please refer to the new JavaCPP Presets subproject for details
 * When catching a C++ exception, the first class declared after `throws` now gets thrown (issue #36) instead of `RuntimeException`, which is still used by default
 * Fixed Java resource leak after catching a C++ exception
 * Upgraded references of the Android NDK to version r9
 * Added new `Builder` option "-copylibs" that copies into the build directory any dependent shared libraries listed in the `@Platform(link={...}, preload={...})` annotation
 * `Loader.getPlatformName()` can now be overridden by setting the `com.googlecode.javacpp.platform.name` system property
 * Refactored the loading code for `@Properties()` into a neat `Loader.ClassProperties` class, among a few other small changes in `Loader`, `Builder`, `Generator`, and the properties
 * Included often used directories such as `/usr/local/include/` and `/usr/local/lib/` to `compiler.includepath` and `compiler.linkpath` default properties
 * New `@Properties(inherit={Class})` value lets users specify properties in common on a similarly annotated shared config class of sorts
 * Fixed callbacks when used with custom class loaders such as with Web containers or frameworks like Tomcat and Play
 * Fixed using `@StdString` (or other `@Adapter` with `@Cast` annotations) on callbacks (issue #34), incidentally allowing them to return a `String`
 * By default, `Builder` now links to the `jvm` library only when required, when using the `-header` command line option (issue #33)
 * Incorporated missing explicit cast on return values when using the `@Cast` annotation
 * Fixed duplicate code getting generated when both specifying the output filename with `-o <name>` and using wildcards on packages containing nested classes 
 * Let `Buffer` or arrays of primitive values be valid return and callback arguments, mostly useful when used along with the `@StdVector` annotation, or some other custom adapter

### April 7, 2013 version 0.5
 * Upgraded references of the Android NDK to version r8e
 * Arguments of `Pointer` type now get handled as `char*` in cases when the `position` can be used for arithmetic
 * Worked around bug of `InputStream.available()` always returning 0 with the `http` protocol in `Loader.extractResource(URL)`

### March 3, 2013 version 0.4
 * Fixed potential problem with methods of `FunctionPointer` annotated with `@Cast("const...")`
 * Upgraded references of the Android NDK to version r8d
 * Fixed callbacks not working on Android anymore (issue #30)
 * Added some Javadoc to most of the code
 * To help diagnose `UnsatisfiedLinkError` thrown by `Loader.load()`, they have been augmented with a potential cause originating from the "preloading" of libraries, whose premature deletion has also been fixed
 * Provided new `@Platform(library="...")` annotation value to let users specify the name of the native library used by both `Builder` and `Loader`, where different classes with the same name get built together, which also works on nested classes (issue #29)
 * Added the ability to change the name of the class of function objects created when defining a `FunctionPointer` with the `@Name` annotation
 * `Builder` would go on a compile spree when all classes specified on the command line could not be loaded
 * Exported `Loader.isLoadLibraries()`, which always returns true, except when the `Builder` loads the classes
 * Made it possible to specify a nested class (with a '$' character in the name) on the command line
 * When `Pointer.limit == 0`, the methods `put()`, `zero()`, and `asBuffer()` now assume a size of 1
 * Fixed compiler error on 32-bit Mac OS X

### November 4, 2012 version 0.3
 * Added `Pointer.withDeallocator(Pointer)` method to attach easily a custom `Deallocator` created out of a `static void deallocate(Pointer)` method in the subclass, including native ones such as `@Namespace @Name("delete") static native void deallocate(Pointer)`
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

### July 21, 2012 version 0.2
 * Fixed problems when trying to map `java.lang.String` to other native types than `char*`, such as `unsigned char*`
 * JavaCPP now uses the `new (std::nothrow)` operator for allocation, which guarantees that allocation of primitive native arrays won't throw exceptions, making it possible to build C++ exception free JNI libraries
 * Added new `Pointer.limit` property, mainly useful to get the `size` of an output parameter, as returned by an adapter specified with the `@Adapter` annotation
 * Renamed the `capacity` field of an adapter to `size` as it now maps to both `Pointer.limit` and `Pointer.capacity` (the latter only for new allocations)
 * Added `Pointer.put(Pointer)` method, the counterpart of `Buffer.put(Buffer)`, to call the native `memcpy()` function on two `Pointer` objects
 * New `@NoException` annotation to reduce the size of generated code and optimize runtime performance of functions that are guaranteed not to throw exceptions, or for cases when we do not mind that the JVM may crash and burn
 * Trying to generate code for non-static native methods inside a class not extending `Pointer` now generates proper warning (issue #19)
 * Fixed regression where the `@Adapter` notation generates incorrect code for types other than `Pointer` (issue #20)

### May 27, 2012 version 0.1
 * Started using version numbers, friendly to tools like Maven, and placing packages in a sort of [Maven repository](http://maven2.javacpp.googlecode.com/git/) (issue #10)
 * Before loading a JNI library, the `Loader` now also tries to extract and load libraries listed in the `@Platform(link={...}, preload={...})` annotation values, and to support library names with version numbers, each value has to follow the format "libname@version" (or "libname@@version" to have `Builder` use it for the compiler as well), where "version" is the version number found in the filename as required by the native dynamic linker, usually a short sequence of digits and dots, but it can be anything (e.g.: "mylib@.4.2" would map to "libmylib.so.4.2", "libmylib.4.2.dylib", and "mylib.4.2.dll" under Linux, Mac OS X, and Windows respectively)
 * All files now get extracted into a temporary subdirectory, and with the appropriate platform-dependent linker options, or with libraries patched up after the fact with tools such as `install_name_tool` of Mac OS X, most native dynamic linkers can load dependent libraries from there
 * Stopped using `java.net.URL` as hash key in `Loader` (very bad idea)
 * Changed the default value of the `@Index` annotation from 0 to 1, and fixed the `Generator` when it is used with member getters and setters
 * Renamed `mingw-*.properties` to `windows-*-mingw.properties` for consistency
 * Made the `Generator` allocate native heap memory for callback arguments passed `@ByVal` (in addition to `FunctionPointer`), rendering their behavior consistent with return `@ByVal` in the case of function calls (issue #16)
 * `Generator` now uses `std::runtime_error(std::string&)` instead of assuming that some nonstandard `std::exception(std::string&)` constructor exists (issue #17)
 * Fixed `Generator` producing incorrect code when applying invalid annotations such as `@ByVal` on a method that returns something else than a `Pointer` object (issue #18)

### May 12, 2012
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

### March 29, 2012
 * Added new `compiler.framework` property and corresponding `@Platform.framework()` value to allow easier binding with Mac OS X frameworks
 * Changed most `Builder` errors into warnings, letting the building process complete successfully more often
 * We may now use the `@NoDeallocator` annotation on a class to disable deallocation for all allocation methods

### March 03, 2012
 * Added new `@NoDeallocator` annotation to prevent `allocate()` and `allocateArray()` methods from registering a native deallocator to `Pointer` objects (issue #1)
 * `Generator` now properly skips as unsupported array parameters that do not have a primitive component type, and logs a warning (issue #7)
 * `Generator` and `Builder` would append the same include files, libraries, or options multiple times when not required: Fixed in `Loader.appendProperty()` (issue #8)
 * Moved the placement of the class-level @Index annotation to the getter and setter methods themselves
 * To process all classes in a package, we may now specify as argument to the `Builder` its name followed by ".*", in a similar fashion to the `import` statement of the Java language, or by ".**" to process recursively all packages, while omitting to specify any class or package results in JavaCPP processing all classes found under the directories or JAR files specified with the "-classpath" option (issue #12)
 * Equipped the `*Pointer` classes with new bulk `get()` and `put()` methods taking an array as argument, to compensate for direct NIO buffers lacking in performance on Android (issue #11)

### February 18, 2012
 * Cleaned up a few minor `Exception` blocks
 * New `Pointer.deallocateReferences()` static method to force immediate deallocation of all native memory allocated by `Pointer` objects that since have been garbage collected
 * Updated `android-arm.properties` to reflect the fact that, starting from Android NDK r7, `libstdc++.a` has been surreptitiously renamed to `libgnustl_static.a`, such that JavaCPP was instead linking to a new bogus `libstdc++.so` library, causing runtime linking errors
 * Included new `android-x86.properties` to compile binaries for that platform as well
 * Added new `compiler.sysroot.prefix` and `compiler.sysroot` platform properties to pass options such as `--sysroot` to satisfy new rituals of the Android NDK starting from r7b
 * Upgraded references of the Android NDK to version r7b

### January 8, 2012
 * Added new `compiler.linkpath.prefix2` platform property to pass options such as `-Wl,-rpath,` to linkers that support them
 * Fixed `Loader.load()` on Android 4.0, where `SecurityManager.getClassContext()` returns `null`
 * Upgraded references of the Android NDK to version r7

### October 29, 2011
 * Changed the following to make MinGW work: `Generator` now maps `jlong` to the more standard `long long` instead of `__int64` type and also includes `stdint.h`, and added `-D_JNI_IMPLEMENTATION_ -Wl,--kill-at` to the compiler options, as recommended by MinGW's documentation for building DLLs compatible with JNI 
 * Added hack for `AttachCurrentThread()`, whose signature differs under Android, and `DetachCurrentThread()` now gets called as appropriate after returning from a callback function, to prevent memory leaks (and also crashes on platforms such as Android) (issue #3)
 * `Generator` now generates correct code for the annotation pairs `@Const @ByRef` and `@Const @ByVal` (issue #4)
 * Worked around callback functions crashing on Android, which is unable to load user classes from native threads (issue #5)
 * Fixed a few potential pitfalls inside `Generator` and `Loader`
 * Removed compiler warnings due to the type of the `capacity` member variable of `VectorAdapter`
 * Callback `FunctionPointer` objects may now return `@ByVal` or `@ByRef`
 * On Android, changed the output of runtime error messages from `stderr` (equivalent to `/dev/null` on Android) to the log

### October 1, 2011
 * Changed default option flag "/MT" to "/MD" (and a few others that Visual Studio uses by default) inside `windows-x86.properties` and `windows-x86_64.properties` because `std::vector`, `VectorAdapter` and C++ memory allocation in general does not work well with static runtime libraries across DLLs under Windows Vista and Windows 7 for some reason, and because Microsoft fixed the manifest file insanity starting with Visual C++ 2010
 * `Builder` now searches for `jni.h` and `jni_md.h` inside `/System/Library/Frameworks/JavaVM.framework/Headers/` if not found inside `java.home`, as with Mac OS X Lion (issue #2)
 * Upgraded references of the Android NDK to version r6b
 * Fixed a few potential pitfalls inside `Generator`
 * Added hack to let `*Pointer` classes with a corresponding `*Buffer` class have constructors for them

### July 5, 2011
 * `Generator` now lets `get()/put()` (or the `ValueGetter/ValueSetter` annotated) methods use non-integer indices for the `Index` annotation
 * Removed calls to `Arrays.copyOf()` inside `getString*()` methods so they may work on Android as well
 * Fixed race condition that could occur in the deallocation code of `Pointer` due to incorrect synchronization
 * `platform.root` now defaults to the current directory

### June 10, 2011
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
 * Upgraded references of the Android NDK to version r5c, which now also works on Android 2.1 or older ([android issue #16008](http://code.google.com/p/android/issues/detail?id=16008))
 * `Loader.load()` no longer requires a JVM that supports annotations to function properly

### April 22, 2011
 * `Generator` now outputs `#include <stdio.h>`, the lack of which prevents Android NDK under Windows from compiling

### April 7, 2011
 * Replaced arrays from constructors with variable-length argument lists for convenience
 * Fixed a few small potential pitfalls previously overlooked

### March 1, 2011
 * Fixed directory search for `jni_md.h`, which did not search deep enough in some cases
 * Added new `path.separator` property to set the path separator of the target platform, regardless of the build platform
 * Added hack to make sure the temporarily extracted library files get properly deleted under Windows
 * Now loads classes more lazily
 * Changed the paths for libstdc++ inside `android-arm.properties` to the non "v7a" versions
 * Added new `platform.root` property to let users specify the path to their toolchains more easily

### February 18, 2011
Initial release


Acknowledgments
---------------
This project was conceived at the [Okutomi & Tanaka Laboratory](http://www.ok.ctrl.titech.ac.jp/), Tokyo Institute of Technology, where I was supported for my doctoral research program by a generous scholarship from the Ministry of Education, Culture, Sports, Science and Technology (MEXT) of the Japanese Government. I extend my gratitude further to all who have reported bugs, donated code, or made suggestions for improvements (details above)!
