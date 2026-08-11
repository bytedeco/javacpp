package org.bytedeco.javacpp.annotation;

import java.lang.annotation.*;

/**
 * Indicates that {@link java.lang.String} should be mapped to array of UTF-16
 * code units ({@code char16_t*}) instead of byte array ({@code const char*}).
 *
 * @see org.bytedeco.javacpp.tools.Generator
 *
 * @author Alexey Rochev
 */

@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface AsUtf16 { }
