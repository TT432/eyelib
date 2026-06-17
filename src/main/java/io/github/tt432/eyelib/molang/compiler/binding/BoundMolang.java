package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * @author TT432
 */
@NullMarked
public final class BoundMolang {
    private BoundMolang() {
    }

    public interface BoundNode {
        SourceSpan span();
    }

    public record BoundExprSet(SourceSpan span, BoundExpr root) implements BoundNode {
    }

    public interface BoundExpr extends BoundNode {
    }

    public interface BoundStmt extends BoundNode {
    }

    public record BoundUnknownExpr(SourceSpan span, String text) implements BoundExpr {
    }

    public record BoundDeferredExpr(
            SourceSpan span,
            String sourceFamily,
            BindDeferredNote.Reason reason
    ) implements BoundExpr {
    }

    public record BoundIdentifierExpr(SourceSpan span, String name) implements BoundExpr {
    }

    public record BoundNumberLiteralExpr(SourceSpan span, String rawText, double value) implements BoundExpr {
    }

    public record BoundStringLiteralExpr(SourceSpan span, String rawText) implements BoundExpr {
    }

    public record BoundThisExpr(SourceSpan span) implements BoundExpr {
    }

    public record BoundBreakExpr(SourceSpan span) implements BoundExpr {
    }

    public record BoundContinueExpr(SourceSpan span) implements BoundExpr {
    }

    public record BoundUnaryExpr(SourceSpan span, String operator, BoundExpr expression) implements BoundExpr {
    }

    public record BoundBinaryExpr(SourceSpan span, String operator, BoundExpr left, BoundExpr right) implements BoundExpr {
    }

    public record BoundNullCoalesceExpr(SourceSpan span, BoundExpr left, BoundExpr right) implements BoundExpr {
    }

    public record BoundGroupingExpr(SourceSpan span, BoundExpr expression) implements BoundExpr {
    }

    public record BoundArrowAccessExpr(SourceSpan span, BoundExpr left, BoundExpr right) implements BoundExpr {
    }

    public record BoundMemberAccessExpr(SourceSpan span, BoundExpr owner, String memberName) implements BoundExpr {
    }

    public record BoundCallExpr(SourceSpan span, BoundExpr callee, List<BoundExpr> arguments) implements BoundExpr {
        public BoundCallExpr {
            arguments = List.copyOf(arguments);
        }
    }

    public record BoundIndexExpr(SourceSpan span, BoundExpr owner, BoundExpr index) implements BoundExpr {
    }

    public record BoundQueryAccessExpr(
            SourceSpan span,
            BoundExpr access,
            QueryProjectionKind projectionKind
    ) implements BoundExpr {
        public enum QueryProjectionKind {
            PROPERTY,
            EXPLICIT_CALL
        }
    }

    public record BoundAssignmentExpr(
            SourceSpan span,
            BoundExpr target,
            BoundExpr value,
            Optional<String> targetRoot,
            boolean writableTarget
    ) implements BoundExpr {
        public BoundAssignmentExpr {
            targetRoot = targetRoot.map(String::trim).filter(rootName -> !rootName.isEmpty());
        }
    }

    public record BoundBlockExpr(SourceSpan span, List<BoundStmt> statements, boolean returnsLastValue) implements BoundExpr {
        public BoundBlockExpr {
            statements = List.copyOf(statements);
        }

        public BoundBlockExpr(SourceSpan span, List<BoundStmt> statements) {
            this(span, statements, false);
        }
    }

    public record BoundLoopExpr(
            SourceSpan span,
            BoundExpr countExpr,
            BoundBlockExpr body
    ) implements BoundExpr {
    }

    public record BoundForEachExpr(
            SourceSpan span,
            BoundExpr variable,
            BoundExpr collection,
            BoundBlockExpr body
    ) implements BoundExpr {
    }

    public record BoundExprStmt(SourceSpan span, BoundExpr expression) implements BoundStmt {
    }

    public record BoundReturnStmt(SourceSpan span, BoundExpr expression) implements BoundStmt {
    }

    public record BoundTernaryConditionalExpr(
            SourceSpan span,
            BoundExpr condition,
            BoundExpr whenTrue,
            BoundExpr whenFalse
    ) implements BoundExpr {
    }

    public record BoundBinaryConditionalExpr(
            SourceSpan span,
            BoundExpr condition,
            BoundExpr whenTrue
    ) implements BoundExpr {
    }

    public record BoundBreakStmt(
            SourceSpan span,
            @Nullable BoundExpr valueExpr
    ) implements BoundStmt {
    }

    public record BoundContinueStmt(
            SourceSpan span,
            @Nullable BoundExpr valueExpr
    ) implements BoundStmt {
    }
}
