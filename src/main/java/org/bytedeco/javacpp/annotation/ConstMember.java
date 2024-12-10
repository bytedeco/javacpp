package org.bytedeco.javacpp.annotation;

import java.lang.annotation.*;

/**
 * Annotation allows to map (mainly virtual) functions, which are const members (e.g.: void func() const)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConstMember {
}
