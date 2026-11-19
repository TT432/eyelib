package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.render.controller.MeshTextures;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 烘焙预热的唯一实现（G8）：键枚举、状态查询、异步预热。
 * <p>
 * 两条入口共用同一套键：
 * <ul>
 *   <li>{@link #enumerate}（静态超集）：只凭 client entity 定义枚举，供「内容还没渲染过」的瞬发内容预热。
 *       运行期派生键（{@code clamped/}、{@code _color_mask/}、missing）推不出来，由渲染门控二次预热兜底。</li>
 *   <li>{@link #state}/{@link #warmData}（精确集）：走活组件的 {@link RenderOutputs}，键与实际绘制完全一致。</li>
 * </ul>
 * 三节点差异由 {@link ModelBakePort} 自己的重载吸收，本类不含 Stonecutter 条件分支。
 *
 * @author TT432
 */
final class RenderBake {

    private RenderBake() {
    }

    /** 单组合状态：已烘焙 → BAKED；预热失败 → UNAVAILABLE；否则 PENDING。 */
    static BakeState state(Model model, boolean isSolid, PortResourceLocation texture, PortResourceLocation meshTexture) {
        var mcTexture = ResourceLocationBridge.toMc(texture);
        var mcMesh = ResourceLocationBridge.toMc(meshTexture);
        if (ModelBakePort.twoSidePeekBakedModel(model, isSolid, mcTexture, mcMesh) != null) {
            return BakeState.BAKED;
        }
        return ModelBakePort.twoSideWarmFailed(model, mcTexture, mcMesh)
                ? BakeState.UNAVAILABLE
                : BakeState.PENDING;
    }

    static void warmAsync(BakeTarget target) {
        var mcTexture = ResourceLocationBridge.toMc(target.texture());
        var mcMesh = ResourceLocationBridge.toMc(target.meshTexture());
        ModelBakePort.twoSideWarmAsync(target.model(), mcTexture, mcMesh);
    }

    /** 后台异步预热（不触碰 GL）；已完成的组合在 {@link ModelBakePort} 内部按飞行中去重。 */
    static void warmAsync(Model model, PortResourceLocation texture, PortResourceLocation meshTexture) {
        warmAsync(new BakeTarget(model, texture, meshTexture));
    }

    /**
     * 静态可推断的预热目标超集。
     * <p>
     * 有 render controller 时取「每个几何 × 每张纹理」的笛卡尔积（RC 可按材质选纹理）；
     * 没有 RC 时按组件默认兜底规则（几何名 → 同名纹理 → 首个纹理）。
     */
    static List<BakeTarget> enumerate(BrClientEntity clientEntity) {
        List<BakeTarget> targets = new ArrayList<>();
        Set<String> distinctTextures = new LinkedHashSet<>(clientEntity.textures().values());
        for (Map.Entry<String, String> geometry : clientEntity.geometry().entrySet()) {
            Model model = ModelManager.INSTANCE.get(geometry.getValue());
            if (model == null) {
                continue;
            }
            PortResourceLocation mesh = MeshTextures.resolve(List.of(model), clientEntity, geometry.getKey());
            if (clientEntity.render_controllers().isEmpty()) {
                String texture = clientEntity.textures().get(geometry.getKey());
                if (texture == null && !distinctTextures.isEmpty()) {
                    texture = distinctTextures.iterator().next();
                }
                if (texture == null) {
                    continue;
                }
                PortResourceLocation location = PortResourceLocation.parse(texture);
                targets.add(new BakeTarget(model, location, mesh != null ? mesh : location));
                continue;
            }
            for (String texture : distinctTextures) {
                PortResourceLocation location = PortResourceLocation.parse(texture);
                targets.add(new BakeTarget(model, location, mesh != null ? mesh : location));
            }
        }
        return targets;
    }

    /** 活组件的整体就绪状态：任一 PENDING 即 PENDING；否则任一 UNAVAILABLE 即 UNAVAILABLE；否则 BAKED。 */
    static BakeState dataState(@Nullable Entity host, RenderData<?> cap) {
        boolean pending = false;
        boolean unavailable = false;
        for (ModelComponent modelComponent : cap.getModelComponents()) {
            Model model = modelComponent.getModel();
            if (model == null) {
                continue;
            }
            RenderOutputs.RenderOutput output = RenderOutputs.resolve(host, modelComponent);
            if (output == null) {
                continue;
            }
            PortResourceLocation mesh = modelComponent.getMeshTexture() != null
                    ? modelComponent.getMeshTexture() : output.texture();
            switch (state(model, output.isSolid(), output.texture(), mesh)) {
                case PENDING -> pending = true;
                case UNAVAILABLE -> unavailable = true;
                case BAKED -> {
                }
            }
        }
        if (pending) {
            return BakeState.PENDING;
        }
        return unavailable ? BakeState.UNAVAILABLE : BakeState.BAKED;
    }

    /** 预热活组件的全部精确键，返回提交的目标数。 */
    static int warmData(@Nullable Entity host, RenderData<?> cap) {
        List<BakeTarget> targets = targets(host, cap);
        for (BakeTarget target : targets) {
            warmAsync(target);
        }
        return targets.size();
    }

    /** 活组件的精确键（与渲染门控同源）：{@code output.texture()} + 组件的 meshTexture（缺省回落图层纹理）。 */
    static List<BakeTarget> targets(@Nullable Entity host, RenderData<?> cap) {
        List<BakeTarget> targets = new ArrayList<>(cap.getModelComponents().size());
        for (ModelComponent modelComponent : cap.getModelComponents()) {
            Model model = modelComponent.getModel();
            if (model == null) {
                continue;
            }
            RenderOutputs.RenderOutput output = RenderOutputs.resolve(host, modelComponent);
            if (output == null) {
                continue;
            }
            PortResourceLocation mesh = modelComponent.getMeshTexture() != null
                    ? modelComponent.getMeshTexture() : output.texture();
            targets.add(new BakeTarget(model, output.texture(), mesh));
        }
        return targets;
    }
}
