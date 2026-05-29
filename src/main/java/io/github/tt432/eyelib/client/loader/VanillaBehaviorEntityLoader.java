package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tt432.eyelib.client.registry.BehaviorEntityAssetRegistry;
import io.github.tt432.eyelibimporter.addon.BrBehaviorEntityFile;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author TT432
 */
@NullMarked
final class VanillaBehaviorEntityLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaBehaviorEntityLoader.class);
    private static final Path DEFAULT_ENTITY_DIR = Paths.get("run/vanilla_behavior_pack/entities");
    private static final String ENTITIES_RESOURCE_PREFIX = "eyelib/vanilla_behavior/entities/";

    private static final List<String> ENTITY_NAMES = List.of(
            "slime",
            "creeper",
            "cat",
            "fox",
            "panda",
            "parrot",
            "rabbit",
            "sheep",
            "wolf",
            "zombie",
            "skeleton"
    );

    private VanillaBehaviorEntityLoader() {
    }

    static void loadAndRegister() {
        var entities = new LinkedHashMap<String, BrBehaviorEntityFile>();
        Path entityDir = DEFAULT_ENTITY_DIR;
        if (Files.isDirectory(entityDir)) {
            loadFromDirectory(entities, entityDir);
        }
        if (entities.isEmpty()) {
            loadFromClasspath(entities);
        }
        if (!entities.isEmpty()) {
            BehaviorEntityAssetRegistry.replaceBehaviorEntities(entities);
            LOGGER.info("Loaded {} vanilla behavior entities", entities.size());
        }
    }

    private static void loadFromDirectory(LinkedHashMap<String, BrBehaviorEntityFile> entities, Path entityDir) {
        try (Stream<Path> files = Files.list(entityDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(jsonFile -> {
                try {
                    JsonObject json = JsonParser.parseReader(Files.newBufferedReader(jsonFile)).getAsJsonObject();
                    BrBehaviorEntityFile entityFile = BrBehaviorEntityFile.parse(json);
                    entities.put(entityFile.identifier(), entityFile);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse behavior entity: {}", jsonFile.getFileName(), e);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to list behavior entity directory: {}", entityDir, e);
        }
    }

    private static void loadFromClasspath(LinkedHashMap<String, BrBehaviorEntityFile> entities) {
        for (String name : ENTITY_NAMES) {
            String resourcePath = ENTITIES_RESOURCE_PREFIX + name + ".json";
            try (var is = VanillaBehaviorEntityLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    LOGGER.warn("Vanilla behavior entity resource not found: {}", resourcePath);
                    continue;
                }
                JsonObject json = JsonParser.parseReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                BrBehaviorEntityFile entityFile = BrBehaviorEntityFile.parse(json);
                entities.put(entityFile.identifier(), entityFile);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse vanilla behavior entity: {}", name, e);
            }
        }
    }
}
