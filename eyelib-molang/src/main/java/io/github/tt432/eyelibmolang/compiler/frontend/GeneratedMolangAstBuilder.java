package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class GeneratedMolangAstBuilder {
    GeneratedMolangAstBuilder() {
        // Transitional builder keeps generated parser active while adding AST shape contracts.
    }

    MolangAst.ExprSet build(MolangParser.ExprSetContext exprSetContext) {
        MolangAst.Expr root;
        if (exprSetContext instanceof MolangParser.OneExprContext oneExprContext) {
            root = visitExpr(oneExprContext.expr());
        } else if (exprSetContext instanceof MolangParser.BaseContext baseContext) {
            root = visitBase(baseContext);
        } else {
            root = new MolangAst.UnknownExpr(span(exprSetContext), text(exprSetContext));
        }
        return new MolangAst.ExprSet(span(exprSetContext), root);
    }

    private MolangAst.Expr visitBase(MolangParser.BaseContext context) {
        List<MolangParser.ExprContext> expressions = context.expr();
        List<MolangAst.Stmt> statements = new ArrayList<>();
        if (expressions.isEmpty()) {
            return new MolangAst.BlockExpr(span(context), statements);
        }

        for (int i = 0; i < expressions.size() - 1; i++) {
            MolangAst.Expr expression = visitExpr(expressions.get(i));
            statements.add(coerceStatement(expression));
        }

        MolangAst.Expr trailingExpression = visitExpr(expressions.get(expressions.size() - 1));
        if (context.RETURN() != null) {
            SourceSpan returnSpan = SourceSpan.covering(span(context.RETURN().getSymbol()), trailingExpression.span());
            statements.add(new MolangAst.ReturnStmt(returnSpan, trailingExpression));
        } else {
            statements.add(coerceStatement(trailingExpression));
        }

        return new MolangAst.BlockExpr(span(context), statements);
    }

    private MolangAst.Expr visitExpr(MolangParser.ExprContext context) {
        if (context instanceof MolangParser.SignedAtomContext signedAtomContext) {
            return visitSignedAtom(signedAtomContext);
        }
        if (context instanceof MolangParser.AssignmentOperatorContext assignmentOperatorContext) {
            return new MolangAst.AssignmentExpr(
                    span(assignmentOperatorContext),
                    parseIdentifierChain(assignmentOperatorContext.ID().getSymbol()),
                    visitExpr(assignmentOperatorContext.expr())
            );
        }
        if (context instanceof MolangParser.ObjectRefContext objectRefContext) {
            return new MolangAst.ArrowAccessExpr(
                    span(objectRefContext),
                    visitValues(objectRefContext.values()),
                    visitExpr(objectRefContext.expr())
            );
        }
        if (context instanceof MolangParser.NullCoalescingContext nullCoalescingContext) {
            return new MolangAst.NullCoalesceExpr(
                    span(nullCoalescingContext),
                    visitValues(nullCoalescingContext.values()),
                    visitExpr(nullCoalescingContext.expr())
            );
        }
        if (context instanceof MolangParser.AddOrSubContext addOrSubContext) {
            return new MolangAst.BinaryExpr(
                    span(addOrSubContext),
                    operator(addOrSubContext.op),
                    visitExpr(addOrSubContext.expr(0)),
                    visitExpr(addOrSubContext.expr(1))
            );
        }
        if (context instanceof MolangParser.MulOrDivContext mulOrDivContext) {
            return new MolangAst.BinaryExpr(
                    span(mulOrDivContext),
                    operator(mulOrDivContext.op),
                    visitExpr(mulOrDivContext.expr(0)),
                    visitExpr(mulOrDivContext.expr(1))
            );
        }
        if (context instanceof MolangParser.ComparisonOperatorContext comparisonOperatorContext) {
            return new MolangAst.BinaryExpr(
                    span(comparisonOperatorContext),
                    operator(comparisonOperatorContext.op),
                    visitExpr(comparisonOperatorContext.expr(0)),
                    visitExpr(comparisonOperatorContext.expr(1))
            );
        }
        if (context instanceof MolangParser.EqualsOperatorContext equalsOperatorContext) {
            return new MolangAst.BinaryExpr(
                    span(equalsOperatorContext),
                    operator(equalsOperatorContext.op),
                    visitExpr(equalsOperatorContext.expr(0)),
                    visitExpr(equalsOperatorContext.expr(1))
            );
        }
        if (context instanceof MolangParser.AndOperatorContext andOperatorContext) {
            return new MolangAst.BinaryExpr(
                    span(andOperatorContext),
                    "&&",
                    visitExpr(andOperatorContext.expr(0)),
                    visitExpr(andOperatorContext.expr(1))
            );
        }
        if (context instanceof MolangParser.OrOperatorContext orOperatorContext) {
            return new MolangAst.BinaryExpr(
                    span(orOperatorContext),
                    "||",
                    visitExpr(orOperatorContext.expr(0)),
                    visitExpr(orOperatorContext.expr(1))
            );
        }
        if (context instanceof MolangParser.BinaryConditionalOperatorContext binaryConditionalOperatorContext) {
            return new MolangAst.BinaryConditionalExpr(
                    span(binaryConditionalOperatorContext),
                    visitExpr(binaryConditionalOperatorContext.expr(0)),
                    visitExpr(binaryConditionalOperatorContext.expr(1))
            );
        }
        if (context instanceof MolangParser.TernaryConditionalOperatorContext ternaryConditionalOperatorContext) {
            return new MolangAst.TernaryConditionalExpr(
                    span(ternaryConditionalOperatorContext),
                    visitExpr(ternaryConditionalOperatorContext.expr(0)),
                    visitExpr(ternaryConditionalOperatorContext.expr(1)),
                    visitExpr(ternaryConditionalOperatorContext.expr(2))
            );
        }
        if (context instanceof MolangParser.LoopContext loopContext) {
            return new MolangAst.LoopExpr(
                    span(loopContext),
                    loopContext.SCIENTIFIC_NUMBER().getText(),
                    coerceBlockExpr(build(loopContext.exprSet()).root(), span(loopContext.exprSet()))
            );
        }
        if (context instanceof MolangParser.ScopedExprSetContext scopedExprSetContext) {
            return coerceBlockExpr(
                    build(scopedExprSetContext.exprSet()).root(),
                    span(scopedExprSetContext)
            );
        }
        if (context instanceof MolangParser.StringValueContext stringValueContext) {
            return new MolangAst.StringLiteralExpr(
                    span(stringValueContext),
                    stringValueContext.STRING().getText()
            );
        }
        if (context instanceof MolangParser.NeExprContext neExprContext) {
            return new MolangAst.UnaryExpr(
                    span(neExprContext),
                    "!",
                    visitExpr(neExprContext.expr())
            );
        }
        return new MolangAst.UnknownExpr(span(context), text(context));
    }

    private MolangAst.BlockExpr coerceBlockExpr(MolangAst.Expr expression, SourceSpan span) {
        if (expression instanceof MolangAst.BlockExpr blockExpr) {
            return new MolangAst.BlockExpr(span, blockExpr.statements());
        }
        return new MolangAst.BlockExpr(span, List.of(coerceStatement(expression)));
    }

    private MolangAst.Stmt coerceStatement(MolangAst.Expr expression) {
        if (expression instanceof MolangAst.IdentifierExpr identifierExpr) {
            if ("break".equals(identifierExpr.name())) {
                return new MolangAst.BreakStmt(identifierExpr.span());
            }
            if ("continue".equals(identifierExpr.name())) {
                return new MolangAst.ContinueStmt(identifierExpr.span());
            }
        }

        return new MolangAst.ExprStmt(expression.span(), expression);
    }

    private MolangAst.Expr visitSignedAtom(MolangParser.SignedAtomContext context) {
        MolangAst.Expr atomExpr = visitAtom(context.atom());
        if (context.op == null) {
            return atomExpr;
        }
        return new MolangAst.UnaryExpr(span(context), operator(context.op), atomExpr);
    }

    private MolangAst.Expr visitAtom(MolangParser.AtomContext context) {
        if (context instanceof MolangParser.NumberContext numberContext) {
            return new MolangAst.NumberLiteralExpr(
                    span(numberContext),
                    numberContext.SCIENTIFIC_NUMBER().getText(),
                    parseNumber(numberContext.SCIENTIFIC_NUMBER().getText())
            );
        }
        if (context instanceof MolangParser.ThisContext thisContext) {
            return new MolangAst.ThisExpr(span(thisContext));
        }
        if (context instanceof MolangParser.ValueContext valueContext) {
            return visitValues(valueContext.values());
        }
        if (context instanceof MolangParser.ParenthesesPrecedenceContext parenthesesPrecedenceContext) {
            return new MolangAst.GroupingExpr(
                    span(parenthesesPrecedenceContext),
                    visitExpr(parenthesesPrecedenceContext.expr())
            );
        }
        return new MolangAst.UnknownExpr(span(context), text(context));
    }

    private MolangAst.Expr visitValues(MolangParser.ValuesContext context) {
        if (context instanceof MolangParser.VariableContext variableContext) {
            return parseIdentifierChain(variableContext.ID().getSymbol());
        }
        if (context instanceof MolangParser.FunctionContext functionContext) {
            String functionName = functionContext.ID().getText();
            List<MolangParser.ExprContext> functionArguments = functionContext.expr();
            if ("for_each".equals(functionName) && functionArguments.size() == 3) {
                MolangParser.ExprContext bodyArgument = functionArguments.get(2);
                return new MolangAst.ForEachExpr(
                        span(functionContext),
                        visitExpr(functionArguments.get(0)),
                        visitExpr(functionArguments.get(1)),
                        coerceBlockExpr(visitExpr(bodyArgument), span(bodyArgument))
                );
            }

            List<MolangAst.Expr> arguments = new ArrayList<>();
            for (MolangParser.ExprContext expressionContext : functionArguments) {
                arguments.add(visitExpr(expressionContext));
            }
            return new MolangAst.CallExpr(
                    span(functionContext),
                    parseIdentifierChain(functionContext.ID().getSymbol()),
                    arguments
            );
        }
        if (context instanceof MolangParser.AccessArrayContext accessArrayContext) {
            return new MolangAst.IndexExpr(
                    span(accessArrayContext),
                    visitValues(accessArrayContext.values()),
                    visitExpr(accessArrayContext.expr())
            );
        }
        return new MolangAst.UnknownExpr(span(context), text(context));
    }

    private MolangAst.Expr parseIdentifierChain(Token token) {
        String rawText = token.getText();
        if (rawText == null || rawText.isBlank()) {
            return new MolangAst.UnknownExpr(span(token), "");
        }

        if (!rawText.contains(".")) {
            return new MolangAst.IdentifierExpr(span(token), rawText);
        }

        List<Segment> segments = splitSegments(rawText);
        if (segments.isEmpty()) {
            return new MolangAst.UnknownExpr(span(token), rawText);
        }

        Segment first = segments.get(0);
        MolangAst.Expr owner = new MolangAst.IdentifierExpr(
                subSpan(token, first.startInclusive(), first.endExclusive()),
                first.text()
        );

        for (int i = 1; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            SourceSpan memberTextSpan = subSpan(token, segment.startInclusive(), segment.endExclusive());
            owner = new MolangAst.MemberAccessExpr(
                    SourceSpan.covering(owner.span(), memberTextSpan),
                    owner,
                    segment.text()
            );
        }

        return owner;
    }

    private List<Segment> splitSegments(String rawText) {
        List<Segment> segments = new ArrayList<>();
        int start = 0;
        for (int index = 0; index <= rawText.length(); index++) {
            if (index == rawText.length() || rawText.charAt(index) == '.') {
                if (index > start) {
                    segments.add(new Segment(start, index, rawText.substring(start, index)));
                }
                start = index + 1;
            }
        }
        return segments;
    }

    private SourceSpan subSpan(Token token, int relativeStartInclusive, int relativeEndExclusive) {
        if (relativeEndExclusive <= relativeStartInclusive) {
            return span(token);
        }
        int startIndex = token.getStartIndex() + relativeStartInclusive;
        int stopIndexInclusive = token.getStartIndex() + relativeEndExclusive - 1;
        int startColumn = token.getCharPositionInLine() + relativeStartInclusive;
        int endColumnExclusive = token.getCharPositionInLine() + relativeEndExclusive;
        return new SourceSpan(
                startIndex,
                stopIndexInclusive,
                token.getLine(),
                startColumn,
                token.getLine(),
                endColumnExclusive
        );
    }

    private SourceSpan span(@Nullable ParserRuleContext context) {
        if (context == null) {
            return SourceSpan.unknown();
        }
        return span(context.getStart(), context.getStop());
    }

    private SourceSpan span(@Nullable Token token) {
        if (token == null) {
            return SourceSpan.unknown();
        }
        return span(token, token);
    }

    private SourceSpan span(@Nullable Token start, @Nullable Token stop) {
        if (start == null || stop == null) {
            return SourceSpan.unknown();
        }
        int startIndex = start.getStartIndex();
        int stopIndexInclusive = stop.getStopIndex();
        int startLine = start.getLine();
        int startColumn = start.getCharPositionInLine();
        int endLine = stop.getLine();
        int endColumnExclusive = stop.getCharPositionInLine() + tokenLength(stop);
        return new SourceSpan(startIndex, stopIndexInclusive, startLine, startColumn, endLine, endColumnExclusive);
    }

    private String operator(@Nullable Token token) {
        if (token == null || token.getText() == null) {
            return "";
        }
        return token.getText();
    }

    private int tokenLength(Token token) {
        String text = token.getText();
        if (text == null || text.isEmpty()) {
            return 1;
        }
        return text.length();
    }

    private String text(ParserRuleContext context) {
        String value = context.getText();
        if (value == null) {
            return "";
        }
        return value;
    }

    private double parseNumber(String rawText) {
        try {
            return Double.parseDouble(rawText);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private record Segment(int startInclusive, int endExclusive, String text) {
    }
}
