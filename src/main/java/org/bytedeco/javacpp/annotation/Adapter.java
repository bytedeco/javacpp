package org.bytedeco.javacpp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.Buffer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.tools.Generator;

/**
 * Specifies a C++ class to act as an adapter between a target type and one or more adaptee type(s).
 * Instances of the adapter class are short-living and last only for the duration of a JNI call.
 * <p></p>
 * Six such C++ classes are made available by {@link Generator} to bridge a few differences, for instance,
 * between {@code std::string} and {@link String}, between {@code std::vector}, Java arrays of primitive
 * types, {@link Buffer}, and {@link Pointer}, or between {@code xyz::shared_ptr} and {@link Pointer}:
 * <blockquote>
 * <table width="80%">
 *     <thead>
 *         <tr>
 *             <th>Adapter class</th><th>Target type</th><th>Adaptee types</th><th>Helper annotation</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td valign="top">{@code VectorAdapter<P,T,A>}</td>
 *             <td valign="top">{@code std::vector<T,A>}</td>
 *             <td valign="top">{@code P}</td>
 *             <td valign="top">{@link StdVector}</td>
 *         </tr>
 *         <tr>
 *             <td valign="top">{@code StringAdapter<T>}</td>
 *             <td valign="top">{@code std::basic_string<T>}</td>
 *             <td valign="top">{@code char}<br>{@code signed char}<br>{@code unsigned char}<br>{@code wchar_t}<br>{@code unsigned short}<br>{@code signed int}</td>
 *             <td valign="top">{@link StdString}</td>
 *         </tr>
 *         <tr>
 *             <td valign="top">{@code SharedPtrAdapter<T>}</td>
 *             <td valign="top">{@code SHARED_PTR_NAMESPACE::shared_ptr<T>}</td>
 *             <td valign="top">{@code T}</td>
 *             <td valign="top">{@link SharedPtr}</td>
 *         </tr>
 *         <tr>
 *             <td valign="top">{@code UniquePtrAdapter<T,D>}</td>
 *             <td valign="top">{@code UNIQUE_PTR_NAMESPACE::unique_ptr<T,D>}</td>
 *             <td valign="top">{@code T}</td>
 *             <td valign="top">{@link UniquePtr}</td>
 *         </tr>
 *         <tr>
 *             <td valign="top">{@code MoveAdapter<T,D>}</td>
 *             <td valign="top">{@code T}</td>
 *             <td valign="top">{@code T}</td>
 *             <td valign="top">{@link StdMove}</td>
 *         </tr>
 *         <tr>
 *             <td valign="top">{@code OptionalAdapter<T>}</td>
 *             <td valign="top">{@code OPTIONAL_NAMESPACE::optional<T>}</td>
 *             <td valign="top">{@code T}</td>
 *             <td valign="top">{@link Optional}</td>
 *         </tr>
 *     </tbody>
 * </table>
 * </blockquote>
 * The helper annotations are shortcuts that infer the template type(s) of the adapter class from the Java
 * class they annotate.
 * <p></p>
 * When an argument of a method is annotated, an instance of the adapter class is created from
 * the Java object passed as argument, and this instance is passed to the C++ function, thus triggering
 * an implicit cast to the type expected by the function (usually a reference or pointer to the target type).
 * If the argument is also annotated with {@link Cast}, the adapter instance is cast to the type(s) specified
 * by the {@link Cast} annotation before being passed to the function.
 * <p></p>
 * When a method is annotated, an instance of the adapter is created from the value (usually a pointer or
 * reference to the target type) returned by the C++ function or by {@code new} if the method is an allocator.
 * If the method is also annotated with {@link Cast}, the value returned by the C++ function is
 * cast by value 3 of the {@link Cast} annotation, if any, before instantiation of the adapter.
 * Then a Java object is created from the adapter to be returned by the method.
 * <p></p>
 * Adapter classes must at least define the following public members:
 *  <ul>
 *  <li> For each adaptee type, a constructor accepting 3 arguments (or more if {@link #argc()} > 1): a pointer to a const value of the adaptee, a size, and the owner pointer
 *  <li> Another constructor that accepts a reference to the target type
 *  <li> A {@code static void deallocate(owner)} function
 *  <li> Overloaded cast operators to both the target type and the adaptee types, for references and pointers
 *  <li> {@code void assign(pointer, size, owner)} functions with the same signature than the constructors accepting 3 arguments
 *  <li> A {@code size} member variable for arrays accessed via pointer
 *  </ul>
 * To reduce further the amount of coding, this annotation can also be used on
 * other annotations, such as with {@link StdString}, {@link StdVector}, and {@link SharedPtr}.
 *
 * @see Generator
 *
 * @author Samuel Audet
 */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface Adapter {
    /** The name of the C++ adapter class. */
    String value();
    /** The number of arguments that {@link Generator} takes from the method as
     *  arguments to the adapter constructor. */
    int argc() default 1;
}
