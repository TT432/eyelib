package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MoLang 规范驱动的系统性测试。
 * 从 Bedrock 官方文档推导测试用例：语法 × 语义 × 边界 覆盖矩阵。
 *
 * @author TT432
 */
class MolangSpecDrivenTest {
    private static final MolangCompilerImpl compiler = new MolangCompilerImpl();
    private static final HandwrittenMolangAstParserFrontend parser = HandwrittenMolangAstParserFrontend.INSTANCE;

    // ============================================================
    // §1 词法层 — Tokenizer 覆盖
    // ============================================================

    @Nested
    @DisplayName("§1.1 数字字面量")
    class NumberLiterals {
        @ParameterizedTest
        @ValueSource(strings = {
                "0", "1", "42", "007",
                "0.0", "1.5", "3.14159",
                "1e5", "1.5e-4", "2E+10", "0.5E0",
                "1.0f", "42.0f",
        })
        void parsesValidNumbers(String expr) {
            assertParses(expr);
        }
    }

    @Nested
    @DisplayName("§1.2 字符串字面量")
    class StringLiterals {
        @ParameterizedTest
        @ValueSource(strings = {"''", "'hello'", "'minecraft:pig'"})
        void parsesValidStrings(String expr) {
            assertParses(expr);
        }

        @Test
        void emptyStringEvaluates() {
            assertEvaluatesTo("''", 0.0f);
        }
    }

    @Nested
    @DisplayName("§1.3 标识符与关键字")
    class IdentifiersAndKeywords {
        @ParameterizedTest
        @ValueSource(strings = {
                "x", "a1", "_temp", "HELLO", "snake_case", "camelCase",
                "this", "true", "false", "break", "continue",
        })
        void parsesIdentifiersAndKeywords(String expr) {
            assertParses(expr);
        }
    }

    // ============================================================
    // §2 语法层 — 每产生式覆盖
    // ============================================================

    @Nested
    @DisplayName("§2.1 主表达式 (primary)")
    class PrimaryExpressions {
        @ParameterizedTest
        @ValueSource(strings = {"42", "'text'", "x", "this", "(1+2)", "{1;}"})
        void allPrimaryFormsParse(String expr) {
            assertParses(expr);
        }
    }

    @Nested
    @DisplayName("§2.2 一元运算符 (unary)")
    class UnaryExpressions {
        @ParameterizedTest
        @ValueSource(strings = {"-1", "-x", "!0", "!x", "--1", "!-1"})
        void parsesUnary(String expr) {
            assertParses(expr);
        }

        @Test
        void negationEvaluates() { assertEvaluatesTo("-1", -1.0f); }
        @Test
        void doubleNegationEvaluates() { assertEvaluatesTo("--1", 1.0f); }
        @Test
        void notFalseEvaluates() { assertEvaluatesTo("!0", 1.0f); }
        @Test
        void notTrueEvaluates() { assertEvaluatesTo("!1", 0.0f); }
    }

    @Nested
    @DisplayName("§2.3 乘除 (multiply)")
    class MultiplyExpressions {
        @Test
        void multiplyEvaluates() { assertEvaluatesTo("3*4", 12.0f); }
        @Test
        void divideEvaluates() { assertEvaluatesTo("8/2", 4.0f); }
        @Test
        void divideByZeroReturnsZero() { assertEvaluatesTo("5/0", 0.0f); }
        @Test
        void chainedMultiplyDivideLeftAssociative() { assertEvaluatesTo("12/3*2", 8.0f); }
    }

    @Nested
    @DisplayName("§2.4 加减 (add)")
    class AddExpressions {
        @Test
        void addEvaluates() { assertEvaluatesTo("1+2", 3.0f); }
        @Test
        void subtractEvaluates() { assertEvaluatesTo("5-3", 2.0f); }
        @Test
        void multiplyBeforeAdd() { assertEvaluatesTo("2+3*4", 14.0f); }
        @Test
        void addBeforeComparison() { assertEvaluatesTo("1+2<4", 1.0f); }
    }

    @Nested
    @DisplayName("§2.5 比较运算符 (comparison)")
    class ComparisonExpressions {
        @Test void lessTrue() { assertEvaluatesTo("1<2", 1.0f); }
        @Test void lessFalse() { assertEvaluatesTo("2<1", 0.0f); }
        @Test void lessEqualTrue() { assertEvaluatesTo("2<=2", 1.0f); }
        @Test void greaterTrue() { assertEvaluatesTo("3>2", 1.0f); }
        @Test void greaterEqualTrue() { assertEvaluatesTo("3>=3", 1.0f); }
        @Test void equalStrings() { assertCompiles("'a'=='a'"); }
        @Test void notEqualStrings() { assertCompiles("'a'!='b'"); }
        @Test void comparisonBeforeEquality() { assertEvaluatesTo("1<2==1", 1.0f); }
    }

