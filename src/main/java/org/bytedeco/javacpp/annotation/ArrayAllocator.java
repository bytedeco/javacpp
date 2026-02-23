package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating that a method should behave like an array allocator.
 * However, methods with signature {@code native void allocateArray(int)} are
 * recognized as array allocators even without annotation. This behavior can be
 * changed by annotating the method with the {@link Function} annotation.
 * <p>
 * In a nutshell, an array allocator uses the C++ {@code new[]} operator, and
 * initializes the {@link org.bytedeco.javacpp.Pointer#address} as well as the {@link org.bytedeco.javacpp.Pointer#deallocator}
 * with {@code NativeDeallocator}, based on the {@code delete[]} operator, if
 * not additionally annotated with {@link NoDeallocator}.
 *
 * @see org.bytedeco.javacpp.Pointer#init(long, long, long, long)
 * @see org.bytedeco.javacpp.tools.Generator
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ArrayAllocator { }
