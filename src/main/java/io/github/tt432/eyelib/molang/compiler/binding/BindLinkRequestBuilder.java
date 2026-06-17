package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 绑定链路请求构建器，推断可见参数类型。
 *
 * @author TT432
 */
@NullMarked
public final class BindLinkRequestBuilder {
    private BindLinkRequestBuilder() {
    }

    public static List<MolangMappingTree.VisibleArgumentKind> visibleCallShape(BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
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

    public static List<MolangMappingTree.VisibleArgumentKind> visibleCallShape(BoundMolang.BoundCallExpr callExpr) {
        List<MolangMappingTree.VisibleArgumentKind> visibleCallShape = new ArrayList<>(callExpr.arguments().size());
        for (BoundMolang.BoundExpr argument : callExpr.arguments()) {
            visibleCallShape.add(inferVisibleArgumentKind(argument));
        }

        return visibleCallShape;
    }

    private static MolangMappingTree.@Nullable VisibleArgumentKind inferVisibleArgumentKind(BoundMolang.BoundExpr argument) {
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

    private static String symbolicCallableName(BoundMolang.BoundExpr callee) {
        List<String> callableSegments = new ArrayList<>();
        if (!collectCallableSymbolicNameSegments(callee, callableSegments) || callableSegments.isEmpty()) {
            return "";
        }

        return String.join(".", callableSegments);
    }

    private static boolean collectCallableSymbolicNameSegments(BoundMolang.BoundExpr expression, List<String> callableSegments) {
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
}