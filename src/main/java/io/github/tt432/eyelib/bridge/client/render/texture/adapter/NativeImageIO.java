package io.github.tt432.eyelib.bridge.client.render.texture.adapter;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.bridge.client.render.texture.EyelibTextureManagerAccess;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.util.color.ColorEncodings;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import org.jspecify.annotations.Nullable;
//? if >=26.1 {
import com.mojang.blaze3d.opengl.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
//?}

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author TT432
 */
@UtilityClass
public class NativeImageIO {
    //? if <26.1 {
    private static final Map<String, ResourceLocation> COLOR_MASK_CACHE = new HashMap<>();
    //?} else {
    private static final Map<String, Identifier> COLOR_MASK_CACHE = new HashMap<>();
    //?}
    /** C6'：download 命中 DynamicTexture CPU 常驻像素（免 GPU 读回）的次数（运行时探针用）。 */
    public static int DYNAMIC_DOWNLOAD_HITS;

    //? if <26.1 {
    public void upload(ResourceLocation texture, NativeImage image) {
    //?} else {
    public void upload(Identifier texture, NativeImage image) {
    //?}
        //? if <26.1 {
        DynamicTexture dynamicTexture = new DynamicTexture(image);
        //?} else {
        DynamicTexture dynamicTexture = new DynamicTexture(() -> "eyelib", image);
        //?}
        Minecraft.getInstance().getTextureManager().register(texture, dynamicTexture);
    }

    public void upload(String texture, NativeImage image) {
        //? if <1.20.6 {
        upload(new ResourceLocation(texture), image);
        //?} elif <26.1 {
        upload(ResourceLocation.parse(texture), image);
        //?} else {
        upload(Identifier.parse(texture), image);

        //?}
    }
    @Nullable
    public <R> R download(String texture, Function<NativeImage, R> imageFunction) {
        //? if <1.20.6 {
        return download(new ResourceLocation(texture), imageFunction);
        //?} elif <26.1 {
        return download(ResourceLocation.parse(texture), imageFunction);
        //?} else {
        return download(Identifier.parse(texture), imageFunction);

        //?}
    }

    @Nullable
    //? if <26.1 {
    public <R> R download(ResourceLocation texture, Function<NativeImage, R> imageFunction) {
    //?} else {
    public <R> R download(Identifier texture, Function<NativeImage, R> imageFunction) {
    //?}
        // 只读查询（不注册、不加载）：vanilla getTexture 对未命中的键 new SimpleTexture(path)
        // 并 register，而运行期派生纹理（clamped/、_color_mask/）在资源里并不存在 → byPath 被
        // 永久写成 MissingTexture（仅资源重载才清除）。像素读取一旦走该路径，派生纹理随即被判为
        // 「已存在」而永不创建，整只实体采样紫黑缺省图（2026-11-20 赛博马赛克根因）。
        AbstractTexture registered = peekTexture(texture.toString());
        // C6'：eyelib 基图由 upload() 注册为 DynamicTexture，其 CPU 侧像素常驻（仅 close 释放，
        // 1.20.1/1.21.1/26.1.2 三版本源码实证）——直接用其像素，避免 glGetTexImage
        // 同步读回造成的 GPU 管线停顿。调用方一律经 copyImage 深拷贝后使用，就地像素只读。
        if (registered instanceof DynamicTexture dynamicTexture) {
            NativeImage pixels = dynamicTexture.getPixels();
            //? if <26.1 {
            if (pixels != null) {
            //?} else {
            if (pixels != null && !pixels.isClosed()) {
            //?}
                DYNAMIC_DOWNLOAD_HITS++;
                return imageFunction.apply(pixels);
            }
        }
        if (registered == null || registered == missingTexture()) {
            // 未注册 / 缺省占位（含「基图暂时未就绪」）：先按资源文件解码（vanilla/pack 纹理），
            // 再按基图派生（clamped/、_color_mask/<hex>/），都没有则返回 null 由调用方重试。
            // 绝不在此注册任何纹理，否则缺省图会被固化（见方法开头注释）。
            NativeImage fromResource = loadFromResource(texture);
            if (fromResource != null) {
                return imageFunction.apply(fromResource);
            }
            NativeImage derived = deriveFromBase(texture.toString());
            return derived == null ? null : imageFunction.apply(derived);
        }
        //? if <26.1 {
        registered.bind();

        int[] width = new int[1];
        int[] height = new int[1];
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);

        if (width[0] != 0 && height[0] != 0) {
            try (NativeImage nativeImage = new NativeImage(width[0], height[0], false)) {
                nativeImage.downloadTexture(0, false);
                return imageFunction.apply(nativeImage);
            }
        }

        return null;
        //?} else {
        try {
            var gpuTexture = registered.getTexture();
            if (gpuTexture instanceof GlTexture glTexture) {
                NativeImage image = readBackTexture(glTexture.glId(), glTexture.getWidth(0), glTexture.getHeight(0));
                return imageFunction.apply(image);
            }
        } catch (Exception ignored) {
        }

        return null;
        //?}
    }

    @Nullable
    private AbstractTexture peekTexture(String namespacedPath) {
        return Minecraft.getInstance().getTextureManager() instanceof EyelibTextureManagerAccess access
                ? access.eyelib$peekTexture(namespacedPath)
                : null;
    }

    /** byPath 中缺失纹理占位（{@code minecraft:missingno}）的对象；无占位时 null。 */
    @Nullable
    private AbstractTexture missingTexture() {
        return peekTexture("minecraft:missingno");
    }

    @Nullable
    //? if <26.1 {
    private NativeImage loadFromResource(ResourceLocation texture) {
    //?} else {
    private NativeImage loadFromResource(Identifier texture) {
    //?}
        try {
            var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resourceOpt.isPresent()) {
                try (InputStream is = resourceOpt.get().open()) {
                    return load(is);
                }
            }
        } catch (IOException e) {
            // fall through
        }
        return null;
    }

    private static final String DERIVED_CLAMPED_PREFIX = "clamped/";
    private static final String DERIVED_COLOR_MASK_PREFIX = "_color_mask/";

    /**
     * 运行期派生键的像素来源 = 基图像素 + 同款变换（{@code clamped/} = alpha 二值化，
     * {@code _color_mask/<hex>/} = 颜色掩码）。纹理尚未生成时就地派生，语义与已生成纹理一致，
     * 且不产生任何 TextureManager 副作用。
     *
     * @return 新分配的像素（所有权移交调用方）；基图不可得或非派生键时 {@code null}
     */
    /**
     * 按需物化运行期派生纹理（{@code clamped/}、{@code _color_mask/}）：基图 + 同款变换 → 注册 DynamicTexture。
     * <p>
     * 供 {@code TextureManager.getTexture} 注入在「派生键未注册」时即时补齐，
     * 使绘制、烘焙、GUI 三条消费路径看到同一份内容，不需要等渲染控制器的预热动作排到队。
     *
     * @return 物化后的纹理；基图尚不可得时 {@code null}（调用方按缺省走，下一帧重试）
     */
    @Nullable
    public AbstractTexture materializeDerivedTexture(String textureKey) {
        NativeImage image = deriveFromBase(textureKey);
        if (image == null) {
            return null;
        }
        upload(textureKey, image);
        return peekTexture(textureKey);
    }

    @Nullable
    public NativeImage deriveFromBase(String textureKey) {
        PortResourceLocation port = PortResourceLocation.parse(textureKey);
        String path = port.path();
        if (path.startsWith(DERIVED_CLAMPED_PREFIX)) {
            NativeImage image = downloadBasePixels(port, path.substring(DERIVED_CLAMPED_PREFIX.length()));
            if (image != null) {
                clampAlphaToBinary(image);
            }
            return image;
        }
        if (path.startsWith(DERIVED_COLOR_MASK_PREFIX)) {
            String rest = path.substring(DERIVED_COLOR_MASK_PREFIX.length());
            int slash = rest.indexOf('/');
            if (slash <= 0) {
                return null;
            }
            float[] color = parseColorKey(rest.substring(0, slash));
            if (color == null) {
                return null;
            }
            NativeImage image = downloadBasePixels(port, rest.substring(slash + 1));
            if (image != null) {
                applyColorMask(image, color);
            }
            return image;
        }
        return null;
    }

    @Nullable
    private NativeImage downloadBasePixels(PortResourceLocation derived, String basePath) {
        return download(ResourceLocationBridge.toMc(PortResourceLocation.of(derived.namespace(), basePath)),
                        NativeImageIO::copyImage);
    }

    /** 解析派生键里的 {@code %02x%02x%02x} 颜色段（与 {@link #colorKey} 同格式）。 */
    @Nullable
    private float[] parseColorKey(String hex) {
        if (hex.length() != 6) {
            return null;
        }
        try {
            return new float[]{
                    Integer.parseInt(hex.substring(0, 2), 16) / 255.0F,
                    Integer.parseInt(hex.substring(2, 4), 16) / 255.0F,
                    Integer.parseInt(hex.substring(4, 6), 16) / 255.0F};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    //? if >=26.1 {
    static NativeImage readBackTexture(int glId, int width, int height) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (x + y * width) * 4;
                int r = Byte.toUnsignedInt(buffer.get(offset));
                int g = Byte.toUnsignedInt(buffer.get(offset + 1));
                int b = Byte.toUnsignedInt(buffer.get(offset + 2));
                int a = Byte.toUnsignedInt(buffer.get(offset + 3));
                image.setPixel(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        return image;
    }
    //?}

    public NativeImage load(InputStream inputStream) throws IOException {
        var bufferedImage = ImageIO.read(inputStream);
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Invalid buffer");
        }

        var image = new NativeImage(NativeImage.Format.RGBA,
                                    bufferedImage.getWidth(), bufferedImage.getHeight(), false);
        var w = bufferedImage.getWidth();
        var h = bufferedImage.getHeight();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                //? if <26.1 {
                image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(bufferedImage.getRGB(x, y)));
                //?} else {
                image.setPixel(x, y, ColorEncodings.argbToAbgr(bufferedImage.getRGB(x, y)));
                //?}
            }
        }

        return image;
    }

