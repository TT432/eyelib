package io.github.tt432.eyelib.molang.port;

import org.jspecify.annotations.Nullable;

/**
 * 抽象实体接口。提供 Bedrock 查询属性和位置访问。
 *
 * @author TT432
 */
public interface PortEntity {
    /**
     * 按键查询 Bedrock 属性（如 {@code "is_baby"}、{@code "pos_x"}）；未知键返回 {@code null}。
     * 按键查询避免了为单个查询构建整表的开销。
     */
    @Nullable Object queryProperty(String key);

    float getX();

    float getY();

    float getZ();
}
