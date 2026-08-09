package io.github.tt432.eyelib.bridge.client.sound.adapter;
//? if <26.1 {

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.importer.addon.SoundAssetRegistry;
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
import java.util.Map;
import java.util.Set;

/**
 * addon 音效资源包（内存态）：把 {@link SoundAssetRegistry} 暂存的音频字节与
 * sound_definitions 以 vanilla 资源包形态暴露——{@code assets/eyelibaddon/sounds/**}
 * 服务 .ogg 字节，{@code assets/eyelibaddon/sounds.json} 按当前定义集现场合成。
 * 注册见 {@link AddonSoundBridge}（required pack，无需手动启用）。
 *
 * @author TT432
 */
public final class AddonSoundPack implements PackResources {
    public static final String PACK_ID = "eyelib_addon_sounds";
    public static final String NAMESPACE = "eyelibaddon";
    private static final String SOUNDS_JSON = "sounds.json";
    private static final Set<String> MC_CATEGORIES = Set.of(
            "master", "music", "record", "weather", "block", "hostile", "neutral",
            "player", "ambient", "voice", "ui");

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) {
            return null;
        }
        String path = location.getPath();
        if (SOUNDS_JSON.equals(path) && NAMESPACE.equals(location.getNamespace())) {
            // MC 事件键 = 当前命名空间 + 键原文（1.20.1 字节码实证，键含 ':' 判非法），且
            // 资源管理器的命名空间集在资源重载时固化——事件统一落本包稳定命名空间，
            // bedrock 的 "ns:path" 映射为键 "ns/path"（与 playPreview 同映射，见类文档）
            return () -> new ByteArrayInputStream(synthesizeSoundsJson());
        }
        if (NAMESPACE.equals(location.getNamespace()) && path.startsWith("sounds/")) {
            var asset = SoundAssetRegistry.filesView().get(path);
            if (asset != null) {
                byte[] bytes = asset.bytes();
                return () -> new ByteArrayInputStream(bytes);
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
        for (Map.Entry<String, io.github.tt432.eyelib.importer.addon.BedrockBinaryAsset> e
                : SoundAssetRegistry.filesView().entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                byte[] bytes = e.getValue().bytes();
                output.accept(location(namespace, e.getKey()),
                        () -> new ByteArrayInputStream(bytes));
            }
        }
        // sounds.json 不走列举：SoundManager 按命名空间直接 getResource（各空间内容不同）
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
                    net.minecraft.network.chat.Component.literal("eyelib addon sounds"),
                    net.minecraft.SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
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
                    net.minecraft.network.chat.Component.literal("eyelib addon sounds"),
                    net.minecraft.server.packs.repository.PackSource.DEFAULT,
                    java.util.Optional.empty());
    //?}

    @Override
    public void close() {
    }

    /**
     * bedrock 音效 id → MC 事件键：带命名空间的 "ns:path" 映射 "ns/path"（键不允许冒号），
     * 无命名空间 id 原样。与 {@link AddonSoundBridge#playPreview} 的映射互逆（单向确定性）。
     */
    static String toEventKey(String soundId) {
        return soundId.replace(':', '/');
    }

    /**
     * 现场合成 sounds.json：bedrock sound_definitions → MC 形态（全部事件落本包命名空间，
     * 键见 {@link #toEventKey}）。仅收录 .ogg 在包的条目（.fsb 无法解码）；
     * name 剥 sounds/ 前缀并冠本包命名空间。
     */
    private static byte[] synthesizeSoundsJson() {
        JsonObject root = new JsonObject();
        for (String id : SoundAssetRegistry.knownSoundIds()) {
            JsonObject def = SoundAssetRegistry.definition(id).orElse(null);
            if (def == null || !def.has("sounds") || !def.get("sounds").isJsonArray()) {
                continue;
            }
            JsonArray sounds = new JsonArray();
            for (JsonElement item : def.getAsJsonArray("sounds")) {
                String name;
                Float volume = null;
                Float pitch = null;
                Boolean stream = null;
                if (item.isJsonPrimitive()) {
                    name = item.getAsString();
                } else {
                    JsonObject obj = item.getAsJsonObject();
                    if (!obj.has("name")) {
                        continue;
                    }
                    name = obj.get("name").getAsString();
                    if (obj.has("volume")) {
                        volume = obj.get("volume").getAsFloat();
                    }
                    if (obj.has("pitch")) {
                        pitch = obj.get("pitch").getAsFloat();
                    }
                    if (obj.has("stream")) {
                        stream = obj.get("stream").getAsBoolean();
                    }
                }
                if (name.startsWith("sounds/")) {
                    name = name.substring("sounds/".length());
                }
                if (!SoundAssetRegistry.filesView().containsKey("sounds/" + name + ".ogg")) {
                    continue;
                }
                JsonObject sound = new JsonObject();
                sound.addProperty("name", NAMESPACE + ":" + name);
                if (volume != null) {
                    sound.addProperty("volume", volume);
                }
                if (pitch != null) {
                    sound.addProperty("pitch", pitch);
                }
                if (stream != null) {
                    sound.addProperty("stream", stream);
                }
                sounds.add(sound);
            }
            if (sounds.isEmpty()) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("category", mapCategory(
                    def.has("category") ? def.get("category").getAsString() : null));
            entry.add("sounds", sounds);
            root.add(toEventKey(id), entry);
        }
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String mapCategory(@org.jspecify.annotations.Nullable String bedrockCategory) {
        return bedrockCategory != null && MC_CATEGORIES.contains(bedrockCategory)
                ? bedrockCategory : "neutral";
    }
}
//?}
