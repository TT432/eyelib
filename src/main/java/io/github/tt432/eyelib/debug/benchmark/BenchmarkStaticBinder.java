package io.github.tt432.eyelib.debug.benchmark;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;

/**
 * 静态实体基准场景的绑定辅助（ADR-0048）：把实体当前的 client entity 换成
 * 「去掉 animations / animation_controllers」的副本，使渲染走非动画静态通道。
 *
 * <p>仅在 {@code -Deyelib.benchmark.staticEntities=true} 时生效；绑定是固定的
 * {@code INSTANCE}，不会被注册表代际重新解析覆盖，重复调用对同一实体幂等（已绑定即跳过）。
 */
final class BenchmarkStaticBinder {
    private BenchmarkStaticBinder() {
    }

    /** 绑定单个实体（FBO 场景的 detached 实体用）。 */
    static void stripEntity(Entity entity) {
        if (!ClientBenchmarkConfig.staticEntities()) {
            return;
        }
        RenderData<?> cap = RenderData.peekComponent(entity);
        if (cap == null) {
            return;
        }
        ClientEntityComponent component = cap.getClientEntityComponent();
        if (component.bindingKind() == ClientEntityComponent.BindingKind.INSTANCE) {
            return;
        }
        BrClientEntity current = component.getClientEntity();
        if (current == null) {
            return;
        }
        component.bindInstance(strip(current));
    }

    /** 绑定当前世界所有已渲染实体（WORLD 场景用；已绑定的直接跳过）。 */
    static void stripLevel(Minecraft minecraft) {
        if (!ClientBenchmarkConfig.staticEntities() || minecraft.level == null) {
            return;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            stripEntity(entity);
        }
    }

    private static BrClientEntity strip(BrClientEntity ce) {
        return new BrClientEntity(
                ce.identifier() + "_static",
                ce.min_engine_version(),
                ce.materials(),
                ce.textures(),
                ce.geometry(),
                Map.of(),
                List.of(),
                ce.particle_effects(),
                ce.sound_effects(),
                ce.render_controllers(),
                Map.of(),
                ce.scripts(),
                ce.spawn_egg(),
                ce.item(),
                ce.toggles());
    }
}