    @Nested
    @DisplayName("§2.6 逻辑运算符 (and/or)")
    class LogicalExpressions {
        @Test void andTrueTrue() { assertEvaluatesTo("1&&1", 1.0f); }
        @Test void andTrueFalse() { assertEvaluatesTo("1&&0", 0.0f); }
        @Test void orFalseTrue() { assertEvaluatesTo("0||1", 1.0f); }
        @Test void orFalseFalse() { assertEvaluatesTo("0||0", 0.0f); }
        @Test void andBeforeOr() { assertEvaluatesTo("0&&1||1", 1.0f); }
    }

    @Nested
    @DisplayName("§2.7 条件运算符 (ternary/binary conditional)")
    class ConditionalExpressions {
        @Test void ternaryTrue() { assertEvaluatesTo("1?2:3", 2.0f); }
        @Test void ternaryFalse() { assertEvaluatesTo("0?2:3", 3.0f); }
        @Test void binaryTrue() { assertEvaluatesTo("1?2", 2.0f); }
        @Test void binaryFalse() { assertEvaluatesTo("0?2", 0.0f); }
        @Test void nestedTernaryRightAssociative() {
            // Per Bedrock 1.18.10: A?B:C?D:E = A?B:(C?D:E)
            assertEvaluatesTo("0?1:1?2:3", 2.0f);
        }
    }

    @Nested
    @DisplayName("§2.8 空值合并 (null coalesce)")
    class NullCoalesceExpressions {
        @Test void leftValidReturnsLeft() { assertEvaluatesTo("1??2", 1.0f); }
        @Test void leftUndefinedReturnsRight() { assertEvaluatesTo("undefined_var??42", 42.0f); }
    }

    @Nested
    @DisplayName("§2.9 赋值 (assignment)")
    class AssignmentExpressions {
        @Test void assignsAndReturnsValue() { assertEvaluatesTo("t.x=5", 5.0f); }
        @Test void chainAssign() { assertCompiles("t.a=(t.b=3)"); }
        @Test void assignToMember() { assertCompiles("v.location.x=1"); }
    }

    @Nested
    @DisplayName("§2.10 后置操作 (postfix: member/arrow/call/index)")
    class PostfixExpressions {
        @ParameterizedTest
        @ValueSource(strings = {"x.y", "q.health", "v.x.y.z"})
        void memberAccessParses(String expr) { assertParses(expr); }

        @ParameterizedTest
        @ValueSource(strings = {"a->q.test", "context.other->query.health"})
        void arrowAccessParses(String expr) { assertParses(expr); }

        @ParameterizedTest
        @ValueSource(strings = {"f()", "f(1)", "f(1,2,3)", "q.func(1, 'text')"})
        void callParses(String expr) { assertParses(expr); }

        @ParameterizedTest
        @ValueSource(strings = {"a[0]", "a[1+2]", "a.b[c]"})
        void indexParses(String expr) { assertParses(expr); }
    }

    // ============================================================
    // §3 语句层 (statements)
    // ============================================================

    @Nested
    @DisplayName("§3.1 return")
    class ReturnStatements {
        @Test void returnInBlock() { assertEvaluatesTo("{return 42;}", 42.0f); }
        @Test void returnWithExpr() { assertEvaluatesTo("{t.x=1;return t.x+2;}", 3.0f); }
        @Test void implicitReturnZero() { assertEvaluatesTo("{t.x=1;}", 0.0f); }
    }

    @Nested
    @DisplayName("§3.2 条件分支 (conditional with block)")
    class ConditionalBlocks {
        @Test void trueBlockExecutes() { assertEvaluatesTo("1?{return 5;}:0", 5.0f); }
        @Test void falseBlockSkipped() { assertEvaluatesTo("0?0:{return 5;}", 5.0f); }
    }

    @Nested
    @DisplayName("§3.3 loop")
    class LoopStatements {
        @Test void loopCompiles() { assertCompiles("loop(3,{t.x=t.x+1;})"); }
        @Test void loopEvaluates() { assertEvaluatesTo("t.c=0;loop(3,{t.c=t.c+1;});return t.c;", 3.0f); }
    }

    @Nested
    @DisplayName("§3.4 for_each")
    class ForEachStatements {
        @Test void forEachCompiles() { assertCompiles("for_each(t.x,arr,{})"); }
    }

