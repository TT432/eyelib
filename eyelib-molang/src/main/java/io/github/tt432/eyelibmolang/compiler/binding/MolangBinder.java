package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.binding.link.MolangQueryBindLinkContract;
import io.github.tt432.eyelibmolang.compiler.binding.link.MolangCallableBindLinkContract;
import io.github.tt432.eyelibmolang.compiler.common.MolangRootAliasCanonicalizer;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
                state.deferredNotes,
                state.queryBindLinkRequests,
                state.callableBindLinkRequests
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
            maybeAddCallableBindLinkRequest(callBoundExpr, state);
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
            String targetRoot = leftMostRoot(unwrapQueryAccess(target)).orElse(null);
            boolean writableTarget = isWritableTarget(targetRoot, target);
            if (!writableTarget) {
                reportInvalidWriteTarget(state, assignmentExpr, targetRoot, target);
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
            boundExpr = deferUnsupportedExpr(state, ternaryConditionalExpr, "TernaryConditionalExpr");
        } else if (expr instanceof MolangAst.BinaryConditionalExpr binaryConditionalExpr) {
            boundExpr = deferUnsupportedExpr(state, binaryConditionalExpr, "BinaryConditionalExpr");
        } else {
            boundExpr = deferUnsupportedExpr(state, expr, expr.getClass().getSimpleName());
        }

        if (!allowQueryProjection) {
            return boundExpr;
        }
        return projectQueryAccess(boundExpr, state);
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

    private BoundMolang.BoundExpr projectQueryAccess(BoundMolang.BoundExpr expression, BindingState state) {
        if (expression instanceof BoundMolang.BoundQueryAccessExpr) {
            return expression;
        }

        Optional<String> root = leftMostRoot(expression);
        if (root.isEmpty() || !"query".equals(root.get())) {
            return expression;
        }

        BoundMolang.BoundQueryAccessExpr.QueryProjectionKind projectionKind = BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY;
        if (expression instanceof BoundMolang.BoundCallExpr callExpr
            && leftMostRoot(callExpr.callee()).filter("query"::equals).isPresent()) {
            projectionKind = BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.EXPLICIT_CALL;
        }

        if (state.diagnosticsMode == BindDiagnosticsMode.DEBUG) {
            state.diagnostics.add(new BindDiagnostic(
                    expression.span(),
                    BindDiagnostic.Severity.INFO,
                    "BIND_DEBUG_QUERY_PROJECTION",
                    "Projected query-rooted access to BoundQueryAccessExpr with kind '" + projectionKind + "'."
            ));
        }

        BoundMolang.BoundQueryAccessExpr queryAccessExpr = new BoundMolang.BoundQueryAccessExpr(
                expression.span(),
                expression,
                projectionKind
        );
        state.queryBindLinkRequests.add(toQueryBindLinkRequest(queryAccessExpr));
        return queryAccessExpr;
    }

    private MolangQueryBindLinkContract.QueryBindLinkRequest toQueryBindLinkRequest(BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
        return new MolangQueryBindLinkContract.QueryBindLinkRequest(
                symbolicQueryName(queryAccessExpr.access()),
                queryAccessExpr.projectionKind(),
                visibleCallShape(queryAccessExpr)
        );
    }

    private void maybeAddCallableBindLinkRequest(BoundMolang.BoundCallExpr callExpr, BindingState state) {
        String symbolicCallableName = symbolicCallableName(callExpr.callee());
        if (symbolicCallableName.isBlank()) {
            return;
        }

        if (leftMostRoot(callExpr.callee()).filter("query"::equals).isPresent()) {
            return;
        }

        state.callableBindLinkRequests.add(new MolangCallableBindLinkContract.CallableBindLinkRequest(
                symbolicCallableName,
                visibleCallShape(callExpr)
        ));
    }

    private String symbolicQueryName(BoundMolang.BoundExpr queryAccess) {
        BoundMolang.BoundExpr symbolicSource = queryAccess;
        if (queryAccess instanceof BoundMolang.BoundCallExpr callExpr) {
            symbolicSource = callExpr.callee();
        }

        List<String> queryMemberSegments = new ArrayList<>();
        if (!collectQueryMemberSegments(symbolicSource, queryMemberSegments) || queryMemberSegments.isEmpty()) {
            return "query";
        }

        return "query." + String.join(".", queryMemberSegments);
    }

    private boolean collectQueryMemberSegments(BoundMolang.BoundExpr expression, List<String> queryMemberSegments) {
        if (expression instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            if (!collectQueryMemberSegments(memberAccessExpr.owner(), queryMemberSegments)) {
                return false;
            }
            queryMemberSegments.add(memberAccessExpr.memberName());
            return true;
        }

        if (expression instanceof BoundMolang.BoundIdentifierExpr identifierExpr) {
            return "query".equals(identifierExpr.name());
        }

        return false;
    }

    private List<MolangMappingTree.VisibleArgumentKind> visibleCallShape(BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
        if (queryAccessExpr.projectionKind() == BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY) {
            return List.of();
        }

        if (!(queryAccessExpr.access() instanceof BoundMolang.BoundCallExpr callExpr)) {
            List<MolangMappingTree.VisibleArgumentKind> invalidShape = new ArrayList<>(1);
            invalidShape.add(null);
            return invalidShape;
        }

        return visibleCallShape(callExpr);
    }

    private List<MolangMappingTree.VisibleArgumentKind> visibleCallShape(BoundMolang.BoundCallExpr callExpr) {
        List<MolangMappingTree.VisibleArgumentKind> visibleCallShape = new ArrayList<>(callExpr.arguments().size());
        for (BoundMolang.BoundExpr argument : callExpr.arguments()) {
            visibleCallShape.add(inferVisibleArgumentKind(argument));
        }

        return visibleCallShape;
    }

    private String symbolicCallableName(BoundMolang.BoundExpr callee) {
        List<String> callableSegments = new ArrayList<>();
        if (!collectCallableSymbolicNameSegments(callee, callableSegments) || callableSegments.isEmpty()) {
            return "";
        }

        return String.join(".", callableSegments);
    }

    private boolean collectCallableSymbolicNameSegments(BoundMolang.BoundExpr expression, List<String> callableSegments) {
        if (expression instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            if (!collectCallableSymbolicNameSegments(memberAccessExpr.owner(), callableSegments)) {
                return false;
            }
            callableSegments.add(memberAccessExpr.memberName());
            return true;
        }

        if (expression instanceof BoundMolang.BoundIdentifierExpr identifierExpr) {
            callableSegments.add(identifierExpr.name());
            return true;
        }

        return false;
    }

    private MolangMappingTree.@Nullable VisibleArgumentKind inferVisibleArgumentKind(BoundMolang.BoundExpr argument) {
        if (argument instanceof BoundMolang.BoundNumberLiteralExpr) {
            return MolangMappingTree.VisibleArgumentKind.NUMBER;
        }

        if (argument instanceof BoundMolang.BoundStringLiteralExpr) {
            return MolangMappingTree.VisibleArgumentKind.STRING;
        }

        if (argument instanceof BoundMolang.BoundIdentifierExpr identifierExpr) {
            if ("true".equals(identifierExpr.name()) || "false".equals(identifierExpr.name())) {
                return MolangMappingTree.VisibleArgumentKind.BOOLEAN;
            }
        }

        if (argument instanceof BoundMolang.BoundGroupingExpr groupingExpr) {
            return inferVisibleArgumentKind(groupingExpr.expression());
        }

        return null;
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

    private void addDeferredNote(BindingState state,
                                 SourceSpan span,
                                 BindDeferredNote.Reason reason,
                                 String sourceFamily) {
        state.deferredNotes.add(new BindDeferredNote(span, reason, sourceFamily));

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

    private BoundMolang.BoundExpr unwrapQueryAccess(BoundMolang.BoundExpr target) {
        BoundMolang.BoundExpr current = target;
        while (current instanceof BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
            current = queryAccessExpr.access();
        }
        return current;
    }

    private Optional<String> leftMostRoot(BoundMolang.BoundExpr expression) {
        if (expression instanceof BoundMolang.BoundIdentifierExpr identifierExpr) {
            return Optional.of(identifierExpr.name());
        }
        if (expression instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            return leftMostRoot(memberAccessExpr.owner());
        }
        if (expression instanceof BoundMolang.BoundIndexExpr indexExpr) {
            return leftMostRoot(indexExpr.owner());
        }
        if (expression instanceof BoundMolang.BoundCallExpr callExpr) {
            return leftMostRoot(callExpr.callee());
        }
        if (expression instanceof BoundMolang.BoundArrowAccessExpr arrowAccessExpr) {
            return leftMostRoot(arrowAccessExpr.left());
        }
        if (expression instanceof BoundMolang.BoundGroupingExpr groupingExpr) {
            return leftMostRoot(groupingExpr.expression());
        }
        if (expression instanceof BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
            return leftMostRoot(queryAccessExpr.access());
        }
        return Optional.empty();
    }

    private boolean isWritableTarget(@Nullable String root, BoundMolang.BoundExpr target) {
        if (isInvalidAssignmentShape(unwrapQueryAccess(target))) {
            return false;
        }
        return root != null && isWritableRoot(root);
    }

    private boolean isInvalidAssignmentShape(BoundMolang.BoundExpr target) {
        return !(target instanceof BoundMolang.BoundIdentifierExpr
                 || target instanceof BoundMolang.BoundMemberAccessExpr
                 || target instanceof BoundMolang.BoundIndexExpr);
    }

    private boolean isWritableRoot(String root) {
        return "variable".equals(root) || "temp".equals(root);
    }

    private void reportInvalidWriteTarget(BindingState state,
                                          MolangAst.AssignmentExpr assignmentExpr,
                                          @Nullable String targetRoot,
                                          BoundMolang.BoundExpr target) {
        if ("query".equals(targetRoot) || "context".equals(targetRoot)) {
            state.diagnostics.add(new BindDiagnostic(
                    assignmentExpr.target().span(),
                    BindDiagnostic.Severity.ERROR,
                    "BIND_INVALID_WRITE_TARGET_ROOT",
                    "Assignment target root '" + targetRoot + "' is read-only; only 'variable' and 'temp' are writable in this slice."
            ));
            return;
        }

        if (isInvalidAssignmentShape(unwrapQueryAccess(target))) {
            state.diagnostics.add(new BindDiagnostic(
                    assignmentExpr.target().span(),
                    BindDiagnostic.Severity.ERROR,
                    "BIND_INVALID_WRITE_TARGET",
                    "Assignment target must be an identifier/member/index access rooted at writable storage."
            ));
            return;
        }

        state.diagnostics.add(new BindDiagnostic(
                assignmentExpr.target().span(),
                BindDiagnostic.Severity.ERROR,
                "BIND_INVALID_WRITE_TARGET_ROOT",
                "Assignment target root must normalize to 'variable' or 'temp'."
        ));
    }

    private static final class BindingState {
        private final BindDiagnosticsMode diagnosticsMode;
        private final List<BindDiagnostic> diagnostics = new ArrayList<>();
        private final List<BindDeferredNote> deferredNotes = new ArrayList<>();
        private final List<MolangQueryBindLinkContract.QueryBindLinkRequest> queryBindLinkRequests = new ArrayList<>();
        private final List<MolangCallableBindLinkContract.CallableBindLinkRequest> callableBindLinkRequests = new ArrayList<>();

        private BindingState(BindDiagnosticsMode diagnosticsMode) {
            this.diagnosticsMode = diagnosticsMode;
        }
    }
}
