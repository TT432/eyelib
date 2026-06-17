package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelibimporter.block.BrBlock;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.item.BrItem;
import io.github.tt432.eyelibimporter.material.BrMaterial;
import io.github.tt432.eyelibimporter.material.BrMaterialEntry;
import io.github.tt432.eyelibimporter.model.importer.BedrockGeometryImporter;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelibimporter.model.importer.ModelImporter;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibimporter.recipe.BrRecipe;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllerEntry;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllers;
import io.github.tt432.eyelibimporter.trading.BrTrading;
import io.github.tt432.eyelibmodel.Model;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bedrock 附加包的加载器，支持目录和压缩包（zip/mcpack/mcaddon）。
 * <p>
 * 压缩包内容通过 ZipFile 流式读取，保留在内存中，不再解压到磁盘。
 *
 * @author TT432
 */
@NullMarked
public final class BedrockAddonLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockAddonLoader.class);

    private BedrockAddonLoader() {
    }

    public static BedrockAddon load(Path source) throws IOException {
        List<PackFiles> packFilesList = new ArrayList<>();
        List<ZipFile> openZips = new ArrayList<>();
        try {
            collectPackRoots(source, packFilesList, openZips);
            packFilesList.sort(Comparator.comparing(pf -> pf.sourceName()));

            List<BedrockAddonPack> unsortedPacks = new ArrayList<>();
            List<BedrockAddonWarning> warnings = new ArrayList<>();
            LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources = new LinkedHashMap<>();

            for (PackFiles pf : packFilesList) {
                BedrockAddonPack pack = loadPack(pf.sourceName(), pf.entries());
                unsortedPacks.add(pack);
            }

            List<BedrockAddonPack> packs = sortPacksByDependencies(unsortedPacks, warnings);

            for (BedrockAddonPack pack : packs) {
                warnings.addAll(pack.warnings());
                pack.unmanagedResources().forEach((path, resource) ->
                        unmanagedResources.put(pack.sourceName() + ":" + path, resource));
            }

            BedrockAddonAggregate aggregate = BedrockAddonAggregate.fromPacks(packs, warnings);

            return new BedrockAddon(packs, warnings, unmanagedResources, aggregate);
        } finally {
            for (ZipFile zf : openZips) {
                try { zf.close(); } catch (IOException ignored) {}
            }
        }
    }

    // 子包选择

    /** 按基岩版规则自动选择子包：取最高 memoryPerformanceTier，同 tier 取最后一个。 */
    private static @Nullable String selectSubpack(List<BedrockPackManifest.Subpack> subpacks) {
        if (subpacks.isEmpty()) {
            return null;
        }
        String lastBest = null;
        int bestTier = -1;
        for (BedrockPackManifest.Subpack sp : subpacks) {
            int tier = sp.memoryPerformanceTier();
            if (tier >= bestTier) {
                bestTier = tier;
                lastBest = sp.folderName();
            }
        }
        return lastBest;
    }

    private static @Nullable String extractSubpackFolder(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("subpacks/")) {
            return null;
        }
        int start = "subpacks/".length();
        int slashAfterFolder = relativePath.indexOf('/', start);
        if (slashAfterFolder > start) {
            return relativePath.substring(start, slashAfterFolder);
        }
        return null;
    }

    private static String stripSubpackPrefix(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.startsWith("subpacks/")) {
            int slashAfterFolder = relativePath.indexOf('/', "subpacks/".length());
            if (slashAfterFolder > 0) {
                return relativePath.substring(slashAfterFolder + 1);
            }
        }
        return relativePath;
    }

    // Pack 加载核心

    /**
     * 从文件条目列表加载一个包。
     *
     * @param sourceName 来源名称（用于警告/日志）
     * @param allEntries 包内所有文件条目
     */
    private static BedrockAddonPack loadPack(String sourceName, List<FileEntry> allEntries) throws IOException {
        // 找到 manifest.json
        FileEntry manifestEntry = allEntries.stream()
                .filter(e -> e.lowerEffectivePath().equals("manifest.json"))
                .findFirst()
                .orElseThrow(() -> new IOException("Missing manifest.json in pack: " + sourceName));

        JsonObject manifestJson = readJsonFile(manifestEntry);
        BedrockPackManifest manifest = BedrockPackManifest.parse(manifestJson);
        String selectedSubpack = selectSubpack(manifest.subpacks());
        PackAccumulator acc = new PackAccumulator(sourceName);

        warnForUnmanagedManifestFields(sourceName, manifest, acc.warnings);

        // 按子包过滤
        List<FileEntry> entries = allEntries.stream()
                .filter(e -> {
                    String subpackFolder = extractSubpackFolder(e.relativePath());
                    return selectedSubpack == null || subpackFolder == null || subpackFolder.equals(selectedSubpack);
                })
                .toList();

        for (FileEntry entry : entries) {
            if (entry.lowerEffectivePath().equals("manifest.json")) {
                continue;
            }
            if (entry.lowerEffectivePath().equals("pack_icon.png")) {
                acc.loadPackIcon(entry);
                continue;
            }
            if (entry.family() == BedrockResourceFamily.TEXTURE) {
                acc.loadTexture(entry);
                continue;
            }
            try {
                processEntry(acc, entry);
            } catch (RuntimeException exception) {
                captureUnmanaged(acc, entry, BedrockUnmanagedReason.SCHEMA_PARSE_FAILED, true,
                        exception.getMessage());
            }
        }

        return acc.build(manifest, selectedSubpack);
    }

    /** 按文件类型分发到对应的解析器。 */
    private static void processEntry(PackAccumulator acc, FileEntry entry) throws IOException {
        switch (entry.family()) {
            case MATERIAL ->                acc.parseAndStore(entry, BrMaterial.CODEC, acc.materialFiles);
            case ANIMATION ->               acc.parseAndStore(entry, BrAnimationSet.CODEC, acc.animationFiles);
            case ANIMATION_CONTROLLER ->    acc.parseAndStore(entry, BrAnimationControllerSet.CODEC, acc.animationControllerFiles);
            case CLIENT_ENTITY ->           acc.parseAndStore(entry, BrClientEntity.CODEC, acc.clientEntityFiles);
            case ATTACHABLE ->              acc.parseAndStore(entry, BrClientEntity.ATTACHABLE_CODEC, acc.attachables);
            case PARTICLE ->                acc.parseAndStore(entry, BrParticle.CODEC, acc.particleFiles);
            case RENDER_CONTROLLER -> {
                BrRenderControllers controllers = BrRenderControllers.CODEC.parse(JsonOps.INSTANCE,
                        readJsonElement(entry)).getOrThrow(false, IllegalArgumentException::new);
                mergeRenderControllers(acc.renderControllerFiles, entry.effectivePath(), controllers.renderControllers());
            }
            case MODEL -> {
                // 优先用 BedrockGeometryImporter.importJson 从内存 JSON 读取
                try {
                    JsonObject json = readJsonFile(entry);
                    acc.modelFiles.put(entry.effectivePath(),
                            new BedrockImportedModels(new LinkedHashMap<>(BedrockGeometryImporter.importJson(json))));
                } catch (Exception ignored) {
                    // fallback: 写入临时文件后调用 ModelImporter.importFile
                    Path modelFile = entry.file();
                    if (modelFile == null) {
                        modelFile = Files.createTempFile("eyelib-model-", ".json");
                        try {
                            Files.write(modelFile, entry.dataSupplier().get());
                        } catch (IOException ex) {
                            Files.deleteIfExists(modelFile);
                            throw ex;
                        }
                    }
                    try {
                        acc.modelFiles.put(entry.effectivePath(),
                                new BedrockImportedModels(new LinkedHashMap<>(ModelImporter.importFile(modelFile))));
                    } finally {
                        if (entry.dataSupplier() != null) {
                            Files.deleteIfExists(modelFile);
                        }
                    }
                }
            }
            case SOUND_INDEX ->
                acc.soundIndexFiles.put(entry.effectivePath(), BrSoundIndex.parse(readJsonFile(entry)));
            case SOUND_DEFINITION ->
                acc.soundDefinitionFiles.put(entry.effectivePath(), BrSoundDefinitions.parse(readJsonFile(entry)));
            case LOCALIZATION -> {
                if (entry.lowerEffectivePath().endsWith("languages.json")) break;
                acc.languageFiles.put(entry.effectivePath(), BrLanguageFile.parse(readString(entry)));
            }
            case BEHAVIOR_ENTITY ->
                acc.behaviorEntityFiles.put(entry.effectivePath(), BrBehaviorEntityFile.parse(readJsonFile(entry)));
            case SPAWN_RULE ->
                acc.spawnRulesFiles.put(entry.effectivePath(), BrSpawnRule.parse(readJsonFile(entry)));
            case SOUND_FILE ->
                acc.soundFiles.put(entry.effectivePath(), new BedrockBinaryAsset(extensionOf(entry.effectivePath()),
                        readBytes(entry)));
            case TEXTURE_INDEX ->
                acc.textureIndexFiles.put(entry.effectivePath(),
                        new BrTextureIndexFile(BedrockResourceValue.fromJsonElement(readJsonElement(entry))));
            case TEXTURE_METADATA ->
                acc.textureMetadataFiles.put(entry.effectivePath(),
                        new BrTextureMetadataFile((BedrockResourceValue.ObjectValue)
                                BedrockResourceValue.fromJsonElement(readJsonFile(entry))));
            case RECIPE ->
                acc.parseAndStore(entry, BrRecipe.CODEC, acc.recipeFiles);
            case LOOT_TABLE ->
                acc.parseAndStore(entry, BrLootTable.CODEC, acc.lootTableFiles);
            case ITEM ->
                acc.parseAndStore(entry, BrItem.CODEC, acc.itemFiles);
            case BLOCK ->
                acc.parseAndStore(entry, BrBlock.CODEC, acc.blockFiles);
            case TRADING ->
                acc.parseAndStore(entry, BrTrading.CODEC, acc.tradeFiles);
            case SPLASHES ->
                acc.splashIndex = BedrockResourceValue.fromJsonElement(readJsonFile(entry));
            case BRARCHIVE ->
                loadBrarchive(acc, entry);
            default ->
                captureUnmanaged(acc, entry, unmanagedReasonFor(entry.family()), false, null);
        }
    }

    // Pack 资源累加器

    /** 单个包加载过程中的资源容器。 */
    @NullMarked
    static final class PackAccumulator {
        final String sourceName;
        final List<BedrockAddonWarning> warnings = new ArrayList<>();

        final LinkedHashMap<String, BrAnimationSet> animationFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrAnimationControllerSet> animationControllerFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrClientEntity> clientEntityFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrClientEntity> attachables = new LinkedHashMap<>();
        final LinkedHashMap<String, BrBehaviorEntityFile> behaviorEntityFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BedrockImportedModels> modelFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, ImportedImageData> textures = new LinkedHashMap<>();
        final LinkedHashMap<String, BrSoundIndex> soundIndexFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrSoundDefinitions> soundDefinitionFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrLanguageFile> languageFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BedrockBinaryAsset> soundFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrTextureIndexFile> textureIndexFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrTextureMetadataFile> textureMetadataFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrRenderControllers> renderControllerFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrParticle> particleFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrMaterial> materialFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrSpawnRule> spawnRulesFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrLootTable> lootTableFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrRecipe> recipeFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrItem> itemFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrBlock> blockFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BrTrading> tradeFiles = new LinkedHashMap<>();
        final LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources = new LinkedHashMap<>();
        @Nullable ImportedImageData packIcon;
        @Nullable BedrockResourceValue splashIndex;

        PackAccumulator(String sourceName) {
            this.sourceName = sourceName;
        }

        String sourceName() {
            return sourceName;
        }

        /** 通用 JSON 解析：读文件 → Codec.parse → 放入目标 map（effectivePath 为 key）。解析失败时回退为未托管资源。 */
        <T> void parseAndStore(FileEntry entry, Codec<T> codec, LinkedHashMap<String, T> dest) throws IOException {
            JsonObject json = readJsonFile(entry);
            DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
            if (result.error().isPresent()) {
                captureUnmanaged(this, entry, BedrockUnmanagedReason.SCHEMA_PARSE_FAILED, true,
                        entry.family() + " codec error: " + result.error().get().message());
                return;
            }
            result.result().ifPresent(value -> dest.put(entry.effectivePath(), value));
        }

        void loadPackIcon(FileEntry entry) throws IOException {
            byte[] data = readBytes(entry);
            try {
                packIcon = ImportedImageData.decodePng(data);
            } catch (RuntimeException e) {
                BedrockResourceContent content = new BedrockResourceContent.BinaryContent(data);
                unmanagedResources.put(entry.relativePath(),
                        new BedrockUnmanagedResource(entry.family(), entry.relativePath(), content,
                                BedrockUnmanagedReason.SCHEMA_PARSE_FAILED));
                warnings.add(warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED, entry.effectivePath(),
                        "pack_icon decode: " + e.getMessage()));
            }
        }

        void loadTexture(FileEntry entry) throws IOException {
            byte[] data = readBytes(entry);
            try {
                if (entry.lowerEffectivePath().endsWith(".tga")) {
                    textures.put(entry.effectivePath(), Objects.requireNonNull(ImportedImageData.decodeTga(data)));
                } else {
                    textures.put(entry.effectivePath(), Objects.requireNonNull(ImportedImageData.decodePng(data)));
                }
            } catch (RuntimeException e) {
                BedrockResourceContent content = new BedrockResourceContent.BinaryContent(data);
                unmanagedResources.put(entry.relativePath(),
                        new BedrockUnmanagedResource(entry.family(), entry.relativePath(), content,
                                BedrockUnmanagedReason.SCHEMA_PARSE_FAILED));
                warnings.add(warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED, entry.effectivePath(),
                        "texture decode: " + e.getMessage()));
            }
        }

        BedrockAddonPack build(BedrockPackManifest manifest, @Nullable String selectedSubpack) {
            return new BedrockAddonPack(
                    sourceName(), manifest, selectedSubpack,
                    animationFiles, animationControllerFiles,
                    clientEntityFiles, attachables, modelFiles, textures,
                    soundIndexFiles, soundDefinitionFiles, languageFiles,
                    behaviorEntityFiles, soundFiles,
                    textureIndexFiles, textureMetadataFiles,
                    renderControllerFiles, particleFiles, materialFiles,
                    spawnRulesFiles, lootTableFiles, itemFiles, blockFiles, recipeFiles, tradeFiles,
                    unmanagedResources, List.copyOf(warnings),
                    packIcon, splashIndex
            );
        }
    }

    // 文件条目

    /**
     * 包中一个待处理文件的元数据。
     * 支持三种来源：磁盘路径（fromPath）、ZIP 引用（fromZipRef）。
     * ZIP 模式下内容按需读取，不预加载到内存。
     */
    private record FileEntry(
            @Nullable Path file,
            String relativePath,
            String effectivePath,
            @Nullable Supplier<byte[]> dataSupplier
    ) {
        /** 从磁盘路径创建文件条目（内容延迟读取）。 */
        static FileEntry fromPath(Path file, String relativePath, String effectivePath) {
            return new FileEntry(file, relativePath, effectivePath, null);
        }

        /** 从 ZIP 引用创建文件条目（内容在首次访问时才从 ZipFile 读取）。 */
        static FileEntry fromZipRef(ZipFile zf, ZipEntry ze, String relativePath, String effectivePath) {
            return new FileEntry(null, relativePath, effectivePath, () -> {
                try {
                    return readZipEntryBytes(zf, ze);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        String lowerEffectivePath() {
            return effectivePath.toLowerCase(Locale.ROOT);
        }

        BedrockResourceFamily family() {
            return BedrockResourceFamily.classify(effectivePath);
        }
    }

    // 包文件集合

    /** 一个包的来源名称及其文件条目列表。 */
    private record PackFiles(String sourceName, List<FileEntry> entries) {
    }

    // BrArchive 加载

    @SuppressWarnings("unchecked")
    private static void loadBrarchive(PackAccumulator acc, FileEntry entry) throws IOException {
        byte[] jsonBytes;
        // BrArchiveDecoder.extractJson 只接受 Path，因此非磁盘来源需写入临时文件
        if (entry.dataSupplier() != null) {
            Path tempFile = Files.createTempFile("eyelib-brarchive-", ".bin");
            try {
                Files.write(tempFile, entry.dataSupplier().get());
                jsonBytes = BrArchiveDecoder.extractJson(tempFile);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } else {
            jsonBytes = BrArchiveDecoder.extractJson(entry.file());
        }

        if (jsonBytes.length == 0) {
            return;
        }
        String fullJson = new String(jsonBytes, StandardCharsets.UTF_8);
        String name = entry.lowerEffectivePath().replace("__brarchive/", "");

        if (name.startsWith("animations.")) {
            var merged = parseMultipleChunks(fullJson, BrAnimationSet.CODEC,
                    BrAnimationSet::animations, acc.warnings, entry, "animation");
            mergeAnimations(acc.animationFiles, entry.effectivePath(), merged);
        } else if (name.startsWith("animation_controllers.")) {
            var merged = new LinkedHashMap<String, BrAnimationControllerSchema>();
            for (String chunk : splitConcatenatedJson(fullJson)) {
                merged.putAll(parseAnimationControllerChunk(chunk, acc, entry));
            }
            mergeAnimationControllers(acc.animationControllerFiles, entry.effectivePath(), merged);
        } else if (name.startsWith("entity.") || name.startsWith("entities.")) {
            for (String chunk : splitConcatenatedJson(fullJson)) {
                try {
                    var element = parseJsonLenient(chunk);
                    normalizeRenderControllers(element);
                    DataResult<BrClientEntity> result = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, element);
                    result.error().ifPresent(err -> acc.warnings.add(
                            warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED, entry.effectivePath(),
                                    "entity: " + err.message())));
                    result.result().ifPresent(entity -> acc.clientEntityFiles.put(entity.identifier(), entity));
                } catch (RuntimeException ignored) {
                }
            }
        } else if (name.startsWith("attachables.")) {
            for (String chunk : splitConcatenatedJson(fullJson)) {
                try {
                    DataResult<BrClientEntity> result = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE,
                            parseJsonLenient(chunk));
                    result.error().ifPresent(err -> acc.warnings.add(
                            warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED, entry.effectivePath(),
                                    "attachable: " + err.message())));
                    result.result().ifPresent(a -> acc.attachables.put(a.identifier(), a));
                } catch (RuntimeException ignored) {
                }
            }
        } else if (name.startsWith("particles.")) {
            for (String chunk : splitConcatenatedJson(fullJson)) {
                try {
                    DataResult<BrParticle> result = BrParticle.CODEC.parse(JsonOps.INSTANCE, parseJsonLenient(chunk));
                    result.error().ifPresent(err -> acc.warnings.add(
                            warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED, entry.effectivePath(),
                                    "particle: " + err.message())));
                    result.result().ifPresent(p ->
                            acc.particleFiles.put(p.particleEffect().description().identifier(), p));
                } catch (RuntimeException ignored) {
                }
            }
        } else if (name.startsWith("render_controllers.")) {
            var merged = new LinkedHashMap<String, BrRenderControllerEntry>();
            for (String chunk : splitConcatenatedJson(fullJson)) {
                merged.putAll(parseRenderControllerChunk(chunk, acc, entry));
            }
            mergeRenderControllers(acc.renderControllerFiles, entry.effectivePath(), merged);
        } else if (name.startsWith("materials.")) {
            var merged = new LinkedHashMap<String, BrMaterialEntry>();
            for (String chunk : splitConcatenatedJson(fullJson)) {
                DataResult<BrMaterial> result = BrMaterial.CODEC.parse(JsonOps.INSTANCE, parseJsonLenient(chunk));
                result.error().ifPresent(err -> acc.warnings.add(
                        warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED, entry.effectivePath(),
                                "material chunk: " + err.message())));
                result.result().ifPresent(mat -> merged.putAll(mat.materials()));
            }
            mergeMaterials(acc.materialFiles, entry.effectivePath(), merged);
        } else if (name.startsWith("models")) {
            var merged = new LinkedHashMap<String, Model>();
            for (String chunk : splitConcatenatedJson(fullJson)) {
                try {
                    merged.putAll(BedrockGeometryImporter.importJson(
                            parseJsonLenient(chunk).getAsJsonObject()));
                } catch (Exception ignored) {
                }
            }
            BedrockImportedModels previous = acc.modelFiles.get(entry.effectivePath());
            if (previous == null) {
                acc.modelFiles.put(entry.effectivePath(), new BedrockImportedModels(merged));
            } else {
                var mergedAll = new LinkedHashMap<String, Model>();
                mergedAll.putAll(previous.models());
                mergedAll.putAll(merged);
                acc.modelFiles.put(entry.effectivePath(), new BedrockImportedModels(mergedAll));
            }
        } else {
            acc.unmanagedResources.put(entry.relativePath(), new BedrockUnmanagedResource(
                    BedrockResourceFamily.BRARCHIVE, entry.relativePath(),
                    new BedrockResourceContent.StructuredContent(
                            BedrockResourceValue.fromJsonElement(parseJsonLenient(fullJson))),
                    BedrockUnmanagedReason.NO_TYPED_SCHEMA_YET));
        }
    }

    /** Codec 驱动的多 chunk 解析器：每个 chunk 用给定 codec 解析，用 keyExtractor 提取条目名。 */
    private static <C, T> LinkedHashMap<String, T> parseMultipleChunks(String json, Codec<C> codec,
                                                                        java.util.function.Function<C, Map<String, T>> keyExtractor,
                                                                        List<BedrockAddonWarning> warnings,
                                                                        FileEntry entry, String label) {
        var merged = new LinkedHashMap<String, T>();
        for (String chunk : splitConcatenatedJson(json)) {
            DataResult<C> result = codec.parse(JsonOps.INSTANCE, parseJsonLenient(chunk));
            result.error().ifPresent(err -> warnings.add(
                    warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED, entry.effectivePath(),
                            label + " chunk: " + err.message())));
            result.result().ifPresent(set -> merged.putAll(keyExtractor.apply(set)));
        }
        return merged;
    }

    // 合并辅助方法

    private static void mergeAnimations(LinkedHashMap<String, BrAnimationSet> files,
                                        String effectivePath,
                                        Map<String, BrAnimationEntrySchema> animations) {
        BrAnimationSet previous = files.get(effectivePath);
        if (previous == null) {
            files.put(effectivePath, new BrAnimationSet(new LinkedHashMap<>(animations)));
            return;
        }
        var merged = new LinkedHashMap<>(previous.animations());
        merged.putAll(animations);
        files.put(effectivePath, new BrAnimationSet(merged));
    }

    private static void mergeAnimationControllers(LinkedHashMap<String, BrAnimationControllerSet> files,
                                                   String effectivePath,
                                                   Map<String, BrAnimationControllerSchema> controllers) {
        BrAnimationControllerSet previous = files.get(effectivePath);
        if (previous == null) {
            files.put(effectivePath, new BrAnimationControllerSet(new LinkedHashMap<>(controllers)));
            return;
        }
        var merged = new LinkedHashMap<>(previous.animationControllers());
        merged.putAll(controllers);
        files.put(effectivePath, new BrAnimationControllerSet(merged));
    }

    private static void mergeRenderControllers(LinkedHashMap<String, BrRenderControllers> files,
                                                String effectivePath,
                                                Map<String, BrRenderControllerEntry> controllers) {
        BrRenderControllers previous = files.get(effectivePath);
        if (previous == null) {
            files.put(effectivePath, new BrRenderControllers(Map.copyOf(controllers)));
            return;
        }
        var merged = new LinkedHashMap<>(previous.renderControllers());
        for (Map.Entry<String, BrRenderControllerEntry> e : controllers.entrySet()) {
            BrRenderControllerEntry existing = merged.get(e.getKey());
            if (existing != null && existing.partVisibility().size() > e.getValue().partVisibility().size()) {
                continue;
            }
            merged.put(e.getKey(), e.getValue());
        }
        files.put(effectivePath, new BrRenderControllers(Map.copyOf(merged)));
    }

    private static void mergeMaterials(LinkedHashMap<String, BrMaterial> files,
                                        String effectivePath,
                                        Map<String, BrMaterialEntry> materials) {
        BrMaterial previous = files.get(effectivePath);
        if (previous == null) {
            files.put(effectivePath, new BrMaterial(new LinkedHashMap<>(materials)));
            return;
        }
        var merged = new LinkedHashMap<>(previous.materials());
        merged.putAll(materials);
        files.put(effectivePath, new BrMaterial(merged));
    }

    // BrArchive 低级解析

    private static Map<String, BrAnimationControllerSchema> parseAnimationControllerChunk(
            String json, PackAccumulator acc, FileEntry entry) {
        var element = parseJsonLenient(json);
        DataResult<BrAnimationControllerSet> setResult = BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, element);
        if (setResult.result().isPresent() && !setResult.result().get().animationControllers().isEmpty()) {
            return setResult.result().get().animationControllers();
        }
        if (!element.isJsonObject()) return Map.of();
        var merged = new LinkedHashMap<String, BrAnimationControllerSchema>();
        for (Map.Entry<String, JsonElement> je : element.getAsJsonObject().entrySet()) {
            if (!je.getKey().startsWith("controller.")) continue;
            try {
                DataResult<BrAnimationControllerSchema> controllerResult =
                        BrAnimationControllerSchema.CODEC.parse(JsonOps.INSTANCE, je.getValue());
                controllerResult.error().ifPresent(err ->
                        acc.warnings.add(warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED,
                                entry.effectivePath(),
                                "animation controller entry '" + je.getKey() + "': " + err.message())));
                controllerResult.result().ifPresent(c -> merged.put(je.getKey(), c));
            } catch (RuntimeException exception) {
                acc.warnings.add(warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED,
                        entry.effectivePath(),
                        "animation controller parse fail for " + je.getKey() + ": " + exception.getMessage()));
            }
        }
        return merged;
    }

    private static Map<String, BrRenderControllerEntry> parseRenderControllerChunk(
            String json, PackAccumulator acc, FileEntry entry) {
        var merged = new LinkedHashMap<String, BrRenderControllerEntry>();
        for (String objectJson : extractNamedObjects(json, "render_controllers")) {
            JsonObject controllers = parseJsonLenient(objectJson).getAsJsonObject();
            for (Map.Entry<String, JsonElement> je : controllers.entrySet()) {
                try {
                    DataResult<BrRenderControllerEntry> result =
                            BrRenderControllerEntry.CODEC.parse(JsonOps.INSTANCE, je.getValue());
                    result.error().ifPresent(err ->
                            acc.warnings.add(warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED,
                                    entry.effectivePath(),
                                    "render controller '" + je.getKey() + "': " + err.message())));
                    result.result().ifPresent(c -> merged.put(je.getKey(), c));
                } catch (RuntimeException exception) {
                    acc.warnings.add(warn(BedrockAddonWarningCode.SCHEMA_PARSE_FAILED,
                            entry.effectivePath(),
                            "render controller parse fail for " + je.getKey() + ": " + exception.getMessage()));
                }
            }
        }
        return merged;
    }

    // 警告 / 日志辅助

    private static String warnSource(PackAccumulator acc) {
        return acc != null ? acc.sourceName() : "<unknown>";
    }

    private static BedrockAddonWarning warn(BedrockAddonWarningCode code, String location, String detail) {
        return new BedrockAddonWarning(BedrockAddonWarningSeverity.WARNING, code, location, location, detail);
    }

    private static BedrockAddonWarning error(BedrockAddonWarningCode code, String source, String location, String detail) {
        return new BedrockAddonWarning(BedrockAddonWarningSeverity.ERROR, code, source, location, detail);
    }

    private static void captureUnmanaged(PackAccumulator acc, FileEntry entry,
                                          BedrockUnmanagedReason reason,
                                          boolean parseFailure,
                                          @Nullable String parseMessage) throws IOException {
        BedrockResourceContent content = readContent(entry);
        acc.unmanagedResources.put(entry.relativePath(),
                new BedrockUnmanagedResource(entry.family(), entry.relativePath(), content, reason));
        acc.warnings.add(new BedrockAddonWarning(
                parseFailure ? BedrockAddonWarningSeverity.ERROR : BedrockAddonWarningSeverity.WARNING,
                parseFailure ? BedrockAddonWarningCode.SCHEMA_PARSE_FAILED : BedrockAddonWarningCode.UNMANAGED_RESOURCE,
                acc.sourceName(), entry.relativePath(),
                parseFailure
                        ? "Managed family parse failed; retained as unmanaged because: " + parseMessage
                        : "Resource is retained but unmanaged because: " + reason));
    }

    // Manifest

    private static void warnForUnmanagedManifestFields(String sourceName, BedrockPackManifest manifest,
                                                        List<BedrockAddonWarning> warnings) {
        manifest.extraFields().forEach((key, value) -> warnings.add(new BedrockAddonWarning(
                BedrockAddonWarningSeverity.WARNING,
                BedrockAddonWarningCode.MANIFEST_FIELD_UNMANAGED,
                sourceName, "manifest.json",
                "Manifest top-level field is retained but not modeled semantically: " + key)));
        manifest.metadata().extraFields().forEach((key, value) -> warnings.add(new BedrockAddonWarning(
                BedrockAddonWarningSeverity.WARNING,
                BedrockAddonWarningCode.MANIFEST_FIELD_UNMANAGED,
                sourceName, "manifest.json",
                "Manifest metadata field is retained but not modeled semantically: metadata." + key)));
    }

    private static BedrockUnmanagedReason unmanagedReasonFor(BedrockResourceFamily family) {
        return switch (family) {
            case ITEM, BLOCK, RECIPE, SPAWN_RULE, TRADING, FEATURE, FEATURE_RULE, STRUCTURE, SCRIPT, BIOME ->
                    BedrockUnmanagedReason.OUTSIDE_IMPORTER_SCOPE;
            case UI, FOG, UNKNOWN_JSON, UNKNOWN_TEXT, UNKNOWN_BINARY ->
                    BedrockUnmanagedReason.NO_TYPED_SCHEMA_YET;
            default -> BedrockUnmanagedReason.UNKNOWN_LAYOUT;
        };
    }

    // 依赖排序

    private static List<BedrockAddonPack> sortPacksByDependencies(List<BedrockAddonPack> packs,
                                                                   List<BedrockAddonWarning> warnings) {
        Map<String, BedrockAddonPack> providedByUuid = new LinkedHashMap<>();
        for (BedrockAddonPack pack : packs) {
            providedByUuid.put(pack.manifest().header().uuid(), pack);
            pack.manifest().modules().forEach(module -> providedByUuid.put(module.uuid(), pack));
        }

        for (BedrockAddonPack pack : packs) {
            for (BedrockPackManifest.Dependency dependency : pack.manifest().dependencies()) {
                if (dependency.uuid() != null && !providedByUuid.containsKey(dependency.uuid())) {
                    warnings.add(new BedrockAddonWarning(
                            BedrockAddonWarningSeverity.WARNING,
                            BedrockAddonWarningCode.DEPENDENCY_NOT_RESOLVED,
                            pack.sourceName(), "manifest.json",
                            "Dependency is not resolved inside addon: uuid=" + dependency.uuid()));
                }
            }
        }

        List<BedrockAddonPack> remaining = new ArrayList<>(packs);
        List<BedrockAddonPack> ordered = new ArrayList<>();
        Set<String> loadedUuids = new LinkedHashSet<>();

        while (!remaining.isEmpty()) {
            boolean progressed = false;
            for (int i = 0; i < remaining.size(); i++) {
                BedrockAddonPack candidate = remaining.get(i);
                boolean blocked = candidate.manifest().dependencies().stream()
                        .map(BedrockPackManifest.Dependency::uuid)
                        .filter(Objects::nonNull)
                        .filter(providedByUuid::containsKey)
                        .anyMatch(uuid -> !loadedUuids.contains(uuid));
                if (blocked) continue;
                ordered.add(candidate);
                loadedUuids.add(candidate.manifest().header().uuid());
                candidate.manifest().modules().forEach(module -> loadedUuids.add(module.uuid()));
                remaining.remove(i);
                progressed = true;
                break;
            }
            if (!progressed) {
                BedrockAddonPack fallback = remaining.remove(0);
                warnings.add(new BedrockAddonWarning(
                        BedrockAddonWarningSeverity.WARNING,
                        BedrockAddonWarningCode.DEPENDENCY_NOT_RESOLVED,
                        fallback.sourceName(), "manifest.json",
                        "Pack ordering fell back to discovery order because dependency graph could not be fully resolved"));
                ordered.add(fallback);
                loadedUuids.add(fallback.manifest().header().uuid());
                fallback.manifest().modules().forEach(module -> loadedUuids.add(module.uuid()));
            }
        }
        return ordered;
    }

    // BrArchive 辅助函数

    private static void normalizeRenderControllers(JsonElement root) {
        if (!root.isJsonObject()) return;
        var obj = root.getAsJsonObject();
        for (String key : obj.keySet()) {
            var val = obj.get(key);
            if (!val.isJsonObject()) continue;
            var desc = val.getAsJsonObject().get("description");
            if (desc == null || !desc.isJsonObject()) continue;
            var rc = desc.getAsJsonObject().get("render_controllers");
            if (rc == null || !rc.isJsonArray()) continue;
            var arr = rc.getAsJsonArray();
            var normalized = new com.google.gson.JsonArray();
            var conditions = new com.google.gson.JsonObject();
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    normalized.add(el);
                } else if (el.isJsonObject()) {
                    var o = el.getAsJsonObject();
                    if (!o.keySet().isEmpty()) {
                        String ctrlName = o.keySet().iterator().next();
                        normalized.add(new com.google.gson.JsonPrimitive(ctrlName));
                        JsonElement condVal = o.get(ctrlName);
                        if (condVal != null && condVal.isJsonPrimitive()) {
                            conditions.addProperty(ctrlName, condVal.getAsString());
                        }
                    }
                }
            }
            desc.getAsJsonObject().add("render_controllers", normalized);
            desc.getAsJsonObject().add("render_controller_conditions", conditions);
        }
    }

    // JSON 分片 / 低级工具

    private static List<String> splitConcatenatedJson(String json) {
        var chunks = new ArrayList<String>();
        int depth = 0, start = -1;
        boolean inString = false;
        int len = json.length();
        for (int i = 0; i < len; i++) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '{') { if (depth++ == 0) start = i; }
                else if (c == '}') {
                    if (--depth == 0 && start >= 0) {
                        chunks.add(json.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return chunks;
    }

    private static List<String> extractNamedObjects(String json, String name) {
        var objects = new ArrayList<String>();
        int index = 0;
        String quotedName = '"' + name + '"';
        while (index < json.length()) {
            int keyIndex = json.indexOf(quotedName, index);
            if (keyIndex < 0) break;
            int colonIndex = findNextNonWhitespace(json, keyIndex + quotedName.length());
            if (colonIndex >= json.length() || json.charAt(colonIndex) != ':') {
                index = keyIndex + quotedName.length();
                continue;
            }
            int valueIndex = findNextNonWhitespace(json, colonIndex + 1);
            if (valueIndex >= json.length() || json.charAt(valueIndex) != '{') {
                index = colonIndex + 1;
                continue;
            }
            int endIndex = findObjectEnd(json, valueIndex);
            if (endIndex < 0) break;
            objects.add(json.substring(valueIndex, endIndex + 1));
            index = endIndex + 1;
        }
        return objects;
    }

    private static int findNextNonWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        return index;
    }

    private static int findObjectEnd(String json, int objectStart) {
        int depth = 0;
        boolean inString = false;
        for (int i = objectStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '{') depth++;
                else if (c == '}') { if (--depth == 0) return i; }
            }
        }
        return -1;
    }

    /**
     * 从 FileEntry 读取资源内容。根据 data / file 选择不同读取方式。
     */
    private static BedrockResourceContent readContent(FileEntry entry) throws IOException {
        return switch (entry.family()) {
            case SOUND_INDEX, SOUND_DEFINITION, BEHAVIOR_ENTITY, ITEM, BLOCK, RECIPE, LOOT_TABLE, SPAWN_RULE, TRADING,
                 FEATURE, FEATURE_RULE, STRUCTURE, SCRIPT, UI, FOG, BIOME, TEXTURE_INDEX, TEXTURE_METADATA, UNKNOWN_JSON ->
                    new BedrockResourceContent.StructuredContent(BedrockResourceValue.fromJsonElement(readJsonElement(entry)));
            case LOCALIZATION, UNKNOWN_TEXT ->
                    new BedrockResourceContent.TextContent(readString(entry));
            default -> new BedrockResourceContent.BinaryContent(readBytes(entry));
        };
    }

    // 统一读取工具方法

    /** 从 FileEntry 读取 JSON 对象。优先从 Supplier 获取数据，否则读磁盘文件。 */
    private static JsonObject readJsonFile(FileEntry entry) throws IOException {
        if (entry.dataSupplier() != null) {
            return JsonParser.parseString(new String(entry.dataSupplier().get(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        return readJsonFile(entry.file());
    }

    /** 从 FileEntry 读取任意 JSON 元素。优先从 Supplier 获取数据，否则读磁盘文件。 */
    private static JsonElement readJsonElement(FileEntry entry) throws IOException {
        if (entry.dataSupplier() != null) {
            return JsonParser.parseString(new String(entry.dataSupplier().get(), StandardCharsets.UTF_8));
        }
        return readJsonElement(entry.file());
    }

    /** 从 FileEntry 读取 UTF-8 字符串。优先从 Supplier 获取数据，否则读磁盘文件。 */
    private static String readString(FileEntry entry) throws IOException {
        if (entry.dataSupplier() != null) {
            return new String(entry.dataSupplier().get(), StandardCharsets.UTF_8);
        }
        return Files.readString(entry.file(), StandardCharsets.UTF_8);
    }

    /** 从 FileEntry 读取字节数组。优先从 Supplier 获取数据，否则读磁盘文件。 */
    private static byte[] readBytes(FileEntry entry) throws IOException {
        if (entry.dataSupplier() != null) {
            return entry.dataSupplier().get();
        }
        return Files.readAllBytes(entry.file());
    }

    // I/O 工具（保留基于 Path 的重载以供内部使用）

    private static String extensionOf(String relativePath) {
        int index = relativePath.lastIndexOf('.');
        return index >= 0 ? relativePath.substring(index + 1) : "";
    }

    private static JsonObject readJsonFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonElement readJsonElement(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static JsonElement parseJsonLenient(String json) {
        var stringReader = new java.io.StringReader(json);
        var jsonReader = new com.google.gson.stream.JsonReader(stringReader);
        jsonReader.setLenient(true);
        return JsonParser.parseReader(jsonReader);
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    // Zip / 目录工具

    /**
     * 收集源路径中的所有包根目录文件条目。
     * <p>
     * 对于压缩包（zip/mcpack/mcaddon），使用 ZipFile 流式读取，内容按需加载。
     * 对于目录，递归搜索含有 manifest.json 的包根目录。
     */
    private static void collectPackRoots(Path source, List<PackFiles> packFilesList, List<ZipFile> openZips)
            throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Source does not exist: " + source);
        }
        if (Files.isRegularFile(source)) {
            String fileName = source.getFileName().toString().toLowerCase(Locale.ROOT);
            if (fileName.equals("manifest.json")) {
                // 单个 manifest.json 文件，处理其父目录
                Path parent = source.getParent();
                if (parent != null) {
                    packFilesList.add(collectFilesFromDir(parent));
                }
                return;
            }
            if (isArchive(fileName)) {
                // 使用 ZipFile 流式读取，不解压到磁盘
                collectFilesFromZip(source, packFilesList, openZips);
                return;
            }
            throw new IOException("Unsupported Bedrock addon source: " + source);
        }
        // 目录处理
        if (Files.exists(source.resolve("manifest.json"))) {
            packFilesList.add(collectFilesFromDir(source));
            return;
        }
        List<Path> children = new ArrayList<>();
        try (var stream = Files.list(source)) {
            stream.forEach(children::add);
        }
        children.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        for (Path child : children) {
            collectPackRoots(child, packFilesList, openZips);
        }
    }

    /**
     * 从磁盘目录收集文件条目。
     * 每个文件对应一个 FileEntry.fromPath，内容暂不读入内存（延迟读取）。
     */
    private static PackFiles collectFilesFromDir(Path packRoot) throws IOException {
        String sourceName = Optional.ofNullable(packRoot.getFileName())
                .map(Path::toString).orElse(packRoot.toString());
        List<FileEntry> entries = new ArrayList<>();
        try (var stream = Files.walk(packRoot)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                String relativePath = normalize(packRoot.relativize(file).toString());
                String effectivePath = stripSubpackPrefix(relativePath);
                entries.add(FileEntry.fromPath(file, relativePath, effectivePath));
            });
        }
        entries.sort(Comparator.comparing(e -> e.relativePath()));
        return new PackFiles(sourceName, entries);
    }

    /**
     * 从压缩包（zip/mcpack/mcaddon）收集文件条目。
     * 所有条目内容读入内存（byte[]），不写入磁盘。
     * <p>
     * - .mcpack 风格：根目录含 manifest.json，所有 entry 属于同一个包
     * - .mcaddon 风格：子目录（behavior_pack/、resource_pack/）各自含 manifest.json
     */
    private static void collectFilesFromZip(Path source, List<PackFiles> packFilesList, List<ZipFile> openZips) throws IOException {
        String archiveName = source.getFileName().toString();
        String baseName = archiveName.contains(".")
                ? archiveName.substring(0, archiveName.lastIndexOf('.'))
                : archiveName;

        ZipFile zf = new ZipFile(source.toFile());
        openZips.add(zf); // 由 load() 的 finally 块统一关闭

        // 收集所有文件条目
        List<ZipEntry> allEntries = new ArrayList<>();
        Enumeration<? extends ZipEntry> en = zf.entries();
        while (en.hasMoreElements()) {
            ZipEntry ze = en.nextElement();
            if (ze.isDirectory()) continue;
            allEntries.add(ze);
        }

        // mcpack 风格：根目录含 manifest.json → 所有 entry 属于同一个包
        boolean hasRootManifest = allEntries.stream()
                .anyMatch(e -> normalize(e.getName()).equals("manifest.json"));
        if (hasRootManifest) {
            List<FileEntry> entries = new ArrayList<>();
            for (ZipEntry ze : allEntries) {
                String relativePath = normalize(ze.getName());
                String effectivePath = stripSubpackPrefix(relativePath);
                entries.add(FileEntry.fromZipRef(zf, ze, relativePath, effectivePath));
            }
            entries.sort(Comparator.comparing(e -> e.relativePath()));
            packFilesList.add(new PackFiles(baseName, entries));
            return;
        }

        // mcaddon 风格：按顶级目录分组，各组独立含 manifest.json
        Map<String, List<ZipEntry>> groups = new LinkedHashMap<>();
        for (ZipEntry ze : allEntries) {
            String name = normalize(ze.getName());
            String topDir = topLevelDir(name);
            groups.computeIfAbsent(topDir, k -> new ArrayList<>()).add(ze);
        }
        for (Map.Entry<String, List<ZipEntry>> group : groups.entrySet()) {
            String prefix = group.getKey();
            boolean hasManifest = group.getValue().stream()
                    .anyMatch(e -> normalize(stripPrefix(e.getName(), prefix)).equals("manifest.json"));
            if (!hasManifest) continue;

            List<FileEntry> entries = new ArrayList<>();
            for (ZipEntry ze : group.getValue()) {
                String relativePath = normalize(ze.getName());
                String innerPath = stripPrefix(relativePath, prefix);
                String effectivePath = stripSubpackPrefix(innerPath);
                entries.add(FileEntry.fromZipRef(zf, ze, relativePath, effectivePath));
            }
            entries.sort(Comparator.comparing(e -> e.relativePath()));
            packFilesList.add(new PackFiles(baseName + "/" + prefix, entries));
        }
    }

    /** 读取 ZipEntry 的全部字节。 */
    private static byte[] readZipEntryBytes(ZipFile zf, ZipEntry ze) throws IOException {
        try (InputStream in = zf.getInputStream(ze)) {
            return in.readAllBytes();
        }
    }

    /** 取路径的顶级目录（无斜杠则返回空字符串）。 */
    private static String topLevelDir(String path) {
        int slash = path.indexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    /** 移除路径的前缀。如果前缀为空，返回原路径。 */
    private static String stripPrefix(String path, String prefix) {
        if (prefix.isEmpty()) return path;
        if (path.startsWith(prefix + "/")) {
            return path.substring(prefix.length() + 1);
        }
        return path;
    }

    private static boolean isArchive(String fileName) {
        return fileName.endsWith(".zip") || fileName.endsWith(".mcpack") || fileName.endsWith(".mcaddon");
    }
}
