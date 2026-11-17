package io.github.tt432.eyelib.importer.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ClientEntityTogglesDefaults} 的缺省值可配置性：解码补缺省、编码省略当前缺省、注销回落。
 *
 * @author TT432
 */
class BrClientEntityDefaultsTest {
    private static final String ABSENT_TOGGLES_JSON = """
            {
              "minecraft:client_entity": {
                "description": {
                  "identifier": "eyelib:defaults_absent",
                  "geometry": { "default": "geometry.test" }
                }
              }
            }
            """;

    private static final String EXPLICIT_FALSE_JSON = """
            {
              "minecraft:client_entity": {
                "description": {
                  "identifier": "eyelib:defaults_explicit_false",
                  "geometry": { "default": "geometry.test" },
                  "hide_armor": false
                }
              }
            }
            """;

    @BeforeEach
    void clearBefore() {
        ClientEntityTogglesDefaults.clear();
        BrClientEntityNormalizations.clear();
    }

    @AfterEach
    void clearAfter() {
        ClientEntityTogglesDefaults.clear();
        BrClientEntityNormalizations.clear();
    }

    private static BrClientEntity parse(String json) {
        return TestCodecUtil.unwrap(BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
    }

    private static JsonObject descriptionOf(BrClientEntity entity) {
        return TestCodecUtil.unwrap(BrClientEntity.CODEC.encodeStart(JsonOps.INSTANCE, entity))
                             .getAsJsonObject()
                             .getAsJsonObject("minecraft:client_entity")
                             .getAsJsonObject("description");
    }

    @Test
    void factoryDefaultsMatchTogglesDefault() {
        assertEquals("enable_attachables", ClientEntityTogglesDefaults.ENABLE_ATTACHABLES.jsonName());
        assertEquals("hide_armor", ClientEntityTogglesDefaults.HIDE_ARMOR.jsonName());
        assertEquals("held_item_ignores_lighting", ClientEntityTogglesDefaults.HELD_ITEM_IGNORES_LIGHTING.jsonName());
        assertEquals("held_item_scale", ClientEntityTogglesDefaults.HELD_ITEM_SCALE.jsonName());

        assertFalse(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.ENABLE_ATTACHABLES));
        assertFalse(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HIDE_ARMOR));
        assertFalse(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HELD_ITEM_IGNORES_LIGHTING));
        assertSame(MolangValue.ONE, ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HELD_ITEM_SCALE));
        assertEquals(BrClientEntity.Toggles.DEFAULT, ClientEntityTogglesDefaults.effective());
    }

    @Test
    void registeredDefaultFillsAbsentKeyAndStaysOmittedOnEncode() {
        try (BrClientEntityNormalizations.Handle ignored = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.HIDE_ARMOR, true)) {
            BrClientEntity entity = parse(ABSENT_TOGGLES_JSON);

            // 缺键 → 取注册缺省
            assertTrue(entity.hide_armor());
            assertFalse(entity.enable_attachables());
            assertFalse(entity.held_item_ignores_lighting());

            // 缺省不写回：编码时值 == 当前缺省 → 省略该键
            JsonObject description = descriptionOf(entity);
            assertFalse(description.has("hide_armor"), description.toString());
            assertFalse(description.has("enable_attachables"), description.toString());
            assertFalse(description.has("held_item_ignores_lighting"), description.toString());
            assertFalse(description.has("held_item_scale"), description.toString());
        }
    }

    @Test
    void explicitValueDifferentFromRegisteredDefaultSurvivesEncode() {
        try (BrClientEntityNormalizations.Handle ignored = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.HIDE_ARMOR, true)) {
            BrClientEntity entity = parse(EXPLICIT_FALSE_JSON);

            assertFalse(entity.hide_armor());

            JsonObject description = descriptionOf(entity);
            assertTrue(description.has("hide_armor"), description.toString());
            assertFalse(description.get("hide_armor").getAsBoolean(), description.toString());
        }
    }

    @Test
    void closeRestoresFactoryDefaultAndIsIdempotent() {
        BrClientEntityNormalizations.Handle handle = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.HIDE_ARMOR, true);
        try {
            assertTrue(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HIDE_ARMOR));
            assertTrue(parse(ABSENT_TOGGLES_JSON).hide_armor());
        } finally {
            handle.close();
        }

        assertFalse(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HIDE_ARMOR));
        assertFalse(parse(ABSENT_TOGGLES_JSON).hide_armor());

        // close 幂等：重复关闭不抛异常、也不会误伤其它注册
        handle.close();
        try (BrClientEntityNormalizations.Handle ignored = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.ENABLE_ATTACHABLES, true)) {
            assertFalse(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HIDE_ARMOR));
            assertTrue(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.ENABLE_ATTACHABLES));
        }
    }

    @Test
    void laterRegistrationOverridesAndCloseFallsBackToPrevious() {
        BrClientEntityNormalizations.Handle first = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.HELD_ITEM_IGNORES_LIGHTING, true);
        try (BrClientEntityNormalizations.Handle second = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.HELD_ITEM_IGNORES_LIGHTING, false)) {
            assertFalse(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HELD_ITEM_IGNORES_LIGHTING));
        }

        // 后注册者关闭后回落到前一次注册
        assertTrue(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HELD_ITEM_IGNORES_LIGHTING));
        first.close();
        assertFalse(ClientEntityTogglesDefaults.of(ClientEntityTogglesDefaults.HELD_ITEM_IGNORES_LIGHTING));
    }

    @Test
    void effectiveReflectsRegistrations() {
        try (BrClientEntityNormalizations.Handle enable = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.ENABLE_ATTACHABLES, true);
             BrClientEntityNormalizations.Handle hide = ClientEntityTogglesDefaults.register(
                     ClientEntityTogglesDefaults.HIDE_ARMOR, true)) {
            BrClientEntity.Toggles effective = ClientEntityTogglesDefaults.effective();
            assertTrue(effective.enable_attachables());
            assertTrue(effective.hide_armor());
            assertFalse(effective.held_item_ignores_lighting());
            assertSame(MolangValue.ONE, effective.held_item_scale());

            // 便捷构造器取同一份缺省
            BrClientEntity entity = new BrClientEntity(
                    "eyelib:defaults_ctor", Map.of(), Map.of(), Map.of(),
                    Map.of(), Map.of(), Map.of(),
                    List.of(), Optional.empty());
            assertTrue(entity.enable_attachables());
            assertTrue(entity.hide_armor());
        }

        assertEquals(BrClientEntity.Toggles.DEFAULT, ClientEntityTogglesDefaults.effective());
    }

    @Test
    void molangScaleDefaultIsDynamicAndComparedByExpression() {
        MolangValue scale = MolangValue.getConstant(2);

        try (BrClientEntityNormalizations.Handle ignored = ClientEntityTogglesDefaults.register(
                ClientEntityTogglesDefaults.HELD_ITEM_SCALE, scale)) {
            BrClientEntity entity = parse(ABSENT_TOGGLES_JSON);
            assertEquals(2.0F, entity.held_item_scale().eval(new MolangScope()), 1e-6F);

            // 当前缺省在编码时省略
            assertFalse(descriptionOf(entity).has("held_item_scale"));

            // 显式写成与缺省等价的表达式（MolangValue.equals 按表达式文本比较）→ 同样省略
            String explicit = """
                    {
                      "minecraft:client_entity": {
                        "description": {
                          "identifier": "eyelib:defaults_scale_explicit",
                          "held_item_scale": "2.0"
                        }
                      }
                    }
                    """;
            assertEquals(2.0F, parse(explicit).held_item_scale().eval(new MolangScope()), 1e-6F);
            assertFalse(descriptionOf(parse(explicit)).has("held_item_scale"));

            // 与缺省不同的表达式必须写回
            String other = """
                    {
                      "minecraft:client_entity": {
                        "description": {
                          "identifier": "eyelib:defaults_scale_other",
                          "held_item_scale": "1.5"
                        }
                      }
                    }
                    """;
            JsonObject description = descriptionOf(parse(other));
            assertTrue(description.has("held_item_scale"), description.toString());
        }

        // 注销后缺省回到 1.0
        assertEquals(1.0F, parse(ABSENT_TOGGLES_JSON).held_item_scale().eval(new MolangScope()), 1e-6F);
    }
}
