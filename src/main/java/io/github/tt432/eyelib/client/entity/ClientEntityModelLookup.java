package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelibimporter.model.Model;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ClientEntityModelLookup {
    @Nullable
    Model get(String modelName);
}

