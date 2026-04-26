package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedParserBackedAstMolangParserFrontendTest {
    private static final String SIMPLE_EXPRESSION_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/simple-expression.molangcase";
    private static final String ASSIGN_RETURN_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/assign-return.molangcase";
    private static final String LOOP_COUNTER_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/loop-counter.molangcase";
    private static final String NULL_COALESCE_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/null-coalesce.molangcase";
    private static final String TERNARY_ARRAY_INDEX_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/ternary-array-index.molangcase";

    @Test
    void buildsAstForSimpleExpressionCaseWithStableSpans() throws IOException {
        String source = loadCaseSource(SIMPLE_EXPRESSION_CASE);

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);

        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();
        assertEquals(0, ast.span().startIndex());
        assertEquals(source.length() - 1, ast.span().stopIndexInclusive());

        MolangAst.CallExpr functionCall = assertInstanceOf(MolangAst.CallExpr.class, ast.root());
        MolangAst.MemberAccessExpr callee = assertInstanceOf(MolangAst.MemberAccessExpr.class, functionCall.callee());
        assertEquals("sin", callee.memberName());
        assertInstanceOf(MolangAst.IdentifierExpr.class, callee.owner());
        assertEquals(1, functionCall.arguments().size());

        MolangAst.BinaryExpr multiply = assertInstanceOf(MolangAst.BinaryExpr.class, functionCall.arguments().get(0));
        assertEquals("*", multiply.operator());
        assertInstanceOf(MolangAst.MemberAccessExpr.class, multiply.left());
        MolangAst.NumberLiteralExpr number = assertInstanceOf(MolangAst.NumberLiteralExpr.class, multiply.right());
        assertEquals("1.23", number.rawText());
        assertTrue(number.span().startIndex() >= 0);
        assertTrue(number.span().stopIndexInclusive() >= number.span().startIndex());
    }

    @Test
    void buildsAstForAssignReturnCaseWithBaseExprShape() throws IOException {
        String source = loadCaseSource(ASSIGN_RETURN_CASE);

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);

        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();
        MolangAst.BlockExpr baseExprSet = assertInstanceOf(MolangAst.BlockExpr.class, ast.root());
        assertEquals(3, baseExprSet.statements().size());

        MolangAst.ExprStmt firstStatement = assertInstanceOf(MolangAst.ExprStmt.class, baseExprSet.statements().get(0));
        MolangAst.AssignmentExpr firstAssignment = assertInstanceOf(MolangAst.AssignmentExpr.class, firstStatement.expression());
        MolangAst.MemberAccessExpr firstTarget = assertInstanceOf(MolangAst.MemberAccessExpr.class, firstAssignment.target());
        assertEquals("is_blinking", firstTarget.memberName());

        MolangAst.ExprStmt secondStatement = assertInstanceOf(MolangAst.ExprStmt.class, baseExprSet.statements().get(1));
        MolangAst.AssignmentExpr secondAssignment = assertInstanceOf(MolangAst.AssignmentExpr.class, secondStatement.expression());
        MolangAst.MemberAccessExpr secondTarget = assertInstanceOf(MolangAst.MemberAccessExpr.class, secondAssignment.target());
        assertEquals("return_from_blink", secondTarget.memberName());

        MolangAst.ReturnStmt returnStmt = assertInstanceOf(MolangAst.ReturnStmt.class, baseExprSet.statements().get(2));
        MolangAst.BinaryExpr trailingAnd = assertInstanceOf(MolangAst.BinaryExpr.class, returnStmt.expression());
        assertEquals("&&", trailingAnd.operator());

        MolangAst.GroupingExpr groupedRight = assertInstanceOf(MolangAst.GroupingExpr.class, trailingAnd.right());
        MolangAst.BinaryExpr comparison = assertInstanceOf(MolangAst.BinaryExpr.class, groupedRight.expression());
        assertEquals(">", comparison.operator());

        MolangAst.GroupingExpr groupedNullCoalesce = assertInstanceOf(MolangAst.GroupingExpr.class, comparison.right());
        MolangAst.NullCoalesceExpr nullCoalesce = assertInstanceOf(MolangAst.NullCoalesceExpr.class, groupedNullCoalesce.expression());
        assertTrue(nullCoalesce.span().startIndex() >= 0);
        assertTrue(nullCoalesce.span().stopIndexInclusive() >= nullCoalesce.span().startIndex());
    }

    @Test
    void buildsAstForLoopCaseWithDedicatedLoopExprShape() throws IOException {
        String source = loadCaseSource(LOOP_COUNTER_CASE);

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);

        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();
        MolangAst.LoopExpr loopExpr = assertInstanceOf(MolangAst.LoopExpr.class, ast.root());
        assertEquals("3", loopExpr.iterationCountRawText());

        MolangAst.BlockExpr body = loopExpr.body();
        assertEquals(1, body.statements().size());

        MolangAst.ExprStmt bodyStatement = assertInstanceOf(MolangAst.ExprStmt.class, body.statements().get(0));
        MolangAst.AssignmentExpr assignmentExpr = assertInstanceOf(MolangAst.AssignmentExpr.class, bodyStatement.expression());
        MolangAst.MemberAccessExpr target = assertInstanceOf(MolangAst.MemberAccessExpr.class, assignmentExpr.target());
        assertEquals("counter", target.memberName());

        MolangAst.BinaryExpr incrementValue = assertInstanceOf(MolangAst.BinaryExpr.class, assignmentExpr.value());
        assertEquals("+", incrementValue.operator());
    }

    @Test
    void buildsAstForNullCoalesceCaseWithNestedTernaryAndGroupingSpans() throws IOException {
        String source = loadCaseSource(NULL_COALESCE_CASE);

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);

        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();
        MolangAst.BlockExpr blockExpr = assertInstanceOf(MolangAst.BlockExpr.class, ast.root());
        assertSpanText(source, blockExpr.span(), source);
        assertEquals(1, blockExpr.statements().size());

        MolangAst.ExprStmt exprStmt = assertInstanceOf(MolangAst.ExprStmt.class, blockExpr.statements().get(0));
        MolangAst.AssignmentExpr assignmentExpr = assertInstanceOf(MolangAst.AssignmentExpr.class, exprStmt.expression());
        assertSpanText(source, assignmentExpr.span(), "variable.rolled_up_time = variable.is_rolled_up ? ((variable.rolled_up_time ?? 0.0) + query.delta_time) : 0.0");

        MolangAst.MemberAccessExpr target = assertInstanceOf(MolangAst.MemberAccessExpr.class, assignmentExpr.target());
        assertSpanText(source, target.span(), "variable.rolled_up_time");

        MolangAst.TernaryConditionalExpr ternary = assertInstanceOf(MolangAst.TernaryConditionalExpr.class, assignmentExpr.value());
        assertSpanText(source, ternary.span(), "variable.is_rolled_up ? ((variable.rolled_up_time ?? 0.0) + query.delta_time) : 0.0");

        MolangAst.MemberAccessExpr condition = assertInstanceOf(MolangAst.MemberAccessExpr.class, ternary.condition());
        assertSpanText(source, condition.span(), "variable.is_rolled_up");

        MolangAst.GroupingExpr whenTrue = assertInstanceOf(MolangAst.GroupingExpr.class, ternary.whenTrue());
        assertSpanText(source, whenTrue.span(), "((variable.rolled_up_time ?? 0.0) + query.delta_time)");

        MolangAst.BinaryExpr addition = assertInstanceOf(MolangAst.BinaryExpr.class, whenTrue.expression());
        assertEquals("+", addition.operator());

        MolangAst.GroupingExpr groupedNullCoalesce = assertInstanceOf(MolangAst.GroupingExpr.class, addition.left());
        assertSpanText(source, groupedNullCoalesce.span(), "(variable.rolled_up_time ?? 0.0)");

        MolangAst.NullCoalesceExpr nullCoalesce = assertInstanceOf(MolangAst.NullCoalesceExpr.class, groupedNullCoalesce.expression());
        assertSpanText(source, nullCoalesce.span(), "variable.rolled_up_time ?? 0.0");

        MolangAst.MemberAccessExpr fallbackTarget = assertInstanceOf(MolangAst.MemberAccessExpr.class, nullCoalesce.left());
        assertSpanText(source, fallbackTarget.span(), "variable.rolled_up_time");

        MolangAst.NumberLiteralExpr fallback = assertInstanceOf(MolangAst.NumberLiteralExpr.class, nullCoalesce.right());
        assertEquals("0.0", fallback.rawText());

        MolangAst.MemberAccessExpr deltaTime = assertInstanceOf(MolangAst.MemberAccessExpr.class, addition.right());
        assertSpanText(source, deltaTime.span(), "query.delta_time");

        MolangAst.NumberLiteralExpr whenFalse = assertInstanceOf(MolangAst.NumberLiteralExpr.class, ternary.whenFalse());
        assertEquals("0.0", whenFalse.rawText());
    }

    @Test
    void buildsAstForTernaryArrayIndexCaseWithIndexedFalseBranchSpans() throws IOException {
        String source = loadCaseSource(TERNARY_ARRAY_INDEX_CASE);

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);

        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();
        MolangAst.TernaryConditionalExpr ternary = assertInstanceOf(MolangAst.TernaryConditionalExpr.class, ast.root());
        assertSpanText(source, ternary.span(), source);

        MolangAst.BinaryExpr condition = assertInstanceOf(MolangAst.BinaryExpr.class, ternary.condition());
        assertEquals("==", condition.operator());
        assertSpanText(source, condition.span(), "query.get_name == 'Toast'");

        MolangAst.MemberAccessExpr queryName = assertInstanceOf(MolangAst.MemberAccessExpr.class, condition.left());
        assertSpanText(source, queryName.span(), "query.get_name");

        MolangAst.StringLiteralExpr toastLiteral = assertInstanceOf(MolangAst.StringLiteralExpr.class, condition.right());
        assertEquals("'Toast'", toastLiteral.rawText());

        MolangAst.MemberAccessExpr whenTrue = assertInstanceOf(MolangAst.MemberAccessExpr.class, ternary.whenTrue());
        assertSpanText(source, whenTrue.span(), "Texture.toast");

        MolangAst.IndexExpr whenFalse = assertInstanceOf(MolangAst.IndexExpr.class, ternary.whenFalse());
        assertSpanText(source, whenFalse.span(), "Array.skins[query.variant]");

        MolangAst.MemberAccessExpr owner = assertInstanceOf(MolangAst.MemberAccessExpr.class, whenFalse.owner());
        assertSpanText(source, owner.span(), "Array.skins");

        MolangAst.MemberAccessExpr index = assertInstanceOf(MolangAst.MemberAccessExpr.class, whenFalse.index());
        assertSpanText(source, index.span(), "query.variant");
    }

    private String loadCaseSource(String resourcePath) throws IOException {
        String fileText;
        try (InputStream stream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resourcePath), resourcePath)) {
            fileText = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int firstMarker = fileText.indexOf("---");
        int secondMarker = fileText.indexOf("---", firstMarker + 3);
        if (firstMarker < 0 || secondMarker < 0) {
            throw new IOException("Malformed corpus case file: " + resourcePath);
        }

        return fileText.substring(secondMarker + 3).trim();
    }

    private void assertSpanText(String source, io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan span, String expected) {
        String actual = source.substring(span.startIndex(), span.stopIndexInclusive() + 1);
        assertEquals(expected, actual);
    }
}
