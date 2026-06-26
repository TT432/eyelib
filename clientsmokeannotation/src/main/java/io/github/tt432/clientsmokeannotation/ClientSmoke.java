package io.github.tt432.clientsmokeannotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为客户端烟雾测试目标。
 *
 * @author TT432
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ClientSmoke {

    /**
     * 人类可读的测试说明，会写入测试报告。
     */
    String description() default "";

    /**
     * 执行优先级，数值越低越早执行。
     */
    int priority() default 0;

    /**
     * 可选的 mod id 命名空间，空字符串表示全局测试。
     */
    String modId() default "";
}
