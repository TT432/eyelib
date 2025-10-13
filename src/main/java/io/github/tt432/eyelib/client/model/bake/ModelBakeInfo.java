package io.github.tt432.eyelib.client.model.bake;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author TT432
 */
public abstract class ModelBakeInfo<Info> {
    private final Map<String, HashMap<ResourceLocation, BakedModel>> modelCache = new HashMap<>();

    {
        MinecraftForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(e -> {
            if (e.getManagerName().equals(ModelManager.class.getSimpleName()))
                modelCache.remove(e.getEntryName());
        });
    }

    public BakedModel getBakedModel(Model model, boolean isSolid, ResourceLocation texture) {
        return modelCache.computeIfAbsent(model.name(), s -> new HashMap<>())
                .computeIfAbsent(texture, i -> {
                    Info bakeInfo = getBakeInfo(model, isSolid, texture);
                    return bake(model, bakeInfo);
                });
    }

    protected abstract Info getBakeInfo(Model model, boolean isSolid, ResourceLocation texture);

    protected abstract BakedModel bake(Model model, Info info);

    @FunctionalInterface
    public interface BoneDataConsumer {
        void execute(String boneName, boolean[] data);
    }

    protected void downloadTexture(ResourceLocation texture, Consumer<NativeImage> imageConsumer) {
        Minecraft.getInstance().getTextureManager().getTexture(texture).bind();

        int[] width = new int[1];
        int[] height = new int[1];
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);

        if (width[0] != 0 && height[0] != 0) {
            try (NativeImage nativeimage = new NativeImage(width[0], height[0], false)) {
                nativeimage.downloadTexture(0, false);

                imageConsumer.accept(nativeimage);
            }
        }
    }

    protected void processBone(Model.Bone bone, NativeImage intBuffer, Int2BooleanFunction f, BoneDataConsumer consumer) {
        bone.children().values().forEach(boneIn -> processBone(boneIn, intBuffer, f, consumer));

        boolean[] result = new boolean[bone.cubes().size()];

        for (int i = 0; i < bone.cubes().size(); i++) {
            var cube = bone.cubes().get(i);

            for (int i1 = 0; i1 < cube.faceCount(); i1++) {
                if (pixelAnyMatch(intBuffer,
                        cube.uvU(i1, 0), cube.uvV(i1, 0),
                        cube.uvU(i1, 2), cube.uvV(i1, 2),
                        f
                )) {
                    result[i] = true;
                    break;
                }
            }
        }

        consumer.execute(bone.name(), result);
    }

    protected boolean pixelAnyMatch(NativeImage buffer,
                                    float uv0u, float uv0v, float uv1u, float uv1v,
                                    Int2BooleanFunction f) {
        int x = Math.round(uv0u * (buffer.getWidth() - 1));
        int y = Math.round(uv0v * (buffer.getHeight() - 1));
        int width = Math.round((uv1u - uv0u) * (buffer.getWidth() - 1));
        int height = Math.round((uv1v - uv0v) * (buffer.getHeight() - 1));

        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                if (f.get(buffer.getPixelRGBA(x + k, y + j))) {
                    return true;
                }
            }
        }

        return false;
    }
}
