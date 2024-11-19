package io.github.tt432.eyelib.molang;


import io.github.tt432.eyelib.molang.grammer.MolangBaseVisitor;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author TT432
 */
public class MolangCompileVisitor extends MolangBaseVisitor<String> {
    private static String alias(String sourceName) {
        return switch (sourceName.toLowerCase(Locale.ROOT)) {
            case "c" -> "context";
            case "m" -> "math";
            case "t" -> "temp";
            case "q" -> "query";
            default -> sourceName;
        };
    }

    private static String rename(String name) {
        int i = name.indexOf(".");

        if (i != -1) {
            return (alias(name.substring(0, i)) + name.substring(i)).toLowerCase(Locale.ROOT);
        }

        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public String visitBase(MolangParser.BaseContext ctx) {
        StringBuilder result = new StringBuilder();
        int size = ctx.children.size();

        for (int i = 0; i < size; i++) {
            result.append(visit(ctx.children.get(i)));
        }

        String string = result.toString();

        String[] split = string.split(";;");

        if (split.length == 0) return "return 0F;";

        for (int i = 0; i < split.length; i++) {
            if (split[i].isEmpty() || split[i].equals("null")) {
                split[i] = "0F";
            }
        }

        StringBuilder r = new StringBuilder();

        for (String s : split) {
            if (!r.isEmpty()) {
                r.append(", ");
            }

            r.append(s);
        }

        if (ctx.RETURN() == null) {
            r.append(", 0.0F");
        }

        return "io.github.tt432.eyelib.util.EyelibUtils.blackhole(" + r.append(")");
    }

    @Override
    public String visitOneExpr(MolangParser.OneExprContext ctx) {
        return "io.github.tt432.eyelib.util.EyelibUtils.blackhole(" + visit(ctx.expr()) + ")";
    }

    @Override
    public String visitScopedExprSet(MolangParser.ScopedExprSetContext ctx) {
        return visit(ctx.exprSet());
    }

    @Override
    public String visitTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx) {
        return "((" + visit(ctx.expr(0)) + ") != 0F ? " + visit(ctx.expr(1)) + " : " + visit(ctx.expr(2)) + ")";
    }

    @Override
    public String visitBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx) {
        return "((" + visit(ctx.expr(0)) + ") != 0F ? " + visit(ctx.expr(1)) + " : 0F)";
    }

    @Override
    public String visitSignedAtom(MolangParser.SignedAtomContext ctx) {
        if (ctx.op != null && ctx.op.getText().charAt(0) == '-') {
            return "-" + visit(ctx.atom());
        } else {
            return visit(ctx.atom());
        }
    }

    @Override
    public String visitComparisonOperator(MolangParser.ComparisonOperatorContext ctx) {
        return "(" + visit(ctx.expr(0)) + " " + ctx.op.getText() + " " + visit(ctx.expr(1)) + " ? 1F : 0F)";
    }

    @Override
    public String visitLogicOperator(MolangParser.LogicOperatorContext ctx) {
        List<MolangParser.ExprContext> expr = ctx.expr();

        return "(((" + visit(expr.getFirst()) + ") != 0F) " + ctx.op.getText() + " ((" + visit(expr.get(1)) + ") != 0F) ? 1F : 0F)";
    }

    @Override
    public String visitNullCoalescing(MolangParser.NullCoalescingContext ctx) {
        return "$1.contains(\"" + ctx.values() + "\") ? (" + visit(ctx.values()) + ") : (" + visit(ctx.expr()) + ")";
    }

    @Override
    public String visitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx) {
        return "($1.set(\"" + rename(ctx.ID().getText()) + "\", " + visit(ctx.expr()) + "))";
    }

    @Override
    public String visitMulOrDiv(MolangParser.MulOrDivContext ctx) {
        return "(" + visit(ctx.expr(0)) + " " + ctx.op.getText().charAt(0) + " " + visit(ctx.expr(1)) + ")";
    }

    @Override
    public String visitAddOrSub(MolangParser.AddOrSubContext ctx) {
        return "(" + visit(ctx.expr(0)) + " " + ctx.op.getText().charAt(0) + " " + visit(ctx.expr(1)) + ")";
    }

    @Override
    public String visitNeExpr(MolangParser.NeExprContext ctx) {
        return "((" + visit(ctx.expr()) + ") != 0F ? 0F : 1F)";
    }

    @Override
    public String visitEqualsOperator(MolangParser.EqualsOperatorContext ctx) {
        return "(" + visit(ctx.expr(0)) + " " + ctx.op.getText() + " " + visit(ctx.expr(1)) + " ? 1F : 0F)";
    }

    @Override
    public String visitFunction(MolangParser.FunctionContext ctx) {
        List<String> params = new ArrayList<>();

        for (var expr : ctx.expr()) {
            if (expr instanceof MolangParser.ScopedExprSetContext) {
                params.add("""
                        new java.lang.Runnable() {
                            @Override
                            public void run() {
                                %s;
                            }
                        }
                        """.formatted(visit(expr)));
            } else {
                params.add(visit(expr));
            }
        }

        String joined = String.join(",", params);

        String methodName = rename(ctx.ID().getText());
        return MolangMappingTree.INSTANCE.findMethod(methodName, joined);
    }

    @Override
    public String visitStringValue(MolangParser.StringValueContext ctx) {
        String text = ctx.STRING().getText();
        return "\"" + text.substring(1, text.length() - 1) + "\"";
    }

    @Override
    public String visitVariable(MolangParser.VariableContext ctx) {
        String fieldName = rename(ctx.getText());
        String field = MolangMappingTree.INSTANCE.findField(fieldName);

        if (!field.equals("0F")) {
            return field;
        } else {
            String method = MolangMappingTree.INSTANCE.findMethod(fieldName, "");

            if (!method.equals("0F")) {
                return method;
            } else {
                return "$1.get(\"" + fieldName + "\")";
            }
        }
    }

    @Override
    public String visitNumber(MolangParser.NumberContext ctx) {
        return "((float) " + ctx.getText() + ")";
    }

    @Override
    public String visitParenthesesPrecedence(MolangParser.ParenthesesPrecedenceContext ctx) {
        return "(" + visit(ctx.expr()) + ")";
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        return ";;";
    }
}
