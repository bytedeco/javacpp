package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.tools.Generator;

/**
 * A shortcut annotation to {@link Cast} that simply adds {@code const} to the parameter type, function, or class.
 *
 * <p><ul>
 * <li>For a parameter type, the first element is for a value like {@code const char*} and the second for a pointer like {@code char const *}.
 * <li>For a function, the first, second, and third ones are used. The first two are applied to the return value/pointer.
 *     The third one determines whether the function is {@code const} or not. For backward compatibility, we keep the third element empty.
 * <li>For a class, only the first one is used, and if it is {@code true}, it means all the functions are {@code const}.
 *     Can also be declared on a {@link FunctionPointer} in the case of {@code const} functions.
 * </ul></p>
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface Const {
    /** If {@code true}, applies {@code const} to the value and to the pointer, respectively. */
    boolean[] value() default {true, false};
}
