package io.github.tt432.eyelibmolang.mapping.api;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author TT432
 */
@NullMarked
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/** @author TT432 */
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