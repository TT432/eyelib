package io.github.tt432.eyelibmolang.util;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author TT432
 */
@NullMarked
@Target(ElementType.METHOD)
/** @author TT432 */
public @interface CalledByGeneratedMethod {
}