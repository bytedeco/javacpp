package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Builder;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacpp.Generator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes it possible to define more than one set of properties for each platform.
 * The effective set of properties are taken from all {@link Platform} values in
 * this annotation, but priority is given to values found later in the list, making
 * it possible to define a default set of properties as the first value of the array,
 * and specializing a smaller set of properties for each platform, subsequently.
 * <p>
 * A class with this annotation gets recognized as top-level enclosing class by
 * {@link Loader#getEnclosingClass(Class)}, with the same implications as with
 * the {@link Platform} annotation.
 * <p>
 * Additionally, it is possible to inherit properties from another class also
 * annotated with this annotation, and specialize further for the current class.
 *
 * @see Builder
 * @see Generator
 * @see Loader
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Properties {
    /** A list of classes from which to inherit properties. */
    Class[] inherit() default {};
    /** A list of properties for different platforms. */
    Platform[] value() default {};
}
