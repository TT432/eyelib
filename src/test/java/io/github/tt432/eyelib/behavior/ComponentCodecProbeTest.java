package io.github.tt432.eyelibbehavior;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibbehavior.component.property.Breathable;
import io.github.tt432.eyelibbehavior.component.property.CanClimb;
import io.github.tt432.eyelibbehavior.component.property.CollisionBox;
import io.github.tt432.eyelibbehavior.component.property.Explode;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 探路测试：验证组件 CODEC 能正确解析 Bedrock 格式 JSON。
 *
 * @author TT432
 */
class ComponentCodecProbeTest {

    // ─── 数据组件 ─────────────────────────────────

    @Test
    void breathableMinimalJson() {
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
        assertTrue(parsed.breathes_lava());
        assertFalse(parsed.breathes_solids());
        assertTrue(parsed.generates_bubbles());
        assertEquals(0.0f, parsed.inhale_time());
        assertTrue(parsed.breathe_blocks().isEmpty());  // Optional, 未提供 → empty
        assertTrue(parsed.non_breathe_blocks().isEmpty());
    }

    @Test
    void breathableWithArrays() {
        var json = """
                {
                    "breathe_blocks": ["sand", "gravel"],
                    "non_breathe_blocks": ["water"]
                }
                """;
        var parsed = Breathable.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals(2, parsed.breathe_blocks().orElseThrow().size());
        assertTrue(parsed.breathe_blocks().orElseThrow().contains("sand"));
        assertEquals(1, parsed.non_breathe_blocks().orElseThrow().size());
    }

    @Test
    void collisionBoxJson() {
        var json = """
                { "width": 0.9, "height": 1.3 }
                """;
        var parsed = CollisionBox.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals(0.9f, parsed.width());
        assertEquals(1.3f, parsed.height());
    }

    @Test
    void explodeMinimalJson() {
        var json = """
                { "fuse_length": 3.0, "power": 4.0, "causes_fire": true }
                """;
        var parsed = Explode.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals(3.0f, parsed.fuse_length());
        assertEquals(4.0f, parsed.power());
        assertTrue(parsed.causes_fire());
        assertFalse(parsed.fuse_lit());           // default
        assertTrue(parsed.breaks_blocks());       // default
    }

    @Test
    void explodeDefaultsJson() {
        var json = """
                {}
                """;
        var parsed = Explode.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals(0.0f, parsed.fuse_length());
        assertEquals(3.0f, parsed.power());
        assertFalse(parsed.fuse_lit());
    }

    // ─── 标记组件 ─────────────────────────────────

    @Test
    void markerComponentRoundTrip() {
        var json = """
                {}
                """;
        var parsed = CanClimb.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals("can_climb", parsed.id());
        // 往返：encode 再 decode
        var encoded = CanClimb.CODEC.encodeStart(JsonOps.INSTANCE, parsed)
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        var decoded = CanClimb.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, s -> { throw new AssertionError(s); });
        assertEquals("can_climb", decoded.id());
    }

    // ─── DISPATCH 分发 ─────────────────────────────

    @Test
    void dispatchToHealth() {
        var json = """
                { "minecraft:health": { "value": 15, "max": 30 } }
                """;
        var result = ComponentGroup.DISPATCH_CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(json)).getOrThrow(false, s -> { throw new AssertionError(s); });
        assertTrue(result.containsKey("minecraft:health"));
        assertEquals("health", result.get("minecraft:health").id());
    }

    @Test
    void dispatchToCollisionBox() {
        var json = """
                { "minecraft:collision_box": { "width": 0.6, "height": 1.8 } }
                """;
        var result = ComponentGroup.DISPATCH_CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(json)).getOrThrow(false, s -> { throw new AssertionError(s); });
        assertTrue(result.containsKey("minecraft:collision_box"));
        assertEquals("collision_box", result.get("minecraft:collision_box").id());
    }

    @Test
    void dispatchToMarkerComponent() {
        var json = """
                { "minecraft:is_baby": {} }
                """;
        var result = ComponentGroup.DISPATCH_CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(json)).getOrThrow(false, s -> { throw new AssertionError(s); });
        assertTrue(result.containsKey("minecraft:is_baby"));
        assertEquals("is_baby", result.get("minecraft:is_baby").id());
    }

    @Test
    void dispatchUnknownFallsBackToEmptyComponent() {
        var json = """
                { "minecraft:completely_unknown_xyz": { "a": 1 } }
                """;
        var result = ComponentGroup.DISPATCH_CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(json)).getOrThrow(false, s -> { throw new AssertionError(s); });
        assertTrue(result.containsKey("minecraft:completely_unknown_xyz"));
        assertEquals("empty", result.get("minecraft:completely_unknown_xyz").id());
    }
}
