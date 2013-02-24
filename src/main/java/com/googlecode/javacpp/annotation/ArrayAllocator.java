package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import com.googlecode.javacpp.Pointer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating that a method should behave like an array allocator.
 * However, methods with signature <tt>native void allocateArray(int)</tt> are
 * recognized as array allocators even without annotation. This behavior can be
 * changed by annotating the method with the {@link Function} annotation.
 * <p>
 * In a nutshell, an array allocator uses the C++ <tt>new[]</tt> operator, and
 * initializes the {@link Pointer#address} as well as the {@link Pointer#deallocator}
 * with <tt>NativeDeallocator</tt>, based on the <tt>delete[]</tt> operator, if
 * not additionally annotated with {@link NoDeallocator}.
 *
 * @see Pointer#init(long, int, long)
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ArrayAllocator { }