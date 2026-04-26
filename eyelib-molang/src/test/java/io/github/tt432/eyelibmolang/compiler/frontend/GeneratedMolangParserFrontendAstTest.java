package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GeneratedMolangParserFrontendAstTest {
    @Test
    void simpleExpressionBuildsCallWithMemberAccessAndSourceSpans() {
        String source = "math.sin(query.anim_time * 1.23)";

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();

        MolangAst.CallExpr callExpr = assertInstanceOf(MolangAst.CallExpr.class, ast.root());
        assertSpanText(source, callExpr.span(), source);

        MolangAst.MemberAccessExpr callee = assertInstanceOf(MolangAst.MemberAccessExpr.class, callExpr.callee());
        assertSpanText(source, callee.span(), "math.sin");

        MolangAst.BinaryExpr multiplyExpr = assertInstanceOf(MolangAst.BinaryExpr.class, callExpr.arguments().get(0));
        assertEquals("*", multiplyExpr.operator());
        assertSpanText(source, multiplyExpr.span(), "query.anim_time * 1.23");

        MolangAst.MemberAccessExpr queryAnimTime = assertInstanceOf(MolangAst.MemberAccessExpr.class, multiplyExpr.left());
        assertSpanText(source, queryAnimTime.span(), "query.anim_time");
    }

    @Test
    void assignReturnBuildsCanonicalBlockExprWithReturnStatement() {
        String source = "variable.is_blinking = 1; variable.return_from_blink = query.life_time; return query.all_animations_finished && (query.life_time > (variable.return_from_blink ?? 0.2));";

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();

        MolangAst.BlockExpr blockExpr = assertInstanceOf(MolangAst.BlockExpr.class, ast.root());
        assertSpanText(source, blockExpr.span(), source);
        assertEquals(3, blockExpr.statements().size());

        MolangAst.ExprStmt firstStmt = assertInstanceOf(MolangAst.ExprStmt.class, blockExpr.statements().get(0));
        MolangAst.AssignmentExpr firstAssignment = assertInstanceOf(MolangAst.AssignmentExpr.class, firstStmt.expression());
        assertSpanText(source, firstAssignment.span(), "variable.is_blinking = 1");

        MolangAst.ExprStmt secondStmt = assertInstanceOf(MolangAst.ExprStmt.class, blockExpr.statements().get(1));
        MolangAst.AssignmentExpr secondAssignment = assertInstanceOf(MolangAst.AssignmentExpr.class, secondStmt.expression());
        assertSpanText(source, secondAssignment.span(), "variable.return_from_blink = query.life_time");

        MolangAst.ReturnStmt returnStmt = assertInstanceOf(MolangAst.ReturnStmt.class, blockExpr.statements().get(2));
        assertFalse(returnStmt.expression() instanceof MolangAst.BlockExpr);
        assertSpanText(source, returnStmt.span(), "return query.all_animations_finished && (query.life_time > (variable.return_from_blink ?? 0.2))");
    }

    @Test
    void structArrowKeepsArrowAccessDistinctFromMemberAccess() {
        String source = "v.location.x = 1; v.location.y = 2; v.location.z = 3; v.another_mobs_location = v.another_mob_set_elsewhere->v.location;";

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();

        MolangAst.BlockExpr blockExpr = assertInstanceOf(MolangAst.BlockExpr.class, ast.root());
        assertEquals(4, blockExpr.statements().size());

        MolangAst.ExprStmt lastStmt = assertInstanceOf(MolangAst.ExprStmt.class, blockExpr.statements().get(3));
        MolangAst.AssignmentExpr assignmentExpr = assertInstanceOf(MolangAst.AssignmentExpr.class, lastStmt.expression());
        assertSpanText(source, assignmentExpr.target().span(), "v.another_mobs_location");

        MolangAst.ArrowAccessExpr arrowValue = assertInstanceOf(MolangAst.ArrowAccessExpr.class, assignmentExpr.value());
        assertSpanText(source, arrowValue.span(), "v.another_mob_set_elsewhere->v.location");

        MolangAst.MemberAccessExpr left = assertInstanceOf(MolangAst.MemberAccessExpr.class, arrowValue.left());
        MolangAst.MemberAccessExpr right = assertInstanceOf(MolangAst.MemberAccessExpr.class, arrowValue.right());
        assertSpanText(source, left.span(), "v.another_mob_set_elsewhere");
        assertSpanText(source, right.span(), "v.location");
    }

    @Test
    void aliasHeavyAccessBuildsAssignmentWithNullCoalesceAndAliasMemberSpans() {
        String source = "v.buff_timer = (v.buff_timer ?? 0) + q.delta_time;";

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();

        MolangAst.BlockExpr blockExpr = assertInstanceOf(MolangAst.BlockExpr.class, ast.root());
        assertSpanText(source, blockExpr.span(), source);
        assertEquals(1, blockExpr.statements().size());

        MolangAst.ExprStmt stmt = assertInstanceOf(MolangAst.ExprStmt.class, blockExpr.statements().get(0));
        MolangAst.AssignmentExpr assignmentExpr = assertInstanceOf(MolangAst.AssignmentExpr.class, stmt.expression());
        assertSpanText(source, assignmentExpr.span(), "v.buff_timer = (v.buff_timer ?? 0) + q.delta_time");

        MolangAst.MemberAccessExpr target = assertInstanceOf(MolangAst.MemberAccessExpr.class, assignmentExpr.target());
        assertSpanText(source, target.span(), "v.buff_timer");
        assertEquals("buff_timer", target.memberName());
        assertInstanceOf(MolangAst.IdentifierExpr.class, target.owner());

        MolangAst.BinaryExpr addition = assertInstanceOf(MolangAst.BinaryExpr.class, assignmentExpr.value());
        assertEquals("+", addition.operator());
        assertSpanText(source, addition.span(), "(v.buff_timer ?? 0) + q.delta_time");

        MolangAst.GroupingExpr groupedNullCoalesce = assertInstanceOf(MolangAst.GroupingExpr.class, addition.left());
        assertSpanText(source, groupedNullCoalesce.span(), "(v.buff_timer ?? 0)");

        MolangAst.NullCoalesceExpr nullCoalesceExpr = assertInstanceOf(MolangAst.NullCoalesceExpr.class, groupedNullCoalesce.expression());
        assertSpanText(source, nullCoalesceExpr.span(), "v.buff_timer ?? 0");

        MolangAst.MemberAccessExpr nullCoalesceLeft = assertInstanceOf(MolangAst.MemberAccessExpr.class, nullCoalesceExpr.left());
        assertSpanText(source, nullCoalesceLeft.span(), "v.buff_timer");

        MolangAst.NumberLiteralExpr nullCoalesceRight = assertInstanceOf(MolangAst.NumberLiteralExpr.class, nullCoalesceExpr.right());
        assertEquals("0", nullCoalesceRight.rawText());

        MolangAst.MemberAccessExpr deltaTime = assertInstanceOf(MolangAst.MemberAccessExpr.class, addition.right());
        assertSpanText(source, deltaTime.span(), "q.delta_time");
    }

    @Test
    void ternaryArrayIndexBuildsTernaryConditionalWithIndexedFalseBranchSpans() {
        String source = "query.get_name == 'Toast' ? Texture.toast : Array.skins[query.variant]";

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();

        MolangAst.TernaryConditionalExpr ternary = assertInstanceOf(MolangAst.TernaryConditionalExpr.class, ast.root());
        assertSpanText(source, ternary.span(), source);

        MolangAst.BinaryExpr condition = assertInstanceOf(MolangAst.BinaryExpr.class, ternary.condition());
        assertEquals("==", condition.operator());
        assertSpanText(source, condition.span(), "query.get_name == 'Toast'");

        MolangAst.MemberAccessExpr conditionLeft = assertInstanceOf(MolangAst.MemberAccessExpr.class, condition.left());
        assertSpanText(source, conditionLeft.span(), "query.get_name");

        MolangAst.StringLiteralExpr conditionRight = assertInstanceOf(MolangAst.StringLiteralExpr.class, condition.right());
        assertEquals("'Toast'", conditionRight.rawText());

        MolangAst.MemberAccessExpr whenTrue = assertInstanceOf(MolangAst.MemberAccessExpr.class, ternary.whenTrue());
        assertSpanText(source, whenTrue.span(), "Texture.toast");

        MolangAst.IndexExpr whenFalse = assertInstanceOf(MolangAst.IndexExpr.class, ternary.whenFalse());
        assertSpanText(source, whenFalse.span(), "Array.skins[query.variant]");

        MolangAst.MemberAccessExpr indexedOwner = assertInstanceOf(MolangAst.MemberAccessExpr.class, whenFalse.owner());
        assertSpanText(source, indexedOwner.span(), "Array.skins");

        MolangAst.MemberAccessExpr indexedValue = assertInstanceOf(MolangAst.MemberAccessExpr.class, whenFalse.index());
        assertSpanText(source, indexedValue.span(), "query.variant");
    }

    private void assertSpanText(String source, SourceSpan span, String expected) {
        String actual = source.substring(span.startIndex(), span.stopIndexInclusive() + 1);
        assertEquals(expected, actual);
    }
}