    public NativeImage fromImportedImageData(ImportedImageData imageData) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, imageData.width(), imageData.height(), false);
        for (int x = 0; x < imageData.width(); x++) {
            for (int y = 0; y < imageData.height(); y++) {
                //? if <26.1 {
                image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(imageData.getPixelArgb(x, y)));
                //?} else {
                image.setPixel(x, y, ColorEncodings.argbToAbgr(imageData.getPixelArgb(x, y)));
                //?}
            }
        }
        return image;
    }

    public void loadAndUpload(String textureKey, InputStream inputStream) throws IOException {
        upload(textureKey, load(inputStream));
    }

    /**
     * eyelib 派生纹理路径判定：clamped/（alpha 二值化副本）与 _color_mask/（颜色掩码副本）。
     * 基图更换后这些派生必须驱逐，否则从陈旧基图回读生成。
     */
    public static boolean isEyelibDerivedTexturePath(String path) {
        return path.startsWith("clamped/") || path.startsWith("_color_mask/");
    }

    /**
     * 按路径谓词驱逐 TextureManager 中的 eyelib DynamicTexture（主线程执行；
     * 非主线程调用转投 {@code minecraft.execute}）。谓词作用于 path 段（无命名空间）。
     */
    public void evictTexturesMatching(java.util.function.Predicate<String> pathFilter) {
        Minecraft minecraft = Minecraft.getInstance();
        Runnable task = () -> {
            if (minecraft.getTextureManager() instanceof EyelibTextureManagerAccess access) {
                access.eyelib$evictTextures(pathFilter);
            }
        };
        if (minecraft.isSameThread()) {
            task.run();
        } else {
            minecraft.execute(task);
        }
    }

    /** 颜色掩码派生纹理缓存随基图更换整体失效。 */
    public void clearColorMaskCache() {
        COLOR_MASK_CACHE.clear();
    }

    public void uploadFromImportedImageData(String textureKey, ImportedImageData imageData) {
        upload(textureKey, fromImportedImageData(imageData));
    }

    @Nullable
    //? if <26.1 {
    public ResourceLocation colorMaskTexture(ResourceLocation texture, float[] color) {
    //?} else {
    public Identifier colorMaskTexture(Identifier texture, float[] color) {
    //?}
        String cacheKey = texture + "/" + colorKey(color);
        //? if <26.1 {
        ResourceLocation cached = COLOR_MASK_CACHE.get(cacheKey);
        //?} else {
        Identifier cached = COLOR_MASK_CACHE.get(cacheKey);
        //?}
        if (cached != null) {
            return cached;
        }

        //? if <1.20.6 {
        ResourceLocation generated = new ResourceLocation(texture.getNamespace(), "_color_mask/" + colorKey(color) + "/" + texture.getPath());
        //?} elif <26.1 {
        ResourceLocation generated = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), "_color_mask/" + colorKey(color) + "/" + texture.getPath());
        //?} else {
        Identifier generated = Identifier.fromNamespaceAndPath(texture.getNamespace(), "_color_mask/" + colorKey(color) + "/" + texture.getPath());

        //?}
        NativeImage image = download(texture, NativeImageIO::copyImage);
        if (image == null) {
            return null;
        }
        applyColorMask(image, color);
        upload(generated, image);
        COLOR_MASK_CACHE.put(cacheKey, generated);
        return generated;
    }

    private static String colorKey(float[] color) {
        int r = Math.round(color[0] * 255.0F);
        int g = Math.round(color[1] * 255.0F);
        int b = Math.round(color[2] * 255.0F);
        return "%02x%02x%02x".formatted(r, g, b);
    }

    private static void applyColorMask(NativeImage image, float[] color) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                //? if <26.1 {
                int abgr = image.getPixelRGBA(x, y);
                //?} else {
                int abgr = image.getPixel(x, y);
                //?}
                int alpha = (abgr >>> 24) & 0xFF;
                int baseR = abgr & 0xFF;
                int baseG = (abgr >>> 8) & 0xFF;
                int baseB = (abgr >>> 16) & 0xFF;
                float mask = alpha / 255.0F;
                int r = maskedChannel(baseR, color[0], mask);
                int g = maskedChannel(baseG, color[1], mask);
                int b = maskedChannel(baseB, color[2], mask);
                int outputAlpha = alpha == 0 ? 0 : 0xFF;
                //? if <26.1 {
                image.setPixelRGBA(x, y, (outputAlpha << 24) | (b << 16) | (g << 8) | r);
                //?} else {
                image.setPixel(x, y, (outputAlpha << 24) | (b << 16) | (g << 8) | r);
                //?}
            }
        }
    }

    private static int maskedChannel(int base, float tint, float mask) {
        float tinted = base * tint;
        return Math.round(base + (tinted - base) * mask);
    }

    /**
     * 深拷贝 NativeImage，用于 download 后传递给外部持有（避免 try-with-resources 释放）。
     */
    public static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.format(), source.getWidth(), source.getHeight(), false);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                //? if <26.1 {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
                //?} else {
                copy.setPixel(x, y, source.getPixel(x, y));
                //?}
            }
        }
        return copy;
    }

    /**
     * 将 alpha 值二值化：alpha>0 → 255，alpha=0 → 保持 0。
     * 解决 Bedrock addon 纹理使用低 alpha（如 alpha=3）做边缘抗锯齿，
     * 被 MC cutout threshold 0.5 误丢弃的问题。
     * alpha=0 必须保持透明：BE 语义即不可见，且包会故意用透明占位图
     * （如 A&S 无业村民职业层 = 1×1 透明 tga）实现不可见 pass。
     */
    public void clampAlphaToBinary(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                //? if <26.1 {
                int rgba = image.getPixelRGBA(x, y);
                //?} else {
                int rgba = image.getPixel(x, y);
                //?}
                int alpha = (rgba >> 24) & 0xFF;
                if (alpha > 0 && alpha < 255) {
                    //? if <26.1 {
                    image.setPixelRGBA(x, y, rgba | 0xFF000000);
                    //?} else {
                    image.setPixel(x, y, rgba | 0xFF000000);
                    //?}
                }
            }
        }
    }
}

