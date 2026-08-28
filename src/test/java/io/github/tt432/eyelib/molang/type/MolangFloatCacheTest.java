package io.github.tt432.eyelib.molang.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link MolangFloat#valueOf(float)} 小整数缓存契约（Opt16）：
 * 缓存范围内整数值返回共享实例（equals 按值，语义透明），范围外/非整数每次新实例。
 *
 * @author TT432
 */
class MolangFloatCacheTest {

    @Test
    void canonicalZeroOnePreserved() {
        assertSame(MolangFloat.ZERO, MolangFloat.valueOf(0f));
        assertSame(MolangFloat.ONE, MolangFloat.valueOf(1f));
        // -0.0f 归并到 ZERO（== 语义，与缓存引入前一致）
        assertSame(MolangFloat.ZERO, MolangFloat.valueOf(-0.0f));
        assertSame(MolangFloat.ONE, MolangFloat.valueOf(true));
        assertSame(MolangFloat.ZERO, MolangFloat.valueOf(false));
    }

    @Test
    void integralValuesInRangeAreCached() {
        assertSame(MolangFloat.valueOf(2f), MolangFloat.valueOf(2f));
        assertSame(MolangFloat.valueOf(-16f), MolangFloat.valueOf(-16f));
        assertSame(MolangFloat.valueOf(256f), MolangFloat.valueOf(256f));
    }

    @Test
    void outOfRangeOrFractionalValuesAreFresh() {
        assertNotSame(MolangFloat.valueOf(-17f), MolangFloat.valueOf(-17f));
        assertNotSame(MolangFloat.valueOf(257f), MolangFloat.valueOf(257f));
        assertNotSame(MolangFloat.valueOf(1.5f), MolangFloat.valueOf(1.5f));
        assertNotSame(MolangFloat.valueOf(Float.NaN), MolangFloat.valueOf(Float.NaN));
    }

    @Test
    void cachedInstancesKeepValueSemantics() {
        assertEquals(2f, MolangFloat.valueOf(2f).asFloat());
        assertEquals(-16f, MolangFloat.valueOf(-16f).asFloat());
        assertEquals(MolangFloat.valueOf(42f), MolangFloat.valueOf(42f));
    }
}
