package io.github.tt432.eyelibmolang.compiler.binding;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 查询表达式投影器，将 query-rooted 访问包装为 BoundQueryAccessExpr。
 *
 * @author TT432
 */
@NullMarked
public final class QueryProjector {
    private QueryProjector() {
    }

    public static BoundMolang.BoundExpr projectQueryAccess(BoundMolang.BoundExpr expression, MolangBinder.BindingState state) {
        if (expression instanceof BoundMolang.BoundQueryAccessExpr) {
            return expression;
        }

        Optional<String> root = AssignmentValidator.leftMostRoot(expression);
        if (root.isEmpty() || !"query".equals(root.get())) {
            return expression;
        }

        BoundMolang.BoundQueryAccessExpr.QueryProjectionKind projectionKind = BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY;
        if (expression instanceof BoundMolang.BoundCallExpr callExpr
            && AssignmentValidator.leftMostRoot(callExpr.callee()).filter("query"::equals).isPresent()) {
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
        return queryAccessExpr;
    }

    public static String symbolicQueryName(BoundMolang.BoundExpr queryAccess) {
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

    private static boolean collectQueryMemberSegments(BoundMolang.BoundExpr expression, List<String> queryMemberSegments) {
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
}