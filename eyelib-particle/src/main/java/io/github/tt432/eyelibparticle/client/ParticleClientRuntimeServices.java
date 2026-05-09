package io.github.tt432.eyelibparticle.client;

import net.minecraft.client.Minecraft;

/**
 * Client-thread submission seam for particle client integration.
 */
@FunctionalInterface
public interface ParticleClientRuntimeServices {
    void submit(Runnable action);

    static ParticleClientRuntimeServices immediate() {
        return Runnable::run;
    }

    static ParticleClientRuntimeServices minecraftClient() {
        return action -> Minecraft.getInstance().submit(action);
    }
}
