package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exception Mapper maps a C/C++ exception to the given java exception.
 * Will overwrite any existing exception-mappings.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ExceptionMapper {

    /**
     * The C/C++ exception to be mapped (e.g.: std::runtime_error).
     *
     * @return A String representation of the C/C++ exception to be mapped.
     */
    String cppException();

    /**
     * The corresponding java-exception.
     *
     * @return The corresponding java-exception.
     */
    Class<? extends Throwable> javaExceptionClass();
}

