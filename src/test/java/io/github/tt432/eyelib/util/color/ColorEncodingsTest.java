package io.github.tt432.eyelib.util.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class ColorEncodingsTest {
    @Test
    void argbToAbgrReordersChannels() {
        assertEquals(0x11443322, ColorEncodings.argbToAbgr(0x11223344));
        assertEquals(0xFFCCBBAA, ColorEncodings.argbToAbgr(0xFFAABBCC));
    }
}