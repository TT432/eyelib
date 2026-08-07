package io.github.tt432.eyelib.molang.compiler.frontend;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.SourceSpan;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 手写 AST 解析前端，用于常量折叠等阶段。
 *
 * @author TT432
 */
public final class HandwrittenMolangAstParserFrontend implements MolangParserFrontend {

    public HandwrittenMolangAstParserFrontend() {
    }

    @Override
    public MolangParserFrontendResult parseExprSet(String source) {
        return new MolangParserFrontendResult(parseExprSetAst(source));
    }

    public Optional<MolangAst.ExprSet> parseExprSetAst(String source) {
        if (source.isBlank()) {
            MolangAst.NumberLiteralExpr zero = new MolangAst.NumberLiteralExpr(
                    SourceSpan.unknown(), "0", 0.0);
            return Optional.of(new MolangAst.ExprSet(zero.span(), zero));
        }
        try {
            Parser parser = new Parser(source);
            return Optional.of(parser.parseExprSet());
        } catch (ParseException | MolangTokenizer.MolangTokenizeException parseException) {
            return Optional.empty();
        }
    }

    private static final class Parser {
        private final List<MolangToken> tokens;
        private final String source;
        private int position;

        private Parser(String source) {
            this.tokens = MolangTokenizer.tokenize(source);
            this.source = source;
            this.position = 0;
        }

        private MolangAst.ExprSet parseExprSet() {
            ParseBlockResult topLevel = parseStatements(MolangTokenKind.EOF);
            if (topLevel.statements.size() == 1 && !topLevel.sawSemicolon) {
                MolangAst.Stmt onlyStatement = topLevel.statements.get(0);
                if (onlyStatement instanceof MolangAst.ExprStmt exprStmt) {
                    return new MolangAst.ExprSet(exprStmt.expression().span(), exprStmt.expression());
                }
            }

            SourceSpan exprSetSpan = topLevel.statements.isEmpty()
                    ? SourceSpan.unknown()
                    : SourceSpan.covering(topLevel.statements.get(0).span(), topLevel.statements.get(topLevel.statements.size() - 1).span());
            MolangAst.BlockExpr blockExpr = new MolangAst.BlockExpr(exprSetSpan, topLevel.statements, true);
            return new MolangAst.ExprSet(blockExpr.span(), blockExpr);
        }

        private ParseBlockResult parseStatements(MolangTokenKind terminator) {
            List<MolangAst.Stmt> statements = new ArrayList<>();
            boolean sawSemicolon = false;

            while (!check(terminator) && !check(MolangTokenKind.EOF)) {
                if (match(MolangTokenKind.SEMICOLON)) {
                    sawSemicolon = true;
                    continue;
                }

                if (match(MolangTokenKind.RETURN)) {
                    MolangToken returnToken = previous();
                    MolangAst.Expr returnExpression = parseExpression();
                    SourceSpan returnSpan = SourceSpan.covering(span(returnToken), returnExpression.span());
                    statements.add(new MolangAst.ReturnStmt(returnSpan, returnExpression));
                } else if (match(MolangTokenKind.BREAK)) {
                    MolangToken breakToken = previous();
                    MolangAst.Expr valueExpr = parseOptionalControlFlowValue(terminator);
                    SourceSpan breakSpan = valueExpr == null ? span(breakToken) : SourceSpan.covering(span(breakToken), valueExpr.span());
                    statements.add(new MolangAst.BreakStmt(breakSpan, valueExpr));
                } else if (match(MolangTokenKind.CONTINUE)) {
                    MolangToken continueToken = previous();
                    MolangAst.Expr valueExpr = parseOptionalControlFlowValue(terminator);
                    SourceSpan continueSpan = valueExpr == null ? span(continueToken) : SourceSpan.covering(span(continueToken), valueExpr.span());
                    statements.add(new MolangAst.ContinueStmt(continueSpan, valueExpr));
                } else {
                    MolangAst.Expr expression = parseExpression();
                    statements.add(new MolangAst.ExprStmt(expression.span(), expression));
                }

                if (match(MolangTokenKind.SEMICOLON)) {
                    sawSemicolon = true;
                } else if (!check(terminator) && !check(MolangTokenKind.EOF)) {
                    throw error("Expected ';' between statements.");
                }
            }

            return new ParseBlockResult(statements, sawSemicolon);
        }

