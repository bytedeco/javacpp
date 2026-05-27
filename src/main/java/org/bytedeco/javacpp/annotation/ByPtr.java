package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an argument should get passed or returned by pointer. By default,
 * all {@link org.bytedeco.javacpp.Pointer} and array arguments get passed by pointer. Since it is
 * not used for any other purposes at the moment, this annotation has no effect.
 *
 * @see org.bytedeco.javacpp.tools.Generator
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface ByPtr { }