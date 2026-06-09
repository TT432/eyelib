package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelibattachment.capability.ModelComponentInfo;
import io.github.tt432.eyelibbridge.material.RenderPassAdapter;
import io.github.tt432.eyelibbridge.material.RenderTypeResolver;
import io.github.tt432.eyelibbridge.material.ResourceLocationBridge;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import io.github.tt432.eyelibmaterial.port.PortRenderPass;
import io.github.tt432.eyelibutil.PortResourceLocation;
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
            PortResourceLocation portTex = ResourceLocationBridge.fromMc(texture);
            PortRenderPass pass = RenderTypeResolver.resolve(portTex, entry, matMap);
            return RenderPassAdapter.toRenderType(pass, portTex);
        }
        PortResourceLocation portId = ResourceLocationBridge.fromMc(serializableInfo.renderType());
        PortResourceLocation portTex = ResourceLocationBridge.fromMc(texture);
        var data = RenderTypeResolver.resolve(portId);
        PortRenderPass pass = data.factory().apply(portTex);
        return RenderPassAdapter.toRenderType(pass, portTex);
    }

    public boolean isSolid() {
        if (serializableInfo == null) return true;
        var matMap = buildMaterialLookupMap();
        var entry = matMap.get(serializableInfo.renderType().getPath());
        if (entry != null) {
            return RenderTypeResolver.isSolid(entry, matMap);
        }
        PortResourceLocation portId = ResourceLocationBridge.fromMc(serializableInfo.renderType());
        return RenderTypeResolver.resolve(portId).isSolid();
    }

    /**
     * 构建材质查找表：同时按完整 key、key 后缀、以及 entry.name() 建立索引。
     * entry.name() 优先——它是 Bedrock 材质继承链中的"真名"。
     */
    private Map<String, BrMaterialEntry> buildMaterialLookupMap() {
        Map<String, BrMaterialEntry> result = new java.util.HashMap<>(MaterialManager.INSTANCE.getAllData());
        for (var entry : MaterialManager.INSTANCE.getAllData().entrySet()) {
            String name = entry.getValue().name();
            String mapKey = entry.getKey();
            int colon = mapKey.lastIndexOf(':');
            // 按 entry.name() 建索引（canonical: suffix 等于 name 的优先）
            if (colon >= 0 && name.equals(mapKey.substring(colon + 1))) {
                result.put(name, entry.getValue());
            } else {
                result.putIfAbsent(name, entry.getValue());
            }
        }
        return result;
    }

    final Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();
}
