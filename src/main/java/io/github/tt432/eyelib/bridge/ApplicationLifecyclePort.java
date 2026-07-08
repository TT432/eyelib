package io.github.tt432.eyelib.bridge;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 应用生命周期 Port，由 application 层实现并注册。
 * bridge 事件处理器通过此 Port 转发 Forge/NeoForge 事件到 application，
 * 避免 bridge 直接依赖 application 类（规则 4）。
 */
public interface ApplicationLifecyclePort {
    AtomicReference<@org.jspecify.annotations.Nullable ApplicationLifecyclePort> INSTANCE = new AtomicReference<>();

    static void install(ApplicationLifecyclePort port) {
        INSTANCE.set(port);
    }

    @org.jspecify.annotations.Nullable
    static ApplicationLifecyclePort get() {
        return INSTANCE.get();
    }

    void registerNetworkHandlers();

    void loadBehaviorPacks(net.minecraft.server.MinecraftServer server);

    void onTextureChanged();

    void onAddonParsed(io.github.tt432.eyelib.importer.addon.BedrockAddon addon);
}
