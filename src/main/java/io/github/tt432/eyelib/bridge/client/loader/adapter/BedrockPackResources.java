package io.github.tt432.eyelib.bridge.client.loader.adapter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * resourcepacks/ 下 .mcpack/.mcaddon 文件的 vanilla 资源包形态：使 Bedrock 附加包
 * 进入 {@link net.minecraft.server.packs.repository.PackRepository} 管理（资源包界面可见、
 * 可启用/禁用/排序，options.txt 持久化）。
 * <p>
 * 内容不提供 vanilla 布局资源（Bedrock 包无 assets/&lt;ns&gt;/ 结构）——文件内容解析仍由
 * {@code BedrockAddonLoader} 按 {@link #sourceFile()} 路径进行；本类只承担「管理面」：
 * pack 元数据（从 manifest.json 派生）+ 空资源视图。
 *
 * @author TT432
 */
public final class BedrockPackResources implements net.minecraft.server.packs.PackResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockPackResources.class);

    /** pack id 前缀（沿用 vanilla FolderRepositorySource 的 "file/" 惯例，options.txt 选择记录随文件名稳定）。 */
    public static final String ID_PREFIX = "file/";

    private final Path file;
    private final String packId;
    private final Component title;
    private final Component description;

    private BedrockPackResources(Path file) {
        this.file = file;
        this.packId = ID_PREFIX + file.getFileName().toString();
        ManifestInfo manifest = readManifestInfo(file);
        this.title = Component.literal(manifest != null && !manifest.name().isEmpty()
                ? manifest.name() : file.getFileName().toString());
        this.description = Component.literal(manifest != null ? manifest.description() : "");
        //? if >=1.20.6 {
        this.locationInfo = new net.minecraft.server.packs.PackLocationInfo(
                packId, title, PackSource.DEFAULT, java.util.Optional.empty());
        //?}
    }

    public static BedrockPackResources of(Path file) {
        return new BedrockPackResources(file);
    }

    /** pack id（与 options.txt 中持久化的一致）。 */
    public static String packIdOf(Path file) {
        return ID_PREFIX + file.getFileName().toString();
    }

    /** .mcpack/.mcaddon 常规文件判据（与历史 BedrockAddonAutoLoader 扫描口径一致）。 */
    public static boolean isBedrockAddonFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return java.nio.file.Files.isRegularFile(path)
                && (name.endsWith(".mcpack") || name.endsWith(".mcaddon"));
    }

    /** 供重载监听器取回文件路径交给 BedrockAddonLoader。 */
    public Path sourceFile() {
        return file;
    }

    public Component title() {
        return title;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        // vanilla 包列表图标读取 pack.png（PackSelectionScreen.loadPackIcon）；
        // Bedrock 惯例图标是包根的 pack_icon.png（.mcaddon 在 resource_pack/ 下）——在此桥接。
        if (elements.length == 1 && ("pack.png".equals(elements[0]) || "pack_icon.png".equals(elements[0]))) {
            return iconSupplier(file);
        }
        return null;
    }

    /** pack_icon.png 的 IoSupplier；无图标返回 null（vanilla 回落 unknown_pack.png）。 */
    private static @Nullable IoSupplier<InputStream> iconSupplier(Path file) {
        try {
            ZipFile zip = new ZipFile(file.toFile());
            ZipEntry entry = zip.getEntry("pack_icon.png");
            if (entry == null) {
                entry = zip.getEntry("resource_pack/pack_icon.png");
            }
            if (entry == null) {
                zip.close();
                return null;
            }
            ZipEntry finalEntry = entry;
            return () -> {
                // 每次 open 新 ZipFile：IoSupplier 可被多次调用，且关闭流时连同 zip 一起释放
                ZipFile opened = new ZipFile(file.toFile());
                InputStream in = opened.getInputStream(opened.getEntry(finalEntry.getName()));
                return new java.io.FilterInputStream(in) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        opened.close();
                    }
                };
            };
        } catch (IOException e) {
            LOGGER.warn("Failed to probe pack icon in {}: {}", file.getFileName(), e.toString());
            return null;
        }
    }

    //? if <26.1 {
    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.ResourceLocation location) {
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
    }
    //?} else {
    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.Identifier location) {
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
    }
    //?}

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Set.of();
    }

    //? if <26.1 {
    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getMetadataSection(
            net.minecraft.server.packs.metadata.MetadataSectionSerializer<T> serializer) {
        // Pack.readMetaAndCreate → readPackInfo 走这里读 pack.mcmeta；返回 null 会被丢弃（不显示在界面）
        if (serializer == net.minecraft.server.packs.metadata.pack.PackMetadataSection.TYPE) {
            return (T) new net.minecraft.server.packs.metadata.pack.PackMetadataSection(
                    description,
                    SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        }
        return null;
    }
    //?} else {
    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getMetadataSection(
            net.minecraft.server.packs.metadata.MetadataSectionType<T> type) {
        if (type == net.minecraft.server.packs.metadata.pack.PackMetadataSection.CLIENT_TYPE) {
            return (T) new net.minecraft.server.packs.metadata.pack.PackMetadataSection(
                    description,
                    new net.minecraft.util.InclusiveRange<>(
                            SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES)));
        }
        return null;
    }
    //?}

    //? if <1.20.6 {
    @Override
    public String packId() {
        return packId;
    }
    //?} else {
    @Override
    public net.minecraft.server.packs.PackLocationInfo location() {
        return locationInfo;
    }

    private final net.minecraft.server.packs.PackLocationInfo locationInfo;
    //?}

    @Override
    public void close() {
    }

    /** manifest.json 的 header.name/description（.mcaddon 取 resource_pack/manifest.json）。
     *  Bedrock 惯例 name="pack.name" 是 texts/*.lang 的键——先按 lang 解析，
     *  解析不到时 name 回落文件名、description 回落空串（界面上「pack.name」没有意义）。 */
    private static @Nullable ManifestInfo readManifestInfo(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry("manifest.json");
            if (entry == null) {
                entry = zip.getEntry("resource_pack/manifest.json");
            }
            if (entry == null) {
                return null;
            }
            String name;
            String description;
            try (InputStream in = zip.getInputStream(entry)) {
                JsonObject root = JsonParser.parseString(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                if (!root.has("header") || !root.get("header").isJsonObject()) {
                    return null;
                }
                JsonObject header = root.getAsJsonObject("header");
                name = header.has("name") && header.get("name").isJsonPrimitive()
                        ? header.get("name").getAsString() : "";
                description = header.has("description") && header.get("description").isJsonPrimitive()
                        ? header.get("description").getAsString() : "";
            }
            Map<String, String> lang = null;
            if (isLangKey(name) || isLangKey(description)) {
                lang = readLangFile(zip, "texts/en_US.lang");
            }
            if (isLangKey(name)) {
                name = lang != null && lang.containsKey(name)
                        ? lang.get(name) : file.getFileName().toString();
            }
            if (isLangKey(description)) {
                description = lang != null ? lang.getOrDefault(description, "") : "";
            }
            return new ManifestInfo(name, description);
        } catch (Exception e) {
            LOGGER.warn("Failed to read manifest from {}: {}", file.getFileName(), e.toString());
            return null;
        }
    }

    /** Bedrock 本地化键形态：无空白、纯 [A-Za-z0-9_.-] 且含点（"pack.name"）。 */
    private static boolean isLangKey(String value) {
        return !value.isEmpty() && value.indexOf(' ') < 0 && value.indexOf('.') > 0
                && value.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-');
    }

    /** texts/en_US.lang 的 key=value 表（文件缺失返回 null）。 */
    private static @Nullable Map<String, String> readLangFile(ZipFile zip, String path) {
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return null;
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return io.github.tt432.eyelib.importer.addon.BedrockLangFile.parse(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    private record ManifestInfo(String name, String description) {
    }
}
