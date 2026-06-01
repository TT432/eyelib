package io.github.tt432.eyelibmodel.entity;

import io.github.tt432.eyelibmodel.Model;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
@FunctionalInterface
public interface ModelResolver {
    @Nullable
    Model resolve(String modelName);
}