package io.github.tt432.eyelib.molang.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Molang {@code ==}/{@code !=} 的类型感知相等语义（被编译产物通过 {@code equalsF} 调用）。
 *
 * <p>Oracle：Bedrock 文档「all numerical values are floats」+ 变量是可写的实体存储；
 * 数值无论以字面量、浮点对象还是惰性供应商表示，都必须按值比较。
 *
 * @author TT432
 */
class MolangEqualityTest {

    @Test
    void 数值按值相等_含惰性供应商变量() {
        MolangObject lazyZero = new MolangFloatSupplierObject(() -> 0.0f);
        MolangObject lazyOne = new MolangFloatSupplierObject(() -> 1.0f);

        assertEquals(1.0f, MolangFloat.valueOf(0.0f).equalsF(lazyZero), 0.0f);
        assertEquals(1.0f, lazyZero.equalsF(MolangFloat.valueOf(0.0f)), 0.0f);
        assertEquals(0.0f, MolangFloat.valueOf(1.0f).equalsF(lazyZero), 0.0f);
        assertEquals(1.0f, lazyOne.nEqualsF(MolangFloat.valueOf(0.0f)), 0.0f);
        assertEquals(0.0f, lazyZero.nEqualsF(MolangFloat.valueOf(0.0f)), 0.0f);
    }

    @Test
    void 数值与字符串不相等_字符串按值相等() {
        MolangObject zeroString = new MolangString("0");

        assertEquals(0.0f, MolangFloat.valueOf(0.0f).equalsF(zeroString), 0.0f);
        assertEquals(1.0f, MolangFloat.valueOf(0.0f).nEqualsF(zeroString), 0.0f);
        assertEquals(1.0f, new MolangString("a").equalsF(new MolangString("a")), 0.0f);
        assertEquals(0.0f, new MolangString("a").equalsF(new MolangString("b")), 0.0f);
    }

    @Test
    void 空值与数值不相等() {
        assertEquals(0.0f, MolangNull.INSTANCE.equalsF(MolangFloat.valueOf(0.0f)), 0.0f);
        assertEquals(1.0f, MolangNull.INSTANCE.nEqualsF(MolangFloat.valueOf(0.0f)), 0.0f);
    }
}
