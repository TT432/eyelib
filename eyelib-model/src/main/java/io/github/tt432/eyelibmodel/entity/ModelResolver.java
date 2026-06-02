package io.github.tt432.eyelibmodel.entity;

import io.github.tt432.eyelibmodel.Model;
import org.jspecify.annotations.Nullable;

/**
 * 模型解析接口，根据名称获取对应的模型数据。
 *
 * @author TT432
 */
@FunctionalInterface
public interface ModelResolver {
    @Nullable
    Model resolve(String modelName);
}