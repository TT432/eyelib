package io.github.tt432.eyelib.common.behavior;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tt432.eyelibimporter.addon.BrBehaviorEntityFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 加载内置 vanilla 行为实体定义，供服务端行为运行时兜底查询。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class VanillaBehaviorEntityLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaBehaviorEntityLoader.class);
    private static final String MCPACK_RESOURCE_PATH = "data/eyelib/vanilla_behavior_pack.mcpack";

    public static int mergeIntoRegistry(Path gameDirectory) {
        var entities = load(gameDirectory);
        if (!entities.isEmpty()) {
            BehaviorPackPublication.mergeBehaviorEntities(entities, LOGGER);
            LOGGER.info("Loaded {} vanilla behavior entities", entities.size());
        }
        return entities.size();
    }

    public static LinkedHashMap<String, BrBehaviorEntityFile> load(Path gameDirectory) {
        var entities = new LinkedHashMap<String, BrBehaviorEntityFile>();
        Path entityDir = gameDirectory.resolve("vanilla_behavior_pack").resolve("entities");
        if (Files.isDirectory(entityDir)) {
            loadFromDirectory(entities, entityDir);
        }
        if (entities.isEmpty()) {
            loadFromMcpack(entities);
        }
        return entities;
    }

    private static void loadFromDirectory(LinkedHashMap<String, BrBehaviorEntityFile> entities, Path entityDir) {
        try (Stream<Path> files = Files.list(entityDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(jsonFile -> {
                try {
                    JsonObject json = JsonParser.parseReader(Files.newBufferedReader(jsonFile)).getAsJsonObject();
                    BrBehaviorEntityFile entityFile = BrBehaviorEntityFile.parse(json);
                    entities.put(entityFile.identifier(), entityFile);
                } catch (RuntimeException | IOException exception) {
                    LOGGER.warn("Failed to parse behavior entity: {}", jsonFile.getFileName(), exception);
                }
            });
        } catch (IOException exception) {
            LOGGER.warn("Failed to list behavior entity directory: {}", entityDir, exception);
        }
    }

    private static void loadFromMcpack(LinkedHashMap<String, BrBehaviorEntityFile> entities) {
        var cl = VanillaBehaviorEntityLoader.class.getClassLoader();
        try (var is = cl.getResourceAsStream(MCPACK_RESOURCE_PATH)) {
            if (is == null) {
                LOGGER.warn("Vanilla behavior pack resource not found: {}", MCPACK_RESOURCE_PATH);
                return;
            }
            try (var zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName().replace('\\', '/');
                    if (name.startsWith("vanilla/entities/") && name.endsWith(".json") && !entry.isDirectory()) {
                        try {
                            JsonObject json = JsonParser.parseReader(new InputStreamReader(zis, StandardCharsets.UTF_8))
                                                        .getAsJsonObject();
                            BrBehaviorEntityFile entityFile = BrBehaviorEntityFile.parse(json);
                            entities.put(entityFile.identifier(), entityFile);
                        } catch (RuntimeException exception) {
                            LOGGER.warn("Failed to parse behavior entity: {}", name, exception);
                        }
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to read behavior pack from classpath: {}", MCPACK_RESOURCE_PATH, exception);
        }
    }
}
