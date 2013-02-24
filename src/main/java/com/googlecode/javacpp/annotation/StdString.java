package com.googlecode.javacpp.annotation;

import com.googlecode.javacpp.Generator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A shorthand for <tt>@Cast("std::string&") @Adapter("StringAdapter")</tt>.
 *
 * @see Adapter
 * @see Generator
 *
 * @author Samuel Audet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Cast("std::string&") @Adapter("StringAdapter")
public @interface StdString { }