        private MolangAst.Expr parseExpression() {
            return parseAssignment();
        }

        private MolangAst.Expr parseAssignment() {
            MolangAst.Expr left = parseTernary();
            if (!match(MolangTokenKind.EQUAL)) {
                return left;
            }

            MolangAst.Expr right = parseAssignment();
            return new MolangAst.AssignmentExpr(SourceSpan.covering(left.span(), right.span()), left, right);
        }

        private MolangAst.Expr parseTernary() {
            MolangAst.Expr condition = parseNullCoalesce();
            if (!match(MolangTokenKind.QUESTION)) {
                return condition;
            }

            MolangAst.Expr whenTrue = parseExpression();
            if (match(MolangTokenKind.COLON)) {
                MolangAst.Expr whenFalse = parseExpression();
                return new MolangAst.TernaryConditionalExpr(
                        SourceSpan.covering(condition.span(), whenFalse.span()),
                        condition,
                        whenTrue,
                        whenFalse
                );
            }

            return new MolangAst.BinaryConditionalExpr(
                    SourceSpan.covering(condition.span(), whenTrue.span()),
                    condition,
                    whenTrue
            );
        }

        private MolangAst.Expr parseNullCoalesce() {
            MolangAst.Expr expression = parseOr();
            while (match(MolangTokenKind.QUESTION_QUESTION)) {
                MolangAst.Expr right = parseOr();
                expression = new MolangAst.NullCoalesceExpr(SourceSpan.covering(expression.span(), right.span()), expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseAnd() {
            MolangAst.Expr expression = parseEquality();
            while (match(MolangTokenKind.AND_AND)) {
                MolangToken operator = previous();
                MolangAst.Expr right = parseEquality();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme(), expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseOr() {
            MolangAst.Expr expression = parseAnd();
            while (match(MolangTokenKind.OR_OR)) {
                MolangToken operator = previous();
                MolangAst.Expr right = parseAnd();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme(), expression, right);
            }
            return expression;
        }
        private MolangAst.Expr parseComparison() {
            MolangAst.Expr expression = parseAdd();
            while (match(MolangTokenKind.GREATER, MolangTokenKind.GREATER_EQUAL, MolangTokenKind.LESS, MolangTokenKind.LESS_EQUAL)) {
                MolangToken operator = previous();
                MolangAst.Expr right = parseAdd();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme(), expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseEquality() {
            MolangAst.Expr expression = parseComparison();
            while (match(MolangTokenKind.EQUAL_EQUAL, MolangTokenKind.BANG_EQUAL)) {
                MolangToken operator = previous();
                MolangAst.Expr right = parseComparison();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme(), expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseAdd() {
            MolangAst.Expr expression = parseMultiply();
            while (match(MolangTokenKind.PLUS, MolangTokenKind.MINUS)) {
                MolangToken operator = previous();
                MolangAst.Expr right = parseMultiply();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme(), expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseMultiply() {
            MolangAst.Expr expression = parseUnary();
            while (match(MolangTokenKind.STAR, MolangTokenKind.SLASH)) {
                MolangToken operator = previous();
                MolangAst.Expr right = parseUnary();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme(), expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseUnary() {
            if (match(MolangTokenKind.MINUS, MolangTokenKind.BANG)) {
                MolangToken operator = previous();
                MolangAst.Expr expression = parseUnary();
                return new MolangAst.UnaryExpr(SourceSpan.covering(span(operator), expression.span()), operator.lexeme(), expression);
            }
            return parsePostfix();
        }

        private MolangAst.Expr parsePostfix() {
            MolangAst.Expr expression = parsePrimary();

            while (true) {
                if (match(MolangTokenKind.DOT)) {
                    MolangToken member = consume(MolangTokenKind.IDENTIFIER, "Expected member name after '.'.");
                    expression = new MolangAst.MemberAccessExpr(SourceSpan.covering(expression.span(), span(member)), expression, member.lexeme());
                    continue;
                }

                if (match(MolangTokenKind.ARROW)) {
                    MolangToken owner = consume(MolangTokenKind.IDENTIFIER, "Expected owner root after '->'.");
                    consume(MolangTokenKind.DOT, "Expected '.' after arrow owner.");
                    MolangToken member = consume(MolangTokenKind.IDENTIFIER, "Expected member name after arrow owner.");
                    MolangAst.MemberAccessExpr right = new MolangAst.MemberAccessExpr(
                            SourceSpan.covering(span(owner), span(member)),
                            new MolangAst.IdentifierExpr(span(owner), owner.lexeme()),
                            member.lexeme()
                    );
                    expression = new MolangAst.ArrowAccessExpr(SourceSpan.covering(expression.span(), span(member)), expression, right);
                    continue;
                }

                if (match(MolangTokenKind.LEFT_PAREN)) {
                    List<MolangAst.Expr> arguments = new ArrayList<>();
                    if (!check(MolangTokenKind.RIGHT_PAREN)) {
                        do {
                            arguments.add(parseExpression());
                        } while (match(MolangTokenKind.COMMA));
                    }
                    MolangToken rightParen = consume(MolangTokenKind.RIGHT_PAREN, "Expected ')' after call arguments.");

                    SourceSpan callSpan = SourceSpan.covering(expression.span(), span(rightParen));
                    expression = new MolangAst.CallExpr(callSpan, expression, arguments);
                    continue;
                }

                if (match(MolangTokenKind.LEFT_BRACKET)) {
                    MolangAst.Expr index = parseExpression();
                    MolangToken rightBracket = consume(MolangTokenKind.RIGHT_BRACKET, "Expected ']' after index expression.");
                    expression = new MolangAst.IndexExpr(SourceSpan.covering(expression.span(), span(rightBracket)), expression, index);
                    continue;
                }

                break;
            }

            return expression;
        }

        private MolangAst.Expr parsePrimary() {
            if (match(MolangTokenKind.NUMBER)) {
                MolangToken number = previous();
                return new MolangAst.NumberLiteralExpr(span(number), number.lexeme(), Double.parseDouble(number.lexeme()));
            }

            if (match(MolangTokenKind.IDENTIFIER)) {
                MolangToken identifier = previous();
                String normalizedIdentifier = identifier.lexeme().toLowerCase(java.util.Locale.ROOT);
                if ("this".equals(normalizedIdentifier)) {
                    return new MolangAst.ThisExpr(span(identifier));
                }
                if ("loop".equals(normalizedIdentifier) && check(MolangTokenKind.LEFT_PAREN)) {
                    return parseLoopControlForm(identifier);
                }
                if ("for_each".equals(normalizedIdentifier) && check(MolangTokenKind.LEFT_PAREN)) {
                    return parseForEachControlForm(identifier);
                }
                return new MolangAst.IdentifierExpr(span(identifier), identifier.lexeme());
            }

            if (match(MolangTokenKind.STRING)) {
                MolangToken string = previous();
                return new MolangAst.StringLiteralExpr(span(string), string.lexeme());
            }

            if (match(MolangTokenKind.LEFT_PAREN)) {
                MolangToken leftParen = previous();
                MolangAst.Expr expression = parseExpression();
                MolangToken rightParen = consume(MolangTokenKind.RIGHT_PAREN, "Expected ')' after grouped expression.");
                return new MolangAst.GroupingExpr(SourceSpan.covering(span(leftParen), span(rightParen)), expression);
            }

            if (match(MolangTokenKind.LEFT_BRACE)) {
                MolangToken leftBrace = previous();
                ParseBlockResult block = parseStatements(MolangTokenKind.RIGHT_BRACE);
                MolangToken rightBrace = consume(MolangTokenKind.RIGHT_BRACE, "Expected '}' after block expression.");
                return new MolangAst.BlockExpr(SourceSpan.covering(span(leftBrace), span(rightBrace)), block.statements);
            }

            if (match(MolangTokenKind.BREAK)) {
                return new MolangAst.BreakExpr(span(previous()));
            }

            if (match(MolangTokenKind.CONTINUE)) {
                return new MolangAst.ContinueExpr(span(previous()));
            }

            throw error("Unexpected token: " + peek().kind());
        }

        private MolangAst.LoopExpr parseLoopControlForm(MolangToken loopToken) {
            consume(MolangTokenKind.LEFT_PAREN, "Expected '(' after loop.");
            MolangAst.Expr count = parseExpression();
            consume(MolangTokenKind.COMMA, "Expected ',' after loop iteration count.");
            MolangAst.BlockExpr body = parseBlockExpression();
            MolangToken rightParen = consume(MolangTokenKind.RIGHT_PAREN, "Expected ')' after loop body.");
            return new MolangAst.LoopExpr(SourceSpan.covering(span(loopToken), span(rightParen)), count, body);
        }

        private MolangAst.@Nullable Expr parseOptionalControlFlowValue(MolangTokenKind terminator) {
            if (check(MolangTokenKind.SEMICOLON) || check(terminator) || check(MolangTokenKind.RIGHT_PAREN) || check(MolangTokenKind.EOF)) {
                return null;
            }
            return parseExpression();
        }

        private MolangAst.ForEachExpr parseForEachControlForm(MolangToken forEachToken) {
            consume(MolangTokenKind.LEFT_PAREN, "Expected '(' after for_each.");
            MolangAst.Expr variable = parseExpression();
            consume(MolangTokenKind.COMMA, "Expected ',' after for_each variable.");
            MolangAst.Expr collection = parseExpression();
            consume(MolangTokenKind.COMMA, "Expected ',' after for_each collection.");
            MolangAst.BlockExpr body = parseBlockExpression();
            MolangToken rightParen = consume(MolangTokenKind.RIGHT_PAREN, "Expected ')' after for_each body.");
            return new MolangAst.ForEachExpr(SourceSpan.covering(span(forEachToken), span(rightParen)), variable, collection, body);
        }

        private MolangAst.BlockExpr parseBlockExpression() {
            MolangToken leftBrace = consume(MolangTokenKind.LEFT_BRACE, "Expected '{' before block body.");
            ParseBlockResult block = parseStatements(MolangTokenKind.RIGHT_BRACE);
            MolangToken rightBrace = consume(MolangTokenKind.RIGHT_BRACE, "Expected '}' after block expression.");
            return new MolangAst.BlockExpr(SourceSpan.covering(span(leftBrace), span(rightBrace)), block.statements);
        }

        private MolangToken consume(MolangTokenKind expectedKind, String message) {
            if (check(expectedKind)) {
                return advance();
            }
            throw error(message);
        }

        private boolean match(MolangTokenKind... kinds) {
            for (MolangTokenKind kind : kinds) {
                if (check(kind)) {
                    advance();
                    return true;
                }
            }
            return false;
        }

        private boolean check(MolangTokenKind kind) {
            return peek().kind() == kind;
        }

        private MolangToken advance() {
            if (!isAtEnd()) {
                position++;
            }
            return previous();
        }

        private boolean isAtEnd() {
            return peek().kind() == MolangTokenKind.EOF;
        }

        private MolangToken peek() {
            return tokens.get(position);
        }

        private MolangToken previous() {
            return tokens.get(position - 1);
        }

        private ParseException error(String message) {
            return new ParseException(message + " at index " + peek().startIndex());
        }

        private SourceSpan span(MolangToken token) {
            return new SourceSpan(
                    token.startIndex(),
                    token.stopIndexInclusive(),
                    token.startLine(),
                    token.startColumn(),
                    token.endLine(),
                    token.endColumnExclusive()
            );
        }

        private record ParseBlockResult(List<MolangAst.Stmt> statements, boolean sawSemicolon) {
        }
    }

    private static final class ParseException extends RuntimeException {
        private ParseException(String message) {
            super(message);
        }
    }
}
