package io.github.tt432.eyelib.molang.mapping.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author TT432
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MolangFunction {
    String value();

    String[] alias() default "";

    String description() default "";
}
