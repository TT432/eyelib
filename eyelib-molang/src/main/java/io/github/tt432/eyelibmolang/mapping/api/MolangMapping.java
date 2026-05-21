package io.github.tt432.eyelibmolang.mapping.api;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将 public static 方法/字段注册为 Molang 可调用函数。
 * 例如 {@code @MolangMapping("math")} 定义的 max 方法可通过 {@code math.max(a, b)} 调用。
 *
 * @author TT432
 */
@NullMarked
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
/** @author TT432 */
public @interface MolangMapping {
    /** Molang 函数或字段名 */
    String value();

    /** 为 false 时 MolangScope 作为第一个参数传入 */
    boolean pureFunction() default true;
}