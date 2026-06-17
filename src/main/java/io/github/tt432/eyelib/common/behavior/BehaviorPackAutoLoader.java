package io.github.tt432.eyelib.common.behavior;

import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.importer.addon.BedrockAddonLoader;
import io.github.tt432.eyelib.importer.addon.BedrockAddonWarning;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * 按服务端目录加载行为包，并发布到服务端行为运行时注册表。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BehaviorPackAutoLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorPackAutoLoader.class);
    private static final String[] BEHAVIOR_PACK_DIRS = {
            "behavior_packs",
            "behaviorpacks",
            "development_behavior_packs"
    };

    public static void load(MinecraftServer server) {
        Path serverDirectory = server.getServerDirectory().toPath();
        BehaviorEntityRegistry.clear();
        SpawnRuleRegistry.clear();
        VanillaBehaviorEntityLoader.mergeIntoRegistry(serverDirectory);

        var entityFiles = new LinkedHashMap<String, io.github.tt432.eyelib.importer.addon.BrBehaviorEntityFile>();
        var spawnRules = new LinkedHashMap<String, io.github.tt432.eyelib.importer.addon.BrSpawnRule>();
        for (String dirName : BEHAVIOR_PACK_DIRS) {
            loadDirectory(serverDirectory.resolve(dirName), entityFiles, spawnRules);
        }
        if (!entityFiles.isEmpty()) {
            BehaviorPackPublication.mergeBehaviorEntities(entityFiles, LOGGER);
        }
        if (!spawnRules.isEmpty()) {
            BehaviorPackPublication.mergeSpawnRules(spawnRules, LOGGER);
        }
        LOGGER.info("Published {} behavior entities and {} spawn rules",
                    BehaviorEntityRegistry.all().size(), SpawnRuleRegistry.allRules().size());
    }

    private static void loadDirectory(
            Path directory,
            LinkedHashMap<String, io.github.tt432.eyelib.importer.addon.BrBehaviorEntityFile> entityFiles,
            LinkedHashMap<String, io.github.tt432.eyelib.importer.addon.BrSpawnRule> spawnRules
    ) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, BehaviorPackAutoLoader::isAddonSource)) {
            for (Path source : stream) {
                BedrockAddon addon = loadOne(source);
                if (addon != null) {
                    entityFiles.putAll(addon.aggregate().behaviorPack().behaviorEntities());
                    spawnRules.putAll(addon.aggregate().behaviorPack().spawnRulesFiles());
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to scan behavior pack directory: {}", directory, exception);
        }
    }

    private static boolean isAddonSource(Path path) {
        if (Files.isDirectory(path)) {
            return Files.exists(path.resolve("manifest.json"));
        }
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".mcpack") || name.endsWith(".mcaddon") || name.endsWith(".zip");
    }

    @Nullable
    private static BedrockAddon loadOne(Path source) {
        LOGGER.info("Loading Bedrock behavior pack source: {}", source.getFileName());
        try {
            BedrockAddon addon = BedrockAddonLoader.load(source);
            logWarnings(addon);
            return addon;
        } catch (Exception exception) {
            LOGGER.error("Failed to load Bedrock behavior pack source: {}", source.getFileName(), exception);
            return null;
        }
    }

    private static void logWarnings(BedrockAddon addon) {
        for (BedrockAddonWarning warning : addon.warnings()) {
            LOGGER.warn("[{}] {}: {}", warning.packSource(), warning.code(), warning.message());
        }
    }
}
