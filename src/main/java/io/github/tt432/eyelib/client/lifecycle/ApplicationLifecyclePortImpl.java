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
        // 粒子：实体离场时移除其动画登记的粒子发射器（looping 发射器不会自然过期）
        io.github.tt432.eyelib.capability.RenderData<?> renderData =
                io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper.getOrNull(
                        io.github.tt432.eyelib.capability.AttachableDataTypes.RENDER_DATA.get(), entity);
        if (renderData != null) {
            var spawner = io.github.tt432.eyelib.bridge.particle.ParticlePort.getSpawnAdapter();
            io.github.tt432.eyelib.client.particle.RootAnimationParticleSpawner.removeTracked(
                    renderData.getAnimationComponent(), spawner);
            io.github.tt432.eyelib.client.particle.RootAnimationParticleSpawner.flushOrphaned(
                    renderData.getAnimationComponent(), spawner);
        }
    }

    @Override
    public void onAddonParsed(BedrockAddon addon) {
        BedrockAddonRuntimeBridge.replaceFromAddon(addon);
    }
}
