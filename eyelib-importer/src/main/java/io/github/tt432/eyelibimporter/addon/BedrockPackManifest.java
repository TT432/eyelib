package io.github.tt432.eyelibimporter.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Bedrock 附加包的清单文件数据结构。
 * @author TT432 */
@NullMarked
public record BedrockPackManifest(
        int formatVersion,
        Header header,
        List<Module> modules,
        List<Dependency> dependencies,
        Metadata metadata,
        List<String> capabilities,
        List<Subpack> subpacks,
        List<BedrockResourceValue.ObjectValue> settings,
        Map<String, BedrockResourceValue> extraFields
) {
    public boolean isResourcePack() {
        return modules.stream().anyMatch(module -> module.type().equals("resources"));
    }

    public boolean isDataPack() {
        return modules.stream().anyMatch(module -> module.type().equals("data"));
    }

    public static BedrockPackManifest parse(com.google.gson.JsonObject root) {
        int formatVersion = root.has("format_version") ? root.get("format_version").getAsInt() : 2;
        Header header = Header.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, root.getAsJsonObject("header"))
                .getOrThrow(false, IllegalArgumentException::new);
        List<Module> modules = root.has("modules")
                ? Module.CODEC.listOf().parse(com.mojang.serialization.JsonOps.INSTANCE, root.get("modules")).getOrThrow(false, IllegalArgumentException::new)
                : List.of();
        List<Dependency> dependencies = new java.util.ArrayList<>();
        if (root.has("dependencies")) {
            for (com.google.gson.JsonElement element : root.getAsJsonArray("dependencies")) {
                dependencies.add(Dependency.parse(element.getAsJsonObject()));
            }
        }
        Metadata metadata = root.has("metadata") ? Metadata.parse(root.getAsJsonObject("metadata")) : Metadata.empty();
        List<String> capabilities = root.has("capabilities")
                ? Codec.STRING.listOf().parse(com.mojang.serialization.JsonOps.INSTANCE, root.get("capabilities")).getOrThrow(false, IllegalArgumentException::new)
                : List.of();
        List<Subpack> subpacks = new java.util.ArrayList<>();
        if (root.has("subpacks")) {
            for (com.google.gson.JsonElement element : root.getAsJsonArray("subpacks")) {
                subpacks.add(Subpack.parse(element.getAsJsonObject()));
            }
        }
        List<BedrockResourceValue.ObjectValue> settings = new java.util.ArrayList<>();
        if (root.has("settings")) {
            for (com.google.gson.JsonElement element : root.getAsJsonArray("settings")) {
                settings.add((BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(element));
            }
        }
        LinkedHashMap<String, BedrockResourceValue> extraFields = new LinkedHashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
            if (List.of("format_version", "header", "modules", "dependencies", "metadata", "capabilities", "subpacks", "settings").contains(entry.getKey())) {
                continue;
            }
            extraFields.put(entry.getKey(), BedrockResourceValue.fromJsonElement(entry.getValue()));
        }
        return new BedrockPackManifest(formatVersion, header, modules, List.copyOf(dependencies), metadata, capabilities, List.copyOf(subpacks), List.copyOf(settings), Map.copyOf(extraFields));
    }

    public record Metadata(
            List<String> authors,
            Map<String, List<String>> generatedWith,
            @Nullable String license,
            @Nullable String url,
            @Nullable String productType,
            Map<String, BedrockResourceValue> extraFields
    ) {
        public static Metadata empty() {
            return new Metadata(List.of(), Map.of(), null, null, null, Map.of());
        }

        public static Metadata parse(com.google.gson.JsonObject root) {
            List<String> authors = root.has("authors")
                    ? Codec.STRING.listOf().parse(com.mojang.serialization.JsonOps.INSTANCE, root.get("authors")).getOrThrow(false, IllegalArgumentException::new)
                    : List.of();
            LinkedHashMap<String, List<String>> generatedWith = new LinkedHashMap<>();
            if (root.has("generated_with")) {
                com.google.gson.JsonObject generatedWithRoot = root.getAsJsonObject("generated_with");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : generatedWithRoot.entrySet()) {
                    generatedWith.put(entry.getKey(), Codec.STRING.listOf().parse(com.mojang.serialization.JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, IllegalArgumentException::new));
                }
            }
            LinkedHashMap<String, BedrockResourceValue> extraFields = new LinkedHashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
                if (List.of("authors", "generated_with", "license", "url", "product_type").contains(entry.getKey())) {
                    continue;
                }
                extraFields.put(entry.getKey(), BedrockResourceValue.fromJsonElement(entry.getValue()));
            }
            return new Metadata(
                    authors,
                    Map.copyOf(generatedWith),
                    root.has("license") ? root.get("license").getAsString() : null,
                    root.has("url") ? root.get("url").getAsString() : null,
                    root.has("product_type") ? root.get("product_type").getAsString() : null,
                    Map.copyOf(extraFields)
            );
        }
    }

    public record Header(
            String name,
            String description,
            String uuid,
            BedrockVersionValue version,
            BedrockVersionValue minEngineVersion,
            @Nullable String packOptimizationVersion
    ) {
        public static final Codec<Header> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name", "").forGetter(Header::name),
                Codec.STRING.optionalFieldOf("description", "").forGetter(Header::description),
                Codec.STRING.fieldOf("uuid").forGetter(Header::uuid),
                BedrockVersionValue.CODEC.optionalFieldOf("version", BedrockVersionValue.numeric(List.of())).forGetter(Header::version),
                BedrockVersionValue.CODEC.optionalFieldOf("min_engine_version", BedrockVersionValue.numeric(List.of())).forGetter(Header::minEngineVersion),
                Codec.STRING.optionalFieldOf("pack_optimization_version").forGetter(header -> Optional.ofNullable(header.packOptimizationVersion()))
        ).apply(instance, (name, description, uuid, version, minEngineVersion, packOptimizationVersion) ->
                new Header(name, description, uuid, version, minEngineVersion, packOptimizationVersion.orElse(null))));
    }

    public record Module(
            String type,
            String uuid,
            BedrockVersionValue version,
            @Nullable String description,
            @Nullable String language,
            @Nullable String entry
    ) {
        public static final Codec<Module> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(value -> value.toLowerCase(Locale.ROOT), value -> value).fieldOf("type").forGetter(Module::type),
                Codec.STRING.fieldOf("uuid").forGetter(Module::uuid),
                BedrockVersionValue.CODEC.optionalFieldOf("version", BedrockVersionValue.numeric(List.of())).forGetter(Module::version),
                Codec.STRING.optionalFieldOf("description").forGetter(module -> java.util.Optional.ofNullable(module.description())),
                Codec.STRING.optionalFieldOf("language").forGetter(module -> java.util.Optional.ofNullable(module.language())),
                Codec.STRING.optionalFieldOf("entry").forGetter(module -> java.util.Optional.ofNullable(module.entry()))
        ).apply(instance, (type, uuid, version, description, language, entry) ->
                new Module(type, uuid, version, description.orElse(null), language.orElse(null), entry.orElse(null))));
    }

    public record Dependency(
            @Nullable String uuid,
            @Nullable String moduleName,
            BedrockVersionValue version
    ) {
        public static Dependency parse(com.google.gson.JsonObject root) {
            return new Dependency(
                    root.has("uuid") ? root.get("uuid").getAsString() : null,
                    root.has("module_name") ? root.get("module_name").getAsString() : null,
                    root.has("version")
                            ? BedrockVersionValue.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, root.get("version")).getOrThrow(false, IllegalArgumentException::new)
                            : BedrockVersionValue.numeric(List.of())
            );
        }

        public boolean hasReference() {
            return uuid != null || moduleName != null;
        }
    }

    /** 子包信息，来自 manifest.json 的 subpacks 字段。
     * @author TT432 */
    public record Subpack(
            String folderName,
            String name,
            int memoryPerformanceTier
    ) {
        public static Subpack parse(com.google.gson.JsonObject root) {
            return new Subpack(
                    root.get("folder_name").getAsString(),
                    root.get("name").getAsString(),
                    root.has("memory_performance_tier") ? root.get("memory_performance_tier").getAsInt() : 0
            );
        }
    }
}