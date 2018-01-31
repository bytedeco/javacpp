package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A shortcut annotation to {@link Cast} that simply adds {@code const} to the parameter type, function or class.
 *
 * <p><ul>
 * <li>For parameter type, the first one is for a value like {@code const char*} and the second for a pointer like {@code char const *}.
 * <li>For function, the first and third one are used. The first is applied to the return value/pointer.
 * The third one determines whether the function is {@code const} or not. For compatible problem, we keep the third value empty.
 * <li>For class, only the first one is used, if it is {@code true}, it means the functions are all {@code const}
 * </ul></p>
 *
 * @see {@link org.bytedeco.javacpp.tools.Generator}
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface Const {
    /** If {@code true}, applies {@code const} to the value and to the pointer, respectively. */
    boolean[] value() default {true, false};
}
