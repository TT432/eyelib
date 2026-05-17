package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibanimation.AnimationLookup;
import io.github.tt432.eyelib.client.entity.AttachableLookup;
import io.github.tt432.eyelib.client.entity.ClientEntityLookup;
import io.github.tt432.eyelibanimation.AnimationManager;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.model.ModelLookup;
import io.github.tt432.eyelib.client.render.controller.RenderControllerLookup;
import io.github.tt432.eyelibimporter.addon.BedrockAddon;
import io.github.tt432.eyelibimporter.addon.BedrockAddonAggregate;
import io.github.tt432.eyelibimporter.addon.BedrockAddonSideAggregate;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.material.BrMaterial;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllers;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BedrockAddonRuntimeBridgeTest {
    @AfterEach
    void tearDown() {
        AnimationManager.INSTANCE.clear();
        ClientEntityManager.writePort().clear();
        AttachableManager.writePort().clear();
        ModelManager.writePort().clear();
        MaterialManager.writePort().clear();
        RenderControllerManager.writePort().clear();
    }

    @Test
    void replaceFromResourcePackPublishesBridgeableFamiliesToRuntimeManagers() {
        BrAnimationSet animationSet = BrAnimationSet.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "animations": {
                    "animation.test.idle": {}
                  }
                }
                """)).getOrThrow(false, AssertionError::new);
        BrAnimationControllerSet controllerSet = BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "animation_controllers": {
                    "controller.animation.test": {
                      "initial_state": "default",
                      "states": {
                        "default": {
                          "animations": ["animation.test.idle"]
                        }
                      }
                    }
                  }
                }
                """)).getOrThrow(false, AssertionError::new);
        BrMaterial material = BrMaterial.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "materials": {
                    "entity_alphatest": {
                      "defines": {},
                      "samplerStates": {},
                      "states": {},
                      "blend": {},
                      "stencil": {}
                    }
                  }
                }
                """)).getOrThrow(false, AssertionError::new);
        BrRenderControllers renderControllers = BrRenderControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "render_controllers": {
                    "controller.render.test": {}
                  }
                }
                """)).getOrThrow(false, AssertionError::new);

        BrClientEntity clientEntity = testEntity("eyelib:client_entity");
        BrClientEntity attachable = testEntity("eyelib:test_attachable");
        Model model = new Model("geometry.test", new Int2ObjectOpenHashMap<>());

        BedrockAddonSideAggregate resourcePack = new BedrockAddonSideAggregate(
                new LinkedHashMap<>(animationSet.animations()),
                new LinkedHashMap<>(controllerSet.animationControllers()),
                linkedMapOf(clientEntity.identifier(), clientEntity),
                linkedMapOf(attachable.identifier(), attachable),
                new LinkedHashMap<>(),
                linkedMapOf(model.name(), model),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                linkedMapOf("render_controllers/test.render_controllers.json", renderControllers),
                new LinkedHashMap<>(),
                linkedMapOf("materials/test.material", material)
        );

        BedrockAddon addon = new BedrockAddon(List.of(), List.of(), new LinkedHashMap<>(), new BedrockAddonAggregate(
                resourcePack,
                BedrockAddonSideAggregate.empty()
        ));

        BedrockAddonRuntimeBridge.replaceFromAddon(addon);

        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(AnimationLookup.get("controller.animation.test"));
        assertEquals(clientEntity, ClientEntityLookup.get(clientEntity.identifier()));
        assertEquals(attachable, AttachableLookup.get(attachable.identifier()));
        assertEquals(model, ModelLookup.get(model.name()));
        assertNotNull(MaterialManager.readPort().get("entity_alphatest"));
        assertNotNull(RenderControllerLookup.get("controller.render.test"));
    }

    private static BrClientEntity testEntity(String identifier) {
        return new BrClientEntity(
                identifier,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Optional.empty()
        );
    }

    private static <K, V> LinkedHashMap<K, V> linkedMapOf(K key, V value) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
