package io.github.tt432.eyelib.mc.impl.molang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangQueryRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class MinecraftMolangQueryRuntime implements MolangQueryRuntime {
    @Override
    public float actorCount() {
        if (Minecraft.getInstance().level == null) {
            return 0;
        }
        return Minecraft.getInstance().level.getEntityCount();
    }

    @Override
    public float timeOfDay() {
        if (Minecraft.getInstance().level == null) {
            return 0;
        }
        return Minecraft.getInstance().level.getDayTime() / 24000F;
    }

    @Override
    public float moonPhase() {
        if (Minecraft.getInstance().level == null) {
            return 0;
        }
        return Minecraft.getInstance().level.getMoonPhase();
    }

    @Override
    public float partialTick() {
        return Minecraft.getInstance().timer.partialTick;
    }

    @Override
    public float distanceFromCamera(Object entity) {
        if (!(entity instanceof Entity e) || Minecraft.getInstance().cameraEntity == null) {
            return 0;
        }

        return Minecraft.getInstance().cameraEntity.distanceTo(e);
    }
}
