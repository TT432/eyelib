package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.registry.SpawnRuleRegistry;
import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.importer.addon.BedrockAddonAggregate;
import io.github.tt432.eyelib.importer.addon.BedrockAddonSideAggregate;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.material.shared.BrMaterial;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.importer.render.controller.BrRenderControllers;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class BedrockAddonRuntimeBridgeTest {
    @AfterEach
    void tearDown() {
        AnimationAssetRegistry.resetStaging();
        ClientEntityManager.INSTANCE.clear();
        AttachableManager.INSTANCE.clear();
        ModelManager.INSTANCE.clear();
        MaterialManager.INSTANCE.clear();
        RenderControllerManager.INSTANCE.clear();
        SpawnRuleRegistry.clear();
    }

    @Test
    void replaceFromResourcePackPublishesBridgeableFamiliesToRuntimeManagers() {
        BrAnimationSet animationSet = TestCodecUtil.unwrap(BrAnimationSet.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                                                                                                                   {
                                                                                                                     "animations": {
                                                                                                                       "animation.test.idle": {}
                                                                                                                     }
                                                                                                                   }
                """)));
        BrAnimationControllerSet controllerSet = TestCodecUtil.unwrap(BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
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
                """)));
        BrMaterial material = TestCodecUtil.unwrap(BrMaterial.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                                                                                                       {
                                                                                                          "materials": {
                                                                                                           "entity_alphatest": {
                                                                                                             "defines": [],
                                                                                                             "+defines": [],
                                                                                                             "-defines": [],
                                                                                                             "samplerStates": [],
                                                                                                             "+samplerStates": [],
                                                                                                             "-samplerStates": [],
                                                                                                             "states": [],
                                                                                                             "+states": [],
                                                                                                             "-states": []
                                                                                                           }
                                                                                                          }
                                                                                                       }
                """)));
        BrRenderControllers renderControllers = TestCodecUtil.unwrap(BrRenderControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                                                                                                                                  {
                                                                                                                                    "render_controllers": {
                    "controller.render.test": {
                      "ignore_lighting": true
                    }
                                                                                                                                    }
                                                                                                                                  }
                """)));

        BrClientEntity clientEntity = testEntity("eyelib:client_entity");
        BrClientEntity attachable = testEntity("eyelib:test_attachable");
        Model model = Model.of("geometry.test", new Int2ObjectOpenHashMap<>());

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
                linkedMapOf("materials/test.material", material),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>()
        );

        BedrockAddon addon = new BedrockAddon(List.of(), List.of(), new LinkedHashMap<>(), new BedrockAddonAggregate(
                resourcePack,
                BedrockAddonSideAggregate.empty()
        ));

        BedrockAddonRuntimeBridge.replaceFromAddon(addon);

        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(AnimationLookup.get("controller.animation.test"));
        assertEquals(clientEntity, ClientEntityManager.INSTANCE.get(clientEntity.identifier()));
        assertEquals(attachable, AttachableManager.INSTANCE.get(attachable.identifier()));
        assertEquals(model, ModelManager.INSTANCE.get(model.name()));
        assertNotNull(MaterialManager.INSTANCE.get("entity_alphatest"));
        assertNotNull(RenderControllerManager.INSTANCE.get("controller.render.test"));
        assertTrue(RenderControllerManager.INSTANCE.get("controller.render.test").ignoreLighting());
    }

    /**
     * 回归：未选中任何 addon 包时发布会携带空 aggregate，客户端实体必须保留
     * {@code BrClientEntityLoader} 加载的 mod 基线条目（replaceAll 语义曾把基线全清，
     * 导致 SetEntityClientEntity 恒解析失败）。
     */
    @Test
    void emptyAggregateDoesNotWipeModBaselineClientEntities() {
        BrClientEntity baseline = testEntity("modid:baseline_entity");
        ClientEntityManager.INSTANCE.put(baseline.identifier(), baseline);

        BedrockAddon noPackSelected = new BedrockAddon(List.of(), List.of(), new LinkedHashMap<>(),
                new BedrockAddonAggregate(BedrockAddonSideAggregate.empty(), BedrockAddonSideAggregate.empty()));
        BedrockAddonRuntimeBridge.replaceFromAddon(noPackSelected);

        assertEquals(baseline, ClientEntityManager.INSTANCE.get(baseline.identifier()));
    }

    /**
     * addon 实体在叠加轮生效、空轮卸载：覆盖基线键时恢复原值，纯 addon 键被移除。
     */
    @Test
    void addonClientEntityOverlayAndUnloadRoundTrip() {
        BrClientEntity baseline = testEntity("modid:baseline_entity");
        ClientEntityManager.INSTANCE.put(baseline.identifier(), baseline);

        BrClientEntity override = testEntity("modid:baseline_entity");
        BrClientEntity addonOnly = testEntity("addon:extra_entity");
        LinkedHashMap<String, BrClientEntity> roundEntities = new LinkedHashMap<>();
        roundEntities.put(override.identifier(), override);
        roundEntities.put(addonOnly.identifier(), addonOnly);
        BedrockAddonSideAggregate round = new BedrockAddonSideAggregate(
                new LinkedHashMap<>(), new LinkedHashMap<>(), roundEntities, new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>());
        BedrockAddonRuntimeBridge.replaceFromAddon(new BedrockAddon(List.of(), List.of(), new LinkedHashMap<>(),
                new BedrockAddonAggregate(round, BedrockAddonSideAggregate.empty())));

        assertEquals(override, ClientEntityManager.INSTANCE.get(override.identifier()));
        assertEquals(addonOnly, ClientEntityManager.INSTANCE.get(addonOnly.identifier()));

        BedrockAddonRuntimeBridge.replaceFromAddon(new BedrockAddon(List.of(), List.of(), new LinkedHashMap<>(),
                new BedrockAddonAggregate(BedrockAddonSideAggregate.empty(), BedrockAddonSideAggregate.empty())));

        assertEquals(baseline, ClientEntityManager.INSTANCE.get(baseline.identifier()));
        assertNull(ClientEntityManager.INSTANCE.get(addonOnly.identifier()));
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
