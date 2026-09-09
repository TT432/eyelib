package io.github.tt432.eyelib.bridge.client.render.bake;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.bridge.client.render.texture.adapter.NativeImageIO;
import io.github.tt432.eyelib.model.Model;
import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import net.minecraft.client.Minecraft;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author TT432
 */
public abstract class ModelBakeInfo<Info, BM> {
    //? if <26.1 {
    private final Map<String, Map<ResourceLocation, BM>> modelCache = new ConcurrentHashMap<>();
    //?} else {
    private final Map<String, Map<Identifier, BM>> modelCache = new ConcurrentHashMap<>();
    //?}

    //? if <26.1 {
    public BM getBakedModel(Model model, boolean isSolid, ResourceLocation texture) {
    //?} else {
    public BM getBakedModel(Model model, boolean isSolid, Identifier texture) {
    //?}
        return modelCache.computeIfAbsent(model.name(), s -> new ConcurrentHashMap<>())
                         .computeIfAbsent(texture, i -> {
                             Info bakeInfo = getBakeInfo(model, isSolid, texture);
                             return bake(model, bakeInfo);
                         });
    }

    public void invalidateModel(String modelName) {
        modelCache.remove(modelName);
    }

    /** 批量替换（资源重载整表写入）时整体失效。 */
    public void invalidateAll() {
        modelCache.clear();
    }

    //? if <26.1 {
    protected abstract Info getBakeInfo(Model model, boolean isSolid, ResourceLocation texture);
    //?} else {
    protected abstract Info getBakeInfo(Model model, boolean isSolid, Identifier texture);
    //?}

    protected abstract BM bake(Model model, Info info);

    @FunctionalInterface
    public interface BoneDataConsumer {
        void execute(int boneId, boolean[] data);
    }

    //? if <26.1 {
    protected void downloadTexture(ResourceLocation texture, Consumer<NativeImage> imageConsumer) {
    //?} else {
    protected void downloadTexture(Identifier texture, Consumer<NativeImage> imageConsumer) {
    //?}
        //? if <26.1 {
        // CPU 优先（后台线程安全），把烘焙从渲染线程/GPU 管线上移走（实测 GPU 读回单次 35-45ms）：
        // 1) Bedrock addon 内存纹理（与 TextureManagerMixin 同数据源，无命名空间路径）
        var addonData = io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry.get(texture.getPath());
        if (addonData != null) {
            try (NativeImage image = NativeImageIO.fromImportedImageData(addonData)) {
                imageConsumer.accept(image);
            }
            return;
        }
        // 2) JE 资源包文件读流解码（MultiPackResourceManager 读路径可被工作线程并发使用）
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture).orElse(null);
            if (resource != null) {
                try (var is = resource.open();
                     NativeImage image = NativeImage.read(is)) {
                    imageConsumer.accept(image);
                }
                return;
            }
        } catch (Exception ignored) {
        }
        // 后台线程不得触碰 TextureManager/GL：留给渲染线程同步兜底（未预热的冷路径）
        if (!Minecraft.getInstance().isSameThread()) {
            return;
        }
        // 3) 渲染线程兜底：DynamicTexture CPU 常驻像素优先（C6'），否则 GPU 读回（原行为）
        NativeImageIO.download(texture, image -> {
            imageConsumer.accept(image);
            return null;
        });
        //?} else {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture).orElse(null);
            if (resource != null) {
                try (var is = resource.open()) {
                    imageConsumer.accept(NativeImage.read(is));
                }
            }
        } catch (Exception ignored) {
        }
        //?}
    }

    protected void processBone(Model.Bone bone, NativeImage intBuffer, Int2BooleanFunction f, BoneDataConsumer consumer) {
        bone.children().values().forEach(boneIn -> processBone(boneIn, intBuffer, f, consumer));

        boolean[] result = new boolean[bone.cubes().size()];

        for (int i = 0; i < bone.cubes().size(); i++) {
            var cube = bone.cubes().get(i);

            for (int j = 0; j < cube.faces().size(); j++) {
                Model.Face face = cube.faces().get(j);
                var uv0 = face.vertexes().get(0).uv();
                var uv2 = face.vertexes().get(2).uv();
                if (pixelAnyMatch(intBuffer,
                                  uv0.x(), uv0.y(),
                                  uv2.x(), uv2.y(),
                                  f
                )) {
                    result[i] = true;
                    break;
                }
            }
        }

        consumer.execute(bone.id(), result);
    }

    protected boolean pixelAnyMatch(NativeImage buffer,
                                    float uv0u, float uv0v, float uv1u, float uv1v,
                                    Int2BooleanFunction f) {
        int bufW = buffer.getWidth() - 1;
        int bufH = buffer.getHeight() - 1;

        int x0 = Math.round(uv0u * bufW);
        int y0 = Math.round(uv0v * bufH);
        int x1 = Math.round(uv1u * bufW);
        int y1 = Math.round(uv1v * bufH);

        int minX = Math.min(x0, x1);
        int maxX = Math.max(x0, x1);
        int minY = Math.min(y0, y1);
        int maxY = Math.max(y0, y1);

        for (int j = minY; j < maxY; j++) {
            for (int k = minX; k < maxX; k++) {
                //? if <26.1 {
                int pixel = buffer.getPixelRGBA(k, j);
                //?} else {
                int pixel = buffer.getPixel(k, j);
                //?}
                if (f.get(pixel)) {
                    return true;
                }
            }
        }

        return false;
    }
}
