package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating that a method should behave like a value setter.
 * However, a pair of methods named {@code get()} and {@code put()}, one with a
 * return value, the other without, but with the same number of parameters, plus 1,
 * are recognized as a value getter/setter pair even without annotation. This behavior
 * can be changed by annotating the methods with the {@link Function} annotation.
 * <p>
 * A value setter must return no value, or its own {@link Class} as return type, while
 * its number of parameters must be greater than 0. The assigned value is assumed
 * to come from pointer dereference, but anything that follows the same syntax as the
 * assignment of a dereferenced pointer could potentially work with this annotation.
 * All but the last argument are considered as indices to access a value array.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ValueSetter { }