package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.manager.ModelManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelLookup {
    @Nullable
    public static Model get(String name) {
        return ModelManager.INSTANCE.get(name);
    }

    public static Map<String, Model> all() {
        return ModelManager.INSTANCE.getAllData();
    }
}