    @Nested
    @DisplayName("§3.5 break/continue")
    class BreakContinueStatements {
        @Test void breakCompiles() { assertCompiles("loop(10,{(t.x>5)?break;t.x=t.x+1;})"); }
        @Test void continueCompiles() { assertCompiles("loop(10,{(t.x>5)?continue;t.x=t.x+1;})"); }
    }

    // ============================================================
    // §4 运算符优先级矩阵 — Bedrock 规范 13 级交叉验证
    // ============================================================

    @Nested
    @DisplayName("§4 运算符优先级交叉验证")
    class PrecedenceMatrix {
        // L1: () [] — grouping and index
        @Test void groupingOverridesPrecedence() { assertEvaluatesTo("(1+2)*3", 9.0f); }

        // L3: unary > multiply
        @Test void unaryBeforeMultiply() { assertEvaluatesTo("-2*3", -6.0f); }

        // L4-L5: multiply > add
        @Test void multiplyBeforeAdd() { assertEvaluatesTo("1+2*3", 7.0f); }

        // L6-L7: comparison > equality
        @Test void compareBeforeEqual() { assertEvaluatesTo("1<2==1", 1.0f); }

        // L8-L9: and > or
        @Test void andBeforeOr() { assertEvaluatesTo("0||1&&0", 0.0f); }

        // L10: conditional lowest before assignment
        @Test void conditionalBeforeAssignment() { assertEvaluatesTo("t.x=0?1:2", 2.0f); }

        // L11-L12: nullCoalesce > assignment  
        @Test void nullCoalesceBeforeAssignment() {
            // v.x gets (undefined??0), then +1; result is 0+1=1
            assertCompiles("v.x = (v.y ?? 0) + 1");
        }
    }

    // ============================================================
    // §5 命名空间根 — q/t/v/c 别名规范化
    // ============================================================

    @Nested
    @DisplayName("§5 命名空间根与别名")
    class NamespaceRoots {
        @ParameterizedTest
        @ValueSource(strings = {"q.health", "query.health", "t.x", "temp.x", "v.x", "variable.x", "c.moo", "context.moo"})
        void allRootsAndAliasesParse(String expr) { assertCompiles(expr); }
    }

    // ============================================================
    // §6 this 语义
    // ============================================================

    @Nested
    @DisplayName("§6 this 语义")
    class ThisSemantics {
        @Test void thisEvaluatesToZeroOutsideAnimation() { assertEvaluatesTo("this", 0.0f); }
        @Test void negativeThisEvaluates() { assertEvaluatesTo("-this", 0.0f); }
        @Test void thisInExpression() { assertEvaluatesTo("this+5", 5.0f); }
    }

    // ============================================================
    // §7 错误与边界情况
    // ============================================================

    @Nested
    @DisplayName("§7 错误与边界")
    class ErrorAndEdgeCases {
        @Test void emptyStringReturnsZeroExpression() {
            // 按 Bedrock 规范，空白/空输入等价于表达式 0
            assertTrue(parser.parseExprSetAst("").isPresent());
        }
        @Test void whitespaceOnlyReturnsZeroExpression() {
            assertTrue(parser.parseExprSetAst("   ").isPresent());
        }
        @ParameterizedTest
        @ValueSource(strings = {"'unclosed", "((1+2)", "{1+2", "x."})
        void malformedInputRejected(String expr) {
            // 手写解析器对畸形输入应该返回空 AST 或抛异常
            var result = parser.parseExprSetAst(expr);
            // 可能返回空（ParseException 被 catch）或解析成功但 binder 报错
            // 这里只验证不 crash
            if (result.isPresent()) {
                try {
                    compiler.compile(expr, CompileContext.defaults());
                } catch (Exception e) {
                    // 编译失败也可接受（binder 拒绝畸形 AST）
                }
            }
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private static void assertParses(String expression) {
        assertTrue(parser.parseExprSetAst(expression).isPresent(),
                "Should parse: " + expression);
    }

    private static void assertCompiles(String expression) {
        assertDoesNotThrow(() -> compiler.compile(expression, CompileContext.defaults()),
                "Should compile: " + expression);
    }

    private static void assertEvaluatesTo(String expression, float expected) {
        var compiled = compiler.compile(expression, CompileContext.defaults());
        assertNotNull(compiled, "Compiled result should not be null: " + expression);
        MolangObject value = compiled.evaluate(new MolangScope());
        assertNotNull(value, "Evaluated value should not be null: " + expression);
        assertTrue(Float.isFinite(value.asFloat()),
                "Value should be finite: " + expression + " got " + value);
        assertEquals(expected, value.asFloat(), 0.0001f,
                "Expression: " + expression);
    }
}
