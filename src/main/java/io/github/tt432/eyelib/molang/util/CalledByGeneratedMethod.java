package io.github.tt432.eyelib.molang.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 标记由生成的解析器调用的方法。
 *
 * @author TT432
 */
@Target(ElementType.METHOD)
public @interface CalledByGeneratedMethod {
}