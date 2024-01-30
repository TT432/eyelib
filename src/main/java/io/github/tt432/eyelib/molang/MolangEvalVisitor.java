package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import io.github.tt432.eyelib.molang.grammer.MolangBaseVisitor;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author TT432
 */
@NoArgsConstructor
public class MolangEvalVisitor extends MolangBaseVisitor<Float> {
    @Getter
    @Setter
    MolangScope scope;

    public MolangEvalVisitor(MolangScope scope) {
        this.scope = scope;
    }

    @Override
    public Float visitExprSet(MolangParser.ExprSetContext ctx) {
        int size = ctx.children.size();

        for (int i = 0; i < size - 1; i++) {
            visit(ctx.children.get(i));
        }

        return visit(ctx.children.get(size - 1));
    }

    @Override
    public Float visitTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx) {
        return visit(ctx.expr(0)) != MolangValue.FALSE ? visit(ctx.expr(1)) : visit(ctx.expr(2));
    }

    @Override
    public Float visitBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx) {
        return visit(ctx.expr(0)) != MolangValue.FALSE ? visit(ctx.expr(1)) : MolangValue.FALSE;
    }

    @Override
    public Float visitSingleSignedAtom(MolangParser.SingleSignedAtomContext ctx) {
        return ctx.op != null && ctx.op.getText().charAt(0) == '-' ? -visit(ctx.atom()) : visit(ctx.atom());
    }

    @Override
    public Float visitComparisonOperator(MolangParser.ComparisonOperatorContext ctx) {
        return switch (ctx.op.getText()) {
            case ">" -> visit(ctx.expr(0)) > visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            case "<=" -> visit(ctx.expr(0)) <= visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            case ">=" -> visit(ctx.expr(0)) >= visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            case "<" -> visit(ctx.expr(0)) < visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            default -> 0F;
        };
    }

    @Override
    public Float visitLogicOperator(MolangParser.LogicOperatorContext ctx) {
        List<MolangParser.ExprContext> expr = ctx.expr();

        return switch (ctx.op.getText().charAt(0)) {
            case '&' ->
                    (visit(expr.get(0)) != MolangValue.FALSE) && (visit(expr.get(1)) != MolangValue.FALSE) ? MolangValue.TRUE : MolangValue.FALSE;
            case '|' ->
                    (visit(expr.get(0)) != MolangValue.FALSE) || (visit(expr.get(1)) != MolangValue.FALSE) ? MolangValue.TRUE : MolangValue.FALSE;
            default -> 0F;
        };
    }

    @Override
    public Float visitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx) {
        Float value = visit(ctx.expr());
        scope.set(ctx.ID().getText(), value);
        return value;
    }

    @Override
    public Float visitMulOrDiv(MolangParser.MulOrDivContext ctx) {
        char c = ctx.op.getText().charAt(0);

        if (c == '/') {
            return visit(ctx.expr(0)) / visit(ctx.expr(1));
        } else if (c == '*') {
            return visit(ctx.expr(0)) * visit(ctx.expr(1));
        }

        return 0F;
    }

    @Override
    public Float visitAddOrSub(MolangParser.AddOrSubContext ctx) {
        char c = ctx.op.getText().charAt(0);

        if (c == '+') {
            return visit(ctx.expr(0)) + visit(ctx.expr(1));
        } else if (c == '-') {
            return visit(ctx.expr(0)) - visit(ctx.expr(1));
        }

        return 0F;
    }

    @Override
    public Float visitNeExpr(MolangParser.NeExprContext ctx) {
        return visit(ctx.expr()) != MolangValue.FALSE ? MolangValue.FALSE : MolangValue.TRUE;
    }

    @Override
    public Float visitReturnOperator(MolangParser.ReturnOperatorContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Float visitEqualsOperator(MolangParser.EqualsOperatorContext ctx) {
        if (ctx.op.getText().charAt(0) == '=') {
            return Objects.equals(visit(ctx.expr(0)), visit(ctx.expr(1))) ? MolangValue.TRUE : MolangValue.FALSE;
        } else {
            return !Objects.equals(visit(ctx.expr(0)), visit(ctx.expr(1))) ? MolangValue.TRUE : MolangValue.FALSE;
        }
    }

    @Override
    public Float visitFunction(MolangParser.FunctionContext ctx) {
        String funcname = ctx.ID().getText();

        if (GlobalMolangFunction.contains(funcname)) {
            List<Object> params = new ArrayList<>();

            for (MolangParser.FuncParamContext funcParamContext : ctx.funcParam()) {
                if (funcParamContext.STRING() != null) {
                    String text = funcParamContext.STRING().getText();
                    params.add(text.substring(1, text.length() - 1));
                } else {
                    visit(funcParamContext.expr());
                }
            }

            return GlobalMolangFunction.get(funcname).invoke(MolangFunctionParameters.upload(scope, params));
        }

        return 0F;
    }

    @Override
    public Float visitVariable(MolangParser.VariableContext ctx) {
        return scope.get(ctx.getText());
    }

    @Override
    public Float visitNumber(MolangParser.NumberContext ctx) {
        // ctx.v 是自定义的变量，重新生成 grammer 后需要
        if (ctx.v == null)
            ctx.v = Float.parseFloat(ctx.getText());

        return ctx.v;
    }

    @Override
    public Float visitParenthesesPrecedence(MolangParser.ParenthesesPrecedenceContext ctx) {
        return visit(ctx.expr());
    }
}
