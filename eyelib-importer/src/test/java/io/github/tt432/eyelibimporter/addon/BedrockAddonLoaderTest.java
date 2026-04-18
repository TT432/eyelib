package io.github.tt432.eyelibimporter.addon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockAddonLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsFolderAddonWithResourceAndBehaviorPacks() throws Exception {
        Path addonRoot = tempDir.resolve("folder-addon");
        Path resourcePack = writeResourcePack(addonRoot.resolve("resource_pack"));
        writeBehaviorPack(addonRoot.resolve("behavior_pack"), resourcePack);

        BedrockAddon addon = BedrockAddonLoader.load(addonRoot);

        assertEquals(2, addon.packs().size());
        assertEquals(1, addon.resourcePacks().size());
        assertEquals(1, addon.dataPacks().size());
        assertTrue(addon.models().containsKey("geometry.test"));
        assertTrue(addon.clientEntities().containsKey("eyelib:test_entity"));
        assertTrue(addon.attachables().containsKey("eyelib:test_attachable"));
        assertTrue(addon.animations().containsKey("animation.test.idle"));
        assertTrue(addon.animationControllers().containsKey("controller.animation.test"));
        assertTrue(addon.renderControllerFiles().containsKey("render_controllers/test.render_controllers.json"));
        assertTrue(addon.flattenedRenderControllers().containsKey("controller.render.test"));
        assertTrue(addon.particleFiles().containsKey("particles/test.particle.json"));
        assertTrue(addon.particlesByIdentifier().containsKey("eyelib:test_particle"));
        assertTrue(addon.textures().containsKey("textures/entity/test.png"));
        assertTrue(addon.materialFiles().containsKey("materials/test.material"));
        assertTrue(addon.flattenedMaterialEntries().containsKey("entity_alphatest"));
        assertNotNull(addon.resourcePacks().get(0).packIcon());
    }

    @Test
    void loadsSingleMcpackArchive() throws Exception {
        Path packDir = writeResourcePack(tempDir.resolve("mcpack-resource"));
        Path archive = tempDir.resolve("sample.mcpack");
        zipDirectory(packDir, archive);

        BedrockAddon addon = BedrockAddonLoader.load(archive);

        assertEquals(1, addon.packs().size());
        assertEquals(1, addon.resourcePacks().size());
        assertTrue(addon.models().containsKey("geometry.test"));
        assertFalse(addon.particleFiles().isEmpty());
    }

    @Test
    void loadsMcaddonContainingNestedMcpacks() throws Exception {
        Path resourcePackDir = writeResourcePack(tempDir.resolve("nested-resource"));
        Path behaviorPackDir = writeBehaviorPack(tempDir.resolve("nested-behavior"), resourcePackDir);
        Path resourceArchive = tempDir.resolve("resource.mcpack");
        Path behaviorArchive = tempDir.resolve("behavior.mcpack");
        zipDirectory(resourcePackDir, resourceArchive);
        zipDirectory(behaviorPackDir, behaviorArchive);

        Path addonArchive = tempDir.resolve("sample.mcaddon");
        try (OutputStream outputStream = Files.newOutputStream(addonArchive);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            addFileToZip(zipOutputStream, resourceArchive, "resource.mcpack");
            addFileToZip(zipOutputStream, behaviorArchive, "behavior.mcpack");
        }

        BedrockAddon addon = BedrockAddonLoader.load(addonArchive);

        assertEquals(2, addon.packs().size());
        assertEquals(1, addon.resourcePacks().size());
        assertEquals(1, addon.dataPacks().size());
        assertTrue(addon.attachables().containsKey("eyelib:test_attachable"));
        assertTrue(addon.clientEntities().containsKey("eyelib:test_entity"));
    }

    @Test
    void warnsWhenManifestDependencyIsMissing() throws Exception {
        Path addonRoot = tempDir.resolve("missing-dependency-addon");
        writeBehaviorPackWithMissingDependency(addonRoot.resolve("behavior_pack"));

        BedrockAddon addon = BedrockAddonLoader.load(addonRoot);

        assertTrue(addon.warnings().stream().anyMatch(warning ->
                warning.code() == BedrockAddonWarningCode.DEPENDENCY_NOT_RESOLVED
                        && "manifest.json".equals(warning.relativePath())));
    }

    @Test
    void warnsWhenManifestContainsUnmodeledField() throws Exception {
        Path addonRoot = tempDir.resolve("manifest-extra-field-addon");
        Path packDir = addonRoot.resolve("resource_pack");
        Files.createDirectories(packDir);
        writeString(packDir.resolve("manifest.json"), resourceManifestJsonWithExtras(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        BedrockAddon addon = BedrockAddonLoader.load(addonRoot);

        assertTrue(addon.warnings().stream().anyMatch(warning ->
                warning.code() == BedrockAddonWarningCode.MANIFEST_FIELD_UNMANAGED
                        && warning.message().contains("metadata.note")));
        assertTrue(addon.warnings().stream().anyMatch(warning ->
                warning.code() == BedrockAddonWarningCode.MANIFEST_FIELD_UNMANAGED
                        && warning.message().contains("custom_field")));
    }

    @Test
    void warnsWhenFlattenedResourceOverridesEarlierPack() throws Exception {
        Path addonRoot = tempDir.resolve("duplicate-animation-addon");
        Path packA = writeResourcePack(addonRoot.resolve("resource_pack_a"));
        Path packB = writeResourcePack(addonRoot.resolve("resource_pack_b"));
        writeString(packA.resolve("animations/test.animation.json"), animationJsonWithName("animation.test.shared"));
        writeString(packB.resolve("animations/test.animation.json"), animationJsonWithName("animation.test.shared"));

        BedrockAddon addon = BedrockAddonLoader.load(addonRoot);

        assertTrue(addon.animations().containsKey("animation.test.shared"));
        assertTrue(addon.warnings().stream().anyMatch(warning ->
                warning.code() == BedrockAddonWarningCode.DUPLICATE_OVERRIDE
                        && "animation.test.shared".equals(warning.relativePath())));
    }

    @Test
    void fallsBackToUnmanagedWhenManagedFamilyParseFails() throws Exception {
        Path addonRoot = tempDir.resolve("parse-failure-addon");
        Path resourcePack = writeResourcePack(addonRoot.resolve("resource_pack"));
        writeString(resourcePack.resolve("particles/test.particle.json"), "{\"format_version\":\"1.10.0\",\"particle_effect\":{}}\n");

        BedrockAddon addon = BedrockAddonLoader.load(addonRoot);

        assertTrue(addon.models().containsKey("geometry.test"));
        assertTrue(addon.unmanagedResources().containsKey("resource_pack:particles/test.particle.json"));
        assertEquals(BedrockUnmanagedReason.SCHEMA_PARSE_FAILED,
                addon.unmanagedResources().get("resource_pack:particles/test.particle.json").reason());
        assertTrue(addon.warnings().stream().anyMatch(warning ->
                warning.code() == BedrockAddonWarningCode.SCHEMA_PARSE_FAILED
                        && "particles/test.particle.json".equals(warning.relativePath())));
    }

    private Path writeResourcePack(Path packDir) throws Exception {
        Files.createDirectories(packDir);
        String uuidBase = UUID.randomUUID().toString();
        String moduleUuid = UUID.randomUUID().toString();
        writeString(packDir.resolve("manifest.json"), resourceManifestJson(uuidBase, moduleUuid));
        writePng(packDir.resolve("pack_icon.png"), 0xFF55AA33);
        writeString(packDir.resolve("models/entity/test.geo.json"), geometryJson());
        writeString(packDir.resolve("entity/test.entity.json"), clientEntityJson());
        writeString(packDir.resolve("attachables/test.attachable.json"), attachableJson());
        writeString(packDir.resolve("animations/test.animation.json"), animationJson());
        writeString(packDir.resolve("animation_controllers/test.animation_controllers.json"), animationControllerJson());
        writeString(packDir.resolve("render_controllers/test.render_controllers.json"), renderControllerJson());
        writeString(packDir.resolve("particles/test.particle.json"), particleJson());
        writeString(packDir.resolve("materials/test.material"), materialJson());
        writePng(packDir.resolve("textures/entity/test.png"), 0xFFFFCC00);
        return packDir;
    }

    private Path writeBehaviorPack(Path packDir, Path resourcePackDir) throws Exception {
        Files.createDirectories(packDir);
        String dependencyUuid = extractPackUuid(resourcePackDir.resolve("manifest.json"));
        String uuidBase = UUID.randomUUID().toString();
        String moduleUuid = UUID.randomUUID().toString();
        writeString(packDir.resolve("manifest.json"), behaviorManifestJson(uuidBase, moduleUuid, dependencyUuid));
        return packDir;
    }

    private Path writeBehaviorPackWithMissingDependency(Path packDir) throws Exception {
        Files.createDirectories(packDir);
        String uuidBase = UUID.randomUUID().toString();
        String moduleUuid = UUID.randomUUID().toString();
        String missingDependencyUuid = UUID.randomUUID().toString();
        writeString(packDir.resolve("manifest.json"), behaviorManifestJson(uuidBase, moduleUuid, missingDependencyUuid));
        return packDir;
    }

    private static String extractPackUuid(Path manifestPath) throws IOException {
        String manifest = Files.readString(manifestPath, StandardCharsets.UTF_8);
        int markerIndex = manifest.indexOf("\"uuid\": \"");
        int start = markerIndex + "\"uuid\": \"".length();
        int end = manifest.indexOf('"', start);
        return manifest.substring(start, end);
    }

    private static void writeString(Path path, String contents) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, contents, StandardCharsets.UTF_8);
    }

    private static void writePng(Path path, int argb) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, argb);
        ImageIO.write(image, "png", path.toFile());
    }

    private static void zipDirectory(Path sourceDir, Path archivePath) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(archivePath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
             var stream = Files.walk(sourceDir)) {
            stream
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> {
                        try {
                            String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                            ZipEntry entry = new ZipEntry(entryName);
                            zipOutputStream.putNextEntry(entry);
                            Files.copy(path, zipOutputStream);
                            zipOutputStream.closeEntry();
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }
    }

    private static void addFileToZip(ZipOutputStream zipOutputStream, Path file, String entryName) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private static String resourceManifestJson(String uuid, String moduleUuid) {
        return """
                {
                  "format_version": 2,
                  "header": {
                    "name": "Test Resource Pack",
                    "description": "fixture",
                    "uuid": "%s",
                    "version": [1, 0, 0],
                    "min_engine_version": [1, 21, 0]
                  },
                  "modules": [
                    {
                      "type": "resources",
                      "uuid": "%s",
                      "version": [1, 0, 0]
                    }
                  ]
                }
                """.formatted(uuid, moduleUuid);
    }

    private static String resourceManifestJsonWithExtras(String uuid, String moduleUuid) {
        return """
                {
                  "format_version": 2,
                  "header": {
                    "name": "Test Resource Pack",
                    "description": "fixture",
                    "uuid": "%s",
                    "version": [1, 0, 0],
                    "min_engine_version": [1, 21, 0]
                  },
                  "metadata": {
                    "authors": ["tester"],
                    "note": "keep me"
                  },
                  "modules": [
                    {
                      "type": "resources",
                      "uuid": "%s",
                      "version": [1, 0, 0]
                    }
                  ],
                  "custom_field": true
                }
                """.formatted(uuid, moduleUuid);
    }

    private static String behaviorManifestJson(String uuid, String moduleUuid, String dependencyUuid) {
        return """
                {
                  "format_version": 2,
                  "header": {
                    "name": "Test Behavior Pack",
                    "description": "fixture",
                    "uuid": "%s",
                    "version": [1, 0, 0],
                    "min_engine_version": [1, 21, 0]
                  },
                  "modules": [
                    {
                      "type": "data",
                      "uuid": "%s",
                      "version": [1, 0, 0]
                    }
                  ],
                  "dependencies": [
                    {
                      "uuid": "%s",
                      "version": [1, 0, 0]
                    }
                  ]
                }
                """.formatted(uuid, moduleUuid, dependencyUuid);
    }

    private static String geometryJson() {
        return """
                {
                  "format_version": "1.12.0",
                  "minecraft:geometry": [
                    {
                      "description": {
                        "identifier": "geometry.test",
                        "texture_width": 16,
                        "texture_height": 16,
                        "visible_bounds_width": 2,
                        "visible_bounds_height": 2,
                        "visible_bounds_offset": [0, 1, 0]
                      },
                      "bones": [
                        {
                          "name": "root",
                          "pivot": [0, 0, 0],
                          "cubes": [
                            {
                              "origin": [-1, 0, -1],
                              "size": [2, 2, 2],
                              "uv": [0, 0]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private static String clientEntityJson() {
        return """
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "eyelib:test_entity",
                      "materials": { "default": "material.default" },
                      "textures": { "default": "textures/entity/test" },
                      "geometry": { "default": "geometry.test" },
                      "animations": {
                        "idle": "animation.test.idle",
                        "controller.main": "controller.animation.test"
                      },
                      "render_controllers": ["controller.render.test"],
                      "scripts": {
                        "animate": ["controller.main"]
                      }
                    }
                  }
                }
                """;
    }

    private static String attachableJson() {
        return """
                {
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "eyelib:test_attachable",
                      "textures": { "default": "textures/entity/test" },
                      "geometry": { "default": "geometry.test" },
                      "scripts": {
                        "animate": ["animation.test.idle"]
                      }
                    }
                  }
                }
                """;
    }

    private static String animationJson() {
        return animationJsonWithName("animation.test.idle");
    }

    private static String animationJsonWithName(String animationName) {
        return """
                {
                  "format_version": "1.8.0",
                  "animations": {
                    "%s": {
                      "loop": true,
                      "bones": {
                        "root": {
                          "rotation": [0, 0, 0]
                        }
                      }
                    }
                  }
                }
                """.formatted(animationName);
    }

    private static String animationControllerJson() {
        return """
                {
                  "animation_controllers": {
                    "controller.animation.test": {
                      "initial_state": "default",
                      "states": {
                        "default": {
                          "animations": {
                            "idle": "1.0"
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static String renderControllerJson() {
        return """
                {
                  "render_controllers": {
                    "controller.render.test": {
                      "geometry": "geometry.default",
                      "textures": ["texture.default"],
                      "materials": [
                        { "*": "material.default" }
                      ]
                    }
                  }
                }
                """;
    }

    private static String particleJson() {
        return """
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "eyelib:test_particle",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "eyelib:test_particle"
                      }
                    }
                  }
                }
                """;
    }

    private static String materialJson() {
        return """
                {
                  "materials": {
                    "entity_alphatest": {
                    }
                  }
                }
                """;
    }
}
