package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.manager.AnimationManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationLookup {
    @Nullable
    public static Animation<?> get(String name) {
        return AnimationManager.readPort().get(name);
    }

    public static Collection<String> names() {
        return AnimationManager.readPort().getAllData().keySet();
    }

    public static int size() {
        return AnimationManager.readPort().getAllData().size();
    }

    public static String managerName() {
        return AnimationManager.readPort().getManagerName();
    }
}
