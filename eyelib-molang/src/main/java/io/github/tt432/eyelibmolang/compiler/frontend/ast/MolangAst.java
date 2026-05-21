package io.github.tt432.eyelibmolang.compiler.frontend.ast;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public final class MolangAst {
    private MolangAst() {
    }

    public interface Node {
        SourceSpan span();
    }

    public record ExprSet(SourceSpan span, Expr root) implements Node {
    }

    public interface Expr extends Node {
    }

    public interface Stmt extends Node {
    }

    public record UnknownExpr(SourceSpan span, String text) implements Expr {
    }

    public record IdentifierExpr(SourceSpan span, String name) implements Expr {
    }

    public record NumberLiteralExpr(SourceSpan span, String rawText, double value) implements Expr {
    }

    public record StringLiteralExpr(SourceSpan span, String rawText) implements Expr {
    }

    public record ThisExpr(SourceSpan span) implements Expr {
    }

    public record UnaryExpr(SourceSpan span, String operator, Expr expression) implements Expr {
    }

    public record BinaryExpr(SourceSpan span, String operator, Expr left, Expr right) implements Expr {
    }

    public record AssignmentExpr(SourceSpan span, Expr target, Expr value) implements Expr {
    }

    public record NullCoalesceExpr(SourceSpan span, Expr left, Expr right) implements Expr {
    }

    public record ArrowAccessExpr(SourceSpan span, Expr left, Expr right) implements Expr {
    }

    public record MemberAccessExpr(SourceSpan span, Expr owner, String memberName) implements Expr {
    }

    public record CallExpr(SourceSpan span, Expr callee, List<Expr> arguments) implements Expr {
        public CallExpr {
            arguments = List.copyOf(arguments);
        }
    }

    public record IndexExpr(SourceSpan span, Expr owner, Expr index) implements Expr {
    }

    public record GroupingExpr(SourceSpan span, Expr expression) implements Expr {
    }

    public record BlockExpr(SourceSpan span, List<Stmt> statements) implements Expr {
        public BlockExpr {
            statements = List.copyOf(statements);
        }
    }

    public record LoopExpr(SourceSpan span, String iterationCountRawText, BlockExpr body) implements Expr {
    }

    public record ForEachExpr(SourceSpan span, Expr variable, Expr collection, BlockExpr body) implements Expr {
    }

    public record TernaryConditionalExpr(SourceSpan span, Expr condition, Expr whenTrue, Expr whenFalse) implements Expr {
    }

    public record BinaryConditionalExpr(SourceSpan span, Expr condition, Expr whenFalse) implements Expr {
    }

    public record ExprStmt(SourceSpan span, Expr expression) implements Stmt {
    }

    public record ReturnStmt(SourceSpan span, Expr expression) implements Stmt {
    }

    public record BreakStmt(SourceSpan span) implements Stmt {
    }

    public record ContinueStmt(SourceSpan span) implements Stmt {
    }
}