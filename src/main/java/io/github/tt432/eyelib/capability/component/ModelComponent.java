package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.model.ModelLookup;
import io.github.tt432.eyelibattachment.capability.ModelComponentInfo;
import io.github.tt432.eyelibmaterial.render.RenderTypeResolver;
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
        return ModelLookup.get(serializableInfo.model());
    }

    @Nullable
    public ResourceLocation getTexture() {
        if (serializableInfo == null) return null;
        return serializableInfo.texture();
    }

    @Nullable
    public RenderType getRenderType(ResourceLocation texture) {
        if (serializableInfo == null) return null;
        return RenderTypeResolver.resolve(serializableInfo.renderType()).factory().apply(texture);
    }

    public boolean isSolid() {
        if (serializableInfo == null) return true;
        return RenderTypeResolver.resolve(serializableInfo.renderType()).isSolid();
    }

    final Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();
}