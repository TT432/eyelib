package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.material.BrMaterial;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelibimporter.model.importer.ModelImporter;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class BedrockAddonLoader {
    private BedrockAddonLoader() {
    }

    public static BedrockAddon load(Path source) throws IOException {
        List<Path> temporaryDirectories = new ArrayList<>();
        try {
            List<Path> packRoots = new ArrayList<>();
            collectPackRoots(source, packRoots, temporaryDirectories);
            packRoots.sort(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString()));

            List<BedrockAddonPack> unsortedPacks = new ArrayList<>();
            List<BedrockAddonWarning> warnings = new ArrayList<>();
            LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources = new LinkedHashMap<>();
            LinkedHashMap<String, io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema> animations = new LinkedHashMap<>();
            LinkedHashMap<String, io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema> animationControllers = new LinkedHashMap<>();
            LinkedHashMap<String, BrClientEntity> clientEntities = new LinkedHashMap<>();
            LinkedHashMap<String, BrClientEntity> attachables = new LinkedHashMap<>();
            LinkedHashMap<String, BrBehaviorEntityFile> behaviorEntities = new LinkedHashMap<>();
            LinkedHashMap<String, Model> models = new LinkedHashMap<>();
            LinkedHashMap<String, ImportedImageData> textures = new LinkedHashMap<>();
            LinkedHashMap<String, BrSoundIndex> soundIndexFiles = new LinkedHashMap<>();
            LinkedHashMap<String, BrSoundDefinitions> soundDefinitionFiles = new LinkedHashMap<>();
            LinkedHashMap<String, BrLanguageFile> languageFiles = new LinkedHashMap<>();
            LinkedHashMap<String, BedrockBinaryAsset> soundFiles = new LinkedHashMap<>();
            LinkedHashMap<String, BrRenderControllers> renderControllerFiles = new LinkedHashMap<>();
            LinkedHashMap<String, BrParticle> particleFiles = new LinkedHashMap<>();
            LinkedHashMap<String, BrMaterial> materialFiles = new LinkedHashMap<>();

            for (Path packRoot : packRoots) {
                BedrockAddonPack pack = loadPack(packRoot);
                unsortedPacks.add(pack);
            }

            List<BedrockAddonPack> packs = sortPacksByDependencies(unsortedPacks, warnings);

            for (BedrockAddonPack pack : packs) {
                warnings.addAll(pack.warnings());
                pack.unmanagedResources().forEach((path, resource) -> unmanagedResources.put(pack.sourceName() + ":" + path, resource));
                mergeWithWarnings(animations, pack.animations(), pack.sourceName(), warnings, BedrockResourceFamily.ANIMATION);
                mergeWithWarnings(animationControllers, pack.animationControllers(), pack.sourceName(), warnings, BedrockResourceFamily.ANIMATION_CONTROLLER);
                mergeWithWarnings(clientEntities, pack.clientEntities(), pack.sourceName(), warnings, BedrockResourceFamily.CLIENT_ENTITY);
                mergeWithWarnings(attachables, pack.attachables(), pack.sourceName(), warnings, BedrockResourceFamily.ATTACHABLE);
                mergeWithWarnings(behaviorEntities, pack.behaviorEntities(), pack.sourceName(), warnings, BedrockResourceFamily.BEHAVIOR_ENTITY);
                mergeWithWarnings(models, pack.modelsView(), pack.sourceName(), warnings, BedrockResourceFamily.MODEL);
                textures.putAll(pack.textures());
                soundIndexFiles.putAll(pack.soundIndexFiles());
                soundDefinitionFiles.putAll(pack.soundDefinitionFiles());
                languageFiles.putAll(pack.languageFiles());
                soundFiles.putAll(pack.soundFiles());
                renderControllerFiles.putAll(pack.renderControllerFiles());
                particleFiles.putAll(pack.particleFiles());
                materialFiles.putAll(pack.materialFiles());
            }

            return new BedrockAddon(
                    packs,
                    warnings,
                    unmanagedResources,
                    animations,
                    animationControllers,
                    clientEntities,
                    attachables,
                    behaviorEntities,
                    models,
                    textures,
                    soundIndexFiles,
                    soundDefinitionFiles,
                    languageFiles,
                    soundFiles,
                    renderControllerFiles,
                    particleFiles,
                    materialFiles
            );
        } finally {
            for (Path temporaryDirectory : temporaryDirectories) {
                deleteRecursively(temporaryDirectory);
            }
        }
    }

    private static List<BedrockAddonPack> sortPacksByDependencies(List<BedrockAddonPack> packs, List<BedrockAddonWarning> warnings) {
        Map<String, BedrockAddonPack> providedByUuid = new LinkedHashMap<>();
        for (BedrockAddonPack pack : packs) {
            providedByUuid.put(pack.manifest().header().uuid(), pack);
            pack.manifest().modules().forEach(module -> providedByUuid.put(module.uuid(), pack));
        }

        for (BedrockAddonPack pack : packs) {
            for (BedrockPackManifest.Dependency dependency : pack.manifest().dependencies()) {
                boolean resolved = dependency.uuid() != null && providedByUuid.containsKey(dependency.uuid())
                        || dependency.moduleName() != null && pack.manifest().modules().stream().anyMatch(module -> module.type().equals(dependency.moduleName()));
                if (!resolved && dependency.hasReference()) {
                    warnings.add(new BedrockAddonWarning(
                            BedrockAddonWarningSeverity.WARNING,
                            BedrockAddonWarningCode.DEPENDENCY_NOT_RESOLVED,
                            pack.sourceName(),
                            "manifest.json",
                            "Dependency is not resolved inside addon: uuid=" + dependency.uuid() + ", module_name=" + dependency.moduleName()
                    ));
                }
            }
        }

        List<BedrockAddonPack> remaining = new ArrayList<>(packs);
        List<BedrockAddonPack> ordered = new ArrayList<>();
        java.util.Set<String> loadedUuids = new java.util.LinkedHashSet<>();

        while (!remaining.isEmpty()) {
            boolean progressed = false;
            for (int i = 0; i < remaining.size(); i++) {
                BedrockAddonPack candidate = remaining.get(i);
                boolean blocked = candidate.manifest().dependencies().stream()
                        .map(BedrockPackManifest.Dependency::uuid)
                        .filter(java.util.Objects::nonNull)
                        .filter(providedByUuid::containsKey)
                        .anyMatch(uuid -> !loadedUuids.contains(uuid));
                if (blocked) {
                    continue;
                }
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
                        fallback.sourceName(),
                        "manifest.json",
                        "Pack ordering fell back to discovery order because dependency graph could not be fully resolved"
                ));
                ordered.add(fallback);
                loadedUuids.add(fallback.manifest().header().uuid());
                fallback.manifest().modules().forEach(module -> loadedUuids.add(module.uuid()));
            }
        }

        return ordered;
    }

    private static BedrockAddonPack loadPack(Path packRoot) throws IOException {
        JsonObject manifestJson = parseJson(packRoot.resolve("manifest.json"));
        BedrockPackManifest manifest = BedrockPackManifest.parse(manifestJson);
        List<BedrockAddonWarning> warnings = new ArrayList<>();
        warnForUnmanagedManifestFields(packRoot, manifest, warnings);
        LinkedHashMap<String, BrAnimationSet> animationFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrAnimationControllerSet> animationControllerFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrClientEntity> clientEntityFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrClientEntity> attachableFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrBehaviorEntityFile> behaviorEntityFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BedrockImportedModels> modelFiles = new LinkedHashMap<>();
        LinkedHashMap<String, ImportedImageData> textures = new LinkedHashMap<>();
        LinkedHashMap<String, BrSoundIndex> soundIndexFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrSoundDefinitions> soundDefinitionFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrLanguageFile> languageFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BedrockBinaryAsset> soundFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrRenderControllers> renderControllerFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrParticle> particleFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrMaterial> materialFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources = new LinkedHashMap<>();
        ImportedImageData packIcon = null;

        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(packRoot)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        }
        files.sort(Comparator.comparing(path -> normalize(packRoot.relativize(path).toString())));

        for (Path file : files) {
            String relativePath = normalize(packRoot.relativize(file).toString());
            String lowerCasePath = relativePath.toLowerCase(Locale.ROOT);
            BedrockResourceFamily family = BedrockResourceFamily.classify(relativePath);
            if (lowerCasePath.equals("manifest.json")) {
                continue;
            }
            if (lowerCasePath.equals("pack_icon.png")) {
                packIcon = ImportedImageData.decodePng(Files.readAllBytes(file));
                continue;
            }
            if (lowerCasePath.startsWith("textures/") && lowerCasePath.endsWith(".png")) {
                textures.put(relativePath, Objects.requireNonNull(ImportedImageData.decodePng(Files.readAllBytes(file))));
                continue;
            }
            try {
                switch (family) {
                    case MATERIAL -> {
                        BrMaterial material = BrMaterial.CODEC.parse(JsonOps.INSTANCE, parseJson(file)).getOrThrow(false, IllegalArgumentException::new);
                        materialFiles.put(relativePath, material);
                    }
                    case ANIMATION -> {
                        BrAnimationSet animationSet = BrAnimationSet.CODEC.parse(JsonOps.INSTANCE, parseJson(file)).getOrThrow(false, IllegalArgumentException::new);
                        animationFiles.put(relativePath, animationSet);
                    }
                    case ANIMATION_CONTROLLER -> {
                        BrAnimationControllerSet controllerSet = BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, parseJson(file)).getOrThrow(false, IllegalArgumentException::new);
                        animationControllerFiles.put(relativePath, controllerSet);
                    }
                    case CLIENT_ENTITY -> {
                        BrClientEntity entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, parseJson(file)).getOrThrow(false, IllegalArgumentException::new);
                        clientEntityFiles.put(relativePath, entity);
                    }
                    case ATTACHABLE -> {
                        BrClientEntity attachable = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, parseJson(file)).getOrThrow(false, IllegalArgumentException::new);
                        attachableFiles.put(relativePath, attachable);
                    }
                    case MODEL -> modelFiles.put(relativePath, new BedrockImportedModels(new LinkedHashMap<>(ModelImporter.importFile(file))));
                    case SOUND_INDEX -> soundIndexFiles.put(relativePath, BrSoundIndex.parse(parseJson(file)));
                    case SOUND_DEFINITION -> soundDefinitionFiles.put(relativePath, BrSoundDefinitions.parse(parseJson(file)));
                    case LOCALIZATION -> languageFiles.put(relativePath, BrLanguageFile.parse(Files.readString(file, StandardCharsets.UTF_8)));
                    case BEHAVIOR_ENTITY -> behaviorEntityFiles.put(relativePath, BrBehaviorEntityFile.parse(parseJson(file)));
                    case SOUND_FILE -> soundFiles.put(relativePath, new BedrockBinaryAsset(extensionOf(relativePath), Files.readAllBytes(file)));
                    case RENDER_CONTROLLER -> {
                        BrRenderControllers controllers = BrRenderControllers.CODEC.parse(JsonOps.INSTANCE, parseJson(file)).getOrThrow(false, IllegalArgumentException::new);
                        renderControllerFiles.put(relativePath, controllers);
                    }
                    case PARTICLE -> {
                        BrParticle particle = BrParticle.CODEC.parse(JsonOps.INSTANCE, parseJson(file)).getOrThrow(false, IllegalArgumentException::new);
                        particleFiles.put(relativePath, particle);
                    }
                    default -> captureUnmanaged(packRoot, file, relativePath, family, warnings, unmanagedResources, unmanagedReasonFor(family), false, null);
                }
            } catch (RuntimeException exception) {
                captureUnmanaged(packRoot, file, relativePath, family, warnings, unmanagedResources, BedrockUnmanagedReason.SCHEMA_PARSE_FAILED, true, exception.getMessage());
            }
        }

        return new BedrockAddonPack(
                packRoot.getFileName() == null ? packRoot.toString() : packRoot.getFileName().toString(),
                manifest,
                animationFiles,
                animationControllerFiles,
                clientEntityFiles,
                attachableFiles,
                modelFiles,
                textures,
                soundIndexFiles,
                soundDefinitionFiles,
                languageFiles,
                behaviorEntityFiles,
                soundFiles,
                renderControllerFiles,
                particleFiles,
                materialFiles,
                unmanagedResources,
                warnings,
                packIcon
        );
    }

    private static BedrockUnmanagedReason unmanagedReasonFor(BedrockResourceFamily family) {
        return switch (family) {
            case ITEM, BLOCK, RECIPE, LOOT_TABLE, SPAWN_RULE, TRADING, FEATURE, FEATURE_RULE, STRUCTURE, SCRIPT, BIOME -> BedrockUnmanagedReason.OUTSIDE_IMPORTER_SCOPE;
            case TEXTURE_METADATA -> BedrockUnmanagedReason.TEXTURE_SIDE_METADATA;
            case UI, FOG, UNKNOWN_JSON, UNKNOWN_TEXT, UNKNOWN_BINARY -> BedrockUnmanagedReason.NO_TYPED_SCHEMA_YET;
            default -> BedrockUnmanagedReason.UNKNOWN_LAYOUT;
        };
    }

    private static void warnForUnmanagedManifestFields(Path packRoot, BedrockPackManifest manifest, List<BedrockAddonWarning> warnings) {
        manifest.extraFields().forEach((key, value) -> warnings.add(new BedrockAddonWarning(
                BedrockAddonWarningSeverity.WARNING,
                BedrockAddonWarningCode.MANIFEST_FIELD_UNMANAGED,
                packRoot.getFileName() == null ? packRoot.toString() : packRoot.getFileName().toString(),
                "manifest.json",
                "Manifest top-level field is retained but not modeled semantically: " + key
        )));
        manifest.metadata().extraFields().forEach((key, value) -> warnings.add(new BedrockAddonWarning(
                BedrockAddonWarningSeverity.WARNING,
                BedrockAddonWarningCode.MANIFEST_FIELD_UNMANAGED,
                packRoot.getFileName() == null ? packRoot.toString() : packRoot.getFileName().toString(),
                "manifest.json",
                "Manifest metadata field is retained but not modeled semantically: metadata." + key
        )));
    }

    private static void captureUnmanaged(Path packRoot, Path file, String relativePath, BedrockResourceFamily family,
                                         List<BedrockAddonWarning> warnings,
                                         LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources,
                                         BedrockUnmanagedReason reason,
                                         boolean parseFailure,
                                         String parseMessage) throws IOException {
        BedrockResourceContent content = readContent(file, family);
        unmanagedResources.put(relativePath, new BedrockUnmanagedResource(family, relativePath, content, reason));
        warnings.add(new BedrockAddonWarning(
                parseFailure ? BedrockAddonWarningSeverity.ERROR : BedrockAddonWarningSeverity.WARNING,
                parseFailure ? BedrockAddonWarningCode.SCHEMA_PARSE_FAILED : BedrockAddonWarningCode.UNMANAGED_RESOURCE,
                packRoot.getFileName() == null ? packRoot.toString() : packRoot.getFileName().toString(),
                relativePath,
                parseFailure
                        ? "Managed family parse failed; retained as unmanaged because: " + parseMessage
                        : "Resource is retained but unmanaged because: " + reason
        ));
    }

    private static BedrockResourceContent readContent(Path file, BedrockResourceFamily family) throws IOException {
        return switch (family) {
            case SOUND_INDEX, SOUND_DEFINITION, BEHAVIOR_ENTITY, ITEM, BLOCK, RECIPE, LOOT_TABLE, SPAWN_RULE, TRADING,
                    FEATURE, FEATURE_RULE, STRUCTURE, SCRIPT, UI, FOG, BIOME, TEXTURE_METADATA, UNKNOWN_JSON ->
                    new BedrockResourceContent.StructuredContent(BedrockResourceValue.fromJsonElement(parseJson(file)));
            case LOCALIZATION, UNKNOWN_TEXT -> new BedrockResourceContent.TextContent(Files.readString(file, StandardCharsets.UTF_8));
            default -> new BedrockResourceContent.BinaryContent(Files.readAllBytes(file));
        };
    }

    private static <T> void mergeWithWarnings(Map<String, T> target, Map<String, T> incoming, String packSource,
                                              List<BedrockAddonWarning> warnings, BedrockResourceFamily family) {
        incoming.forEach((key, value) -> {
            if (target.containsKey(key)) {
                warnings.add(new BedrockAddonWarning(
                        BedrockAddonWarningSeverity.WARNING,
                        BedrockAddonWarningCode.DUPLICATE_OVERRIDE,
                        packSource,
                        key,
                        "Flattened resource key was overridden for family " + family + ": " + key
                ));
            }
            target.put(key, value);
        });
    }

    private static String extensionOf(String relativePath) {
        int index = relativePath.lastIndexOf('.');
        return index >= 0 ? relativePath.substring(index + 1) : "";
    }

    private static JsonObject parseJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void collectPackRoots(Path source, List<Path> packRoots, List<Path> temporaryDirectories) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Source does not exist: " + source);
        }

        if (Files.isRegularFile(source)) {
            String fileName = source.getFileName().toString().toLowerCase(Locale.ROOT);
            if (fileName.equals("manifest.json")) {
                packRoots.add(source.getParent());
                return;
            }
            if (isArchive(fileName)) {
                Path extracted = extractArchive(source);
                temporaryDirectories.add(extracted);
                collectPackRoots(extracted, packRoots, temporaryDirectories);
                return;
            }
            throw new IOException("Unsupported Bedrock addon source: " + source);
        }

        if (Files.exists(source.resolve("manifest.json"))) {
            packRoots.add(source);
            return;
        }

        List<Path> children = new ArrayList<>();
        try (var stream = Files.list(source)) {
            stream.forEach(children::add);
        }
        children.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        for (Path child : children) {
            if (Files.isDirectory(child)) {
                collectPackRoots(child, packRoots, temporaryDirectories);
                continue;
            }
            String childName = child.getFileName().toString().toLowerCase(Locale.ROOT);
            if (isArchive(childName)) {
                Path extracted = extractArchive(child);
                temporaryDirectories.add(extracted);
                collectPackRoots(extracted, packRoots, temporaryDirectories);
            }
        }
    }

    private static boolean isArchive(String fileName) {
        return fileName.endsWith(".zip") || fileName.endsWith(".mcpack") || fileName.endsWith(".mcaddon");
    }

    private static Path extractArchive(Path archive) throws IOException {
        Path targetDir = Files.createTempDirectory("eyelib-addon-");
        try (InputStream inputStream = Files.newInputStream(archive);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName()).normalize();
                if (!targetPath.startsWith(targetDir)) {
                    throw new IOException("Zip entry escapes target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Path parent = targetPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipInputStream, targetPath);
                }
                zipInputStream.closeEntry();
            }
        }
        return targetDir;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        List<Path> paths = new ArrayList<>();
        try (var stream = Files.walk(path)) {
            stream.forEach(paths::add);
        }
        paths.sort(Comparator.reverseOrder());
        for (Path current : paths) {
            Files.deleteIfExists(current);
        }
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }
}
