package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A shorthand for {@code @Adapter("SharedPtrAdapter<type>")}.
 * We can also define the {@code SHARED_PTR_NAMESPACE} macro
 * to something like {@code boost} instead of the default {@code std}.
 *
 * @see Adapter
 * @see org.bytedeco.javacpp.tools.Generator
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Adapter("SharedPtrAdapter")
public @interface SharedPtr {
    /** The template type of {@code SharedPtrAdapter}. If not specified, it is
     *  inferred from the value type of the {@link org.bytedeco.javacpp.Pointer} or Java array. */
    String value() default "";
}