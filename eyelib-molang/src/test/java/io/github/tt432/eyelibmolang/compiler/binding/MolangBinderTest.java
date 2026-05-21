package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontends;
import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangBinderTest {
    private static final String DEFERRED_TERNARY_SOURCE = "query.get_name == 'Toast' ? Texture.toast : Array.skins[query.variant]";

    private final MolangBinder binder = new MolangBinder();

    @Test
    void canonicalizesAliasRootsAndRetainsWritableVariableAssignment() {
        BindResult bindResult = bind("v.buff_timer = (v.buff_timer ?? 0) + q.delta_time");

        BoundMolang.BoundAssignmentExpr assignmentExpr = assertInstanceOf(BoundMolang.BoundAssignmentExpr.class, bindResult.root().root());
        assertEquals("variable", assignmentExpr.targetRoot().orElseThrow());
        assertTrue(assignmentExpr.writableTarget());

        BoundMolang.BoundBinaryExpr plusExpr = assertInstanceOf(BoundMolang.BoundBinaryExpr.class, assignmentExpr.value());
        BoundMolang.BoundQueryAccessExpr queryAccessExpr = assertInstanceOf(BoundMolang.BoundQueryAccessExpr.class, plusExpr.right());
        assertEquals(BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY, queryAccessExpr.projectionKind());

        BoundMolang.BoundMemberAccessExpr queryMember = assertInstanceOf(BoundMolang.BoundMemberAccessExpr.class, queryAccessExpr.access());
        BoundMolang.BoundIdentifierExpr queryRoot = assertInstanceOf(BoundMolang.BoundIdentifierExpr.class, queryMember.owner());
        assertEquals("query", queryRoot.name());
        assertTrue(bindResult.diagnostics().isEmpty());
    }

    @Test
    void canonicalizesTempAliasRootAndRetainsWritableTempAssignment() {
        BindResult bindResult = bind("t.foo = 1;");

        BoundMolang.BoundBlockExpr blockExpr = assertInstanceOf(BoundMolang.BoundBlockExpr.class, bindResult.root().root());
        assertEquals(1, blockExpr.statements().size());

        BoundMolang.BoundExprStmt exprStmt = assertInstanceOf(BoundMolang.BoundExprStmt.class, blockExpr.statements().get(0));
        BoundMolang.BoundAssignmentExpr assignmentExpr = assertInstanceOf(BoundMolang.BoundAssignmentExpr.class, exprStmt.expression());
        assertEquals("temp", assignmentExpr.targetRoot().orElseThrow());
        assertTrue(assignmentExpr.writableTarget());

        BoundMolang.BoundMemberAccessExpr tempAccess = assertInstanceOf(BoundMolang.BoundMemberAccessExpr.class, assignmentExpr.target());
        BoundMolang.BoundIdentifierExpr tempRoot = assertInstanceOf(BoundMolang.BoundIdentifierExpr.class, tempAccess.owner());
        assertEquals("temp", tempRoot.name());
        assertTrue(bindResult.diagnostics().isEmpty());
    }

    @Test
    void preservesArrowAndDotFamiliesAsDistinctBoundNodeShapes() {
        BindResult bindResult = bind("v.out = v.another_mob_set_elsewhere->v.location");

        BoundMolang.BoundAssignmentExpr assignmentExpr = assertInstanceOf(BoundMolang.BoundAssignmentExpr.class, bindResult.root().root());
        BoundMolang.BoundArrowAccessExpr arrowAccessExpr = assertInstanceOf(BoundMolang.BoundArrowAccessExpr.class, assignmentExpr.value());

        assertInstanceOf(BoundMolang.BoundMemberAccessExpr.class, arrowAccessExpr.left());
        assertInstanceOf(BoundMolang.BoundMemberAccessExpr.class, arrowAccessExpr.right());
    }

    @Test
    void projectsCanonicalQueryAccessForIdentifierMemberChain() {
        BindResult bindResult = bind("q.life_time");

        BoundMolang.BoundQueryAccessExpr queryAccessExpr = assertInstanceOf(BoundMolang.BoundQueryAccessExpr.class, bindResult.root().root());
        assertEquals(BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY, queryAccessExpr.projectionKind());

        BoundMolang.BoundMemberAccessExpr accessExpr = assertInstanceOf(BoundMolang.BoundMemberAccessExpr.class, queryAccessExpr.access());
        BoundMolang.BoundIdentifierExpr queryRoot = assertInstanceOf(BoundMolang.BoundIdentifierExpr.class, accessExpr.owner());
        assertEquals("query", queryRoot.name());
    }

    @Test
    void projectsExplicitCallQueryAccessWhenQueryRootIsCalled() {
        BindResult bindResult = bind("q.foo(1)");

        BoundMolang.BoundQueryAccessExpr queryAccessExpr = assertInstanceOf(BoundMolang.BoundQueryAccessExpr.class, bindResult.root().root());
        assertEquals(BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.EXPLICIT_CALL, queryAccessExpr.projectionKind());

        BoundMolang.BoundCallExpr callExpr = assertInstanceOf(BoundMolang.BoundCallExpr.class, queryAccessExpr.access());
        BoundMolang.BoundMemberAccessExpr calleeExpr = assertInstanceOf(BoundMolang.BoundMemberAccessExpr.class, callExpr.callee());
        BoundMolang.BoundIdentifierExpr queryRoot = assertInstanceOf(BoundMolang.BoundIdentifierExpr.class, calleeExpr.owner());
        assertEquals("query", queryRoot.name());
        assertEquals("foo", calleeExpr.memberName());
    }

    @Test
    void rejectsQueryWriteTargetWithExplicitBinderDiagnostic() {
        BindResult bindResult = bind("q.life_time = 1");

        BoundMolang.BoundAssignmentExpr assignmentExpr = assertInstanceOf(BoundMolang.BoundAssignmentExpr.class, bindResult.root().root());
        assertEquals("query", assignmentExpr.targetRoot().orElseThrow());
        assertFalse(assignmentExpr.writableTarget());
        assertTrue(bindResult.hasErrors());
        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("BIND_INVALID_WRITE_TARGET_ROOT")
                && diagnostic.message().contains("read-only")
        ));
    }

    @Test
    void bindsDeferredTernaryAsUnsupportedInThisSliceWithDeferredNote() {
        BindResult bindResult = bind(DEFERRED_TERNARY_SOURCE);

        BoundMolang.BoundDeferredExpr deferredExpr = assertInstanceOf(BoundMolang.BoundDeferredExpr.class, bindResult.root().root());
        assertEquals(BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, deferredExpr.reason());
        assertEquals("TernaryConditionalExpr", deferredExpr.sourceFamily());

        assertTrue(bindResult.deferredNotes().stream().anyMatch(note ->
                note.reason() == BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                && note.sourceFamily().equals("TernaryConditionalExpr")
        ));
        assertDeferredUnsupportedWarning(bindResult, "TernaryConditionalExpr");
    }

    @Test
    void bindsDeferredBinaryConditionalWithNormalWarning() {
        BindResult bindResult = bind("a ? b");

        BoundMolang.BoundDeferredExpr deferredExpr = assertInstanceOf(BoundMolang.BoundDeferredExpr.class, bindResult.root().root());
        assertEquals(BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, deferredExpr.reason());
        assertEquals("BinaryConditionalExpr", deferredExpr.sourceFamily());

        assertTrue(bindResult.deferredNotes().stream().anyMatch(note ->
                note.reason() == BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                && note.sourceFamily().equals("BinaryConditionalExpr")
        ));
        assertDeferredUnsupportedWarning(bindResult, "BinaryConditionalExpr");
    }

    @Test
    void strictModeEmitsDeferredWarningForTernaryBinding() {
        BindResult bindResult = bind(DEFERRED_TERNARY_SOURCE, BindDiagnosticsMode.STRICT);

        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.WARNING
                && diagnostic.code().equals("BIND_STRICT_UNSUPPORTED_DEFERRED")
                && diagnostic.message().contains("TernaryConditionalExpr")
        ));
    }

    @Test
    void debugModeEmitsDeferredInfoForTernaryBinding() {
        BindResult bindResult = bind(DEFERRED_TERNARY_SOURCE, BindDiagnosticsMode.DEBUG);

        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.INFO
                && diagnostic.code().equals("BIND_DEBUG_DEFERRED_NOTE")
                && diagnostic.message().contains("TernaryConditionalExpr")
        ));
    }

    @Test
    void bindsLoopToExplicitLoopContractWithTypedDeferralNote() {
        BindResult bindResult = bind("loop(3, {variable.counter = variable.counter + 1;})");

        BoundMolang.BoundLoopExpr loopExpr = assertInstanceOf(BoundMolang.BoundLoopExpr.class, bindResult.root().root());
        assertEquals("3", loopExpr.iterationCountRawText());
        assertEquals(BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, loopExpr.deferredReason());

        BoundMolang.BoundExprStmt bodyStatement = assertInstanceOf(BoundMolang.BoundExprStmt.class, loopExpr.body().statements().get(0));
        BoundMolang.BoundAssignmentExpr assignmentExpr = assertInstanceOf(BoundMolang.BoundAssignmentExpr.class, bodyStatement.expression());
        assertTrue(assignmentExpr.writableTarget());
        assertEquals("variable", assignmentExpr.targetRoot().orElseThrow());

        assertTrue(bindResult.deferredNotes().stream().anyMatch(note ->
                note.reason() == BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                && note.sourceFamily().equals("LoopExpr")
        ));
        assertDeferredUnsupportedWarning(bindResult, "LoopExpr");
    }

    @Test
    void strictModeEmitsDeferredWarningForLoopBinding() {
        BindResult bindResult = bind("loop(3, {variable.counter = variable.counter + 1;})", BindDiagnosticsMode.STRICT);

        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.WARNING
                && diagnostic.code().equals("BIND_STRICT_UNSUPPORTED_DEFERRED")
                && diagnostic.message().contains("LoopExpr")
        ));
    }

    @Test
    void debugModeEmitsDeferredInfoForLoopBinding() {
        BindResult bindResult = bind("loop(3, {variable.counter = variable.counter + 1;})", BindDiagnosticsMode.DEBUG);

        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.INFO
                && diagnostic.code().equals("BIND_DEBUG_DEFERRED_NOTE")
                && diagnostic.message().contains("LoopExpr")
        ));
    }

    @Test
    void bindsForEachToExplicitForEachContractWithDeferral() {
        String source = "for_each(t.pig, query.get_nearby_entities(4, 'minecraft:pig'), {variable.counter = variable.counter + 1;})";
        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();

        BindResult bindResult = binder.bind(ast, BindDiagnosticsMode.NORMAL);

        BoundMolang.BoundForEachExpr forEachExpr = assertInstanceOf(BoundMolang.BoundForEachExpr.class, bindResult.root().root());
        assertEquals(BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, forEachExpr.deferredReason());

        BoundMolang.BoundMemberAccessExpr variableAccess = assertInstanceOf(BoundMolang.BoundMemberAccessExpr.class, forEachExpr.variable());
        BoundMolang.BoundIdentifierExpr variableRoot = assertInstanceOf(BoundMolang.BoundIdentifierExpr.class, variableAccess.owner());
        assertEquals("temp", variableRoot.name());

        BoundMolang.BoundQueryAccessExpr collectionExpr = assertInstanceOf(BoundMolang.BoundQueryAccessExpr.class, forEachExpr.collection());
        assertEquals(BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.EXPLICIT_CALL, collectionExpr.projectionKind());

        BoundMolang.BoundExprStmt bodyStatement = assertInstanceOf(BoundMolang.BoundExprStmt.class, forEachExpr.body().statements().get(0));
        assertInstanceOf(BoundMolang.BoundAssignmentExpr.class, bodyStatement.expression());

        assertTrue(bindResult.deferredNotes().stream().anyMatch(note ->
                note.reason() == BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                && note.sourceFamily().equals("ForEachExpr")
        ));
        assertDeferredUnsupportedWarning(bindResult, "ForEachExpr");
    }

    @Test
    void bindsLoopBreakAndContinueAsDedicatedBoundStatementsWithDeferredNotes() {
        BindResult bindResult = bindFromHandwrittenFrontend("loop(2, {break; continue;})");

        BoundMolang.BoundLoopExpr loopExpr = assertInstanceOf(BoundMolang.BoundLoopExpr.class, bindResult.root().root());
        assertEquals("2", loopExpr.iterationCountRawText());
        assertEquals(BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, loopExpr.deferredReason());

        assertEquals(2, loopExpr.body().statements().size());
        BoundMolang.BoundBreakStmt breakStmt = assertInstanceOf(BoundMolang.BoundBreakStmt.class, loopExpr.body().statements().get(0));
        BoundMolang.BoundContinueStmt continueStmt = assertInstanceOf(BoundMolang.BoundContinueStmt.class, loopExpr.body().statements().get(1));
        assertEquals(BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, breakStmt.deferredReason());
        assertEquals(BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, continueStmt.deferredReason());

        assertTrue(bindResult.deferredNotes().stream().anyMatch(note ->
                note.reason() == BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                && note.sourceFamily().equals("LoopExpr")
        ));
        assertTrue(bindResult.deferredNotes().stream().anyMatch(note ->
                note.reason() == BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                && note.sourceFamily().equals("BreakStmt")
        ));
        assertTrue(bindResult.deferredNotes().stream().anyMatch(note ->
                note.reason() == BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                && note.sourceFamily().equals("ContinueStmt")
        ));
        assertDeferredUnsupportedWarning(bindResult, "LoopExpr");
        assertDeferredUnsupportedWarning(bindResult, "BreakStmt");
        assertDeferredUnsupportedWarning(bindResult, "ContinueStmt");
    }

    @Test
    void strictModeEmitsDeferredWarningsForLoopBreakAndContinueBinding() {
        BindResult bindResult = bindFromHandwrittenFrontend("loop(2, {break; continue;})", BindDiagnosticsMode.STRICT);

        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.WARNING
                && diagnostic.code().equals("BIND_STRICT_UNSUPPORTED_DEFERRED")
                && diagnostic.message().contains("BreakStmt")
        ));
        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.WARNING
                && diagnostic.code().equals("BIND_STRICT_UNSUPPORTED_DEFERRED")
                && diagnostic.message().contains("ContinueStmt")
        ));
    }

    @Test
    void debugModeEmitsDeferredInfoForLoopBreakAndContinueBinding() {
        BindResult bindResult = bindFromHandwrittenFrontend("loop(2, {break; continue;})", BindDiagnosticsMode.DEBUG);

        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.INFO
                && diagnostic.code().equals("BIND_DEBUG_DEFERRED_NOTE")
                && diagnostic.message().contains("BreakStmt")
        ));
        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.INFO
                && diagnostic.code().equals("BIND_DEBUG_DEFERRED_NOTE")
                && diagnostic.message().contains("ContinueStmt")
        ));
    }

    @Test
    void preservesThisAsBoundThisExprWithoutDiagnosticsOrDeferredNotes() {
        BindResult bindResult = bind("this");

        assertInstanceOf(BoundMolang.BoundThisExpr.class, bindResult.root().root());
        assertTrue(bindResult.diagnostics().isEmpty());
        assertTrue(bindResult.deferredNotes().isEmpty());
    }

    private BindResult bind(String source) {
        return bind(source, BindDiagnosticsMode.NORMAL);
    }

    private BindResult bind(String source, BindDiagnosticsMode diagnosticsMode) {
        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();
        return binder.bind(ast, diagnosticsMode);
    }

    private BindResult bindFromHandwrittenFrontend(String source) {
        return bindFromHandwrittenFrontend(source, BindDiagnosticsMode.NORMAL);
    }

    private BindResult bindFromHandwrittenFrontend(String source, BindDiagnosticsMode diagnosticsMode) {
        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();
        return binder.bind(ast, diagnosticsMode);
    }

    private static void assertDeferredUnsupportedWarning(BindResult bindResult, String sourceFamily) {
        assertTrue(bindResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == BindDiagnostic.Severity.WARNING
                && diagnostic.code().equals("BIND_DEFERRED_UNSUPPORTED")
                && diagnostic.message().contains(sourceFamily)
        ));
    }
}