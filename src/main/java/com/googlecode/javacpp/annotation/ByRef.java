package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import com.googlecode.javacpp.FunctionPointer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an argument gets passed or returned by reference. When used
 * alongside {@link FunctionPointer}, the {@link Generator} passes the underlying
 * C++ function object (aka functor) instead of a function pointer.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface ByRef { }