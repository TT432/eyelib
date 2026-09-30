package io.github.tt432.eyelib.client.gui.snowstorm.texture;
//? if >=1.20.1 {

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.snowstorm.texture.RasterCanvas;
import io.github.tt432.eyelib.snowstorm.texture.TextureClass;
import io.github.tt432.eyelib.snowstorm.texture.TextureSourceCodec;
import io.github.tt432.eyelib.util.color.ColorEncodings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if <1.20.6 {
import net.minecraft.resources.ResourceLocation;
//?} elif <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 贴图编辑器 MC 侧桥（ADR-0036 D5）：{@link RasterCanvas} 栅格 ↔ 可渲染纹理 +
 * {@link TextureSourceCodec}/{@code SaveHandler} 接缝安装。
 *
 * <p>canvas 栅格经单一常驻 {@link DynamicTexture} 上传 GPU（按尺寸重建、笔画间复用，
 * 避免逐笔画 GL 泄漏），id 为 {@link #CANVAS_TEXTURE_ID}。wintersky 预览链路：
 * {@code EditorRuntime.fetchTexture}（internal_changes 分支）返回 {@code Texture.source}
 * = 本桥 encode 产生的固定 id 串 → {@code Config.updateTexture().setSrc(id)} →
 * {@code WinterskyParticleRenderer.resolveTexture} 按 "namespace:path" 解析 →
 * {@code TextureManager.getTexture} 命中本 DynamicTexture。
 *
 * <p>偏离（vs JS）：
 * <ul>
 *   <li>JS source 为 PNG dataURL；本桥 source 为固定纹理 id 串（codec 定义的不透明串，
 *       texture 切片已授权）。dataURL 形态永不出现，外部 source（内置/资源包纹理路径）
 *       decode 走 ResourceManager。</li>
 *   <li>JS 保存 = 浏览器 PNG 下载；本桥写 {@code <gameDir>/snowstorm_exports/<name>.png}
 *       （ADR-0036 D6），文件名非法字符（{@code \\/:*?"<>|}）归一化为 '_'。</li>
 * </ul>
 */
public final class TextureBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("snowstorm/TextureBridge");

    /** 编辑器画布纹理 id（"namespace:path" 形式，wintersky resolveTexture 直解析）。 */
    public static final String CANVAS_TEXTURE_ID = "eyelib:snowstorm/editor_canvas.png";

    //? if <1.20.6 {
    private static final ResourceLocation CANVAS_ID = new ResourceLocation(CANVAS_TEXTURE_ID);
    //?} elif <26.1 {
    private static final ResourceLocation CANVAS_ID = ResourceLocation.parse(CANVAS_TEXTURE_ID);
    //?} else {
    private static final Identifier CANVAS_ID = Identifier.parse(CANVAS_TEXTURE_ID);
    //?}

    private static boolean installed;
    private static @Nullable DynamicTexture canvasTexture;
    private static @Nullable NativeImage canvasImage;

    private TextureBridge() {
    }

    /** 安装 codec + save 接缝（幂等）。贴图编辑器 UI 打开前必须调用（canvas 操作依赖 codec）。 */
    public static void install() {
        if (installed) return;
        installed = true;
        TextureClass.Texture.sourceCodec = new EditorCodec();
        TextureClass.Texture.saveHandler = TextureBridge::savePng;
        // 预置空白 16×16 DynamicTexture：视口在首个笔画前即可绘制（JS 初始 canvas 即 16×16 空白）；
        // 不走 encode（Texture.source 必须保持 JS 初始 '' 语义）
        RasterCanvas canvas = TextureClass.Texture.canvas;
        ensureImage(canvas.width, canvas.height);
        uploadTexture();
    }

    // ---------------------------------------------------------------- codec

    private static final class EditorCodec implements TextureSourceCodec {
        @Override
        public String encode(int[] abgr, int width, int height) {
            NativeImage image = ensureImage(width, height);
            // RasterCanvas ABGR == NativeImage 原生格式（A<<24|B<<16|G<<8|R），逐像素直写
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    setPixel(image, x, y, abgr[y * width + x]);
                }
            }
            uploadTexture();
            return CANVAS_TEXTURE_ID;
        }

        @Override
        public @Nullable DecodedImage decode(String source) {
            if (CANVAS_TEXTURE_ID.equals(source)) {
                return readBackCanvas();
            }
            return decodeExternal(source);
        }
    }
    private static NativeImage ensureImage(int width, int height) {
        NativeImage image = canvasImage;
        if (image == null || image.getWidth() != width || image.getHeight() != height) {
            if (canvasTexture != null) {
                canvasTexture.close();
                canvasTexture = null;
            }
            image = new NativeImage(width, height, true);
            canvasImage = image;
            //? if <26.1 {
            canvasTexture = new DynamicTexture(image);
            //?} else {
            canvasTexture = new DynamicTexture(() -> "snowstorm editor canvas", image);
            //?}
            Minecraft.getInstance().getTextureManager().register(CANVAS_ID, canvasTexture);
        }
        return image;
    }

    private static void uploadTexture() {
        DynamicTexture texture = canvasTexture;
        if (texture != null) {
            texture.upload();
        }
    }

    /** 画布 GPU 纹理的 CPU 常驻像素读回（DynamicTexture 像素常驻，NativeImageIO C6' 实证）。 */
    private static TextureSourceCodec.@Nullable DecodedImage readBackCanvas() {
        NativeImage image = canvasImage;
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] abgr = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                abgr[y * width + x] = getPixel(image, x, y);
            }
        }
        return new TextureSourceCodec.DecodedImage(width, height, abgr);
    }

    /**
     * 外部 source 解码：wintersky 内置（"wintersky/…" → eyelib 命名空间）/ "namespace:path" /
     * addon 注册表（无命名空间键，小写）。失败返回 null（JS img.onerror 语义）。
     */
    @SuppressWarnings("DataFlowIssue")
    private static TextureSourceCodec.@Nullable DecodedImage decodeExternal(String source) {
        String namespace = "eyelib"; // WinterskyParticleRenderer.resolveTexture 无冒号回退
        String path = source;
        int colon = source.indexOf(':');
        if (colon >= 0) {
            namespace = source.substring(0, colon);
            path = source.substring(colon + 1);
        }
        // addon 注册表（TextureManagerMixin 同款数据源，ARGB → ABGR）
        ImportedImageData addon = AddonTextureRegistry.get(path);
        if (addon != null) {
            return toDecoded(argbToAbgr(addon), addon.width(), addon.height());
        }
        // ResourceManager（内置/vanilla/资源包 PNG）
        //? if <1.20.6 {
        ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
        //?} elif <26.1 {
        ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
        //?} else {
        Identifier location = Identifier.tryBuild(namespace, path);
        //?}
        if (location == null) return null;
        var resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) return null;
        try (InputStream in = resource.get().open()) {
            ImportedImageData png = ImportedImageData.decodePng(in.readAllBytes());
            if (png == null) return null;
            return toDecoded(argbToAbgr(png), png.width(), png.height());
        } catch (Exception e) {
            LOGGER.warn("decode texture source failed: {}", source, e);
            return null;
        }
    }

    private static TextureSourceCodec.DecodedImage toDecoded(int[] abgr, int width, int height) {
        return new TextureSourceCodec.DecodedImage(width, height, abgr);
    }

    /** ImportedImageData 为 ARGB 像素数组；转 NativeImage/RasterCanvas 的 ABGR（R↔B 交换）。 */
    private static int[] argbToAbgr(ImportedImageData data) {
        int width = data.width();
        int height = data.height();
        int[] abgr = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                abgr[y * width + x] = ColorEncodings.argbToAbgr(data.getPixelArgb(x, y));
            }
        }
        return abgr;
    }

    // ---------------------------------------------------------------- save（SaveHandler 接缝）

    /**
     * 默认保存实现：JS {@code IO.export({name, extensions:['png'], savetype:'image'})} 的
     * ADR-0036 D6 落地——写 {@code <gameDir>/snowstorm_exports/<name>.png}。
     * Main 接文件系统时可经 {@code TextureClass.Texture.saveHandler} 整体替换。
     */
    private static void savePng(String name, String extension, String source) {
        RasterCanvas canvas = TextureClass.Texture.canvas;
        BufferedImage image = new BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < canvas.height; y++) {
            for (int x = 0; x < canvas.width; x++) {
                int abgr = canvas.pixels[y * canvas.width + x];
                int argb = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >>> 16) & 0xFF);
                image.setRGB(x, y, argb);
            }
        }
        String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("snowstorm_exports");
            Files.createDirectories(dir);
            Path file = dir.resolve(safeName + "." + extension);
            ImageIO.write(image, "png", file.toFile());
            LOGGER.info("texture saved: {}", file);
        } catch (Exception e) {
            LOGGER.warn("texture save failed", e);
        }
    }

    // ---------------------------------------------------------------- 版本守卫像素访问

    private static void setPixel(NativeImage image, int x, int y, int abgr) {
        //? if <26.1 {
        image.setPixelRGBA(x, y, abgr);
        //?} else {
        image.setPixel(x, y, abgr);
        //?}
    }

    private static int getPixel(NativeImage image, int x, int y) {
        //? if <26.1 {
        return image.getPixelRGBA(x, y);
        //?} else {
        return image.getPixel(x, y);
        //?}
    }
}
//?}
