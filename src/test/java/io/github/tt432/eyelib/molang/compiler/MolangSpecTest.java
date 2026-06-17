package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.MolangMath;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.MolangClass;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对照 Bedrock Molang 规范（Mojang Creator 文档 + Bedrock Wiki）的编译→求值管线测试。
 * Oracle 来自 Bedrock 文档定义的语义，不来自当前实现输出。
 *
 * @author TT432
 */
class MolangSpecTest {

    private MolangCompilerImpl compiler;

    @BeforeEach
    void setUp() {
        compiler = new MolangCompilerImpl();
        // 注册 math.* 映射（在纯 JUnit 中运行时需要手动注册）
        if (MolangMappingTree.INSTANCE.findClasses("math").isEmpty()) {
            MolangMappingTree.INSTANCE.addNode("math", new MolangClass(MolangMath.class, true));
        }
    }

    // === Core arithmetic ===

    @Test
    @DisplayName("Bedrock §算术运算: 加减乘除 + 除零返回 0.0")
    void arithmeticOperations() {
        assertEvaluatesTo("1+2", 3.0f);
        assertEvaluatesTo("5-3", 2.0f);
        assertEvaluatesTo("4*3", 12.0f);
        assertEvaluatesTo("8/2", 4.0f);
        assertEvaluatesTo("5/0", 0.0f, "Bedrock: 除零返回 0.0");
    }

    @Test
    @DisplayName("Bedrock §比较运算: 1.0/0.0 返回布尔")
    void comparisonOperators() {
        assertEvaluatesTo("1<2", 1.0f);
        assertEvaluatesTo("2<1", 0.0f);
        assertEvaluatesTo("2<=2", 1.0f);
        assertEvaluatesTo("3>=3", 1.0f);
        assertEvaluatesTo("3==3", 1.0f);
        assertEvaluatesTo("3!=3", 0.0f);
    }

    @Test
    @DisplayName("Bedrock §逻辑运算: && 和 ||")
    void logicalOperators() {
        assertEvaluatesTo("1&&1", 1.0f);
        assertEvaluatesTo("1&&0", 0.0f);
        assertEvaluatesTo("0&&1", 0.0f);
        assertEvaluatesTo("1||0", 1.0f);
        assertEvaluatesTo("0||0", 0.0f);
    }

    @Test
    @DisplayName("Bedrock §空值合并: ?? 操作符")
    void nullCoalesce() {
        assertEvaluatesTo("1??2", 1.0f);
        assertEvaluatesTo("does_not_exist??42", 42.0f,
                "未知变量 → 回退到右值");
    }

    @Test
    @DisplayName("Bedrock §三元条件: condition ? true : false")
    void ternaryConditional() {
        assertEvaluatesTo("1>0?10:20", 10.0f);
        assertEvaluatesTo("0>1?10:20", 20.0f);
    }

    // === Variables ===

    @Test
    @DisplayName("Bedrock §变量赋值与读取: variable.X 和 temp.X")
    void variableAssignmentAndRead() {
        assertEvaluatesTo("{ variable.test = 5; return variable.test; }", 5.0f);
        assertEvaluatesTo("{ temp.x = 3; temp.y = 4; return temp.x + temp.y; }", 7.0f);
    }

    @Test
    @DisplayName("变量重赋值覆盖旧值")
    void variableReassignment() {
        assertEvaluatesTo("{ v.a = 1; v.a = 2; return v.a; }", 2.0f);
    }

    // === Built-in math functions ===

    @Test
    @DisplayName("Bedrock §math.abs(): 绝对值")
    void mathAbs() {
        assertEvaluatesTo("math.abs(-5)", 5.0f);
        assertEvaluatesTo("math.abs(3)", 3.0f);
    }

    @Test
    @DisplayName("Bedrock §math.clamp(): 夹值")
    void mathClamp() {
        assertEvaluatesTo("math.clamp(5, 0, 10)", 5.0f);
        assertEvaluatesTo("math.clamp(-1, 0, 10)", 0.0f);
        assertEvaluatesTo("math.clamp(15, 0, 10)", 10.0f);
    }

