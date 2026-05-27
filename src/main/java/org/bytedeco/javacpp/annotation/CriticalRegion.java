package org.bytedeco.javacpp.annotation;

import java.lang.annotation.*;

/**
 * In some methods, {@link org.bytedeco.javacpp.tools.Generator} will generate code to transfer arrays from the
 * JVM to native code using the {@code Get/Release<primitivetype>ArrayElements} methods.
 * However these methods copy the underlying data. With this annotation, the generated
 * code will always call the {@code Get/ReleasePrimitiveArrayCritical} methods instead.
 *
 * @see org.bytedeco.javacpp.tools.Generator
 *
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CriticalRegion { }
