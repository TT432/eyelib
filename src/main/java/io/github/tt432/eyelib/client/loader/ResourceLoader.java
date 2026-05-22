package io.github.tt432.eyelib.client.loader;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author TT432
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@NullMarked
public @interface ResourceLoader {
}
