package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelibmodel.Model;


import io.github.tt432.eyelib.client.manager.ModelManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

/** @author TT432 */
@NullMarked
public final class ModelLookup {
    @Nullable
    public static Model get(String name) {
        return ModelManager.readPort().get(name);
    }

    public static Map<String, Model> all() {
        return ModelManager.readPort().getAllData();
    }
}
