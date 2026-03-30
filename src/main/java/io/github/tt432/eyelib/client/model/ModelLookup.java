package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.manager.ModelManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelLookup {
    @Nullable
    public static Model get(String name) {
        return ModelManager.INSTANCE.get(name);
    }
}
