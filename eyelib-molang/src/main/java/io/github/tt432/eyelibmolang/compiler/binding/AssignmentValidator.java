package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 赋值语句目标合法性验证器。
 *
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public final class AssignmentValidator {
    private AssignmentValidator() {
    }

    public static BoundMolang.BoundExpr unwrapQueryAccess(BoundMolang.BoundExpr target) {
        BoundMolang.BoundExpr current = target;
        while (current instanceof BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
            current = queryAccessExpr.access();
        }
        return current;
    }

    public static Optional<String> leftMostRoot(BoundMolang.BoundExpr expression) {
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

    public static boolean isWritableTarget(@Nullable String root, BoundMolang.BoundExpr target) {
        if (isInvalidAssignmentShape(unwrapQueryAccess(target))) {
            return false;
        }
        return root != null && isWritableRoot(root);
    }

    private static boolean isInvalidAssignmentShape(BoundMolang.BoundExpr target) {
        return !(target instanceof BoundMolang.BoundIdentifierExpr
                 || target instanceof BoundMolang.BoundMemberAccessExpr
                 || target instanceof BoundMolang.BoundIndexExpr);
    }

    private static boolean isWritableRoot(String root) {
        return "variable".equals(root) || "temp".equals(root);
    }

    public static void reportInvalidWriteTarget(MolangBinder.BindingState state,
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
}