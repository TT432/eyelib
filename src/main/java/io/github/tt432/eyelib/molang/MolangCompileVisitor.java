package io.github.tt432.eyelib.molang;


import io.github.tt432.eyelib.molang.grammer.MolangBaseVisitor;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

/**
 * @author TT432
 */
public class MolangCompileVisitor extends MolangBaseVisitor<String> {
    @Override
    public String visitExprSet(MolangParser.ExprSetContext ctx) {
        StringBuilder result = new StringBuilder();
        int size = ctx.children.size();

        for (int i = 0; i < size; i++) {
            result.append(visit(ctx.children.get(i)));
        }

        String string = result.toString();

        String[] split = string.split(";");

        for (int i = 0; i < split.length; i++) {
            if (split[i].isEmpty() || split[i].equals("null")) {
                split[i] = "0F";
            }
        }

        StringBuilder r = new StringBuilder();

        for (int i = 0; i < split.length - 1; i++) {
            r.append("emptyEval(${split[i]});");
        }

        r.append("return ").append(split[split.length - 1]).append(";");

        return r.toString();
    }

    @Override
    public String visitTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx) {
        return "((${visit(ctx.expr(0))}) != 0F ? ${visit(ctx.expr(1))} : ${visit(ctx.expr(2))})";
    }

    @Override
    public String visitBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx) {
        return "((${visit(ctx.expr(0))}) != 0F ? ${visit(ctx.expr(1))} : 0F)";
    }

    @Override
    public String visitSingleSignedAtom(MolangParser.SingleSignedAtomContext ctx) {
        if (ctx.op != null && ctx.op.getText().charAt(0) == '-') {
            return "-${visit(ctx.atom())}";
        } else {
            return visit(ctx.atom());
        }
    }

    @Override
    public String visitComparisonOperator(MolangParser.ComparisonOperatorContext ctx) {
        return "(${visit(ctx.expr(0))} ${ctx.op.getText()} ${visit(ctx.expr(1))} ? 1F : 0F)";
    }

    @Override
    public String visitLogicOperator(MolangParser.LogicOperatorContext ctx) {
        List<MolangParser.ExprContext> expr = ctx.expr();

        return "(((${visit(expr.get(0))}) != 0F) ${ctx.op.getText()} ((${visit(expr.get(1))}) != 0F) ? 1F : 0F)";
    }

    @Override
    public String visitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx) {
        return "($1.set(\"${ctx.ID().getText()}\", ${visit(ctx.expr())}))";
    }

    @Override
    public String visitMulOrDiv(MolangParser.MulOrDivContext ctx) {
        return "(${visit(ctx.expr(0))} ${ctx.op.getText().charAt(0)} ${visit(ctx.expr(1))})";
    }

    @Override
    public String visitAddOrSub(MolangParser.AddOrSubContext ctx) {
        return "(${visit(ctx.expr(0))} ${ctx.op.getText().charAt(0)} ${visit(ctx.expr(1))})";
    }

    @Override
    public String visitNeExpr(MolangParser.NeExprContext ctx) {
        return "((${visit(ctx.expr())}) != 0F ? 0F : 1F)";
    }

    @Override
    public String visitReturnOperator(MolangParser.ReturnOperatorContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public String visitEqualsOperator(MolangParser.EqualsOperatorContext ctx) {
        return "(${visit(ctx.expr(0))} ${ctx.op.getText()} ${visit(ctx.expr(1))} ? 1F : 0F)";
    }

    @Override
    public String visitFunction(MolangParser.FunctionContext ctx) {
        StringBuilder builder = new StringBuilder();

        for (MolangParser.FuncParamContext funcParamContext : ctx.funcParam()) {
            if (funcParamContext.STRING() != null) {
                builder.append(funcParamContext.STRING().getText()).append(",");
            } else {
                builder.append(visit(funcParamContext.expr())).append(",");
            }
        }

        return "(MolangFunctionHandler.tryExecuteFunction(\"${ctx.ID().getText()}\", $1, new Object[] {${builder.toString()}}}))";
    }

    @Override
    public String visitVariable(MolangParser.VariableContext ctx) {
        return "$1.get(\"${ctx.getText()}\")";
    }

    @Override
    public String visitNumber(MolangParser.NumberContext ctx) {
        return "((float) ${ctx.getText()})";
    }

    @Override
    public String visitParenthesesPrecedence(MolangParser.ParenthesesPrecedenceContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        return ";";
    }
}
