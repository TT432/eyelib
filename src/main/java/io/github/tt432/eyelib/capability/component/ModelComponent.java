package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelibattachment.capability.ModelComponentInfo;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import io.github.tt432.eyelibmaterial.render.RenderTypeResolver;
import io.github.tt432.eyelibmodel.Model;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.Getter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * @author TT432
 */
@Getter
@NullMarked
public class ModelComponent {
    @Nullable
    ModelComponentInfo serializableInfo;

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.model() != null
                && serializableInfo.texture() != null
                && serializableInfo.renderType() != null;
    }

    public void setInfo(ModelComponentInfo serializableInfo) {
        if (Objects.equals(serializableInfo, this.serializableInfo)) return;

        this.serializableInfo = serializableInfo;
    }

    public boolean readyForRendering() {
        return getModel() != null && getTexture() != null;
    }

    @Nullable
    public Model getModel() {
        if (serializableInfo == null) return null;
        return ModelManager.INSTANCE.get(serializableInfo.model());
    }

    @Nullable
    public ResourceLocation getTexture() {
        if (serializableInfo == null) return null;
        return serializableInfo.texture();
    }

    @Nullable
    public RenderType getRenderType(ResourceLocation texture) {
        if (serializableInfo == null) return null;
        var matMap = buildMaterialLookupMap();
        var entry = matMap.get(serializableInfo.renderType().getPath());
        if (entry != null) {
            if (entry.hasBlending(matMap)) return RenderType.entityTranslucent(texture);
            if (isBaseType(entry, "entity_alphatest", matMap)) return RenderType.entityCutoutNoCull(texture);
            return RenderType.entitySolid(texture);
        }
        return RenderTypeResolver.resolve(serializableInfo.renderType()).factory().apply(texture);
    }

    public boolean isSolid() {
        if (serializableInfo == null) return true;
        var matMap = buildMaterialLookupMap();
        var entry = matMap.get(serializableInfo.renderType().getPath());
        if (entry != null) {
            if (entry.hasBlending(matMap)) return false;
            if (isBaseType(entry, "entity_alphatest", matMap)) return false;
            return true;
        }
        return RenderTypeResolver.resolve(serializableInfo.renderType()).isSolid();
    }

    /**
     * 遍历材质继承链，检查是否以指定 Bedrock 基材质为根。
     * 当 materials 中查不到时，直接用 base 字段字符串比对（处理 Bedrock 原生基材质）。
     */
    private boolean isBaseType(BrMaterialEntry entry, String baseType, Map<String, BrMaterialEntry> materials) {
        BrMaterialEntry current = entry;
        java.util.Set<String> visited = new java.util.HashSet<>();
        while (current != null && visited.add(current.name())) {
            if (baseType.equals(current.name())) return true;
            String nextName = current.base();
            if (nextName == null || nextName.isEmpty()) break;
            // Bedrock 原生基材质（如 entity_alphatest）不在 MaterialManager 中
            if (baseType.equals(nextName)) return true;
            current = materials.get(nextName);
        }
        return false;
    }

    /**
     * 构建材质查找表：同时按完整 key、key 后缀、以及 entry.name() 建立索引。
     * entry.name() 优先——它是 Bedrock 材质继承链中的"真名"。
     */
    private Map<String, BrMaterialEntry> buildMaterialLookupMap() {
        Map<String, BrMaterialEntry> result = new java.util.HashMap<>(MaterialManager.INSTANCE.getAllData());
        for (var entry : MaterialManager.INSTANCE.getAllData().entrySet()) {
            // 按 entry.name() 建索引（用于 base 引用解析）
            result.putIfAbsent(entry.getValue().name(), entry.getValue());
            // 按 key 后缀建索引
            String key = entry.getKey();
            int colon = key.lastIndexOf(':');
            if (colon >= 0) {
                result.putIfAbsent(key.substring(colon + 1), entry.getValue());
            }
        }
        return result;
    }

    @Nullable
    private BrMaterialEntry findMaterial(String materialName) {
        for (var entry : MaterialManager.INSTANCE.getAllData().entrySet()) {
            if (entry.getKey().endsWith(":" + materialName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    final Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();
}