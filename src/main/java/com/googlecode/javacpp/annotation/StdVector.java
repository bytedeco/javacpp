package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import com.googlecode.javacpp.Pointer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A shorthand for <tt>@Adapter("VectorAdapter<type>")</tt>.
 *
 * @see Adapter
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Adapter("VectorAdapter")
public @interface StdVector {
    /** The template type of <tt>VectorAdapter</tt>. If not specified, it is
     *  inferred from the value type of the {@link Pointer} or Java array. */
    String value() default "";
}