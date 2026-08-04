package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link ColorValues} 契约：hex ↔ 通道互转、ARGB int 互转、8bit 精确判据。
 */
class ColorValuesTest {

    @Test
    void parsesArgbHex() {
        float[] c = ColorValues.parse("#FF00FF00");
        assert c != null;
        assertArrayEquals(new float[]{0f, 1f, 0f, 1f}, c, 1e-6f);
    }

    @Test
    void parsesRgbHexAsOpaque() {
        float[] c = ColorValues.parse("#00FF00");
        assert c != null;
        assertArrayEquals(new float[]{0f, 1f, 0f, 1f}, c, 1e-6f);
    }

    @Test
    void parsesHalfAlpha() {
        float[] c = ColorValues.parse("#80FFFFFF");
        assert c != null;
        assertArrayEquals(new float[]{1f, 1f, 1f, 128 / 255f}, c, 1e-6f);
    }

    @Test
    void rejectsGarbage() {
        assertNull(ColorValues.parse("FFFFFF"));
        assertNull(ColorValues.parse("#FFF"));
        assertNull(ColorValues.parse("#GGGGGGGG"));
        assertNull(ColorValues.parse(""));
    }

    @Test
    void hexRoundTrips() {
        assertEquals("#FF00FF00", ColorValues.toHex(0f, 1f, 0f, 1f));
        float[] c = ColorValues.parse("#80102030");
        assert c != null;
        assertEquals("#80102030", ColorValues.toHex(c[0], c[1], c[2], c[3]));
    }

    @Test
    void clampsOutOfRangeChannels() {
        assertEquals("#FFFFFFFF", ColorValues.toHex(2f, 2f, 2f, 2f));
        assertEquals("#00000000", ColorValues.toHex(-1f, -1f, -1f, -1f));
    }

    @Test
    void argbIntInterconversion() {
        assertEquals(0xFF00FF00, ColorValues.toArgbInt("#FF00FF00"));
        assertEquals("#FF00FF00", ColorValues.fromArgbInt(0xFF00FF00));
        // 非法输入 → 不透明白
        assertEquals(0xFFFFFFFF, ColorValues.toArgbInt("garbage"));
    }

    @Test
    void byteExactness() {
        assertTrue(ColorValues.isByteExact(0));
        assertTrue(ColorValues.isByteExact(1));
        // float 可表示的字节值（管线往返一致）
        assertTrue(ColorValues.isByteExact((double) (128 / 255f)));
        // 0.5 不是字节值；1/3 与 128/255.0 经 float 管线有损
        assertFalse(ColorValues.isByteExact(0.5));
        assertFalse(ColorValues.isByteExact(1.0 / 3.0));
        assertFalse(ColorValues.isByteExact(128.0 / 255.0));
        assertFalse(ColorValues.isByteExact(-0.1));
        assertFalse(ColorValues.isByteExact(1.5));
    }
}
