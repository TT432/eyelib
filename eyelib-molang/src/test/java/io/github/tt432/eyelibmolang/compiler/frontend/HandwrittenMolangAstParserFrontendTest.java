package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class HandwrittenMolangAstParserFrontendTest {
    private static final String SIMPLE_EXPRESSION_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/simple-expression.molangcase";
    private static final String ASSIGN_RETURN_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/assign-return.molangcase";
    private static final String LOOP_COUNTER_CASE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter/loop-counter.molangcase";

    @Test
    void parsesSimpleExpressionCaseIntoCallWithMultiplyArgument() throws IOException {
        String source = loadCaseSource(SIMPLE_EXPRESSION_CASE);

        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();

        MolangAst.CallExpr functionCall = assertInstanceOf(MolangAst.CallExpr.class, ast.root());
        MolangAst.MemberAccessExpr callee = assertInstanceOf(MolangAst.MemberAccessExpr.class, functionCall.callee());
        assertEquals("sin", callee.memberName());
        assertEquals(1, functionCall.arguments().size());

        MolangAst.BinaryExpr multiply = assertInstanceOf(MolangAst.BinaryExpr.class, functionCall.arguments().get(0));
        assertEquals("*", multiply.operator());
        assertInstanceOf(MolangAst.MemberAccessExpr.class, multiply.left());

        MolangAst.NumberLiteralExpr number = assertInstanceOf(MolangAst.NumberLiteralExpr.class, multiply.right());
        assertEquals("1.23", number.rawText());
    }

    @Test
    void parsesAssignReturnCaseIntoBlockWithReturnAndGroupedNullCoalesce() throws IOException {
        String source = loadCaseSource(ASSIGN_RETURN_CASE);

        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();

        MolangAst.BlockExpr block = assertInstanceOf(MolangAst.BlockExpr.class, ast.root());
        assertEquals(3, block.statements().size());

        MolangAst.ExprStmt firstStatement = assertInstanceOf(MolangAst.ExprStmt.class, block.statements().get(0));
        MolangAst.AssignmentExpr firstAssignment = assertInstanceOf(MolangAst.AssignmentExpr.class, firstStatement.expression());
        MolangAst.MemberAccessExpr firstTarget = assertInstanceOf(MolangAst.MemberAccessExpr.class, firstAssignment.target());
        assertEquals("is_blinking", firstTarget.memberName());

        MolangAst.ExprStmt secondStatement = assertInstanceOf(MolangAst.ExprStmt.class, block.statements().get(1));
        MolangAst.AssignmentExpr secondAssignment = assertInstanceOf(MolangAst.AssignmentExpr.class, secondStatement.expression());
        MolangAst.MemberAccessExpr secondTarget = assertInstanceOf(MolangAst.MemberAccessExpr.class, secondAssignment.target());
        assertEquals("return_from_blink", secondTarget.memberName());

        MolangAst.ReturnStmt returnStmt = assertInstanceOf(MolangAst.ReturnStmt.class, block.statements().get(2));
        MolangAst.BinaryExpr andExpr = assertInstanceOf(MolangAst.BinaryExpr.class, returnStmt.expression());
        assertEquals("&&", andExpr.operator());

        MolangAst.GroupingExpr groupedComparison = assertInstanceOf(MolangAst.GroupingExpr.class, andExpr.right());
        MolangAst.BinaryExpr comparison = assertInstanceOf(MolangAst.BinaryExpr.class, groupedComparison.expression());
        assertEquals(">", comparison.operator());

        MolangAst.GroupingExpr groupedNullCoalesce = assertInstanceOf(MolangAst.GroupingExpr.class, comparison.right());
        MolangAst.NullCoalesceExpr nullCoalesce = assertInstanceOf(MolangAst.NullCoalesceExpr.class, groupedNullCoalesce.expression());
        assertInstanceOf(MolangAst.MemberAccessExpr.class, nullCoalesce.left());
        MolangAst.NumberLiteralExpr fallback = assertInstanceOf(MolangAst.NumberLiteralExpr.class, nullCoalesce.right());
        assertEquals("0.2", fallback.rawText());
    }

    @Test
    void parsesLoopCaseIntoLoopExprWithCounterAssignmentBody() throws IOException {
        String source = loadCaseSource(LOOP_COUNTER_CASE);

        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();

        MolangAst.LoopExpr loop = assertInstanceOf(MolangAst.LoopExpr.class, ast.root());
        MolangAst.NumberLiteralExpr count = assertInstanceOf(MolangAst.NumberLiteralExpr.class, loop.count());
        assertEquals("3", count.rawText());
        assertEquals(1, loop.body().statements().size());

        MolangAst.ExprStmt bodyStatement = assertInstanceOf(MolangAst.ExprStmt.class, loop.body().statements().get(0));
        MolangAst.AssignmentExpr assignment = assertInstanceOf(MolangAst.AssignmentExpr.class, bodyStatement.expression());
        MolangAst.MemberAccessExpr target = assertInstanceOf(MolangAst.MemberAccessExpr.class, assignment.target());
        assertEquals("counter", target.memberName());

        MolangAst.BinaryExpr increment = assertInstanceOf(MolangAst.BinaryExpr.class, assignment.value());
        assertEquals("+", increment.operator());
    }

    @Test
    void parsesForEachAsDedicatedControlForm() {
        String source = "for_each(t.pig, query.get_nearby_entities(4, 'minecraft:pig'), {v.x = v.x + 1;})";

        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();

        MolangAst.ForEachExpr forEach = assertInstanceOf(MolangAst.ForEachExpr.class, ast.root());
        MolangAst.MemberAccessExpr variable = assertInstanceOf(MolangAst.MemberAccessExpr.class, forEach.variable());
        assertEquals("pig", variable.memberName());
        MolangAst.CallExpr collection = assertInstanceOf(MolangAst.CallExpr.class, forEach.collection());
        assertEquals(2, collection.arguments().size());
        assertEquals(1, forEach.body().statements().size());
    }

    @Test
    void parsesBreakAndContinueAsControlFlowStatements() {
        String source = "loop(2, {break 1.0; continue t.x;})";

        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();

        MolangAst.LoopExpr loop = assertInstanceOf(MolangAst.LoopExpr.class, ast.root());
        assertEquals(2, loop.body().statements().size());
        MolangAst.BreakStmt breakStmt = assertInstanceOf(MolangAst.BreakStmt.class, loop.body().statements().get(0));
        MolangAst.ContinueStmt continueStmt = assertInstanceOf(MolangAst.ContinueStmt.class, loop.body().statements().get(1));
        assertInstanceOf(MolangAst.NumberLiteralExpr.class, breakStmt.valueExpr());
        assertInstanceOf(MolangAst.MemberAccessExpr.class, continueStmt.valueExpr());
    }

    @Test
    void parsesLogicalAndWithHigherPrecedenceThanNullCoalesce() {
        String source = "query.a && query.b ?? query.c";

        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();

        MolangAst.NullCoalesceExpr nullCoalesce = assertInstanceOf(MolangAst.NullCoalesceExpr.class, ast.root());
        MolangAst.BinaryExpr andExpr = assertInstanceOf(MolangAst.BinaryExpr.class, nullCoalesce.left());
        assertEquals("&&", andExpr.operator());
        assertInstanceOf(MolangAst.MemberAccessExpr.class, nullCoalesce.right());
    }

    @Test
    void parsesControlKeywordsCaseInsensitively() {
        String source = "LOOP(2, {BREAK; Continue;})";

        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();

        MolangAst.LoopExpr loop = assertInstanceOf(MolangAst.LoopExpr.class, ast.root());
        assertEquals(2, loop.body().statements().size());
        assertInstanceOf(MolangAst.BreakStmt.class, loop.body().statements().get(0));
        assertInstanceOf(MolangAst.ContinueStmt.class, loop.body().statements().get(1));
    }

    @Test
    void parsesBareIdentifierIntoIdentifierExpr() {
        var result = parse("variable_name");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.IdentifierExpr.class, exprSet.root());
        assertEquals("variable_name", ((MolangAst.IdentifierExpr) exprSet.root()).name());
    }

    @Test
    void parsesSingleQuotedStringIntoStringLiteralExpr() {
        var result = parse("'minecraft:pig'");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.StringLiteralExpr.class, exprSet.root());
        assertEquals("'minecraft:pig'", ((MolangAst.StringLiteralExpr) exprSet.root()).rawText());
    }

    @Test
    void parsesCallThenMemberAccessAsPostfixChain() {
        var result = parse("query.foo(1, 2).bar");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.MemberAccessExpr.class, exprSet.root());
        var memberAccess = (MolangAst.MemberAccessExpr) exprSet.root();
        assertEquals("bar", memberAccess.memberName());
        assertInstanceOf(MolangAst.CallExpr.class, memberAccess.owner());
    }

    @Test
    void parsesTwoTopLevelAssignmentsIntoBlockExpr() {
        var result = parse("a = 1; b = 2;");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.BlockExpr.class, exprSet.root());
        assertEquals(2, ((MolangAst.BlockExpr) exprSet.root()).statements().size());
    }

    @Test
    void parsesAssignmentRightAssociatively() {
        var result = parse("a = b = 1");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.AssignmentExpr.class, exprSet.root());
        var outer = (MolangAst.AssignmentExpr) exprSet.root();
        assertInstanceOf(MolangAst.IdentifierExpr.class, outer.target());
        assertInstanceOf(MolangAst.AssignmentExpr.class, outer.value());
    }

    @Test
    void parsesComparisonWithAddMultiplyPrecedence() {
        var result = parse("1 + 2 * 3 > 4");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.BinaryExpr.class, exprSet.root());
        var bin = (MolangAst.BinaryExpr) exprSet.root();
        assertEquals(">", bin.operator());
    }

    @Test
    void parsesNullCoalesceLeftAssociatively() {
        var result = parse("a ?? b ?? c");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.NullCoalesceExpr.class, exprSet.root());
        var nc = (MolangAst.NullCoalesceExpr) exprSet.root();
        assertInstanceOf(MolangAst.NullCoalesceExpr.class, nc.left());
        assertInstanceOf(MolangAst.IdentifierExpr.class, nc.right());
    }

    @Test
    void parsesGroupingInsideMultiply() {
        var result = parse("(1 + 2) * 3");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.BinaryExpr.class, exprSet.root());
        var bin = (MolangAst.BinaryExpr) exprSet.root();
        assertEquals("*", bin.operator());
        assertInstanceOf(MolangAst.GroupingExpr.class, bin.left());
    }

    @Test
    void parsesTopLevelReturnAsBlockExprWithReturnStmt() {
        var result = parse("return 1");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.BlockExpr.class, exprSet.root());
        var block = (MolangAst.BlockExpr) exprSet.root();
        assertEquals(1, block.statements().size());
        assertInstanceOf(MolangAst.ReturnStmt.class, block.statements().get(0));
    }

    @Test
    void parsesForEachWithReturnInBody() {
        var result = parse("for_each(t.e, query.list(), {return t.e;})");
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        assertInstanceOf(MolangAst.ForEachExpr.class, exprSet.root());
        var forEach = (MolangAst.ForEachExpr) exprSet.root();
        assertTrue(forEach.body().statements().stream().anyMatch(s -> s instanceof MolangAst.ReturnStmt));
    }

    @Test
    void rejectsMissingSemicolonBetweenTopLevelStatements() {
        var result = parse("a = 1 b = 2");
        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsLoopMissingClosingParenthesis() {
        var result = parse("loop(2, {a = 1;}");
        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsUnterminatedStringLiteral() {
        var result = parse("'abc");
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "1 < 2, <",
            "1 <= 2, <=",
            "1 > 2, >",
            "1 >= 2, >=",
            "1 == 2, ==",
            "1 != 2, !="
    })
    void parsesAllSixComparisonOperatorsIntoBinaryExpr(String source, String expectedOperator) {
        var result = parse(source);
        assertTrue(result.isPresent());
        MolangAst.ExprSet exprSet = result.orElseThrow();
        var bin = assertInstanceOf(MolangAst.BinaryExpr.class, exprSet.root());
        assertEquals(expectedOperator, bin.operator());
    }

    private Optional<MolangAst.ExprSet> parse(String source) {
        return HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source);
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
}
