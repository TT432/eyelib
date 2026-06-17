package io.github.tt432.eyelib.behavior;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.behavior.component.property.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对照 Mojang Creator 文档 EntityBehaviorIntroduction.md 的 CODEC 规范测试。
 * Oracle 来自 Bedrock 行为 JSON schema 定义。
 *
 * @author TT432
 */
class BehaviorCodecSpecTest {

    // === Mojang: CollisionBox 定义实体碰撞体积 ===

    @Test
    @DisplayName("Mojang §CollisionBox: width/height 往返")
    void collisionBoxRoundTrip() {
        var json = "{\"width\": 0.6, \"height\": 1.8}";
        var parsed = CollisionBox.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals(0.6f, parsed.width(), 0.001f);
        assertEquals(1.8f, parsed.height(), 0.001f);
    }

    // === Mojang: Explode 定义爆炸参数 ===

    @Test
    @DisplayName("Mojang §Explode: 爆炸参数解析")
    void explodeRoundTrip() {
        var json = """
                {
                    "fuse_length": 3.0,
                    "fuse_lit": true,
                    "power": 4,
                    "max_resistance": 999.0,
                    "destroy_affected_by_griefing": true,
                    "fire_affected_by_griefing": true
                }
                """;
        var parsed = Explode.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals(3.0f, parsed.fuse_length(), 0.001f);
        assertTrue(parsed.fuse_lit());
        assertEquals(4.0f, parsed.power(), 0.001f);
    }

    // === Mojang: Breathable 定义呼吸属性 ===

    @Test
    @DisplayName("Mojang §Breathable: 总供氧 + 窒息时间")
    void breathableRoundTrip() {
        var json = """
                {
                    "total_supply": 15,
                    "suffocate_time": -20,
                    "breathes_air": true,
                    "breathes_water": false,
                    "breathes_lava": true,
                    "breathes_solids": false,
                    "generates_bubbles": true,
                    "inhale_time": 0.0
                }
                """;
        var parsed = Breathable.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals(15, parsed.total_supply());
        assertEquals(-20, parsed.suffocate_time());
        assertTrue(parsed.breathes_air());
        assertFalse(parsed.breathes_water());
        assertEquals(0.0f, parsed.inhale_time());
    }

    // === Mojang: CanClimb (marker 组件 — 无数据字段) ===

    @Test
    @DisplayName("Mojang §CanClimb: marker 组件 CODEC 存在")
    void canClimbMarkerComponent() {
        assertNotNull(CanClimb.CODEC);
        var json = "{}";
        var parsed = CanClimb.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertNotNull(parsed);
    }
}
