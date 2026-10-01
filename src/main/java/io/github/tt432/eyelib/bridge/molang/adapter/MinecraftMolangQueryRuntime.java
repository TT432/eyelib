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
    /** 读取真实镜头旋转，包含第三人称前视和越肩镜头与玩家朝向的解耦。 */
    public static float renderCameraRotation(float axis, float partialTick) {
        //? if <26.1 {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return axis == 0 ? camera.getXRot() : camera.getYRot();
        //?} else {
        var entity = Minecraft.getInstance().getCameraEntity();
        return entity == null ? 0F : axis == 0 ? entity.getViewXRot(partialTick) : entity.getViewYRot(partialTick);
        //?}
    }

    /** 本帧最终渲染摄像机的位置，包含第三人称及相机模组的偏移。 */
    public static net.minecraft.world.phys.Vec3 renderCameraPosition() {
        //? if <26.1 {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        //?} else {
        return Minecraft.getInstance().gameRenderer.getMainCamera().position();
        //?}
    }

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

