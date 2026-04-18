package io.github.tt432.eyelibimporter.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record BedrockPackManifest(
        int formatVersion,
        Header header,
        List<Module> modules,
        List<Dependency> dependencies,
        Metadata metadata,
        List<String> capabilities,
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
        List<BedrockResourceValue.ObjectValue> settings = new java.util.ArrayList<>();
        if (root.has("settings")) {
            for (com.google.gson.JsonElement element : root.getAsJsonArray("settings")) {
                settings.add((BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(element));
            }
        }
        LinkedHashMap<String, BedrockResourceValue> extraFields = new LinkedHashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
            if (List.of("format_version", "header", "modules", "dependencies", "metadata", "capabilities", "settings").contains(entry.getKey())) {
                continue;
            }
            extraFields.put(entry.getKey(), BedrockResourceValue.fromJsonElement(entry.getValue()));
        }
        return new BedrockPackManifest(formatVersion, header, modules, List.copyOf(dependencies), metadata, capabilities, List.copyOf(settings), Map.copyOf(extraFields));
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
            List<Integer> version,
            List<Integer> minEngineVersion
    ) {
        public static final Codec<Header> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name", "").forGetter(Header::name),
                Codec.STRING.optionalFieldOf("description", "").forGetter(Header::description),
                Codec.STRING.fieldOf("uuid").forGetter(Header::uuid),
                Codec.INT.listOf().optionalFieldOf("version", List.of()).forGetter(Header::version),
                Codec.INT.listOf().optionalFieldOf("min_engine_version", List.of()).forGetter(Header::minEngineVersion)
        ).apply(instance, Header::new));
    }

    public record Module(
            String type,
            String uuid,
            List<Integer> version,
            @Nullable String description
    ) {
        public static final Codec<Module> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(value -> value.toLowerCase(Locale.ROOT), value -> value).fieldOf("type").forGetter(Module::type),
                Codec.STRING.fieldOf("uuid").forGetter(Module::uuid),
                Codec.INT.listOf().optionalFieldOf("version", List.of()).forGetter(Module::version),
                Codec.STRING.optionalFieldOf("description").forGetter(module -> java.util.Optional.ofNullable(module.description()))
        ).apply(instance, (type, uuid, version, description) -> new Module(type, uuid, version, description.orElse(null))));
    }

    public record Dependency(
            @Nullable String uuid,
            @Nullable String moduleName,
            List<Integer> version
    ) {
        public static Dependency parse(com.google.gson.JsonObject root) {
            return new Dependency(
                    root.has("uuid") ? root.get("uuid").getAsString() : null,
                    root.has("module_name") ? root.get("module_name").getAsString() : null,
                    root.has("version")
                            ? Codec.INT.listOf().parse(com.mojang.serialization.JsonOps.INSTANCE, root.get("version")).getOrThrow(false, IllegalArgumentException::new)
                            : List.of()
            );
        }

        public boolean hasReference() {
            return uuid != null || moduleName != null;
        }
    }
}
