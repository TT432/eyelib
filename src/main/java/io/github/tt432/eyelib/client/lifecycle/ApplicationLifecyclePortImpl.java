package io.github.tt432.eyelib.client.lifecycle;

import io.github.tt432.eyelib.bridge.ApplicationLifecyclePort;
import io.github.tt432.eyelib.client.render.AttachableItemRenderSetup;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.loader.BedrockAddonRuntimeBridge;
import io.github.tt432.eyelib.common.behavior.BehaviorPackAutoLoader;
import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 由 bridge 层反射创建实例后调 {@link ApplicationLifecyclePort#install} 注册。
 */
public final class ApplicationLifecyclePortImpl implements ApplicationLifecyclePort {
    ApplicationLifecyclePortImpl() {
    }

    @Override
    public void registerNetworkHandlers() {
        EyelibNetworkManager.register();
    }

    @Override
    public void loadBehaviorPacks(MinecraftServer server) {
        BehaviorPackAutoLoader.load(server);
    }

    @Override
    public void onTextureChanged() {
        RenderControllerComponent.onTextureStateChanged();
    }

    @Override
    public void onLivingEntityLeaveLevel(LivingEntity entity) {
        AttachableItemRenderSetup.clearEntity(entity);
    }

    @Override
    public void onAddonParsed(BedrockAddon addon) {
        BedrockAddonRuntimeBridge.replaceFromAddon(addon);
    }
}
