
### June 6, 2023 version 1.5.9
 * Fix `Loader.extractResource()` for nested JAR files from Spring Boot ([pull #685](https://github.com/bytedeco/javacpp/pull/685))
 * Have `Parser` desugar `...` varargs to array `[]` for return types as well ([pull #682](https://github.com/bytedeco/javacpp/pull/682))
 * Fix `Parser` failing on some `friend` functions for `operator` overloading ([pull #681](https://github.com/bytedeco/javacpp/pull/681))
 * Fix `Parser` incorrectly casting `const` pointers to template arguments of pointer types ([pull #677](https://github.com/bytedeco/javacpp/pull/677))
 * Fix `Parser` with `Info.enumerate` failing to translate `enum` values based on other `enum` values
 * Fix `Parser` prematurely expanding macros defined in `class`, `struct` or `union` ([issue #674](https://github.com/bytedeco/javacpp/issues/674))
 * Add `Info.upcast` to support class hierarchies with virtual inheritance ([pull #671](https://github.com/bytedeco/javacpp/pull/671))
 * Pick up `@Adapter`, `@SharedPtr`, etc annotations on `allocate()` as well ([pull #668](https://github.com/bytedeco/javacpp/pull/668))
 * Provide `@Virtual(subclasses=false)` to prevent `Generator` from subclassing subclasses ([pull #660](https://github.com/bytedeco/javacpp/pull/660))
 * Fix `Loader.getPlatform()` detection for `linux-armhf` with Temurin JDK ([issue bytedeco/javacv#2001](https://github.com/bytedeco/javacv/issues/2001))
 * Fix `Parser` ignoring `Info.skip` for enumerators that do not get translated ([issue bytedeco/javacpp-presets#1315](https://github.com/bytedeco/javacpp-presets/issues/1315))
 * Fix `Parser` error on C++17 style namespace declarations containing `::` separators ([issue #595](https://github.com/bytedeco/javacpp/issues/595))
 * Fix `Parser` observing `Info.virtualize` for non-virtual functions ([pull #658](https://github.com/bytedeco/javacpp/pull/658))
 * Use regex in `Parser` to match more robustly templates and namespaces ([pull #657](https://github.com/bytedeco/javacpp/pull/657))
 * Fix `Builder` default output path for class names with the same length ([pull #654](https://github.com/bytedeco/javacpp/pull/654))
 * Add `Info.friendly` to have `Parser` map some `friend` functions to Java methods ([pull #649](https://github.com/bytedeco/javacpp/pull/649))
 * Add `Loader.loadProperties(boolean forceReload)` to reset platform properties ([issue deepjavalibrary/djl#2318](https://github.com/deepjavalibrary/djl/issues/2318))
 * Prevent `TokenIndexer` from recursively expanding macros
 * Fix `Generator` passing empty `String` objects on callback for arguments using adapters
 * Fix `Parser` failure on `enum` enumerators generated using the concat `##` operator

### November 2, 2022 version 1.5.8
 * Add `static long Pointer.getDirectBufferAddress(Buffer)` method for convenience ([pull #629](https://github.com/bytedeco/javacpp/pull/629))
 * Fix `UniquePtrAdapter` incorrectly deallocating pointers on callbacks ([issue #613](https://github.com/bytedeco/javacpp/issues/613))
 * Fix `Generator` incorrectly casting `@ByVal` or `@ByRef` annotated `FunctionPointer` arguments ([issue bytedeco/javacpp-presets#1244](https://github.com/bytedeco/javacpp-presets/issues/1244))
 * Fix `Generator` compiler errors for `FunctionPointer` with `@UniquePtr` arguments ([issue #613](https://github.com/bytedeco/javacpp/issues/613))
 * Fix `Generator` compiler errors on Mac for Clang without Objective-C support ([pull #610](https://github.com/bytedeco/javacpp/pull/610))
 * Prevent `Parser` from outputting cast methods for base classes that are `Info.skip` ([pull #607](https://github.com/bytedeco/javacpp/pull/607))
 * Ensure `Generator` and `Parser` process header files from `cinclude` before `include` ([issue #580](https://github.com/bytedeco/javacpp/issues/580))
 * Remove `sun.misc.Unsafe` config incompatible/unneeded with GraalVM Native Image 22.x ([issue bytedeco/sample-projects#63](https://github.com/bytedeco/sample-projects/issues/63))
 * Define default `SHARED_PTR_NAMESPACE`, `UNIQUE_PTR_NAMESPACE`, `OPTIONAL_NAMESPACE` to `std` on supported compilers ([issue #577](https://github.com/bytedeco/javacpp/issues/577))
 * Let `Generator` treat `long` arguments and return values `@ByVal` or `@ByRef` with `@Cast("...*")` ([issue #576](https://github.com/bytedeco/javacpp/issues/576))
 * Add `BytePointer.getUnsigned()` and `putUnsigned()` methods for convenience ([pull #574](https://github.com/bytedeco/javacpp/pull/574))
 * Let `Parser` consider `alignas` as an explicit attribute to be ignored by default ([issue bytedeco/javacpp-presets#1168](https://github.com/bytedeco/javacpp-presets/issues/1168))
 * Add "org.bytedeco.javacpp.findLibraries" system property to disable search for libraries ([pull #565](https://github.com/bytedeco/javacpp/pull/565))
 * Fix `Generator` causing memory leaks for `String` parameters on callback ([issue bytedeco/javacpp-presets#1141](https://github.com/bytedeco/javacpp-presets/issues/1141))
 * Add `Loader.new/access/deleteGlobalRef()` methods to store JNI `Object` references in `Pointer` ([issue bytedeco/javacpp-presets#1141](https://github.com/bytedeco/javacpp-presets/issues/1141))
 * Make `Loader.findLibrary()` also search in "sun.boot.library.path" for jlink ([pull #565](https://github.com/bytedeco/javacpp/pull/565))
 * Add `__int8`, `__int16`, `__int32`, and `__int64` to `InfoMap` as "basic/types" to support combinations allowed by Visual Studio
 * Add "org.bytedeco.javacpp.cacheLibraries" system property to disable cache for libraries ([pull bytedeco/gradle-javacpp#21](https://github.com/bytedeco/gradle-javacpp/pull/21))
 * Add public getters for the address fields of `Pointer.NativeDeallocator` ([discussion bytedeco/javacpp-presets#1160](https://github.com/bytedeco/javacpp-presets/discussions/1160))
 * Add support for `std::function` basic container instances with corresponding `FunctionPointer` ([issue bytedeco/javacpp-presets#1051](https://github.com/bytedeco/javacpp-presets/issues/1051))
 * Fix `Builder` parsing of command line options for platform properties ([issue #564](https://github.com/bytedeco/javacpp/issues/564))
 * Use thread local in `Generator` to detach automatically native threads on exit for Windows as well ([pull #562](https://github.com/bytedeco/javacpp/pull/562))
 * Add compiler options for C++14 and C++17 to platform properties files for Visual Studio
 * Fix `Parser` incorrectly shortening type names for nested class template instances
 * Make `Parser` output `boolean has_value()` methods for basic containers like `std::optional`
 * Add `OptionalAdapter` and corresponding `@Optional` annotation for containers like `std::optional`
 * Switch to `AttachCurrentThreadAsDaemon()` when attaching native threads on callback ([pull #561](https://github.com/bytedeco/javacpp/pull/561))
 * Add to `InfoMap` default pointer and value types for integer types in `std::` namespace
 * Fix Android platform properties for NDK r23b

### February 11, 2022 version 1.5.7
 * Add `Loader.clearCacheDir()` along with new `ClearMojo` and `-clear` command line option
 * Speed up `Loader` on Windows when there are no symbolic links or library versions ([pull #512](https://github.com/bytedeco/javacpp/pull/512))
 * Enhance `Pointer.physicalBytes()` by excluding shared pages from memory-mapped files, etc ([issue #468](https://github.com/bytedeco/javacpp/issues/468))
 * Fix `Parser` not correctly encoding files of top-level classes produced with `@Properties(target=..., global=...)`
 * Add `Pointer.interruptDeallocatorThread()` method to make JavaCPP classes eligible for GC ([discussion bytedeco/javacpp-presets#1115](https://github.com/bytedeco/javacpp-presets/discussions/1115))
 * Let `Parser` output the content of `Info.javaText` in the case of `FunctionPointer` as well
 * Fix `TokenIndexer` failure on macros using the concat `##` operator with empty arguments ([issue #525](https://github.com/bytedeco/javacpp/issues/525))
 * Let `Parser` support arrays of anonymous `struct` or `union` containing another one ([discussion #528](https://github.com/bytedeco/javacpp/discussions/528))
 * Prevent `Parser` from outputting duplicate `Pointer` constructors for basic containers
 * Fix `Generator` compiler errors on callback functions returning objects without default constructors
 * Ensure `Builder` copies resources only from the first directories found in the paths
 * Add `Loader.getCanonicalPath()` to work around bugs in `File.getCanonicalPath()` on Windows ([pull #519](https://github.com/bytedeco/javacpp/pull/519))
 * Add `FunctionPointer` and `@Virtual` methods missing from config files required by GraalVM Native Image
 * Let `Tokenizer` convert characters using ASCII escape sequences `'\x...'` to normal hexadecimal values `0x...`
 * Fix `Parser` incorrectly mapping default function arguments containing multiple template arguments
 * Fix `Parser` failures on variadic templates calling `sizeof...()` and on variables initialized from template values
 * Prevent `Parser` from failing on nonexistent header files contained in `@Platform(exclude=...)`
 * Add `classOrPackageNames` parameter to `CacheMojo` ([pull #510](https://github.com/bytedeco/javacpp/pull/510))

### August 2, 2021 version 1.5.6
 * Add missing export to `module-info.java` file for presets package ([pull #508](https://github.com/bytedeco/javacpp/pull/508))
 * Add `@NoException(true)` value to support overriding `virtual noexcept` functions
 * Bundle more DLLs from UCRT to fix the systems presets on Windows
 * Fix `Pointer.sizeof()` method for subclasses of subclasses for primitive types ([issue bytedeco/javacpp-presets#1064](https://github.com/bytedeco/javacpp-presets/issues/1064))
 * Throw more accurate `UnsatisfiedLinkError` when `Loader.load()` fails to find JNI libraries
 * Let `Parser` check `Info.skipDefaults` also for types to ignore default constructors ([issue #493](https://github.com/bytedeco/javacpp/issues/493))
 * Fix `Parser` failure on `enum` declarations without enumerators
 * Let `Generator` use the third element of `@Cast(value)` on return values passed to adapters ([issue tensorflow/java#345](https://github.com/tensorflow/java/issues/345))
 * Prevent `Generator` from swallowing exceptions caught on `Buffer.array()` ([pull #504](https://github.com/bytedeco/javacpp/pull/504))
 * Add `enum` classes as well as resources missing from config files required by GraalVM Native Image
 * Let `Parser` annotate `&&` parameters with new `@ByRef(true)` value used by `Generator` to call `std::move()`
 * Fix `Parser` overlooking anonymous `class`, `struct` or `union` with comments after `}` ([issue #501](https://github.com/bytedeco/javacpp/issues/501))
 * Add `Info.beanify` to have `Parser` generate JavaBeans-style getters and setters ([pull #495](https://github.com/bytedeco/javacpp/pull/495))
 * Allow `Parser` to use `Info.javaNames` for function names containing parameters as well ([issue #492](https://github.com/bytedeco/javacpp/issues/492))
 * Fix `Parser` producing incorrect calls to function templates with non-type parameters ([issue #491](https://github.com/bytedeco/javacpp/issues/491))
 * Add missing `presets/package-info.java` required for OSGi and add profile for M2Eclipse ([pull #490](https://github.com/bytedeco/javacpp/pull/490))
 * Remove unnecessary mutex lock for pthreads on callbacks in `Generator` ([pull #489](https://github.com/bytedeco/javacpp/pull/489))
 * Fix `@AsUtf16` handling for setter methods paired with getters in `Generator` ([pull #488](https://github.com/bytedeco/javacpp/pull/488))
 * Allow defining `NO_JNI_DETACH_THREAD` to avoid overhead from pthreads on callbacks ([issue #486](https://github.com/bytedeco/javacpp/issues/486))
 * Pick up `@Allocator`, `@CriticalRegion`, `@NoException` in `Generator` from `@Properties(inherit=classes)` as well ([issue #484](https://github.com/bytedeco/javacpp/issues/484))
 * Add support for `Deleter` of pointer types to `UniquePtrAdapter`
 * Add `@Platform(pattern=...)` annotation value to allow matching with regular expressions as well
 * Allow `Parser` to consider function pointers declared with `using` but without indirections
 * Let `Parser` map to annotations whole expressions starting with the `__attribute__` keyword
 * Fix `Parser` sometimes failing to create template instances with default arguments ([issue #478](https://github.com/bytedeco/javacpp/issues/478))
 * Enhance `Parser` to handle `typedef` correctly in the case of `enum` as well ([issue #477](https://github.com/bytedeco/javacpp/issues/477))
 * Upon `Pointer.getPointer(Class<P>)` scale `position`, `limit`, and `capacity` with `sizeof()` ([pull #476](https://github.com/bytedeco/javacpp/pull/476))
 * Fix `Parser` incorrectly translating non-documentation comments as part of documentation comments ([issue #475](https://github.com/bytedeco/javacpp/issues/475))
 * Set `Pointer.maxPhysicalBytes` to `4 * Runtime.maxMemory()` by default as workaround for Android, memory-mapped files, ZGC, etc ([issue #468](https://github.com/bytedeco/javacpp/issues/468))
 * Ensure `synchronized` code in `Pointer` gets skipped with "org.bytedeco.javacpp.nopointergc" ([issue tensorflow/java#313](https://github.com/tensorflow/java/issues/313))
 * Add `protected Pointer.offsetAddress()` and use it for `getPointer()` instead of `position()`
 * Fix potential infinite loop in `Parser` when processing `class`, `struct`, or `union` declarations
 * Have `Parser` wrap the `erase()` methods of basic containers with iterators to allow removing from maps
 * Let `Parser` output the content of `Info.javaText` in the case of basic containers as well
 * Fix `Parser` failure on arguments containing multiple array accesses ending with `]]`
 * Fix `Parser` incorrectly considering some array definitions with expressions as multidimensional
 * Log loading errors of optional `jnijavacpp` as debug messages instead of warnings ([issue tensorflow/java#189](https://github.com/tensorflow/java/issues/189))
 * Fix memory leak in `Pointer.releaseReference()` with "org.bytedeco.javacpp.nopointergc" ([issue awslabs/djl#690](https://github.com/awslabs/djl/issues/690))
 * Fix `Parser` not stripping annotations from `Info.pointerTypes` when creating Java peer classes
 * Fix `Parser` not inheriting constructors with existing `Info` or with nested templates
 * Add support for `std::tuple`, `std::optional`, and `std::variant` basic containers and fix various `Parser` failures
 * Add parameter for `Loader.load()` to return path of a specific executable ([pull #466](https://github.com/bytedeco/javacpp/pull/466))
 * Use `std::uninitialized_copy` in `VectorAdapter` to make sure copy constructors get called ([pull #465](https://github.com/bytedeco/javacpp/pull/465))

### March 8, 2021 version 1.5.5
 * Ensure `System.gc()` never gets called with "org.bytedeco.javacpp.nopointergc" ([issue tensorflow/java#208](https://github.com/tensorflow/java/issues/208))
 * Add `Info.immutable` to disable generating setters for public data members ([pull #461](https://github.com/bytedeco/javacpp/pull/461))
 * Map `String` to `char*` with `Charset.forName(STRING_BYTES_CHARSET)` when that macro is defined ([pull #460](https://github.com/bytedeco/javacpp/pull/460))
 * Fix `Loader.ClassProperties` not always getting overridden correctly when defined multiple times
 * Allow `Loader.load()` to also rename executables on extraction to output filenames specified with the `#` character
 * Add `@AsUtf16` annotation to map `java.lang.String` to `unsigned short*` (array of UTF-16 code units) ([pull #442](https://github.com/bytedeco/javacpp/pull/442))
 * Add `BasicStringAdapter` and corresponding `@StdBasicString`, `@StdU16String`, and `@StdU32String` annotations ([pull #448](https://github.com/bytedeco/javacpp/pull/448))
 * Fix `Parser` failures on `try` blocks as function body, nested class templates, and aliases to namespaces starting with `::`
 * Prevent `Loader` from failing to find, load, or link libraries multiple times
 * Fix `Pointer.getPointer()` methods sometimes calling the wrong constructor ([issue bytedeco/javacv#1556](https://github.com/bytedeco/javacv/issues/1556))
 * Prevent Android from trying to load `PointerBufferPoolMXBean` by using it via reflection ([pull #447](https://github.com/bytedeco/javacpp/pull/447))
 * Fix Android build properties for NDK r22 and move legacy to `android-*-gcc.properties` ([pull #444](https://github.com/bytedeco/javacpp/pull/444))
 * Add support for Mac on ARM processors
 * Fix `Loader` not searching for libraries in more than one package
 * Prevent `Builder` from linking with `-framework JavaVM` when a path to the JVM library is found
 * Replace `requires` with `requires static` in JPMS `.platform` module ([pull #436](https://github.com/bytedeco/javacpp/pull/436))
 * Let `Parser` output `Info.javaText` even for template declarations with no instances
 * Prevent `Tokenizer` from using `long` literals for unsigned integers of 16 bits or less
 * Ensure `Parser` considers `>=` and `<=` as single tokens to prevent failures
 * Make `Parser` use `Info.cppTypes` to override the type of `enum` values
 * Fix `Parser` not using the correct `Info.pointerTypes` for `const&` declarations
 * Use pthreads in `Generator` to detach automatically native threads on exit for Linux and Mac as well
 * Let `Loader.load()` always succeed on optional libraries only available with extensions
 * Fix `Builder.addProperty()` incorrectly appending values together

### September 9, 2020 version 1.5.4
 * Fix `Parser` not producing `PointerPointer` parameters for `FunctionPointer` subclasses
 * Let `Builder` copy even those `platform.executable` files without prefix or suffix
 * Add missing declaration for `GetCurrentThreadId()` in `Generator` when `NO_WINDOWS_H` is defined
 * Process `#undef` directives to allow redefining macros with `Parser` ([issue bytedeco/javacpp-presets#935](https://github.com/bytedeco/javacpp-presets/issues/935))
 * Pick up in `Parser` methods specified with `override`, in addition to `virtual` ([issue #419](https://github.com/bytedeco/javacpp/issues/419))
 * Let `Parser` create a separate Java peer class when `Info.pointerTypes` is different for types prefixed with `const `
 * Fix `Generator` for `@Virtual` methods protected in subclasses by casting to superclass ([issue #419](https://github.com/bytedeco/javacpp/issues/419))
 * Add missing values to `Info.Info(Info)` and fix incorrect `Info.skipDefaults(boolean)` ([issue #420](https://github.com/bytedeco/javacpp/issues/420))
 * Add `PointerBufferPoolMXBean` to track allocations and deallocations of `Pointer` ([pull #413](https://github.com/bytedeco/javacpp/pull/413))
 * Change the `@Platform(executable=...` property to an array and allow bundling multiple files per class
 * Prevent `Builder` unnecessarily linking with `-framework JavaVM` to fix GraalVM Native Image on Mac ([issue #417](https://github.com/bytedeco/javacpp/issues/417))
 * Add `Pointer.getPointer()` methods as shortcuts for `new P(p).position(p.position + i)` ([issue #155](https://github.com/bytedeco/javacpp/issues/155))
 * Fix `Generator` for cases when a `FunctionPointer` returns another `FunctionPointer`
 * Fix `Parser` failure with `auto` keyword of C++11 used as placeholder type specifier or for trailing return type ([issue #407](https://github.com/bytedeco/javacpp/issues/407))
 * Add `Builder.configDirectory` option to let `Generator` output files that GraalVM needs for AOT compilation ([issue eclipse/deeplearning4j#7362](https://github.com/eclipse/deeplearning4j/issues/7362))
 * Fix `Parser` error on `template<>` containing non-type parameters without names ([issue bytedeco/javacpp-presets#889](https://github.com/bytedeco/javacpp-presets/issues/889))
 * Bundle also the `vcruntime140_1.dll` and `msvcp140_1.dll` redist files from Visual Studio
 * Fix `Builder` for different "java.home" path returned by latest JDKs from Oracle ([pull #400](https://github.com/bytedeco/javacpp/pull/400))
 * Refactor `Builder` a little to work around issues with Gradle
 * Log as warnings `SecurityException` thrown on `Loader.getCacheDir()` instead of swallowing them
 * Fix memory leak that occurs with "org.bytedeco.javacpp.nopointergc" ([issue bytedeco/javacpp-presets#878](https://github.com/bytedeco/javacpp-presets/issues/878))
 * Take into account `platform.library.path` when extracting executables and their libraries on `Loader.load()` ([issue bytedeco/javacv#1410](https://github.com/bytedeco/javacv/issues/1410))
 * Move init code for `Loader.getPlatform()` to `Detector` to avoid warning messages ([issue #393](https://github.com/bytedeco/javacpp/issues/393))
 * Add `HyperslabIndex` class with `offsets`, `strides`, `counts`, and `blocks` parameters ([pull #392](https://github.com/bytedeco/javacpp/pull/392))
 * Add `Index` class to allow overriding how the index is calculated in `Indexer` ([issue #391](https://github.com/bytedeco/javacpp/issues/391))

### April 14, 2020 version 1.5.3
 * Deprecate but also fix `Indexer.rows()`, `cols()`, `width()`, `height()`, and `channels()` ([pull #390](https://github.com/bytedeco/javacpp/pull/390))
 * Fix `Parser` producing invalid wrappers for basic containers like `std::set<std::pair<...> >`
 * Add compiler options for C++98, C++03, C++14, and C++17 to platform properties files ([pull #389](https://github.com/bytedeco/javacpp/pull/389))
 * Remove default compiler options from `linux-armhf.properties` that work mostly only for Raspberry Pi
 * Add `Generator` support for `enum` classes with `boolean` values ([issue #388](https://github.com/bytedeco/javacpp/issues/388))
 * Fix `Parser` outputting invalid Java code for `enum` of `boolean`, `byte`, and `short` types ([issue #388](https://github.com/bytedeco/javacpp/issues/388))
 * Pick up in `Generator` the `@Namespace` annotation from paired method too for global getters and setters ([issue #387](https://github.com/bytedeco/javacpp/issues/387))
 * Add presets for `jnijavacpp` and `javacpp-platform` artifact to fix issues at load time ([issue bytedeco/javacv#1305](https://github.com/bytedeco/javacv/issues/1305))
 * Prevent potential `NullPointerException` in `Loader.checkVersion()` ([pull #385](https://github.com/bytedeco/javacpp/pull/385))
 * Allow using `Charset` to avoid `UnsupportedEncodingException` from `BytePointer` ([pull #384](https://github.com/bytedeco/javacpp/pull/384))
 * Add static `Pointer.isNull(Pointer p)` helper method, for convenience
 * Add `MoveAdapter` and corresponding `@StdMove` annotation to support objects that require `std::move` from C++11
 * Always use `File.pathSeparator` when passing multiple paths via the `BUILD_PATH` environment variable
 * Fix `Parser` not picking up `Info` for declarations with `decltype()` specifier
 * Fix `Pointer` losing its owner when mistakenly ignoring deallocators for `const` values returned from adapters
 * Remove unnecessary declared `Exception` from `Indexer.close()` signature ([pull #382](https://github.com/bytedeco/javacpp/pull/382))
 * Make sure `Parser` recognizes base classes of `struct` as `public` by default
 * Fix `Parser` error on initializer lists containing C++11 style `{ ... }` for template instances
 * Change the default mapping of `jboolean` to `BooleanPointer` instead of `BoolPointer`
 * Fix `Parser` error on function declarations with `...` varargs as single parameter
 * Make `Parser` skip over `&&`-qualified functions automatically since they cannot be supported
 * Fix `Parser` annotating pointer cast `operator` methods with incorrect `@Cast` ([issue #379](https://github.com/bytedeco/javacpp/issues/379))
 * Allow `Parser` to inherit constructors from template classes with `using`
 * Make `Parser` honor `Info.skip` for anonymous `struct` or `union` as well
 * Optimize `Pointer.sizeof()` method of subclasses for primitive types
 * Let users override `Info.enumerate` on a per-`enum` basis and allow attributes after `enum class`
 * Fix `Parser` not considering identifiers as type names when placed directly after `friend` or in `template<>`
 * Check for defined `NO_WINDOWS_H` macro in `Generator` to skip `#include <windows.h>`
 * Provide `UIntIndexer` and `ULongIndexer`, treating array and buffer data as unsigned 32- or 64-bit integers, for convenience ([issue #376](https://github.com/bytedeco/javacpp/issues/376))
 * Fix `Parser` not evaluating `using namespace` with respect to the current block ([issue #370](https://github.com/bytedeco/javacpp/issues/370))
 * Fix exception in `Loader` when running jlink image with JDK 13+ ([pull #375](https://github.com/bytedeco/javacpp/pull/375))
 * Fix errors with `@Virtual @Name("operator ...")` in `Generator` by using Java names for C++ ([issue #362](https://github.com/bytedeco/javacpp/issues/362))
 * Apply in `Parser` missing `const` to parameters of `@Virtual` functions using adapters
 * Use in `Generator` C++11 `override` keyword for `@Virtual` functions ([pull #373](https://github.com/bytedeco/javacpp/pull/373))
 * Speed up `Loader.load()` by caching results returned from `Loader.findLibrary()` ([issue #287](https://github.com/bytedeco/javacpp/issues/287))
 * Pick up `Info` correctly in `Parser` also for anonymous function pointers with `const` parameters
 * Make `Parser` apply `Info.translate` in the case of enumerators as well
 * Fix compiler failures in `Builder` with platform properties containing relative paths
 * Let `Parser` rename types using `Info.javaNames` in addition to `valueTypes` and `pointerTypes` ([pull #367](https://github.com/bytedeco/javacpp/pull/367))
 * Include in the defaults of `InfoMap` mappings missing for the `std::array` and `jchar` types
 * Fix various `Parser` failures with attributes on constructors, empty macros, enum classes, friend classes, inherited constructors, and keywords in parameter names
 * Add to `Parser` support for C++11 attributes found within `[[` and `]]` brackets
 * Consider `Pointer` values `maxBytes` or `maxPhysicalBytes` suffixed with `%` as relative to `Runtime.maxMemory()` ([pull #365](https://github.com/bytedeco/javacpp/pull/365))
 * Prevent `Parser` from considering `constexpr operator` declarations as `const` types
 * Fix on `Loader.load()` the default library name of classes without `@Properties(target=..., global=...)`
 * Prevent `Parser` from outputting `asPointer()` cast methods with multiple inheritance ([issue #360](https://github.com/bytedeco/javacpp/issues/360))
 * Add `CacheMojo` to help extract binaries and resources used by command line tools outside of the JVM
 * Fix Android build properties for NDK r20b ([pull #357](https://github.com/bytedeco/javacpp/pull/357))

### November 5, 2019 version 1.5.2
 * Provide `ByteIndexer` with value getters and setters for unsigned `byte` or `short`, `half`, `bfloat16`, and `boolean` types as well
 * Introduce `PointerScope.extend()` to prevent deallocation on the next call to `close()`
 * Make `Generator` avoid ambiguous conversion errors from `UniquePtrAdapter` to `std::unique_ptr` ([pull #353](https://github.com/bytedeco/javacpp/pull/353))
 * Fix `Parser` using fully qualified names for `@Name` annotations of nested classes ([issue #352](https://github.com/bytedeco/javacpp/issues/352))
 * Add `Parser` support for macro expansion of `__VA_ARGS__`
 * Fix `Builder` not processing all classes when given `.**` as input ([issue bytedeco/javacv#1311](https://github.com/bytedeco/javacv/issues/1311))
 * Introduce reference counting in `Pointer` and retrofit `PointerScope` to use it
 * Fix `Parser` incorrectly inheriting default constructors multiple times with `using`
 * Allow in `Parser` fully qualified names as `Info.valueTypes` for enumerators as well
 * Perform template substitution in `Parser` also for default argument values ([pull #343](https://github.com/bytedeco/javacpp/pull/343))
 * Introduce `PointerScope.forClasses` to limit the `Pointer` classes that can be attached to a given instance
 * Add support for custom `Allocator` to `VectorAdapter` and custom `Deleter` to `UniquePtrAdapter`
 * Enable support for OSGi bundles ([pull #332](https://github.com/bytedeco/javacpp/pull/332))

### September 5, 2019 version 1.5.1-1
 * Use the native thread ID as name on `AttachCurrentThread()` ([pull #339](https://github.com/bytedeco/javacpp/pull/339))
 * Make sure we `canRead()`, `canWrite()`, and `canExecute()` what `Loader.getCacheDir()` returns
 * Prevent `Generator` from copying data unnecessarily when returning Java arrays from adapters ([issue #317](https://github.com/bytedeco/javacpp/issues/317))
 * Fix `Parser` issues when casting `const` pointers or enumerating anonymous `enum` declarations
 * Add `Info.objectify` to map global functions without using the `static` modifier, similarly to Scala companion objects
 * Allow once more `abstract` subclasses of `FunctionPointer` ([issue #318](https://github.com/bytedeco/javacpp/issues/318))

### July 9, 2019 version 1.5.1
 * Make sure `Generator` ignores deallocators on `const` values returned from adapters ([issue #317](https://github.com/bytedeco/javacpp/issues/317))
 * Accelerate `Loader.extractResource()` for directories already cached, also preventing failures ([issue #197](https://github.com/bytedeco/javacpp/issues/197))
 * Avoid `Parser` writing `allocateArray()` when single `int`, `long`, `float`, or `double` constructor already exists ([issue bytedeco/javacv#1224](https://github.com/bytedeco/javacv/issues/1224))
 * Expose all platform properties to process executed with `Builder.buildCommand` via environment variables, with names uppercase and all `.` replaced with `_`
 * Let `Parser` add `@Name` or `@Namespace` annotations to non-translated enumerators as well
 * Make `Parser` pick up the names of type aliases for function pointers declared with `using` and prevent `NullPointerException`
 * Fix `Parser` failing on lambda expressions found inside member initialization lists of constructors
 * Add special support for `constexpr` variables in `Parser` by disabling their member setters automatically
 * Fix `Parser` not placing `&` and `*` at the right place inside template arguments containing function declarations
 * Support more basic containers in `Parser` by comparing their names in a case-insensitive manner and add annotations missing from index types
 * Fix `Generator` taking the `@By*` annotation of the paired method for the index instead of the value argument of a setter
 * Fix `Parser` sometimes considering global C++ identifiers starting with `::` as if they were local
 * Change default value for `Pointer.maxPhysicalBytes` to `Pointer.maxBytes + Runtime.maxMemory()` ([pull #310](https://github.com/bytedeco/javacpp/pull/310))
 * Add `Loader.getVersion()` and `checkVersion()` to get versions of Maven artifacts and check against JavaCPP ([issue #194](https://github.com/bytedeco/javacpp/issues/194))
 * Fix compile errors caused by `Generator` occurring with callback functions returning a value by reference
 * Make `Builder` expand entries from the user class path with `*` as basename to all JAR files in the directory
 * Prevent `Loader` from creating symbolic links pointing to themselves by comparing with `Path.normalize()` ([pull #307](https://github.com/bytedeco/javacpp/pull/307))
 * Fix `Loader.cacheResource()` with the "jrt" protocol as used by jlink ([pull #305](https://github.com/bytedeco/javacpp/pull/305))
 * Fix compiler error with `SharedPtrAdapter` and `UniquePtrAdapter` in callback functions ([pull #304](https://github.com/bytedeco/javacpp/pull/304))
 * Start `Pointer.DeallocatorThread` with `setContextClassLoader(null)` as required by containers ([issue deeplearning4j/deeplearning4j#7737](https://github.com/deeplearning4j/deeplearning4j/issues/7737))
 * Add `-print` command line option to access platform properties externally, for example, inside build scripts
 * Add to `InfoMap` default pointer and value types for `ssize_t`, `char16_t`, `char32_t`, `std::u16string`, and `std::u32string`
 * Support multiple instances of `FunctionPointer` subclasses, up to the value in `@Allocator(max=...)` ([issue bytedeco/javacpp-presets#683](https://github.com/bytedeco/javacpp-presets/issues/683))
 * Allow suffixing library names with `:` to specify exact relative paths to libraries, ignoring any additional prefix or suffix
 * Prevent `Loader.load()` from trying to load library files that do not exist or to create symbolic links to them
 * Let `Loader.load()` extract libraries suffixed with `##`, but still ignored for copying by `Builder`
 * Make sure `Loader.load()` also initializes classes that are passed explicitly
 * Fix `Loader.createLibraryLink()` incorrectly truncating library versions when there is one before and another after the suffix
 * Iterate extensions of libraries or executables on `Loader.load()` in reverse to be consistent with properties overriding
 * Allow prefixing library names with `:` to have `Loader` consider them as filenames with prefix and suffix already included
 * Add `Loader.loadGlobal()` to load symbols globally as often required by Python libraries ([issue ContinuumIO/anaconda-issues#6401](https://github.com/ContinuumIO/anaconda-issues/issues/6401))

### April 11, 2019 version 1.5
 * Have `Parser` output `setter` as dummy parameter name for setter methods to clarify usage
 * Add `Indexer.strides(long... sizes)` and use as default strides when not specified by the user
 * Add `long...` constructors, getters, and setters to `CLongPointer` and `SizeTPointer` for convenience
 * Fix some `Generator` issues with `FunctionPointer` passed or returned `@ByPtrPtr`
 * Use ModiTect to compile `module-info.java` with JDK 8 and preserve backward compatibility
 * Add `platform.executable` and `platform.executablepath` properties to bundle executables and extract them with `Loader.load()`
 * Create symbolic links for all libraries preloaded by `Loader` as they get loaded to satisfy libraries like MKL
 * Prevent `ClassCastException` in `Loader` on illegal system properties ([issue #289](https://github.com/bytedeco/javacpp/issues/289))
 * Fix `Parser` not replacing all type names of the base class with `Info.flatten` ([issue #288](https://github.com/bytedeco/javacpp/issues/288))
 * Let `BuildMojo` return to the Maven project the detected host platform as `${javacpp.platform.host}`
 * Have `BuildMojo` output a JPMS friendly name for the platform and extension back to the Maven project as `${javacpp.platform.module}`
 * Add `Builder.clean` option to delete the `outputDirectory` before generating files
 * Let `Parser` pick up `Info` explicitly for all constructors by considering their names as functions ([issue #284](https://github.com/bytedeco/javacpp/issues/284))
 * Fix `Parser` not always generating files using the simple names of classes
 * Add a `BuildMojo.targetDirectories` parameter to allow setting multiple directories where to find generated Java files
 * Add `Parser` support for attributes appearing after `struct` declarations ([issue bytedeco/javacpp-presets#685](https://github.com/bytedeco/javacpp-presets/issues/685))
 * Fix `Parser` overlooking `Info` for constructors inside namespaces or templates ([issue #284](https://github.com/bytedeco/javacpp/issues/284))
 * Fix `Parser` applying some `Info.annotations` at the wrong place ([issue #284](https://github.com/bytedeco/javacpp/issues/284))
 * Make `Parser` behave the same with `@Properties` having only one out of `global` or `target` value set
 * Enhance `UniquePtrAdapter` with the ability to move pointers out with the `&&` operator
 * Let `Parser` map constructors also for abstract classes with `Info.virtualize`
 * Fix `Parser` taking the `global` package as the `target` package even when both are set
 * Consider `@Properties(global=..., helper=...)` class names without "." as relative to `target` ([pull bytedeco/javacpp-presets#669](https://github.com/bytedeco/javacpp-presets/pull/669))
 * Use regex in `Parser` to translate more Doxygen commands into Javadoc tags ([pull #278](https://github.com/bytedeco/javacpp/pull/278) and [pull #281](https://github.com/bytedeco/javacpp/pull/281))
 * Do not let `Parser` map `operator=()` when prefixing container name with `const ` ([pull #280](https://github.com/bytedeco/javacpp/pull/280))

### January 11, 2019 version 1.4.4
 * Allow users to override platform properties via system properties starting with "org.bytedeco.javacpp.platform."
 * Have `BuildMojo` output its class path back to the Maven project as `${javacpp.platform.artifacts}`
 * Fix potential `NullPointerException` in `Loader.findResources()` under the bootstrap class loader
 * Add `size()` and `stride()` methods to `Indexer` for convenience
 * Let `Parser` skip over C++11 style `{ ... }` member initializer lists ([pull bytedeco/javacpp-presets#642](https://github.com/bytedeco/javacpp-presets/pull/642))
 * Fix `Parser` not picking up `Info` for cast `operator` declarations with `const`, `&`, or `*` ([issue bytedeco/javacpp-presets#377](https://github.com/bytedeco/javacpp-presets/issues/377))
 * Add validation for `Builder.environmentVariables` to prevent `NullPointerException` in `executeCommand()`
 * Update `android-arm-clang.properties` and `android-x86-clang.properties` to API level 21 (Android 5.0) for consistency and forward compatibility
 * Replace calls to `Class.getResource()` with `Loader.findResource()` to work around issues with JPMS ([pull #276](https://github.com/bytedeco/javacpp/pull/276))
 * Enhance `Loader.findResources()` with `Class.getResource()` and search among parent packages
 * Take longest common package name among all user classes for the default output path of `Builder`
 * Add `Bfloat16Indexer` to access `short` arrays as `bfloat16` floating point numbers
 * When `Indexer.sizes.length != 3`, return -1 for `rows()`, `cols()`, `width()`, `height()`, and `channels()` ([pull #275](https://github.com/bytedeco/javacpp/pull/275))
 * Synchronize `Loader.cacheResources()` on `Runtime` to avoid `OverlappingFileLockException` with multiple class loaders ([issue bytedeco/javacpp-presets#650](https://github.com/bytedeco/javacpp-presets/issues/650))
 * Annotate `BuildMojo` as `threadSafe`
 * Fix `Generator` errors for `@StdString` and other `@Adapter` on `@Virtual` return values
 * Use simple name from `@Properties(target=..., global=...)` class as default for `@Platform(library=...)` name
 * Make sure `Generator` does not use `position` of `@Opaque Pointer` output parameters with `@Adapter` ([pull bytedeco/javacpp-presets#642](https://github.com/bytedeco/javacpp-presets/pull/642))
 * Prevent `Builder` from trying to use `Pointer` as library name for the output
 * Add `Builder.generate` option and corresponding `ParseMojo` to prioritize parsing header files
 * Fix `Parser` mapping of `const` function pointer variable declarations
 * Enhance `Loader.cacheResource()` with support for HTTP connections
 * Add `module-info.java` and create a multi-release JAR to comply with JPMS ([pull #252](https://github.com/bytedeco/javacpp/pull/252))
 * Prevent `Parser` from outputting twice the same `Info.javaText` by using it as declaration signature
 * Provide default `Info` for `std::string*` and `std::wstring*` mapping to `BytePointer`, and `CharPointer` and `IntPointer`
 * Ensure `Parser` skips over attributes of friend declarations or function definitions that are not used
 * Do not let `Parser` output `@Override` when overloading a method with less parameters using default arguments
 * Allow `Builder` to execute `javac` and `java` for convenience, and remove "." from class path ([issue #192](https://github.com/bytedeco/javacpp/issues/192))
 * Enhance support for `java.nio.Buffer` by taking into account `offset`, `position`, `limit`, and `capacity` on function calls
 * Make sure `Parser` always uses the short version of identifiers for Java class declarations
 * Prevent `Parser` from inheriting constructors with `using` when not accessible or of incomplete template instances
 * Add default `Info` to map `noexcept` attribute from C++11 to `@NoException` annotation
 * Fix `Parser` failures on variadic function template arguments `...` and destructor attributes ([pull bytedeco/javacpp-presets#622](https://github.com/bytedeco/javacpp-presets/pull/622))
 * Add `@Properties(global=...)` value to allow `Parser` to target Java packages ([pull #252](https://github.com/bytedeco/javacpp/pull/252))
 * Fix `Generator` output for `@Const` parameters of function pointers

### October 15, 2018 version 1.4.3
 * Add support for `linux-mips64el` with `linux-mips64el.properties` ([pull #268](https://github.com/bytedeco/javacpp/pull/268))
 * Enhance `Generator` with `@ByPtr` for primitive types and `@NoException` for `FunctionPointer` methods
 * Add `BooleanPointer` and `BooleanIndexer` to access arrays of boolean values with `sizeof(jboolean) == 1`
 * Let `Parser` skip over `static_assert()` declarations of C++11
 * Fix `android-arm-clang.properties` and `android-x86-clang.properties` for builds with NDK r18 ([pull #263](https://github.com/bytedeco/javacpp/pull/263))
 * Add to default `InfoMap` missing `int` value type and `IntPointer` pointer type for `wchar_t`
 * Add `Loader.getLoadedLibraries()` method for debugging purposes and fix flaky `BuilderTest` ([issue #245](https://github.com/bytedeco/javacpp/issues/245))
 * Call `PointerScope.attach()` as part of `Pointer.deallocator()`, instead of `init()`, to support custom deallocators as well
 * Fix `Parser` failing when a value of an `std::pair` basic container is also an `std::pair` ([issue bytedeco/javacpp-presets#614](https://github.com/bytedeco/javacpp-presets/issues/614))
 * Fix build issues with `android-arm` and recent versions of the NDK ([pull #256](https://github.com/bytedeco/javacpp/pull/256))
 * Add `platform.preloadresource` property to be able to preload libraries from other Java packages
 * Make `Builder` accept multiple options for `platform.link.prefix` and `platform.link.suffix` ([pull #250](https://github.com/bytedeco/javacpp/pull/250))
 * Let `Loader` rename JNI libraries when "already loaded in another classloader" ([issue deeplearning4j/deeplearning4j#6166](https://github.com/deeplearning4j/deeplearning4j/issues/6166))
 * Add new `@CriticalRegion` annotation to allow zero-copy access to data of Java arrays ([pull #254](https://github.com/bytedeco/javacpp/pull/254))
 * Allow `Builder` to create links for resource libraries even when no Java classes are built
 * Fix `Loader.cacheResource()` creating a subdirectory named "null" when caching a top-level file
 * Update `README.md` with references to newly published [Basic Architecture of JavaCPP](https://github.com/bytedeco/javacpp/wiki/Basic-Architecture) and [Mapping Recipes for C/C++ Libraries](https://github.com/bytedeco/javacpp/wiki/Mapping-Recipes)
 * Prevent `Parser` from appending annotations to setter methods of variables and for basic containers to satisfy the `Generator`
 * Have `Parser` wrap the `insert()` and `erase()` methods of basic containers to allow modifying lists and sets
 * Let `Parser` create mutable instances of map containers without `const ` prefix ([issue bytedeco/javacpp-presets#595](https://github.com/bytedeco/javacpp-presets/issues/595))
 * Fix `Parser` sometimes ignoring `define` of `const ` containers ([pull bytedeco/javacpp-presets#547](https://github.com/bytedeco/javacpp-presets/pull/547))
 * Explain the purpose of the `intern()` methods generated for Java enums
 * Clarify that `Loader.load()` can throw `UnsatisfiedLinkError` when interrupted
 * Synchronize `Loader.loadLibrary()` to fix potential race condition ([pull #246](https://github.com/bytedeco/javacpp/pull/246))

### July 17, 2018 version 1.4.2
 * Add `Loader.getJavaVM()` method to get the JNI `JavaVM` object as required to initialize some libraries
 * Fix `Parser` from outputting accessors not available with `std::forward_list` or `std::list`
 * Use `pthread_setspecific()` in `Generator` to detach automatically native threads on exit for Android ([pull #243](https://github.com/bytedeco/javacpp/pull/243))
 * Fix issues with anonymous classes by calling `getEnclosingClass()` instead of `getDeclaringClass()`
 * Add `android-arm-clang.properties`, `android-arm64-clang.properties`, `android-x86-clang.properties` and `android-x86_64-clang.properties`
 * Search in `linkpath` before `preloadpath` to avoid copying or loading unwanted libraries
 * Fix `Builder` not bundling libraries containing a `#` fragment only useful at load time
 * Make `Parser` take into account implicit constructors even when inheriting some with `using` declarations
 * Pick up `Parser` translation of enum and macro expressions from `Info.javaNames`
 * Let `Parser` define `Info.pointerTypes` also for partially specialized templates with default arguments
 * Tweak `Pointer.formatBytes()` to increase the number of digits returned ([issue #240](https://github.com/bytedeco/javacpp/issues/240))
 * Enhance `InfoMap` and `StringAdapter` with default mappings and casts for `std::wstring`
 * Templatize `StringAdapter` to allow other character types like `wchar_t` and add corresponding `@StdWString` annotation
 * Prevent `Loader` from creating symbolic links to rename libraries, which does not always work
 * Fix memory leak that occurs with "org.bytedeco.javacpp.nopointergc" ([issue #239](https://github.com/bytedeco/javacpp/issues/239))
 * Make `Generator` use `GENERIC_EXCEPTION_TOSTRING` macro on `GENERIC_EXCEPTION_CLASS` instead of the default `what()`
 * Fall back on Android-friendly `System.loadLibrary()` in `Loader.load()` instead of "java.library.path" ([issue bytedeco/javacv#970](https://github.com/bytedeco/javacv/issues/970))
 * Add to Java enums an `intern()` method and use it in `toString()` to return non-null strings
 * Add `PointerScope` to manage more easily the resources of a group of `Pointer` objects
 * Fix `Parser` failing on `const void*&` or similar function arguments, and on constructors of class templates
 * Add `Info.skipDefaults` to have the `Parser` ignore default function arguments and prevent method overloading
 * Accelerate copy and extraction of resources by using larger buffers for file operations
 * Fix `Parser` incorrectly referring to function arguments with impossibly qualified names
 * Allow using `new Info().enumerate()` to map all C++ `enum` to Java `enum` types by default
 * Fix `Parser` issues surrounding enum classes, anonymous namespaces, and pure virtual classes
 * Avoid `synchronized` on first call to `physicalBytes()` in `Pointer.deallocator()` to reduce contention ([pull #232](https://github.com/bytedeco/javacpp/pull/232))

### March 29, 2018 version 1.4.1
 * Enhance `Loader.createLibraryLink()` by allowing to create symbolic links in other directories
 * Fix `Parser` failing on `enum` declarations where the first line is a macro ([issue #230](https://github.com/bytedeco/javacpp/issues/230))
 * Make call to `Pointer.physicalBytes()` thread safe and remove lock ([issue #231](https://github.com/bytedeco/javacpp/issues/231))
 * Add `Info.enumerate` to let `Parser` map C++ enum classes to Java enum types ([issue #108](https://github.com/bytedeco/javacpp/issues/108))
 * Prevent `Loader` from loading twice copies of the same DLL ([issue deeplearning4j/deeplearning4j#4776](https://github.com/deeplearning4j/deeplearning4j/issues/4776))
 * Add a `BuildMojo.targetDirectory` parameter to set a directory containing Java files generated by `buildCommand`
 * Fix missing `jnijavacpp.cpp` when processing classes from different packages ([issue #228](https://github.com/bytedeco/javacpp/issues/228))
 * Enhance `Loader.addressof()` by making it try harder to find symbols on Linux and Mac OS X
 * Add `get()` and `toString()` methods to basic containers defined in `Parser`
 * Fix `Parser` ignoring `Info.define` and other information for macros actually defined
 * Fix `SharedPtrAdapter` and `UniquePtrAdapter` failing to take ownership of temporary objects
 * Fix properties for `android-arm64` and `android-x86_64` platforms that need API level 21
 * Add "org.bytedeco.javacpp.pathsfirst" system property to let users search "java.library.path", etc before the class path
 * Add `Parser` support for `_Bool`, `_Complex`, `_Imaginary`, `complex`, `imaginary` types from C99
 * Fix `Generator` incorrectly splitting type names for template arguments containing function types
 * Fix `NullPointerException` in `Builder` when copying resources for static libraries
 * Let `Generator` pick up `@NoException` annotations from super classes as well
 * Add `-stdlib=libc++` option to iOS properties, required by `clang++` to support C++11 ([pull #221](https://github.com/bytedeco/javacpp/pull/221))
 * Make it possible to define read-only containers with `Parser` by prepending `const ` ([issue #223](https://github.com/bytedeco/javacpp/issues/223))
 * Fix `Parser` failure of variable or function declarations on names starting with `::`, among other various small issues
 * Access elements of basic containers defined in `Parser` with `at()` instead of `operator[]` ([issue #223](https://github.com/bytedeco/javacpp/issues/223))
 * Add third element to `@Const` annotation to support `virtual const` functions ([pull #224](https://github.com/bytedeco/javacpp/pull/224))
 * Create more symbolic links to libraries preloaded by `Loader` to satisfy libraries like MKL
 * Work around in `Builder` the inability to pass empty arguments on Windows
 * Catch more exceptions that can occur in `Loader` when caching resources ([pull #226](https://github.com/bytedeco/javacpp/pull/226))
 * Add `.a` as an allowed library extension for iOS so they can get bundled
 * Fix `Parser` failing on variables with direct list initialization `{ ... }` ([issue #223](https://github.com/bytedeco/javacpp/issues/223))
 * Allow `Parser` to map and cast function pointers to `Pointer`

### January 16, 2018 version 1.4
 * Output to log all commands executed for `Builder.buildCommand` via `ProcessBuilder`
 * Switch architecture in `android-arm.properties` to ARMv7-A
 * Fix `Parser` not producing `@Cast` annotations for types with `Info.cast()` on `operator()`, as well as failing on `using operator` statements
 * Fix `Parser` issue with multiple container types (`std::vector`, etc) getting mixed up when mapped to adapters (`@StdVector`, etc)
 * Fix "Negative Buffer Capacity" errors happening in subclasses on `Pointer.asBuffer()` ([issue deeplearning4j/deeplearning4j#4061](https://github.com/deeplearning4j/deeplearning4j/issues/4061))
 * Prevent `JNI_OnLoad()` from failing when `Loader.putMemberOffset()` cannot find a class
 * Throw clear error message when `Loader.load()` gets called on a class not supporting current platform
 * Create symbolic links to libraries preloaded by `Loader` as needed on Mac for renamed libraries
 * Update platform properties to support recent versions of the Android NDK
 * Fix `Generator` issues with `@ByPtrPtr` return of `String` or `Pointer` types ([issue bytedeco/javacpp-presets#499](https://github.com/bytedeco/javacpp-presets/issues/499))
 * Define `clear()`, `empty()`, `pop_back()`, and `push_back()` for resizable basic containers in `Parser` ([issue bytedeco/javacv#659](https://github.com/bytedeco/javacv/issues/659))
 * Add "nowarnings" option for the `@Platform(compiler=...)` value to suppress all warnings
 * Have `Builder` generate base JNI functions into `jnijavacpp.cpp` for better iOS support ([issue #213](https://github.com/bytedeco/javacpp/issues/213))
 * Output single value setters for containers in `Parser` to avoid surprises ([issue #217](https://github.com/bytedeco/javacpp/issues/217))
 * Add `Parser` support for C++11 `using` declarations inheriting constructors ([issue bytedeco/javacpp-presets#491](https://github.com/bytedeco/javacpp-presets/issues/491))
 * Fix compiler error when defining `std::set` or `std::unordered_set` with `Parser`
 * Make `Parser` honor `Info.skip()` for enumerators and function pointers as well
 * Add `LoadEnabled` interface to allow classes to modify their `ClassProperties` at runtime
 * Move `sizeof()` and `offsetof()` data to global variables to prevent `StackOverflowError` in `JNI_OnLoad()` ([issue bytedeco/javacpp-presets#331](https://github.com/bytedeco/javacpp-presets/issues/331))
 * Propagate within `Parser` type information from macros to other macros referencing them
 * Add support for `JNI_OnLoad_libname()` naming scheme for iOS via new `platform.library.static=true` property
 * Improve the clarity of error messages on `Parser` failures
 * Fix `Parser` issues with multiple `typedef` declarations in a single statement
 * Require `Info.annotations("@Name")` to pick up alternate names from attributes
 * Add `@Platform(exclude=...)` annotation value to remove header files from inherited `@Platform(include=...`
 * Fix a few issues with `Parser`, including missing `PointerPointer` member setters ([issue bytedeco/javacpp-presets#478](https://github.com/bytedeco/javacpp-presets/issues/478))
 * Fix potential race conditions and various issues with `Loader` that could prevent libraries like MKL from working properly
 * Add `Loader.addressof()` to access native symbols, usable via optional `ValueGetter/ValueSetter` in `FunctionPointer`
 * Add `BuildEnabled` interface to allow `InfoMapper` classes to participate in the build
 * Try to use symbolic links in `Loader.load()` for output filenames specified with the `#` character (useful for libraries like MKL)
 * Fix `Parser` incorrectly resolving type definitions with classes of the same name in parent namespaces
 * Fix `Generator` compile errors for `const` template types of `@Adapter` classes using the `@Cast` annotation
 * Call `Loader.createLibraryLink()` when executing the user specified `Builder.buildCommand` as well
 * Introduce new `platform.extension` property to manage more than one set of binaries per platform
 * Catch `SecurityException` in `Loader.getCacheDir()` ([pull #198](https://github.com/bytedeco/javacpp/pull/198))

### July 25, 2017 version 1.3.3
 * Call `malloc_trim(0)` after `System.gc()` on Linux to make sure memory gets released ([issue bytedeco/javacpp-presets#423](https://github.com/bytedeco/javacpp-presets/issues/423))
 * Make public the `Pointer.formatBytes()` and `Pointer.parseBytes()` static methods
 * Use `Integer.decode()` instead of `parseInt()` on integer literals to support hexadecimal and octal numbers
 * Add `Builder.encoding` option to let users specify I/O character set name ([issue bytedeco/javacpp-presets#195](https://github.com/bytedeco/javacpp-presets/issues/195))
 * Prevent race condition that could occur in `Loader.cacheResource()` ([pull #188](https://github.com/bytedeco/javacpp/pull/188))
 * Fix potential compile errors with Android caused by superfluous `typedef` from `Generator` ([issue #186](https://github.com/bytedeco/javacpp/issues/186))
 * Fix `Parser` translation of strings containing the "::" subsequence ([issue #184](https://github.com/bytedeco/javacpp/issues/184))
 * Prevent `Parser` from overwriting target classes when nothing was parsed
 * Fix `Parser` error on member variables with initializers plus `Info.skip()` ([issue #179](https://github.com/bytedeco/javacpp/issues/179))
 * Fix `Parser` incorrectly recognizing values as pointers when `const` is placed after type ([issue #173](https://github.com/bytedeco/javacpp/issues/173))
 * Add `Parser` support for C++11 `using` declarations that act as `typedef` ([issue #169](https://github.com/bytedeco/javacpp/issues/169))
 * Let `Parser` accept variables initialized with parentheses ([issue #179](https://github.com/bytedeco/javacpp/issues/179))
 * Fix `Parser` confusion between attributes and namespace-less templates ([issue #181](https://github.com/bytedeco/javacpp/issues/181))
 * Fix issue with `Loader.getCallerClass()` when a `SecurityManager` cannot be created ([issue #176](https://github.com/bytedeco/javacpp/issues/176))
 * Make it possible to rename enumerators of C++ `enum class` ([issue #180](https://github.com/bytedeco/javacpp/issues/180))
 * Make the arbitrary resources available to process executed with `Builder.buildCommand` via the `BUILD_PATH` environment variable
 * Prevent `Parser` from outputting setters for `const` member pointers
 * Add support for arrays of function pointers
 * Let users bundle arbitrary resources, have them extracted in cache, and used as `include` or `link` paths ([pull #43](https://github.com/bytedeco/javacpp/pull/43))
 * Fix potential formatting issues with `OutOfMemoryError` thrown from `Pointer`
 * Fix `Loader.getCallerClass()` not using the output from the `SecurityManager` ([pull #175](https://github.com/bytedeco/javacpp/pull/175))
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
