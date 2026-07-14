package io.github.tt432.eyelib.importer.model.importer;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/**
 * 全局纹理注册表：存储由 BedrockAddonLoader 解码的纹理数据（ImportedImageData）。
 * <p>
 * 由 {@link io.github.tt432.eyelib.bridge.client.loader.BedrockAddonAutoLoader} 在资源重载时填充，
 * 由 {@code TextureManagerMixin} 在 MC 原版 {@code TextureManager.getTexture()} 中查询。
 * <p>
 * .tga 文件以 .png 路径注册，使 MC 原版纹理加载机制能透明地加载 .tga 纹理——
 * "能加载 .png 的地方就能加载 .tga"。
 *
 * @author TT432
 */
public final class AddonTextureRegistry {
    private AddonTextureRegistry() {
    }

    /**
     * path（已归一化，小写，.tga→.png）→ 解码后的图像数据
     */
    private static final Map<String, ImportedImageData> TEXTURES = new ConcurrentHashMap<>();

    /**
     * 注册纹理。路径自动归一化：小写 + .tga→.png。
     */
    public static void put(String path, ImportedImageData data) {
        TEXTURES.put(normalizePath(path), data);
    }

    /**
     * 查询纹理。路径自动归一化。
     *
     * @return 解码数据，或 null
     */
    public static @Nullable ImportedImageData get(String path) {
        return TEXTURES.get(path.toLowerCase(Locale.ROOT));
    }

    /**
     * 清空注册表（资源重载前调用）。
     */
    public static void clear() {
        TEXTURES.clear();
    }

    /**
     * 归一化路径：小写 + .tga → .png。
     */
    static String normalizePath(String path) {
        path = path.toLowerCase(Locale.ROOT);
        if (path.endsWith(".tga")) {
            return path.substring(0, path.length() - 4) + ".png";
        }
        return path;
    }
}
