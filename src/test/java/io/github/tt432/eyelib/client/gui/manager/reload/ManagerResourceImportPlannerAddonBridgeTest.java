package io.github.tt432.eyelib.client.gui.manager.reload;

import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.client.gui.manager.reload.ManagerResourceImportPlanner;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.particle.loading.ParticleDefinitionRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ManagerResourceImportPlannerAddonBridgeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerResourceImportPlannerAddonBridgeTest.class);

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
        RenderControllerManager.INSTANCE.clear();
        ClientEntityManager.INSTANCE.clear();
        AttachableManager.INSTANCE.clear();
        ModelManager.INSTANCE.clear();
        ParticleDefinitionRegistry.store().clear();
        MaterialManager.INSTANCE.clear();
    }

    @Test
    void loadResourceFolderBridgesPackRootWithManifest() throws Exception {
        Path packRoot = writeAddonResourcePack(tempDir.resolve("resource_pack"), "animation.test.idle");

        boolean addonMode = ManagerResourceImportPlanner.loadResourceFolder(packRoot, LOGGER);

        assertTrue(addonMode);
        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(AnimationLookup.get("controller.animation.test"));
        assertNotNull(RenderControllerManager.INSTANCE.get("controller.render.test"));
        assertNotNull(ClientEntityManager.INSTANCE.get("eyelib:test_entity"));
        assertNotNull(AttachableManager.INSTANCE.get("eyelib:test_attachable"));
        assertNotNull(ModelManager.INSTANCE.get("geometry.test"));
        assertNotNull(MaterialManager.INSTANCE.get("entity_alphatest"));
        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:addon_particle"));
        assertNull(ParticleDefinitionRegistry.store().get("particles/test.particle"));
    }

    @Test
    void loadResourceFolderBridgesNestedAddonRoot() throws Exception {
        Path addonRoot = tempDir.resolve("addon_root");
        writeAddonResourcePack(addonRoot.resolve("resource_pack"), "animation.test.idle");

        boolean addonMode = ManagerResourceImportPlanner.loadResourceFolder(addonRoot, LOGGER);

        assertTrue(addonMode);
        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(ClientEntityManager.INSTANCE.get("eyelib:test_entity"));
    }

    @Test
    void loadResourceFolderFallsBackToLegacyPlainFolderWhenAddonStructureMissing() throws Exception {
        Path plainRoot = tempDir.resolve("plain_resource_folder");
        writeString(plainRoot.resolve("animations/test.animation.json"), animationJson("animation.test.legacy"));
        writeString(plainRoot.resolve("attachables/test.attachable.json"), attachableJson());
        writeString(plainRoot.resolve("materials/test.material"), materialJson());

        boolean addonMode = ManagerResourceImportPlanner.loadResourceFolder(plainRoot, LOGGER);

        assertFalse(addonMode);
        assertNotNull(AnimationLookup.get("animation.test.legacy"));
        assertNotNull(AttachableManager.INSTANCE.get("eyelib:test_attachable"));
        assertNotNull(MaterialManager.INSTANCE.get("entity_alphatest"));
    }

    @Test
    void addonReloadFlowCanRefreshNestedPackChangesViaFolderReload() throws Exception {
        Path addonRoot = tempDir.resolve("addon_for_reload");
        Path packRoot = writeAddonResourcePack(addonRoot.resolve("resource_pack"), "animation.test.before");

        boolean addonMode = ManagerResourceImportPlanner.loadResourceFolder(addonRoot, LOGGER);
        assertTrue(addonMode);
        assertNotNull(AnimationLookup.get("animation.test.before"));

        writeString(packRoot.resolve("animations/test.animation.json"), animationJson("animation.test.after"));
        addonMode = ManagerResourceImportPlanner.loadResourceFolder(addonRoot, LOGGER);

        assertTrue(addonMode);
        assertNull(AnimationLookup.get("animation.test.before"));
        assertNotNull(AnimationLookup.get("animation.test.after"));
    }

    private Path writeAddonResourcePack(Path packDir, String animationName) throws IOException {
        Files.createDirectories(packDir);
        writeString(packDir.resolve("manifest.json"), resourceManifestJson(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        writeString(packDir.resolve("models/entity/test.geo.json"), geometryJson());
        writeString(packDir.resolve("entity/test.entity.json"), clientEntityJson());
        writeString(packDir.resolve("attachables/test.attachable.json"), attachableJson());
        writeString(packDir.resolve("animations/test.animation.json"), animationJson(animationName));
        writeString(packDir.resolve("animation_controllers/test.animation_controllers.json"), animationControllerJson());
        writeString(packDir.resolve("render_controllers/test.render_controllers.json"), renderControllerJson());
        writeString(packDir.resolve("particles/test.particle.json"), particleJson("eyelib:addon_particle"));
        writeString(packDir.resolve("materials/test.material"), materialJson());
        return packDir;
    }

    private static void writeString(Path path, String contents) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, contents, StandardCharsets.UTF_8);
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
                      "animation_controllers": [
                        { "controller.main": "controller.animation.test" }
                      ],
                      "render_controllers": ["controller.render.test"],
                      "scripts": {
                        "animate": ["controller.main"]
                      }
                    }
                  }
                }
                """;
    }

    private static String animationJson(String animationName) {
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

    private static String attachableJson() {
        return """
                {
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "eyelib:test_attachable",
                      "materials": { "default": "material.default" },
                      "textures": { "default": "textures/entity/test" },
                      "geometry": { "default": "geometry.test" },
                      "animations": {
                        "idle": "animation.test.idle"
                      },
                      "render_controllers": ["controller.render.test"],
                      "scripts": {
                        "animate": ["idle"]
                      }
                    }
                  }
                }
                """;
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

    private static String particleJson(String identifier) {
        return """
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "%s",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "textures/particle/particles"
                      }
                    }
                  }
                }
                """.formatted(identifier);
    }
}
