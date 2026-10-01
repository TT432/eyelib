package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.bridge.molang.adapter.MinecraftMolangQueryRuntime;
import net.minecraft.world.phys.Vec3;

/** 摄像机查询的版本适配入口，供客户端 Molang 读取本帧渲染位置。 */
public interface CameraQueryPort {
    static Vec3 position() {
        return MinecraftMolangQueryRuntime.renderCameraPosition();
    }

    static float rotation(float axis, float partialTick) {
        return MinecraftMolangQueryRuntime.renderCameraRotation(axis, partialTick);
    }
}
