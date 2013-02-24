package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows using method arguments to call <tt>operator[]</tt> in some circumstances.
 * For example, a call like <tt>*this[i].foo(str)</tt> could be accomplished with
 * <tt>@Index native void foo(int i, String str)</tt>.
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