    @Test
    @DisplayName("Bedrock §math.min() / math.max()")
    void mathMinMax() {
        assertEvaluatesTo("math.min(3, 7)", 3.0f);
        assertEvaluatesTo("math.max(3, 7)", 7.0f);
    }

    @Test
    @DisplayName("Bedrock §math.lerp(): 线性插值")
    void mathLerp() {
        assertEvaluatesTo("math.lerp(0, 10, 0.5)", 5.0f);
        assertEvaluatesTo("math.lerp(0, 10, 0)", 0.0f);
        assertEvaluatesTo("math.lerp(0, 10, 1)", 10.0f);
    }

    // === Complex patterns ===

    @Test
    @DisplayName("Bedrock 动画模式: math.sin(90°) ≈ 1.0")
    void sinPattern() {
        assertEvaluatesTo("math.sin(0)", 0.0f);
        assertEvaluatesTo("math.sin(90)", 1.0f, "sin(90°) ≈ 1.0");
    }

    @Test
    @DisplayName("Bedrock 粒子模式: variable 赋值链")
    void variableChainPattern() {
        assertEvaluatesTo("{ v.a = 3; v.b = v.a * 2; return v.b; }", 6.0f);
    }

    @Test
    @DisplayName("条件表达式: condition ? A : B 嵌套")
    void nestedConditional() {
        assertEvaluatesTo("{ v.x = 3; return v.x > 2 ? (v.x > 4 ? 100 : 50) : 0; }", 50.0f);
    }

    // === this ===

    @Test
    @DisplayName("this 表达式 → 返回 0.0")
    void thisExpressionReturnsZero() {
        assertEvaluatesTo("this", 0.0f);
        assertEvaluatesTo("this + 5", 5.0f);
    }

    // === return ===

    @Test
    @DisplayName("return 语句终止求值并返回指定值")
    void returnStatement() {
        assertEvaluatesTo("{ return 42; }", 42.0f);
        assertEvaluatesTo("{ t.x = 1; return t.x + 2; return 99; }", 3.0f,
                "第一个 return 后的代码不执行");
    }

    // === String ===

    @Test
    @DisplayName("字符串字面量编译求值不抛异常")
    void stringLiteralCompiles() {
        CompiledMolangExpression expr = compiler.compile("'hello'", CompileContext.defaults());
        assertNotNull(expr);
        MolangObject result = expr.evaluate(new MolangScope());
        assertNotNull(result);
    }

    // === Edge cases ===

    @Test
    @DisplayName("空 block {} → 编译成功")
    void emptyBlock() {
        CompiledMolangExpression expr = compiler.compile("{}", CompileContext.defaults());
        assertNotNull(expr);
        assertNotNull(expr.evaluate(new MolangScope()));
    }

    @Test
    @DisplayName("深层运算链: 20 个 +1 → 编译不超时")
    void deepExpressionChain() {
        assertEvaluatesTo("1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1", 20.0f);
    }

    @Test
    @DisplayName("嵌套 block: { { return 3; } } → 3.0")
    void nestedBlock() {
        assertEvaluatesTo("{ return { return 3; }; }", 3.0f);
    }

    @Test
    @DisplayName("编译缓存: 相同表达式重复编译返回同等结果")
    void compileCacheConsistency() {
        float r1 = compiler.compile("42", CompileContext.defaults()).evaluate(new MolangScope()).asFloat();
        float r2 = compiler.compile("42", CompileContext.defaults()).evaluate(new MolangScope()).asFloat();
        assertEquals(r1, r2, 0.0001f);
    }

    // === Helpers ===

    private void assertEvaluatesTo(String expression, float expected) {
        assertEvaluatesTo(expression, expected, null);
    }

    private void assertEvaluatesTo(String expression, float expected, String message) {
        CompiledMolangExpression compiled = compiler.compile(expression, CompileContext.defaults());
        assertNotNull(compiled, "编译不应返回 null: " + expression);
        MolangObject value = compiled.evaluate(new MolangScope());
        assertNotNull(value, "求值不应返回 null: " + expression);
        float actual = value.asFloat();
        String msg = message != null ? message : expression + " → " + expected;
        assertEquals(expected, actual, 0.01f, msg);
    }
}
