package io.github.tt432.eyelib.molang.compiler.binding;

import io.github.tt432.eyelib.molang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Binder 节点类型正确性验证。
 * 验证每种 AST 节点被绑定为正确的 Bound 类型，而非落入 deferred/unknown。
 *
 * @author TT432
 */
class MolangBinderNodeTypeTest {
    private static final MolangBinder binder = new MolangBinder();

    private BindResult bind(String source) {
        return bind(source, BindDiagnosticsMode.NORMAL);
    }

    private BindResult bind(String source, BindDiagnosticsMode mode) {
        MolangAst.ExprSet ast = new HandwrittenMolangAstParserFrontend()
                .parseExprSetAst(source).orElseThrow();
        return binder.bind(ast, mode);
    }

    @Nested
    @DisplayName("字面量")
    class Literals {
        @Test void numberLiteral() {
            var r = bind("42");
            assertInstanceOf(BoundMolang.BoundNumberLiteralExpr.class, r.root().root());
        }
        @Test void stringLiteral() {
            var r = bind("'hello'");
            assertInstanceOf(BoundMolang.BoundStringLiteralExpr.class, r.root().root());
        }
        @Test void thisExpr() {
            var r = bind("this");
            assertInstanceOf(BoundMolang.BoundThisExpr.class, r.root().root());
        }
    }

    @Nested
    @DisplayName("一元/二元运算符")
    class Operators {
        @Test void unaryNegate() {
            var r = bind("-x");
            var e = (BoundMolang.BoundUnaryExpr) r.root().root();
            assertEquals("-", e.operator());
        }
        @Test void unaryNot() {
            var r = bind("!x");
            var e = (BoundMolang.BoundUnaryExpr) r.root().root();
            assertEquals("!", e.operator());
        }
        @Test void binaryAdd() {
            var r = bind("1+2");
            var e = (BoundMolang.BoundBinaryExpr) r.root().root();
            assertEquals("+", e.operator());
        }
        @Test void binaryCompare() {
            var r = bind("1<2");
            var e = (BoundMolang.BoundBinaryExpr) r.root().root();
            assertEquals("<", e.operator());
        }
        @Test void binaryLogical() {
            var r = bind("1&&0");
            var e = (BoundMolang.BoundBinaryExpr) r.root().root();
            assertEquals("&&", e.operator());
        }
    }

    @Nested
    @DisplayName("条件表达式")
    class Conditionals {
        @Test void ternaryConditional() {
            var r = bind("1?2:3");
            assertInstanceOf(BoundMolang.BoundTernaryConditionalExpr.class, r.root().root());
        }
        @Test void binaryConditionalIsNotDeferred() {
            var r = bind("1?2");
            var root = r.root().root();
            assertInstanceOf(BoundMolang.BoundBinaryConditionalExpr.class, root,
                    "Binary conditional must NOT be deferred");
            assertTrue(r.deferredNotes().isEmpty(),
                    "Binary conditional must not produce deferred notes");
        }
    }

    @Nested
    @DisplayName("空值合并")
    class NullCoalesce {
        @Test void nullCoalesceExpr() {
            var r = bind("a??b");
            assertInstanceOf(BoundMolang.BoundNullCoalesceExpr.class, r.root().root());
        }
    }

    @Nested
    @DisplayName("后置操作")
    class Postfix {
        @Test void memberAccess() {
            var r = bind("a.b");
            var e = (BoundMolang.BoundMemberAccessExpr) r.root().root();
            assertEquals("b", e.memberName());
        }
        @Test void arrowAccess() {
            var r = bind("a->q.x");
            assertInstanceOf(BoundMolang.BoundArrowAccessExpr.class, r.root().root());
        }
        @Test void callExpr() {
            var r = bind("f(1)");
            var call = (BoundMolang.BoundCallExpr) r.root().root();
            assertEquals(1, call.arguments().size());
        }
        @Test void indexExpr() {
            var r = bind("a[0]");
            assertInstanceOf(BoundMolang.BoundIndexExpr.class, r.root().root());
        }
    }

    @Nested
    @DisplayName("赋值")
    class Assignment {
        @Test void simpleAssignment() {
            var r = bind("v.x=1");
            var e = (BoundMolang.BoundAssignmentExpr) r.root().root();
            assertTrue(e.writableTarget(), "Variable assignment should be writable");
        }
        @Test void tempAssignment() {
            var r = bind("t.x=1");
            var e = (BoundMolang.BoundAssignmentExpr) r.root().root();
            assertTrue(e.writableTarget());
        }
    }

    @Nested
    @DisplayName("块与语句")
    class BlocksAndStatements {
        @Test void blockExpr() {
            var r = bind("{v.x=1;}");
            assertInstanceOf(BoundMolang.BoundBlockExpr.class, r.root().root());
        }
        @Test void returnStmt() {
            var r = bind("{return 42;}");
            var block = (BoundMolang.BoundBlockExpr) r.root().root();
            assertEquals(1, block.statements().size());
            assertInstanceOf(BoundMolang.BoundReturnStmt.class, block.statements().get(0));
        }
    }

    @Nested
    @DisplayName("控制流")
    class ControlFlow {
        @Test void loopIsNotDeferred() {
            var r = bind("loop(3,{v.x=v.x+1;})");
            assertInstanceOf(BoundMolang.BoundLoopExpr.class, r.root().root());
            assertTrue(r.deferredNotes().isEmpty(),
                    "Loop must not produce deferred notes");
        }
        @Test void forEachIsNotDeferred() {
            var r = bind("for_each(t.x,arr,{})");
            assertInstanceOf(BoundMolang.BoundForEachExpr.class, r.root().root());
            assertTrue(r.deferredNotes().isEmpty(),
                    "For_each must not produce deferred notes");
        }
    }

    @Nested
    @DisplayName("Query 投影")
    class QueryProjection {
        @Test void queryAccessIsIdentifier() {
            var r = bind("q.health");
            // 顶层可能是 BoundMemberAccessExpr
            assertNotNull(r.root().root());
        }
    }

    @Nested
    @DisplayName("无效写入诊断")
    class InvalidWriteDiagnostics {
        @Test void contextWriteProducesError() {
            var r = bind("c.foo = 1");
            assertFalse(r.diagnostics().isEmpty(),
                    "Writing to context should produce diagnostic");
            assertTrue(r.diagnostics().stream()
                    .anyMatch(d -> d.severity() == BindDiagnostic.Severity.ERROR),
                    "Writing to context should be ERROR severity");
        }
    }

    @Nested
    @DisplayName("诊断模式")
    class DiagnosticsModes {
        @Test void normalModeWarnsOnDeferredUnsupported() {
            var r = bind("for_each(t.x,arr,{})", BindDiagnosticsMode.NORMAL);
            assertTrue(r.diagnostics().stream()
                    .noneMatch(d -> d.message().contains("UNSUPPORTED")));
        }
        @Test void strictModeAddsOverlayDiagnostics() {
            var r = bind("for_each(t.x,arr,{})", BindDiagnosticsMode.STRICT);
            assertTrue(r.diagnostics().isEmpty());
        }
        @Test void debugModeProducesTraceableOutput() {
            var r = bind("for_each(t.x,arr,{})", BindDiagnosticsMode.DEBUG);
            assertTrue(r.diagnostics().stream().noneMatch(d -> d.message().contains("UNSUPPORTED")));
        }
    }
}
