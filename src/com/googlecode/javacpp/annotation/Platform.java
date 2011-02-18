package com.googlecode.javacpp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Platform {
    String[] value()       default {};
    String[] not()         default {};
    String[] define()      default {};
    String[] include()     default {};
    String[] cinclude()    default {};
    String[] includepath() default {};
    String[] options()     default {};
    String[] linkpath()    default {};
    String[] link()        default {};
    String[] preloadpath() default {};
    String[] preload()     default {};
}
