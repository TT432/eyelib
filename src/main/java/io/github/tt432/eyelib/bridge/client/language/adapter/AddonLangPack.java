package io.github.tt432.eyelib.bridge.client.language.adapter;
//? if <26.1 {

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.importer.addon.LangAssetRegistry;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * addon 语言资源包（内存态）：把 {@link LangAssetRegistry} 暂存的 texts/*.lang
 * 以 vanilla 资源包形态暴露——{@code assets/eyelibaddon/lang/<code>.json} 按当前
 * 条目集现场合成；{@code LanguageMetadataSection} 声明全部已知 code 使
 * LanguageManager.extractLanguages 能发现（注册见 {@link AddonLangBridge}，required pack）。
 *
 * @author TT432
 */
public final class AddonLangPack implements PackResources {
    public static final String PACK_ID = "eyelib_addon_lang";
    public static final String NAMESPACE = "eyelibaddon";
    private static final String LANG_PREFIX = "lang/";

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES || !NAMESPACE.equals(location.getNamespace())) {
            return null;
        }
        // ClientLanguage.loadFrom 遍历 getNamespaces 后按 getResourceStack(lang/<code>.json) 读取
        String path = location.getPath();
        if (path.startsWith(LANG_PREFIX) && path.endsWith(".json")) {
            String code = path.substring(LANG_PREFIX.length(), path.length() - ".json".length());
            Map<String, String> entries = LangAssetRegistry.entriesByLanguageCode().get(code);
            if (entries != null && !entries.isEmpty()) {
                return () -> new ByteArrayInputStream(synthesizeLangJson(entries));
            }
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !NAMESPACE.equals(namespace)) {
            return;
        }
        String prefix = path.isEmpty() ? "" : path + "/";
        for (Map.Entry<String, Map<String, String>> e : LangAssetRegistry.entriesByLanguageCode().entrySet()) {
            String langPath = LANG_PREFIX + e.getKey() + ".json";
            if (langPath.startsWith(prefix) && !e.getValue().isEmpty()) {
                Map<String, String> entries = e.getValue();
                output.accept(location(namespace, langPath),
                        () -> new ByteArrayInputStream(synthesizeLangJson(entries)));
            }
        }
    }

    //? if <1.20.6 {
    private static ResourceLocation location(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
    //?} else {
    private static ResourceLocation location(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
    //?}

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(NAMESPACE) : Set.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        // Pack.readMetaAndCreate → readPackInfo 走这里读 pack.mcmeta；不实现会警告
        // 「Missing metadata」并返回 null Pack（启动 NPE，2026-08-09 实证）
        if (serializer == net.minecraft.server.packs.metadata.pack.PackMetadataSection.TYPE) {
            return (T) new net.minecraft.server.packs.metadata.pack.PackMetadataSection(
                    net.minecraft.network.chat.Component.literal("eyelib addon lang"),
                    net.minecraft.SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        }
        // LanguageManager.extractLanguages 走这里发现语言 code（参数序 region/name/bidirectional
        // 以 1.20.1 sources 实证；新 code 进语言选择列表，已有 code putIfAbsent 先到先得）
        if (serializer == net.minecraft.client.resources.metadata.language.LanguageMetadataSection.TYPE) {
            Map<String, LanguageInfo> languages = new LinkedHashMap<>();
            for (String code : LangAssetRegistry.knownLanguageCodes()) {
                languages.put(code, new LanguageInfo(code.toUpperCase(java.util.Locale.ROOT), code, false));
            }
            return (T) new net.minecraft.client.resources.metadata.language.LanguageMetadataSection(languages);
        }
        return null;
    }

    //? if <1.20.6 {
    @Override
    public String packId() {
        return PACK_ID;
    }
    //?} else {
    @Override
    public net.minecraft.server.packs.PackLocationInfo location() {
        return LOCATION_INFO;
    }

    private static final net.minecraft.server.packs.PackLocationInfo LOCATION_INFO =
            new net.minecraft.server.packs.PackLocationInfo(PACK_ID,
                    net.minecraft.network.chat.Component.literal("eyelib addon lang"),
                    net.minecraft.server.packs.repository.PackSource.DEFAULT,
                    java.util.Optional.empty());
    //?}

    @Override
    public void close() {
    }

    /** 现场合成 lang/<code>.json：bedrock .lang key=value 表 → MC 翻译 JSON。 */
    private static byte[] synthesizeLangJson(Map<String, String> entries) {
        JsonObject root = new JsonObject();
        entries.forEach(root::addProperty);
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }
}
//?}
