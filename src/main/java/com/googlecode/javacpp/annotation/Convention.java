package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.FunctionPointer;
import com.googlecode.javacpp.Generator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the calling convention of a {@link FunctionPointer}.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Convention {
    String value();
}