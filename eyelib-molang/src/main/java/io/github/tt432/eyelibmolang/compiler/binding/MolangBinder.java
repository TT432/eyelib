package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.common.MolangRootAliasCanonicalizer;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Molang AST 绑定器，解析标识符并验证语义。
 *
 * @author TT432
 */
@NullMarked
public final class MolangBinder {
    public BindResult bind(MolangAst.ExprSet ast) {
        return bind(ast, BindDiagnosticsMode.NORMAL);
    }
    public BindResult bind(MolangAst.ExprSet ast, BindDiagnosticsMode diagnosticsMode) {
        BindingState state = new BindingState(diagnosticsMode);
        BoundMolang.BoundExpr root = bindExpr(ast.root(), state, true);
        return new BindResult(
                new BoundMolang.BoundExprSet(ast.span(), root),
                state.diagnostics,
                state.deferredNotes
        );
    }
    private BoundMolang.BoundExpr bindExpr(MolangAst.Expr expr, BindingState state, boolean allowQueryProjection) {
        BoundMolang.BoundExpr boundExpr;
        if (expr instanceof MolangAst.UnknownExpr unknownExpr) {
            boundExpr = new BoundMolang.BoundUnknownExpr(unknownExpr.span(), unknownExpr.text());
        } else if (expr instanceof MolangAst.IdentifierExpr identifierExpr) {
            boundExpr = new BoundMolang.BoundIdentifierExpr(
                    identifierExpr.span(),
                    normalizeIdentifier(identifierExpr.name(), identifierExpr.span(), state)
            );
        } else if (expr instanceof MolangAst.NumberLiteralExpr numberLiteralExpr) {
            boundExpr = new BoundMolang.BoundNumberLiteralExpr(numberLiteralExpr.span(), numberLiteralExpr.rawText(), numberLiteralExpr.value());
        } else if (expr instanceof MolangAst.StringLiteralExpr stringLiteralExpr) {
            boundExpr = new BoundMolang.BoundStringLiteralExpr(stringLiteralExpr.span(), stringLiteralExpr.rawText());
        } else if (expr instanceof MolangAst.ThisExpr thisExpr) {
            boundExpr = new BoundMolang.BoundThisExpr(thisExpr.span());
        } else if (expr instanceof MolangAst.UnaryExpr unaryExpr) {
            boundExpr = new BoundMolang.BoundUnaryExpr(
                    unaryExpr.span(),
                    unaryExpr.operator(),
                    bindExpr(unaryExpr.expression(), state, true)
            );
        } else if (expr instanceof MolangAst.BinaryExpr binaryExpr) {
            boundExpr = new BoundMolang.BoundBinaryExpr(
                    binaryExpr.span(),
                    binaryExpr.operator(),
                    bindExpr(binaryExpr.left(), state, true),
                    bindExpr(binaryExpr.right(), state, true)
            );
        } else if (expr instanceof MolangAst.NullCoalesceExpr nullCoalesceExpr) {
            boundExpr = new BoundMolang.BoundNullCoalesceExpr(
                    nullCoalesceExpr.span(),
                    bindExpr(nullCoalesceExpr.left(), state, true),
                    bindExpr(nullCoalesceExpr.right(), state, true)
            );
        } else if (expr instanceof MolangAst.GroupingExpr groupingExpr) {
            boundExpr = new BoundMolang.BoundGroupingExpr(
                    groupingExpr.span(),
                    bindExpr(groupingExpr.expression(), state, true)
            );
        } else if (expr instanceof MolangAst.MemberAccessExpr memberAccessExpr) {
            boundExpr = new BoundMolang.BoundMemberAccessExpr(
                    memberAccessExpr.span(),
                    bindExpr(memberAccessExpr.owner(), state, false),
                    memberAccessExpr.memberName()
            );
        } else if (expr instanceof MolangAst.CallExpr callExpr) {
            List<BoundMolang.BoundExpr> arguments = new ArrayList<>();
            for (MolangAst.Expr argument : callExpr.arguments()) {
                arguments.add(bindExpr(argument, state, true));
            }
            BoundMolang.BoundCallExpr callBoundExpr = new BoundMolang.BoundCallExpr(
                    callExpr.span(),
                    bindExpr(callExpr.callee(), state, false),
                    arguments
            );
            boundExpr = callBoundExpr;
        } else if (expr instanceof MolangAst.IndexExpr indexExpr) {
            boundExpr = new BoundMolang.BoundIndexExpr(
                    indexExpr.span(),
                    bindExpr(indexExpr.owner(), state, false),
                    bindExpr(indexExpr.index(), state, true)
            );
        } else if (expr instanceof MolangAst.ArrowAccessExpr arrowAccessExpr) {
            boundExpr = new BoundMolang.BoundArrowAccessExpr(
                    arrowAccessExpr.span(),
                    bindExpr(arrowAccessExpr.left(), state, false),
                    bindExpr(arrowAccessExpr.right(), state, false)
            );
        } else if (expr instanceof MolangAst.AssignmentExpr assignmentExpr) {
            BoundMolang.BoundExpr target = bindExpr(assignmentExpr.target(), state, true);
            BoundMolang.BoundExpr value = bindExpr(assignmentExpr.value(), state, true);
            String targetRoot = AssignmentValidator.leftMostRoot(AssignmentValidator.unwrapQueryAccess(target)).orElse(null);
            boolean writableTarget = AssignmentValidator.isWritableTarget(targetRoot, target);
            if (!writableTarget) {
                AssignmentValidator.reportInvalidWriteTarget(state, assignmentExpr, targetRoot, target);
            }
            boundExpr = new BoundMolang.BoundAssignmentExpr(
                    assignmentExpr.span(),
                    target,
                    value,
                    Optional.ofNullable(targetRoot),
                    writableTarget
            );
        } else if (expr instanceof MolangAst.BlockExpr blockExpr) {
            List<BoundMolang.BoundStmt> statements = new ArrayList<>();
            for (MolangAst.Stmt statement : blockExpr.statements()) {
                statements.add(bindStmt(statement, state));
            }
            boundExpr = new BoundMolang.BoundBlockExpr(blockExpr.span(), statements);
        } else if (expr instanceof MolangAst.LoopExpr loopExpr) {
            boundExpr = bindLoopExpr(loopExpr, state);
        } else if (expr instanceof MolangAst.ForEachExpr forEachExpr) {
            boundExpr = bindForEachExpr(forEachExpr, state);
        } else if (expr instanceof MolangAst.TernaryConditionalExpr ternaryConditionalExpr) {
            boundExpr = new BoundMolang.BoundTernaryConditionalExpr(
                    ternaryConditionalExpr.span(),
                    bindExpr(ternaryConditionalExpr.condition(), state, true),
                    bindExpr(ternaryConditionalExpr.whenTrue(), state, true),
                    bindExpr(ternaryConditionalExpr.whenFalse(), state, true)
            );
        } else if (expr instanceof MolangAst.BinaryConditionalExpr binaryConditionalExpr) {
            boundExpr = deferUnsupportedExpr(state, binaryConditionalExpr, "BinaryConditionalExpr");
        } else {
            boundExpr = deferUnsupportedExpr(state, expr, expr.getClass().getSimpleName());
        }
        if (!allowQueryProjection) {
            return boundExpr;
        }
        return QueryProjector.projectQueryAccess(boundExpr, state);
    }

