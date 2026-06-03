package io.github.tt432.eyelibbehavior;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibbehavior.component.Health;
import io.github.tt432.eyelibbehavior.component.RawComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BehaviorComponents 及相关组件的单元测试。
 *
 * @author TT432
 */
class BehaviorEntityComponentsTest {

    @Test
    void emptyComponents() {
        var bc = BehaviorComponents.EMPTY;
        assertTrue(bc.components().isEmpty());
    }

    @Test
    void healthComponentCodec() {
        var json = """
                { "value": 20, "max": 20 }
                """;
        var parsed = Health.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> {});
        assertEquals(20, parsed.value());
        assertEquals(20, parsed.max());
    }

    @Test
    void healthComponentDefaultMax() {
        var json = """
                { "value": 10 }
                """;
        var parsed = Health.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> {});
        assertEquals(10, parsed.value());
        assertEquals(20, parsed.max());  // 默认值
    }

    @Test
    void rawComponentPreservesComponentId() {
        JsonObject rawData = new JsonObject();
        rawData.addProperty("some_field", true);
        var raw = new RawComponent("minecraft:is_baby", rawData);
        assertEquals("minecraft:is_baby", raw.componentId());
        assertNotNull(raw.rawData());
        assertTrue(raw.rawData().get("some_field").getAsBoolean());
    }

    @Test
    void rawComponentId() {
        var raw = new RawComponent("minecraft:unknown_test", new JsonObject());
        assertEquals("raw:minecraft:unknown_test", raw.id());
    }
}
