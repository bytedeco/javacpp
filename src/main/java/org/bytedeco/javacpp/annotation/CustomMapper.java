package org.bytedeco.javacpp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allows to implement custom jni-mappings for specific methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CustomMapper {

    /**
     * Custom mapping logic, used only when filePath is not set.
     *
     * @return A String which will be directly used for the mapping.
     */
    String customMapping() default "// do Nothing";

    /**
     * FilePath of the file, which contains the mapping.
     *
     * @return The filePath of the file, which contains the mapping.
     */
    String filePath() default "";

    String deallocatorName() default "";

    /**
     * @return True if the CType of the parameters shall be used instead of the jType, to feed the calling function.
     */
    boolean passCTypeParams() default false;

    /**
     *
     * @return True if the parameter of the function shall be dereferenced.
     */
    boolean dereferenceParams() default false;
}