    private BoundMolang.BoundStmt bindStmt(MolangAst.Stmt statement, BindingState state) {
        if (statement instanceof MolangAst.ExprStmt exprStmt) {
            return new BoundMolang.BoundExprStmt(exprStmt.span(), bindExpr(exprStmt.expression(), state, true));
        }
        if (statement instanceof MolangAst.ReturnStmt returnStmt) {
            return new BoundMolang.BoundReturnStmt(returnStmt.span(), bindExpr(returnStmt.expression(), state, true));
        }
        if (statement instanceof MolangAst.BreakStmt breakStmt) {
            addDeferredNote(state, breakStmt.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, "BreakStmt");
            return new BoundMolang.BoundBreakStmt(breakStmt.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE);
        }
        if (statement instanceof MolangAst.ContinueStmt continueStmt) {
            addDeferredNote(state, continueStmt.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, "ContinueStmt");
            return new BoundMolang.BoundContinueStmt(continueStmt.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE);
        }

        addDeferredNote(state, statement.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, statement.getClass().getSimpleName());
        return new BoundMolang.BoundExprStmt(
                statement.span(),
                new BoundMolang.BoundDeferredExpr(
                        statement.span(),
                        statement.getClass().getSimpleName(),
                        BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
                )
        );
    }

    private BoundMolang.BoundExpr deferUnsupportedExpr(BindingState state, MolangAst.Expr source, String sourceFamily) {
        addDeferredNote(state, source.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, sourceFamily);
        return new BoundMolang.BoundDeferredExpr(
                source.span(),
                sourceFamily,
                BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
        );
    }

