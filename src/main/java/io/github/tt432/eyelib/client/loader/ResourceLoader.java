package io.github.tt432.eyelib.client.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

/** @author TT432 */
@NullMarked
public @interface ResourceLoader {
}
