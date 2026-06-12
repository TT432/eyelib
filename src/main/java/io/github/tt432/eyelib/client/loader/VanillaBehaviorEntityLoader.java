package io.github.tt432.eyelib.client.loader;

import org.jspecify.annotations.NullMarked;

import java.nio.file.Paths;

/**
 * 兼容旧调用点的 vanilla 行为实体加载入口；实际加载逻辑由 common 行为包边界维护。
 *
 * @author TT432
 */
@Deprecated(forRemoval = false)
@NullMarked
public final class VanillaBehaviorEntityLoader {
    private static volatile boolean loaded;

    private VanillaBehaviorEntityLoader() {
    }

    public static void ensureLoaded() {
        if (loaded) return;
        synchronized (VanillaBehaviorEntityLoader.class) {
            if (loaded) return;
            loadAndRegister();
            loaded = true;
        }
    }

    static void loadAndRegister() {
        io.github.tt432.eyelib.common.behavior.VanillaBehaviorEntityLoader.mergeIntoRegistry(Paths.get("run"));
    }
}
