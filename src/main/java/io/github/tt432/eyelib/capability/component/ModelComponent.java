package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelibattachment.capability.ModelComponentInfo;
import io.github.tt432.eyelibbridge.material.RenderPassAdapter;
import io.github.tt432.eyelibbridge.material.RenderTypeResolver;
import io.github.tt432.eyelibbridge.material.ResourceLocationBridge;
import io.github.tt432.eyelibmaterial.material.BrMaterialResolver;
import io.github.tt432.eyelibmaterial.material.ResolvedBrMaterial;
import io.github.tt432.eyelibmaterial.port.PortRenderPass;
import io.github.tt432.eyelibutil.PortResourceLocation;
import io.github.tt432.eyelibmodel.Model;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.Getter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
        var matMap = MaterialManager.INSTANCE.getAllData();
        var entry = BrMaterialResolver.find(matMap, serializableInfo.renderType().getPath()).orElse(null);
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
        var matMap = MaterialManager.INSTANCE.getAllData();
        var entry = BrMaterialResolver.find(matMap, serializableInfo.renderType().getPath()).orElse(null);
        if (entry != null) {
            return RenderTypeResolver.isSolid(entry, matMap);
        }
        PortResourceLocation portId = ResourceLocationBridge.fromMc(serializableInfo.renderType());
        return RenderTypeResolver.resolve(portId).isSolid();
    }

    public boolean usesColorMask() {
        if (serializableInfo == null) return false;
        var matMap = MaterialManager.INSTANCE.getAllData();
        var entry = BrMaterialResolver.find(matMap, serializableInfo.renderType().getPath()).orElse(null);
        if (entry == null) {
            return false;
        }
        try {
            ResolvedBrMaterial material = BrMaterialResolver.resolve(entry, matMap);
            return material.hasDefine("USE_COLOR_MASK");
        } catch (IllegalStateException exception) {
            return entry.defines()
                        .add()
                        .stream()
                        .flatMap(java.util.Collection::stream)
                        .anyMatch("USE_COLOR_MASK"::equals);
        }
    }

    final Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();
}
