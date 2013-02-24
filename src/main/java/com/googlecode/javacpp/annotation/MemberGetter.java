package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating that a method should behave like a member getter.
 * However, a pair of methods with the same name, one with a return value, the
 * other without, but with the same number of parameters, plus 1, are recognized
 * as a member getter/setter pair even without annotation. This behavior can be
 * changed by annotating the methods with the {@link Function} annotation.
 * <p>
 * A member getter either needs to return a value or accept a primitive array
 * as argument. The value returned is assumed to come from a member variable,
 * but anything that follows the same syntax as member variable access could
 * potential work with this annotation. For getters with a return value, all
 * arguments are considered as indices to access a member array.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MemberGetter { }