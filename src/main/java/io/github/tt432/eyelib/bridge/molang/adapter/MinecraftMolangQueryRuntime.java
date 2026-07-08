package io.github.tt432.eyelib.bridge.molang.adapter;

import io.github.tt432.eyelib.molang.mapping.api.MolangQueryRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
/**
 * Minecraft Molang 查询运行时实现。
 *
 * @author TT432
 */
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
        //? if <26.1 {
        return Minecraft.getInstance().level.getDayTime() / 24000F;
        //?} else {
        throw new UnsupportedOperationException("26.1 migration");
        //?}
    }

    @Override
    public float moonPhase() {
        if (Minecraft.getInstance().level == null) {
            return 0;
        }
        //? if <26.1 {
        return Minecraft.getInstance().level.getMoonPhase();
        //?} else {
        throw new UnsupportedOperationException("26.1 migration");
        //?}
    }

    @Override
    public float partialTick() {
        //? if <1.20.6
        return Minecraft.getInstance().getFrameTime();
        //? if >=1.20.6 && <26.1
        return Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        //? if >=26.1
        return Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
    }

    @Override
    public float distanceFromCamera(Object entity) {
        //? if <26.1 {
        if (!(entity instanceof Entity e) || Minecraft.getInstance().cameraEntity == null) {
        //?} else {
        if (!(entity instanceof Entity e) || Minecraft.getInstance().getCameraEntity() == null) {
        //?}
            return 0;
        }

        //? if <26.1 {
        return Minecraft.getInstance().cameraEntity.distanceTo(e);
        //?} else {
        return Minecraft.getInstance().getCameraEntity().distanceTo(e);
        //?}
    }
}

