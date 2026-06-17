package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangObject;
import io.github.tt432.eyelibmolang.type.MolangString;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MolangConstantExpressionEvaluator 编译期常量折叠测试。
 * 覆盖每种 AST 节点类型的常量折叠行为。
 *
 * @author TT432
 */
@NullMarked
class MolangConstantExpressionEvaluatorTest {

    @Test
    void 数字字面量可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("42");
        assertTrue(result.isPresent());
        assertEquals(42.0f, result.get().asFloat(), 0.0001f);
        assertInstanceOf(MolangFloat.class, result.get());
    }

    @Test
    void 负数可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("-5");
        assertTrue(result.isPresent());
        assertEquals(-5.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 逻辑非零为真() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("!0");
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 逻辑非一为假() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("!1");
        assertTrue(result.isPresent());
        assertEquals(0.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 加法可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("1+2");
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 乘法可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("3*4");
        assertTrue(result.isPresent());
        assertEquals(12.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 除法可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("8/2");
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 除零不可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("5/0");
        assertTrue(result.isEmpty());
    }

    @Test
    void 三元条件真值取WhenTrue分支() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("1?2:3");
        assertTrue(result.isPresent());
        assertEquals(2.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 三元条件假值取WhenFalse分支() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("0?2:3");
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 二元条件真值为WhenTrue分支的值() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("1?2");
        assertTrue(result.isPresent());
        assertEquals(2.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 二元条件假值返回零() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("0?2");
        assertTrue(result.isPresent());
        assertEquals(0.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 字符串字面量可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("'hello'");
        assertTrue(result.isPresent());
        assertInstanceOf(MolangString.class, result.get());
        assertEquals("hello", ((MolangString) result.get()).v());
    }

    @Test
    void 标识符不可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("x");
        assertTrue(result.isEmpty());
    }

    @Test
    void mathAbs通过evaluateNumber可求值() {
        // evaluateConstantExpr 不处理 CallExpr，故 tryEvaluate 返回 empty；
        // math.abs 优化在 evaluateNumber 路径中，通过 tryEvaluateNumber 可达
        Optional<Float> result = MolangConstantExpressionEvaluator.tryEvaluateNumber("math.abs(-5)");
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.get(), 0.0001f);
    }

    @Test
    void mathAbs通过tryEvaluate不可折叠() {
        // CallExpr 未被 evaluateConstantExpr 处理
        assertTrue(MolangConstantExpressionEvaluator.tryEvaluate("math.abs(-5)").isEmpty());
    }

    @Test
    void tryEvaluateNumber返回浮点数() {
        Optional<Float> result = MolangConstantExpressionEvaluator.tryEvaluateNumber("1+2");
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get(), 0.0001f);
    }

    @Test
    void 括号分组表达式可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("(1+2)*3");
        assertTrue(result.isPresent());
        assertEquals(9.0f, result.get().asFloat(), 0.0001f);
    }

    @Test
    void 减法可折叠() {
        Optional<MolangObject> result = MolangConstantExpressionEvaluator.tryEvaluate("5-3");
        assertTrue(result.isPresent());
        assertEquals(2.0f, result.get().asFloat(), 0.0001f);
    }
}
