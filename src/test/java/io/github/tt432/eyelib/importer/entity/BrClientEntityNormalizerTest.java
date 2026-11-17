package io.github.tt432.eyelib.importer.entity;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BrClientEntityNormalizations} 的注册链语义与 {@link BrClientEntity} 公开 codec 的挂载验证。
 *
 * @author TT432
 */
class BrClientEntityNormalizerTest {
    private static final String MINIMAL_JSON = """
            {
              "minecraft:client_entity": {
                "description": {
                  "identifier": "eyelib:normalizer",
                  "geometry": { "default": "geometry.test" }
                }
              }
            }
            """;

    @BeforeEach
    void clearBefore() {
        BrClientEntityNormalizations.clear();
        ClientEntityTogglesDefaults.clear();
    }

    @AfterEach
    void clearAfter() {
        BrClientEntityNormalizations.clear();
        ClientEntityTogglesDefaults.clear();
    }

    private static BrClientEntity minimal() {
        return TestCodecUtil.unwrap(BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(MINIMAL_JSON)));
    }

    private static BrClientEntity withToggles(BrClientEntity entity, BrClientEntity.Toggles toggles) {
        return new BrClientEntity(
                entity.identifier(), entity.min_engine_version(), entity.materials(), entity.textures(),
                entity.geometry(), entity.animations(), entity.animation_controllers(), entity.particle_effects(),
                entity.sound_effects(), entity.render_controllers(), entity.renderControllerConditions(),
                entity.scripts(), entity.spawn_egg(), entity.item(), toggles);
    }

    @Test
    void emptyRegistryReturnsSameInstance() {
        assertEquals(0, BrClientEntityNormalizations.size());

        BrClientEntity entity = minimal();
        assertSame(entity, BrClientEntityNormalizations.apply(entity));
    }

    @Test
    void unchangedHookKeepsInstanceIdentity() {
        try (BrClientEntityNormalizations.Handle ignored = BrClientEntityNormalizations.register(e -> e)) {
            BrClientEntity entity = minimal();
            assertSame(entity, BrClientEntityNormalizations.apply(entity));
        }
    }

    @Test
    void registerFirstRunsBeforeTailRegistrations() {
        // 先取实体：解析本身会走归一化链，放在注册之后会污染顺序记录
        BrClientEntity entity = minimal();
        List<String> order = new ArrayList<>();

        BrClientEntityNormalizations.register(e -> {
            order.add("tail");
            return e;
        });
        BrClientEntityNormalizations.registerFirst(e -> {
            order.add("head");
            return e;
        });

        BrClientEntityNormalizations.apply(entity);
        assertEquals(List.of("head", "tail"), order);

        // 后注册的 registerFirst 更靠前
        order.clear();
        BrClientEntityNormalizations.registerFirst(e -> {
            order.add("head2");
            return e;
        });
        BrClientEntityNormalizations.apply(entity);
        assertEquals(List.of("head2", "head", "tail"), order);
    }

    @Test
    void closeRemovesOnlyItsOwnEntryAndIsIdempotent() {
        BrClientEntityNormalizer first = e -> e;
        BrClientEntityNormalizer second = e -> e;
        BrClientEntityNormalizations.Handle firstHandle = BrClientEntityNormalizations.register(first);
        BrClientEntityNormalizations.Handle secondHandle = BrClientEntityNormalizations.register(second);
        assertEquals(2, BrClientEntityNormalizations.size());

        firstHandle.close();
        assertEquals(1, BrClientEntityNormalizations.size());
        firstHandle.close();
        assertEquals(1, BrClientEntityNormalizations.size());

        secondHandle.close();
        assertEquals(0, BrClientEntityNormalizations.size());
    }

    @Test
    void sameInstanceRegisteredTwiceClosesIndependently() {
        BrClientEntityNormalizer normalizer = e -> e;
        BrClientEntityNormalizations.Handle firstHandle = BrClientEntityNormalizations.register(normalizer);
        BrClientEntityNormalizations.register(normalizer);
        assertEquals(2, BrClientEntityNormalizations.size());

        // 只移除自己那一条：同一实例两次注册互不影响
        firstHandle.close();
        assertEquals(1, BrClientEntityNormalizations.size());
    }

    @Test
    void throwingHookIsIsolatedAndLaterHooksStillRun() {
        BrClientEntityNormalizations.register(e -> {
            throw new IllegalStateException("boom");
        });
        BrClientEntityNormalizations.register(e -> withToggles(
                e, new BrClientEntity.Toggles(true, true, true, e.toggles().held_item_scale())));

        BrClientEntity result = BrClientEntityNormalizations.apply(minimal());
        assertTrue(result.enable_attachables());
        assertTrue(result.hide_armor());
        assertTrue(result.held_item_ignores_lighting());
    }

    @Test
    void publicCodecsNormalizeOnDecode() {
        try (BrClientEntityNormalizations.Handle ignored = BrClientEntityNormalizations.register(e -> withToggles(
                e, new BrClientEntity.Toggles(false, true, false, e.toggles().held_item_scale())))) {
            // 解码方向：两个公开 codec 都走归一化（单点收口，无需各 loader 逐处改）
            assertTrue(minimal().hide_armor());

            String attachableJson = """
                    {
                      "minecraft:attachable": {
                        "description": {
                          "identifier": "eyelib:normalizer_attachable",
                          "materials": { "default": "entity_alphatest" }
                        }
                      }
                    }
                    """;
            BrClientEntity attachable = TestCodecUtil.unwrap(
                    BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(attachableJson)));
            assertTrue(attachable.hide_armor());

            // 编码方向恒等：归一化结果照常写出（这里 hide_armor 已不等于缺省）
            var encoded = TestCodecUtil.unwrap(BrClientEntity.CODEC.encodeStart(JsonOps.INSTANCE, minimal()));
            assertTrue(encoded.getAsJsonObject()
                             .getAsJsonObject("minecraft:client_entity")
                             .getAsJsonObject("description")
                             .get("hide_armor").getAsBoolean());
        }
    }
}
