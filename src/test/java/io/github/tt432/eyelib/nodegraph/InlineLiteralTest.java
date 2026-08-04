package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

/**
 * {@link InlineLiteral} 契约：ANY 端口行内文本的智能解析（int/float/bool/string）
 * 与回显文本（数字去 .0）。
 */
class InlineLiteralTest {

    @Test
    void parsesIntegers() {
        JsonElement v = InlineLiteral.parse("5");
        assertTrue(v.getAsJsonPrimitive().isNumber());
        assertEquals(5, v.getAsInt());
        assertEquals(-12, InlineLiteral.parse("-12").getAsInt());
        assertEquals(0, InlineLiteral.parse("0").getAsInt());
    }

    @Test
    void parsesDecimals() {
        assertEquals(1.5, InlineLiteral.parse("1.5").getAsDouble(), 1e-9);
        assertEquals(0.25, InlineLiteral.parse(".25").getAsDouble(), 1e-9);
        assertEquals(-2.5e2, InlineLiteral.parse("-2.5e2").getAsDouble(), 1e-9);
    }

    @Test
    void parsesBooleans() {
        assertTrue(InlineLiteral.parse("true").getAsBoolean());
        assertFalse(InlineLiteral.parse("false").getAsBoolean());
    }

    @Test
    void fallsBackToString() {
        JsonElement v = InlineLiteral.parse("query.health");
        assertTrue(v.getAsJsonPrimitive().isString());
        assertEquals("query.health", v.getAsString());
        // 非 true/false 的大小写变体 → 字符串
        assertEquals("True", InlineLiteral.parse("True").getAsString());
        assertEquals("", InlineLiteral.parse("").getAsString());
        // 混合格式不按数字解析
        assertEquals("1.2.3", InlineLiteral.parse("1.2.3").getAsString());
    }

    @Test
    void toTextRoundTrip() {
        assertEquals("5", InlineLiteral.toText(new JsonPrimitive(5)));
        assertEquals("1.5", InlineLiteral.toText(new JsonPrimitive(1.5)));
        assertEquals("1", InlineLiteral.toText(new JsonPrimitive(1.0)));
        assertEquals("true", InlineLiteral.toText(new JsonPrimitive(true)));
        assertEquals("foo", InlineLiteral.toText(new JsonPrimitive("foo")));
    }
}
