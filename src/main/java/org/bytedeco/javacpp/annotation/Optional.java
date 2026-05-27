package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A shorthand for {@code @Adapter("OptionalAdapter<type>")}.
 * We can also define the {@code OPTIONAL_NAMESPACE} macro
 * to something like {@code boost} instead of the default {@code std}.
 *
 * @see Adapter
 * @see org.bytedeco.javacpp.tools.Generator
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Adapter("OptionalAdapter")
public @interface Optional {
    /** The template type of {@code OptionalAdapter}. If not specified, it is
     *  inferred from the value type of the {@link org.bytedeco.javacpp.Pointer} or Java array. */
    String value() default "";
}
