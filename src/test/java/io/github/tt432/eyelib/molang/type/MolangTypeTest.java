package io.github.tt432.eyelib.molang.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link MolangType} 契约：运行时推断只能分 string/array/number(float)；
 * 格式化按声明类型呈现（float 去尾零、int 取整、bool 带底层值、string 引号、array 递归）。
 */
class MolangTypeTest {

    @Test
    void inferDistinguishesStringArrayNumber() {
        assertEquals(MolangType.STRING, MolangType.infer(MolangString.valueOf("abc")));
        assertEquals(MolangType.ARRAY, MolangType.infer(new MolangArray<>(List.of(MolangFloat.ONE))));
        assertEquals(MolangType.FLOAT, MolangType.infer(MolangFloat.valueOf(1)));
        assertEquals(MolangType.FLOAT, MolangType.infer(MolangFloat.valueOf(3.5f)));
        assertEquals(MolangType.DYNAMIC, MolangType.infer(null));
        assertEquals(MolangType.DYNAMIC, MolangType.infer(MolangNull.INSTANCE));
    }

    @Test
    void numberSubtypesReportIsNumber() {
        assertTrue(MolangType.FLOAT.isNumber());
        assertTrue(MolangType.INT.isNumber());
        assertTrue(MolangType.BOOL.isNumber());
        assertFalse(MolangType.STRING.isNumber());
        assertFalse(MolangType.ARRAY.isNumber());
        assertFalse(MolangType.DYNAMIC.isNumber());
    }

    @Test
    void floatFormatStripsTrailingZero() {
        assertEquals("3", MolangType.FLOAT.format(MolangFloat.valueOf(3)));
        assertEquals("3.5", MolangType.FLOAT.format(MolangFloat.valueOf(3.5f)));
        assertEquals("-2", MolangType.FLOAT.format(MolangFloat.valueOf(-2)));
    }

    @Test
    void intFormatTruncatesTowardZero() {
        assertEquals("3", MolangType.INT.format(MolangFloat.valueOf(3.9f)));
        assertEquals("-3", MolangType.INT.format(MolangFloat.valueOf(-3.9f)));
    }

    @Test
    void boolFormatKeepsUnderlyingValue() {
        assertEquals("true (1)", MolangType.BOOL.format(MolangFloat.ONE));
        assertEquals("false (0)", MolangType.BOOL.format(MolangFloat.ZERO));
    }

    @Test
    void stringFormatQuotes() {
        assertEquals("'abc'", MolangType.STRING.format(MolangString.valueOf("abc")));
    }

    @Test
    void arrayFormatRecursesWithInferredElementTypes() {
        MolangArray<MolangObject> array = new MolangArray<>(List.of(
                MolangFloat.valueOf(1.5f), MolangString.valueOf("x")));
        assertEquals("[1.5, 'x']", MolangType.ARRAY.format(array));
    }

    @Test
    void nullFormatsAsPlaceholder() {
        assertEquals("<null>", MolangType.FLOAT.format(null));
    }

    @Test
    void displayNamesFollowNumberAngleConvention() {
        assertEquals("number<float>", MolangType.FLOAT.displayName());
        assertEquals("number<int>", MolangType.INT.displayName());
        assertEquals("number<bool>", MolangType.BOOL.displayName());
        assertEquals("string", MolangType.STRING.displayName());
        assertEquals("array", MolangType.ARRAY.displayName());
        assertEquals("dynamic", MolangType.DYNAMIC.displayName());
    }
}
