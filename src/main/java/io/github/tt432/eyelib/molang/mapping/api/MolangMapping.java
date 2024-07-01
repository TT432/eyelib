package io.github.tt432.eyelib.molang.mapping.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * usage:
 *
 * <pre>{@code
 * @MolangMapping("math")
 * class MolangMath {
 *     public static float max(float a, float b) {
 *         return a > b ? a : b;
 *     }
 * }
 * }</pre>
 * <p>
 * 此处的 max 可以对应 math.max(a, b) <br/>
 * 目标类的方法或字段都需要是 public static 才可调用
 *
 * @author TT432
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MolangMapping {
    /**
     * @return molang function or field name
     */
    String value();

    /**
     * @return will add MolangScope arg first if false
     */
    boolean pureFunction() default true;
}
