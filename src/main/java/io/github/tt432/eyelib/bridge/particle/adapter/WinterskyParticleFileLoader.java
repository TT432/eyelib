package io.github.tt432.eyelib.bridge.particle.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.importer.addon.BedrockAddonLoader;
import io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry;
import io.github.tt432.eyelib.wintersky.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * wintersky 粒子文件加载（ADR-0035 §3.1）：两个数据源——
 * <ol>
 *   <li>MC ResourceManager 内 {@code assets/<ns>/particles/*.json}（资源包/模组资源，含已被
 *       vanilla 包管理选中的 Bedrock 包）；</li>
 *   <li>{@code resourcepacks/} 下 .mcpack/.mcaddon 直接扫描（未选中的包对预览同样可见；
 *       原始 JSON 文本无损读取，纹理经 {@link BedrockAddonLoader} 桥接进
 *       {@link AddonTextureRegistry}，由 TextureManagerMixin 按需创建 DynamicTexture）。</li>
 * </ol>
 *
 * <p>同时承担 {@code Scene.fetchParticleFile} 默认实现（子发射器按 identifier 查 JSON）。
 */
public final class WinterskyParticleFileLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinterskyParticleFileLoader.class);
    private static final Gson GSON = new Gson();

    private WinterskyParticleFileLoader() {
    }

    /** identifier → 资源位置（仅 ResourceManager 来源；附加包直扫直接进 JSON_CACHE）。 */
    private static final Map<String, ResourceLocation> BY_IDENTIFIER = new LinkedHashMap<>();
    /** identifier → Gson 解析后的 JSON（Map 结构，数字为 Double，与 JsonValues 语义匹配）。 */
    private static final Map<String, Map<String, Object>> JSON_CACHE = new LinkedHashMap<>();

    private static boolean scanned = false;

    /** 全部已发现粒子 identifier（ResourceManager 资源 + 附加包直扫，排序副本）。 */
    public static List<String> identifiers() {
        ensureScanned();
        Set<String> union = new java.util.LinkedHashSet<>(BY_IDENTIFIER.keySet());
        union.addAll(JSON_CACHE.keySet());
        List<String> result = new ArrayList<>(union);
        Collections.sort(result);
        return result;
    }

    /** identifier → 解析后的 JSON Map；未找到返回 null。 */
    public static @Nullable Map<String, Object> jsonFor(String identifier) {
        ensureScanned();
        Map<String, Object> cached = JSON_CACHE.get(identifier);
        if (cached != null) {
            return cached;
        }
        ResourceLocation location = BY_IDENTIFIER.get(identifier);
        if (location == null) {
            return null;
        }
        try {
            Map<String, Object> parsed = readJson(Minecraft.getInstance().getResourceManager(), location);
            if (parsed != null) {
                JSON_CACHE.put(identifier, parsed);
            }
            return parsed;
        } catch (Exception e) {
            LOGGER.warn("[wintersky] 读取粒子文件失败 {}: {}", location, e.toString());
            return null;
        }
    }

    /** {@code Scene.fetchParticleFile} 钩子实现：返回 JSON Map（loadChildConfig 直接 setFromJSON）。 */
    public static @Nullable Object fetchParticleFile(String identifier, Config config) {
        return jsonFor(identifier);
    }

    /** 缓存失效（切世界/资源重载后下次访问重扫）。 */
    public static void invalidate() {
        scanned = false;
        BY_IDENTIFIER.clear();
        JSON_CACHE.clear();
    }

    private static void ensureScanned() {
        if (scanned) {
            return;
        }
        scanned = true;
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        for (Map.Entry<ResourceLocation, Resource> entry
                : manager.listResources("particles", loc -> loc.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                String identifier = parseIdentifier(reader);
                if (identifier != null) {
                    BY_IDENTIFIER.putIfAbsent(identifier, entry.getKey());
                }
            } catch (Exception e) {
                LOGGER.warn("[wintersky] 扫描粒子文件失败 {}: {}", entry.getKey(), e.toString());
            }
        }
        scanAddonPacks();
    }

    /**
     * 直扫 {@code resourcepacks/} 下 .mcpack/.mcaddon：粒子 JSON 原文进缓存
     * （不依赖 vanilla 包选择状态）；纹理经 BedrockAddonLoader 桥接进
     * AddonTextureRegistry（已存在的键不覆盖，避免与选中包管线竞争）。
     */
    private static void scanAddonPacks() {
        Path packsDir = Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks");
        if (!Files.isDirectory(packsDir)) {
            return;
        }
        List<Path> files;
        try (var stream = Files.list(packsDir)) {
            files = stream.filter(WinterskyParticleFileLoader::isAddonFile).toList();
        } catch (IOException e) {
            LOGGER.warn("[wintersky] 枚举 resourcepacks 失败: {}", e.toString());
            return;
        }
        for (Path file : files) {
            scanAddonParticles(file);
            bridgeAddonTextures(file);
        }
    }

    private static boolean isAddonFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return Files.isRegularFile(path) && (name.endsWith(".mcpack") || name.endsWith(".mcaddon"));
    }

    /** 包根 = 含 manifest.json 的目录（.mcpack 根目录；.mcaddon 的 resource_packs/ 等子目录）。 */
    private static void scanAddonParticles(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            Set<String> roots = new HashSet<>();
            roots.add("");
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith("manifest.json")) {
                    roots.add(name.substring(0, name.length() - "manifest.json".length()));
                }
            }
            var scan = zip.entries();
            while (scan.hasMoreElements()) {
                ZipEntry entry = scan.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                for (String root : roots) {
                    if (!name.startsWith(root + "particles/") || !name.endsWith(".json")) {
                        continue;
                    }
                    try (Reader reader = new java.io.InputStreamReader(
                            zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                        String identifier = parseIdentifier(reader);
                        if (identifier == null || JSON_CACHE.containsKey(identifier)) {
                            continue;
                        }
                        try (Reader reread = new java.io.InputStreamReader(
                                zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                            Map<String, Object> parsed = GSON.fromJson(reread, Map.class);
                            if (parsed != null) {
                                JSON_CACHE.put(identifier, parsed);
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[wintersky] 扫描附加包粒子失败 {}: {}", file.getFileName(), e.toString());
        }
    }

    private static void bridgeAddonTextures(Path file) {
        try {
            BedrockAddon addon = BedrockAddonLoader.load(file, null);
            addon.aggregate().textures().forEach((relativePath, imageData) -> {
                String key = relativePath.toLowerCase(java.util.Locale.ROOT);
                if (AddonTextureRegistry.get(key) == null) {
                    AddonTextureRegistry.put(relativePath, imageData);
                }
            });
        } catch (Exception e) {
            LOGGER.warn("[wintersky] 桥接附加包纹理失败 {}: {}", file.getFileName(), e.toString());
        }
    }

    private static @Nullable String parseIdentifier(Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            return null;
        }
        JsonObject effect = root.getAsJsonObject("particle_effect");
        JsonObject description = effect == null ? null : effect.getAsJsonObject("description");
        if (description == null || !description.has("identifier")) {
            return null;
        }
        return description.get("identifier").getAsString();
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> readJson(ResourceManager manager, ResourceLocation location)
            throws IOException {
        try (Reader reader = manager.getResourceOrThrow(location).openAsReader()) {
            return GSON.fromJson(reader, Map.class);
        }
    }
}
