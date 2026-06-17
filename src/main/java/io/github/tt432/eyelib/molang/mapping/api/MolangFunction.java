package io.github.tt432.eyelib.molang.mapping.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将方法标记为 Molang 可调用函数。
 *
 * @author TT432
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MolangFunction {
    String value();

    String[] alias() default "";

    String description() default "";

    int specificity() default 0;

    int priority() default 0;

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Role {
        ParameterRole value();
    }

    enum ParameterRole {
        VISIBLE_ARG,
        RECEIVER,
        INJECTED_HOST,
        SPECIAL_ENGINE_ARG
    }
}