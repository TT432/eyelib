package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangObject;
import io.github.tt432.eyelibmolang.type.MolangString;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MolangConstantExpressionEvaluator {
    /**
     * Attempts to evaluate a Molang expression at compile time if it
     * consists entirely of constant expressions (no variables, queries, or calls).
     */
    public static Optional<MolangObject> tryEvaluate(String expression) {
        return HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(expression)
                                                          .flatMap(ast -> evaluateConstantExpr(ast.root()));
    }

    public static Optional<Float> tryEvaluateNumber(String expression) {
        return HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(expression)
                                                          .flatMap(ast -> evaluateNumber(ast.root()));
    }

    private static Optional<Float> evaluateNumber(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.NumberLiteralExpr numberLiteralExpr) {
            return Optional.of((float) numberLiteralExpr.value());
        }
        if (expr instanceof MolangAst.GroupingExpr groupingExpr) {
            return evaluateNumber(groupingExpr.expression());
        }
        if (expr instanceof MolangAst.UnaryExpr unaryExpr) {
            Optional<Float> value = evaluateNumber(unaryExpr.expression());
            if (value.isEmpty()) {
                return Optional.empty();
            }
            return switch (unaryExpr.operator()) {
                case "+" -> value;
                case "-" -> Optional.of(-value.orElseThrow());
                default -> Optional.empty();
            };
        }
        if (expr instanceof MolangAst.BinaryExpr binaryExpr) {
            Optional<Float> left = evaluateNumber(binaryExpr.left());
            Optional<Float> right = evaluateNumber(binaryExpr.right());
            if (left.isEmpty() || right.isEmpty()) {
                return Optional.empty();
            }
            float l = left.orElseThrow();
            float r = right.orElseThrow();
            return switch (binaryExpr.operator()) {
                case "+" -> Optional.of(l + r);
                case "-" -> Optional.of(l - r);
                case "*" -> Optional.of(l * r);
                case "/" -> r == 0F ? Optional.empty() : Optional.of(l / r);
                default -> Optional.empty();
            };
        }
        if (expr instanceof MolangAst.CallExpr callExpr
                && callExpr.arguments().size() == 1
                && callExpr.callee() instanceof MolangAst.MemberAccessExpr memberAccessExpr
                && memberAccessExpr.owner() instanceof MolangAst.IdentifierExpr identifierExpr
                && "math".equalsIgnoreCase(identifierExpr.name())
                && "abs".equalsIgnoreCase(memberAccessExpr.memberName())) {
            return evaluateNumber(callExpr.arguments().get(0)).map(Math::abs);
        }
        return Optional.empty();
    }

    /**
     * Evaluates a compile-time constant expression, returning the full MolangObject result.
     * Handles number literals, string literals, unary/binary/grouping, ternary conditionals,
     * and simple math.abs calls.
     */
    private static Optional<MolangObject> evaluateConstantExpr(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.NumberLiteralExpr numberLiteralExpr) {
            return Optional.of(MolangFloat.valueOf((float) numberLiteralExpr.value()));
        }
        if (expr instanceof MolangAst.StringLiteralExpr stringLiteralExpr) {
            String raw = stringLiteralExpr.rawText();
            String unquoted = raw.length() >= 2
                    ? raw.substring(1, raw.length() - 1)
                    : raw;
            return Optional.of(MolangString.valueOf(unquoted));
        }
        if (expr instanceof MolangAst.GroupingExpr groupingExpr) {
            return evaluateConstantExpr(groupingExpr.expression());
        }
        if (expr instanceof MolangAst.UnaryExpr unaryExpr) {
            Optional<MolangObject> operand = evaluateConstantExpr(unaryExpr.expression());
            if (operand.isEmpty()) return Optional.empty();
            float val = operand.get().asFloat();
            return switch (unaryExpr.operator()) {
                case "+" -> Optional.of(MolangFloat.valueOf(val));
                case "-" -> Optional.of(MolangFloat.valueOf(-val));
                case "!" -> Optional.of(MolangFloat.valueOf(val == 0));
                default -> Optional.empty();
            };
        }
        if (expr instanceof MolangAst.BinaryExpr binaryExpr) {
            // Only pure arithmetic/comparison on number constants can be folded
            Optional<Float> result = evaluateNumber(binaryExpr);
            return result.map(MolangFloat::valueOf);
        }
        if (expr instanceof MolangAst.TernaryConditionalExpr ternary) {
            Optional<MolangObject> condition = evaluateConstantExpr(ternary.condition());
            if (condition.isEmpty()) return Optional.empty();
            if (condition.get().asBoolean()) {
                return evaluateConstantExpr(ternary.whenTrue());
            } else {
                return evaluateConstantExpr(ternary.whenFalse());
            }
        }
        if (expr instanceof MolangAst.BinaryConditionalExpr binaryCond) {
            Optional<MolangObject> condition = evaluateConstantExpr(binaryCond.condition());
            if (condition.isEmpty()) return Optional.empty();
            if (condition.get().asBoolean()) {
                return evaluateConstantExpr(binaryCond.condition());
            } else {
                return evaluateConstantExpr(binaryCond.whenFalse());
            }
        }
        // Non-constant: identifiers, calls with variables, member access, etc.
        return Optional.empty();
    }
}
