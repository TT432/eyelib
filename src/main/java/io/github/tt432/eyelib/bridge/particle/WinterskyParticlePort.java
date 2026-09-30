package io.github.tt432.eyelib.bridge.particle;

import io.github.tt432.eyelib.bridge.particle.adapter.WinterskyParticleFileLoader;
import io.github.tt432.eyelib.bridge.particle.adapter.WinterskySceneManager;

import java.util.List;

/**
 * wintersky 粒子预览 Port —— 隔离 application 对 adapter 具体类的直接依赖（ADR-0018 I-5）。
 */
public interface WinterskyParticlePort {
    /** 资源包内全部已发现的 Bedrock 粒子 identifier（排序）。 */
    static List<String> identifiers() {
        return WinterskyParticleFileLoader.identifiers();
    }

    /** 在世界坐标 spawn 粒子效果；true 表示 emitter 已创建并 start。 */
    static boolean spawn(String identifier, double x, double y, double z) {
        return WinterskySceneManager.spawn(identifier, x, y, z);
    }

    /** 清空场景全部 emitter 并重建缓存。 */
    static void clear() {
        WinterskySceneManager.clear();
    }

    /** wintersky 材质 × 纹理 → 版本 RenderType（Object 存放，消费点按版本 //? 转型；ADR-0035 D2）。 */
    static Object renderType(String material, io.github.tt432.eyelib.util.PortResourceLocation texture) {
        return io.github.tt432.eyelib.bridge.particle.adapter.WinterskyRenderTypes.get(material, texture);
    }
}
