package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import io.github.tt432.eyelib.bridge.material.RenderPassAdapter;
import io.github.tt432.eyelib.bridge.material.RenderTypeResolver;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.model.Model;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.Getter;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author TT432
 */
@Getter
public class ModelComponent {
    @Nullable
    ModelComponentInfo serializableInfo;
    private boolean ignoreLighting;
    private float @Nullable [] rcColor;

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

    public void setIgnoreLighting(boolean ignoreLighting) {
        this.ignoreLighting = ignoreLighting;
    }

    public void setRcColor(float @Nullable [] rcColor) {
        this.rcColor = rcColor;
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
    public PortResourceLocation getTexture() {
        if (serializableInfo == null) return null;
        return serializableInfo.texture();
    }

    @Nullable
    //? if <26.1 {
    public RenderType getRenderType(ResourceLocation texture) {
    //?} else {
    public RenderType getRenderType(Identifier texture) {
    //?}
        if (serializableInfo == null) return null;
        var matMap = MaterialManager.INSTANCE.all();
        var entry = BrMaterialResolver.find(matMap, serializableInfo.renderType().path()).orElse(null);
        if (entry != null) {
            PortResourceLocation portTex = ResourceLocationBridge.fromMc(texture);
            PortRenderPass pass = RenderTypeResolver.resolve(portTex, entry, matMap);
            return RenderPassAdapter.toRenderType(pass, portTex);
        }
        PortResourceLocation portId = serializableInfo.renderType();
        PortResourceLocation portTex = ResourceLocationBridge.fromMc(texture);
        var data = RenderTypeResolver.resolve(portId);
        PortRenderPass pass = data.factory().apply(portTex);
        return RenderPassAdapter.toRenderType(pass, portTex);
    }

    public boolean isSolid() {
        if (serializableInfo == null) return true;
        var matMap = MaterialManager.INSTANCE.all();
        var entry = BrMaterialResolver.find(matMap, serializableInfo.renderType().path()).orElse(null);
        if (entry != null) {
            return RenderTypeResolver.isSolid(entry, matMap);
        }
        PortResourceLocation portId = serializableInfo.renderType();
        return RenderTypeResolver.resolve(portId).isSolid();
    }

    public boolean usesColorMask() {
        if (serializableInfo == null) return false;
        var matMap = MaterialManager.INSTANCE.all();
        var entry = BrMaterialResolver.find(matMap, serializableInfo.renderType().path()).orElse(null);
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
