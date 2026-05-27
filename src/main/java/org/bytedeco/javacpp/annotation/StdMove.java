package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A shorthand for {@code @Adapter("MoveAdapter<type>")}.
 *
 * @see Adapter
 * @see org.bytedeco.javacpp.tools.Generator
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Adapter("MoveAdapter")
public @interface StdMove {
    /** The template type of {@code MoveAdapter}. If not specified, it is
     *  inferred from the value type of the {@link org.bytedeco.javacpp.Pointer} or Java array. */
    String value() default "";
}
