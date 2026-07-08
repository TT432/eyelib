package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import io.github.tt432.eyelib.bridge.material.RenderTypeResolver;
import io.github.tt432.eyelib.material.render.RenderTypeResolver.EntityRenderTypeData;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.model.Model;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Map;
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

    @Nullable
    private Map<String, BrMaterialEntry> matMapRef;
    @Nullable
    private BrMaterialEntry cachedEntry;
    private boolean entryResolved;
    @Nullable
    private ResolvedBrMaterial cachedMaterial;
    private boolean materialResolved;
    @Nullable
    private EntityRenderTypeData cachedFallback;

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.model() != null
                && serializableInfo.texture() != null
                && serializableInfo.renderType() != null;
    }

    public void setInfo(ModelComponentInfo serializableInfo) {
        if (Objects.equals(serializableInfo, this.serializableInfo)) return;

        this.serializableInfo = serializableInfo;
        this.matMapRef = null;
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
    public PortRenderPass getRenderType(PortResourceLocation texture) {
        ModelComponentInfo info = serializableInfo;
        if (info == null) return null;
        Map<String, BrMaterialEntry> matMap = currentMaterialMap();
        BrMaterialEntry entry = resolveEntry(matMap, info.renderType().path());
        if (entry != null) {
            ResolvedBrMaterial material = resolveCachedMaterial(entry, matMap);
            return material != null
                    ? RenderTypeResolver.resolve(texture, material)
                    : RenderTypeResolver.resolve(texture, entry, matMap);
        }
        return resolveFallback(info.renderType()).factory().apply(texture);
    }

    public boolean isSolid() {
        ModelComponentInfo info = serializableInfo;
        if (info == null) return true;
        Map<String, BrMaterialEntry> matMap = currentMaterialMap();
        BrMaterialEntry entry = resolveEntry(matMap, info.renderType().path());
        if (entry != null) {
            ResolvedBrMaterial material = resolveCachedMaterial(entry, matMap);
            return material != null ? RenderTypeResolver.isSolid(material) : RenderTypeResolver.isSolid(entry, matMap);
        }
        return resolveFallback(info.renderType()).isSolid();
    }

    public boolean usesColorMask() {
        ModelComponentInfo info = serializableInfo;
        if (info == null) return false;
        Map<String, BrMaterialEntry> matMap = currentMaterialMap();
        BrMaterialEntry entry = resolveEntry(matMap, info.renderType().path());
        if (entry == null) {
            return false;
        }
        ResolvedBrMaterial material = resolveCachedMaterial(entry, matMap);
        if (material != null) {
            return material.hasDefine("USE_COLOR_MASK");
        }
        try {
            return BrMaterialResolver.resolve(entry, matMap).hasDefine("USE_COLOR_MASK");
        } catch (IllegalStateException exception) {
            return entry.defines()
                        .add()
                        .stream()
                        .flatMap(java.util.Collection::stream)
                        .anyMatch("USE_COLOR_MASK"::equals);
        }
    }

    private Map<String, BrMaterialEntry> currentMaterialMap() {
        Map<String, BrMaterialEntry> current = MaterialManager.INSTANCE.all();
        if (current != matMapRef) {
            matMapRef = current;
            cachedEntry = null;
            entryResolved = false;
            cachedMaterial = null;
            materialResolved = false;
            cachedFallback = null;
        }
        return current;
    }

    @Nullable
    private BrMaterialEntry resolveEntry(Map<String, BrMaterialEntry> matMap, String path) {
        if (!entryResolved) {
            entryResolved = true;
            cachedEntry = BrMaterialResolver.find(matMap, path).orElse(null);
        }
        return cachedEntry;
    }

    @Nullable
    private ResolvedBrMaterial resolveCachedMaterial(BrMaterialEntry entry, Map<String, BrMaterialEntry> matMap) {
        if (!materialResolved) {
            materialResolved = true;
            try {
                cachedMaterial = BrMaterialResolver.resolve(entry, matMap);
            } catch (IllegalStateException e) {
                cachedMaterial = null;
            }
        }
        return cachedMaterial;
    }

    private EntityRenderTypeData resolveFallback(PortResourceLocation portId) {
        if (cachedFallback == null) {
            cachedFallback = RenderTypeResolver.resolve(portId);
        }
        return cachedFallback;
    }

    final Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();
}

