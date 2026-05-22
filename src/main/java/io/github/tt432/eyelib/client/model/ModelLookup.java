package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelibmodel.Model;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;


/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
