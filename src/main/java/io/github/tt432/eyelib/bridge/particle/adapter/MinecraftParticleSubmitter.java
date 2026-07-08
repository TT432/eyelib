package io.github.tt432.eyelib.bridge.particle.adapter;

import io.github.tt432.eyelib.particle.ParticleClientRuntimeServices;
import net.minecraft.client.Minecraft;

/**
 * 通过 Minecraft 客户端线程提交任务的 {@link ParticleClientRuntimeServices} 实现。
 */
/** @author TT432 */
public final class MinecraftParticleSubmitter implements ParticleClientRuntimeServices {
    public static final MinecraftParticleSubmitter INSTANCE = new MinecraftParticleSubmitter();

    private MinecraftParticleSubmitter() {
    }

    @Override
    public void submit(Runnable action) {
        Minecraft.getInstance().submit(action);
    }
}