    private BoundMolang.BoundExpr bindLoopExpr(MolangAst.LoopExpr loopExpr, BindingState state) {
        addDeferredNote(state, loopExpr.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, "LoopExpr");
        List<BoundMolang.BoundStmt> statements = new ArrayList<>();
        for (MolangAst.Stmt statement : loopExpr.body().statements()) {
            statements.add(bindStmt(statement, state));
        }
        return new BoundMolang.BoundLoopExpr(
                loopExpr.span(),
                loopExpr.iterationCountRawText(),
                new BoundMolang.BoundBlockExpr(loopExpr.body().span(), statements),
                BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
        );
    }

    private BoundMolang.BoundExpr bindForEachExpr(MolangAst.ForEachExpr forEachExpr, BindingState state) {
        addDeferredNote(state, forEachExpr.span(), BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE, "ForEachExpr");
        BoundMolang.BoundExpr variable = bindExpr(forEachExpr.variable(), state, true);
        BoundMolang.BoundExpr collection = bindExpr(forEachExpr.collection(), state, true);
        List<BoundMolang.BoundStmt> statements = new ArrayList<>();
        for (MolangAst.Stmt statement : forEachExpr.body().statements()) {
            statements.add(bindStmt(statement, state));
        }
        return new BoundMolang.BoundForEachExpr(
                forEachExpr.span(),
                variable,
                collection,
                new BoundMolang.BoundBlockExpr(forEachExpr.body().span(), statements),
                BindDeferredNote.Reason.UNSUPPORTED_IN_THIS_SLICE
        );
    }

    private String normalizeIdentifier(String name, SourceSpan span, BindingState state) {
        String normalized = name.toLowerCase(Locale.ROOT);
        String canonical = MolangRootAliasCanonicalizer.canonicalizeRoot(name);
        if (state.diagnosticsMode == BindDiagnosticsMode.DEBUG && !canonical.equals(normalized)) {
            state.diagnostics.add(new BindDiagnostic(
                    span,
                    BindDiagnostic.Severity.INFO,
                    "BIND_DEBUG_ALIAS_NORMALIZED",
                    "Normalized alias root '" + name + "' to canonical root '" + canonical + "'."
            ));
        }
        return canonical;
    }

    private void addDeferredNote(BindingState state, SourceSpan span, BindDeferredNote.Reason reason, String sourceFamily) {
        state.deferredNotes.add(new BindDeferredNote(span, reason, sourceFamily));
        state.diagnostics.add(new BindDiagnostic(
                span,
                BindDiagnostic.Severity.WARNING,
                "BIND_DEFERRED_UNSUPPORTED",
                "Deferred unsupported binder node '" + sourceFamily + "' with reason '" + reason + "'."
        ));
        if (state.diagnosticsMode == BindDiagnosticsMode.STRICT) {
            state.diagnostics.add(new BindDiagnostic(
                    span,
                    BindDiagnostic.Severity.WARNING,
                    "BIND_STRICT_UNSUPPORTED_DEFERRED",
                    "Strict mode flags deferred binder node '" + sourceFamily + "' with reason '" + reason + "'."
            ));
            return;
        }
        if (state.diagnosticsMode == BindDiagnosticsMode.DEBUG) {
            state.diagnostics.add(new BindDiagnostic(
                    span,
                    BindDiagnostic.Severity.INFO,
                    "BIND_DEBUG_DEFERRED_NOTE",
                    "Debug mode recorded deferred binder node '" + sourceFamily + "' with reason '" + reason + "'."
            ));
        }
    }

    static final class BindingState {
        final BindDiagnosticsMode diagnosticsMode;
        final List<BindDiagnostic> diagnostics = new ArrayList<>();
        final List<BindDeferredNote> deferredNotes = new ArrayList<>();

        BindingState(BindDiagnosticsMode diagnosticsMode) {
            this.diagnosticsMode = diagnosticsMode;
        }
    }
}