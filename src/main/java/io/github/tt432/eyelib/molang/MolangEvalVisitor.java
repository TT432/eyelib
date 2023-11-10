package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import io.github.tt432.eyelib.molang.grammer.MolangBaseVisitor;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
        List<Float> result = ctx.expr().stream().map(this::visit).toList();
        return result.get(result.size() - 1);
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
    public Float visitOrOperator(MolangParser.OrOperatorContext ctx) {
        return (visit(ctx.expr(0)) != MolangValue.FALSE || visit(ctx.expr(1)) != MolangValue.FALSE) ? MolangValue.TRUE : MolangValue.FALSE;
    }

    @Override
    public Float visitAndOperator(MolangParser.AndOperatorContext ctx) {
        return (visit(ctx.expr(0)) != MolangValue.FALSE && visit(ctx.expr(1)) != MolangValue.FALSE) ? MolangValue.TRUE : MolangValue.FALSE;
    }

    @Override
    public Float visitSingleSignedAtom(MolangParser.SingleSignedAtomContext ctx) {
        return visit(ctx.signedAtom());
    }

    @Override
    public Float visitComparisonOperator(MolangParser.ComparisonOperatorContext ctx) {
        return switch (ctx.COMPARISON_OPERATOR().getSymbol().getText()) {
            case ">" -> visit(ctx.expr(0)) > visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            case "<=" -> visit(ctx.expr(0)) <= visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            case ">=" -> visit(ctx.expr(0)) >= visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            case "<" -> visit(ctx.expr(0)) < visit(ctx.expr(1)) ? MolangValue.TRUE : MolangValue.FALSE;
            default -> 0F;
        };
    }

    @Override
    public Float visitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx) {
        Float value = visit(ctx.expr());
        scope.set(ctx.variable().getText(), value);
        return value;
    }

    @Override
    public Float visitMulOrDiv(MolangParser.MulOrDivContext ctx) {
        if (ctx.DIV() != null) {
            return visit(ctx.expr(0)) / visit(ctx.expr(1));
        } else if (ctx.MUL() != null) {
            return visit(ctx.expr(0)) * visit(ctx.expr(1));
        }

        return 0F;
    }

    @Override
    public Float visitAddOrSub(MolangParser.AddOrSubContext ctx) {
        if (ctx.ADD() != null) {
            return visit(ctx.expr(0)) + visit(ctx.expr(1));
        } else if (ctx.SUB() != null) {
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
        if (ctx.EQUALS_OPERATOR().getText().equals("==")) {
            return Objects.equals(visit(ctx.expr(0)), visit(ctx.expr(1))) ? MolangValue.TRUE : MolangValue.FALSE;
        } else {
            return !Objects.equals(visit(ctx.expr(0)), visit(ctx.expr(1))) ? MolangValue.TRUE : MolangValue.FALSE;
        }
    }

    @Override
    public Float visitFunction(MolangParser.FunctionContext ctx) {
        return GlobalMolangFunction.contains(ctx.funcname().getText())
                ? GlobalMolangFunction.get(ctx.funcname().getText())
                .invoke(new MolangFunctionParameters(
                        scope,
                        ctx.funcParam()
                                .stream()
                                .map(fpc -> fpc.string() != null ? fpc.string().getText() : (Object) visit(fpc.expr()))
                                .toList()))
                : 0;
    }

    @Override
    public Float visitVariable(MolangParser.VariableContext ctx) {
        return scope.get(ctx.getText());
    }

    @Override
    public Float visitSignedAtom(MolangParser.SignedAtomContext ctx) {
        return ctx.SUB() != null ? -visit(ctx.atom()) : visit(ctx.atom());
    }

    @Override
    public Float visitAtom(MolangParser.AtomContext ctx) {
        if (ctx.function() != null) {
            return visit(ctx.function());
        } else if (ctx.variable() != null) {
            return visit(ctx.variable());
        } else if (ctx.CONSTANT() != null) {
            return scope.get(ctx.CONSTANT().getText());
        } else if (ctx.scientific() != null) {
            return Float.parseFloat(ctx.scientific().getText());
        } else if (ctx.expr() != null) {
            return visit(ctx.expr());
        }

        return 0F;
    }
}
