package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows using method arguments to call {@code operator[]} in some circumstances.
 * For example, a call like {@code *this[i].foo(str)} could be accomplished with
 * {@code @Index native void foo(int i, String str)}.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Index {
    /** The number of indices spread over the parameters, for multidimensional access. */
    int value() default 1;
}
