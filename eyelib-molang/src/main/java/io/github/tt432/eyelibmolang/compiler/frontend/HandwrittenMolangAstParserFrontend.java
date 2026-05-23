package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;
import io.github.tt432.eyelibmolang.generated.MolangLexer;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 手写 AST 解析前端，用于常量折叠等阶段。
 *
 * @author TT432
 */
@NullMarked
public final class HandwrittenMolangAstParserFrontend implements MolangParserFrontend {
    public static final HandwrittenMolangAstParserFrontend INSTANCE = new HandwrittenMolangAstParserFrontend();

    private HandwrittenMolangAstParserFrontend() {
    }

    @Override
    public MolangParserFrontendResult parseExprSet(
            String source,
            Consumer<MolangLexer> lexerConfigurator,
            Consumer<MolangParser> parserConfigurator) {
        return new MolangParserFrontendResult(null, null, parseExprSetAst(source));
    }

    public Optional<MolangAst.ExprSet> parseExprSetAst(String source) {
        try {
            Parser parser = new Parser(source);
            return Optional.of(parser.parseExprSet());
        } catch (ParseException parseException) {
            return Optional.empty();
        }
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final String source;
        private int position;

        private Parser(String source) {
            this.tokens = new Tokenizer(source).tokenize();
            this.source = source;
            this.position = 0;
        }

        private MolangAst.ExprSet parseExprSet() {
            ParseBlockResult topLevel = parseStatements(TokenKind.EOF);
            if (topLevel.statements.size() == 1 && !topLevel.sawSemicolon) {
                MolangAst.Stmt onlyStatement = topLevel.statements.get(0);
                if (onlyStatement instanceof MolangAst.ExprStmt exprStmt) {
                    return new MolangAst.ExprSet(exprStmt.expression().span(), exprStmt.expression());
                }
            }

            SourceSpan exprSetSpan = topLevel.statements.isEmpty()
                    ? SourceSpan.unknown()
                    : SourceSpan.covering(topLevel.statements.get(0).span(), topLevel.statements.get(topLevel.statements.size() - 1).span());
            MolangAst.BlockExpr blockExpr = new MolangAst.BlockExpr(exprSetSpan, topLevel.statements);
            return new MolangAst.ExprSet(blockExpr.span(), blockExpr);
        }

        private ParseBlockResult parseStatements(TokenKind terminator) {
            List<MolangAst.Stmt> statements = new ArrayList<>();
            boolean sawSemicolon = false;

            while (!check(terminator) && !check(TokenKind.EOF)) {
                if (match(TokenKind.SEMICOLON)) {
                    sawSemicolon = true;
                    continue;
                }

                if (match(TokenKind.RETURN)) {
                    Token returnToken = previous();
                    MolangAst.Expr returnExpression = parseExpression();
                    SourceSpan returnSpan = SourceSpan.covering(span(returnToken), returnExpression.span());
                    statements.add(new MolangAst.ReturnStmt(returnSpan, returnExpression));
                } else if (match(TokenKind.BREAK)) {
                    statements.add(new MolangAst.BreakStmt(span(previous())));
                } else if (match(TokenKind.CONTINUE)) {
                    statements.add(new MolangAst.ContinueStmt(span(previous())));
                } else {
                    MolangAst.Expr expression = parseExpression();
                    statements.add(new MolangAst.ExprStmt(expression.span(), expression));
                }

                if (match(TokenKind.SEMICOLON)) {
                    sawSemicolon = true;
                } else if (!check(terminator) && !check(TokenKind.EOF)) {
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
            if (!match(TokenKind.EQUAL)) {
                return left;
            }

            MolangAst.Expr right = parseAssignment();
            return new MolangAst.AssignmentExpr(SourceSpan.covering(left.span(), right.span()), left, right);
        }

        private MolangAst.Expr parseTernary() {
            MolangAst.Expr condition = parseNullCoalesce();
            if (!match(TokenKind.QUESTION)) {
                return condition;
            }

            MolangAst.Expr whenTrue = parseExpression();
            if (match(TokenKind.COLON)) {
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
            while (match(TokenKind.QUESTION_QUESTION)) {
                MolangAst.Expr right = parseOr();
                expression = new MolangAst.NullCoalesceExpr(SourceSpan.covering(expression.span(), right.span()), expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseAnd() {
            MolangAst.Expr expression = parseComparison();
            while (match(TokenKind.AND_AND)) {
                Token operator = previous();
                MolangAst.Expr right = parseComparison();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme, expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseOr() {
            MolangAst.Expr expression = parseAnd();
            while (match(TokenKind.OR_OR)) {
                Token operator = previous();
                MolangAst.Expr right = parseAnd();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme, expression, right);
            }
            return expression;
        }
        private MolangAst.Expr parseComparison() {
            MolangAst.Expr expression = parseAdd();
            while (match(TokenKind.GREATER, TokenKind.GREATER_EQUAL, TokenKind.LESS, TokenKind.LESS_EQUAL, TokenKind.EQUAL_EQUAL, TokenKind.BANG_EQUAL)) {
                Token operator = previous();
                MolangAst.Expr right = parseAdd();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme, expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseAdd() {
            MolangAst.Expr expression = parseMultiply();
            while (match(TokenKind.PLUS, TokenKind.MINUS)) {
                Token operator = previous();
                MolangAst.Expr right = parseMultiply();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme, expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseMultiply() {
            MolangAst.Expr expression = parseUnary();
            while (match(TokenKind.STAR, TokenKind.SLASH)) {
                Token operator = previous();
                MolangAst.Expr right = parseUnary();
                expression = new MolangAst.BinaryExpr(SourceSpan.covering(expression.span(), right.span()), operator.lexeme, expression, right);
            }
            return expression;
        }

        private MolangAst.Expr parseUnary() {
            if (match(TokenKind.MINUS, TokenKind.BANG)) {
                Token operator = previous();
                MolangAst.Expr expression = parseUnary();
                return new MolangAst.UnaryExpr(SourceSpan.covering(span(operator), expression.span()), operator.lexeme, expression);
            }
            return parsePostfix();
        }

        private MolangAst.Expr parsePostfix() {
            MolangAst.Expr expression = parsePrimary();

            while (true) {
                if (match(TokenKind.DOT)) {
                    Token member = consume(TokenKind.IDENTIFIER, "Expected member name after '.'.");
                    expression = new MolangAst.MemberAccessExpr(SourceSpan.covering(expression.span(), span(member)), expression, member.lexeme);
                    continue;
                }

                if (match(TokenKind.ARROW)) {
                    Token owner = consume(TokenKind.IDENTIFIER, "Expected owner root after '->'.");
                    consume(TokenKind.DOT, "Expected '.' after arrow owner.");
                    Token member = consume(TokenKind.IDENTIFIER, "Expected member name after arrow owner.");
                    MolangAst.MemberAccessExpr right = new MolangAst.MemberAccessExpr(
                            SourceSpan.covering(span(owner), span(member)),
                            new MolangAst.IdentifierExpr(span(owner), owner.lexeme),
                            member.lexeme
                    );
                    expression = new MolangAst.ArrowAccessExpr(SourceSpan.covering(expression.span(), span(member)), expression, right);
                    continue;
                }

                if (match(TokenKind.LEFT_PAREN)) {
                    List<MolangAst.Expr> arguments = new ArrayList<>();
                    if (!check(TokenKind.RIGHT_PAREN)) {
                        do {
                            arguments.add(parseExpression());
                        } while (match(TokenKind.COMMA));
                    }
                    Token rightParen = consume(TokenKind.RIGHT_PAREN, "Expected ')' after call arguments.");

                    SourceSpan callSpan = SourceSpan.covering(expression.span(), span(rightParen));
                    expression = new MolangAst.CallExpr(callSpan, expression, arguments);
                    continue;
                }

                if (match(TokenKind.LEFT_BRACKET)) {
                    MolangAst.Expr index = parseExpression();
                    Token rightBracket = consume(TokenKind.RIGHT_BRACKET, "Expected ']' after index expression.");
                    expression = new MolangAst.IndexExpr(SourceSpan.covering(expression.span(), span(rightBracket)), expression, index);
                    continue;
                }

                break;
            }

            return expression;
        }

        private MolangAst.Expr parsePrimary() {
            if (match(TokenKind.NUMBER)) {
                Token number = previous();
                return new MolangAst.NumberLiteralExpr(span(number), number.lexeme, Double.parseDouble(number.lexeme));
            }

            if (match(TokenKind.IDENTIFIER)) {
                Token identifier = previous();
                String normalizedIdentifier = identifier.lexeme.toLowerCase(java.util.Locale.ROOT);
                if ("this".equals(normalizedIdentifier)) {
                    return new MolangAst.ThisExpr(span(identifier));
                }
                if ("loop".equals(normalizedIdentifier) && check(TokenKind.LEFT_PAREN)) {
                    return parseLoopControlForm(identifier);
                }
                if ("for_each".equals(normalizedIdentifier) && check(TokenKind.LEFT_PAREN)) {
                    return parseForEachControlForm(identifier);
                }
                return new MolangAst.IdentifierExpr(span(identifier), identifier.lexeme);
            }

            if (match(TokenKind.STRING)) {
                Token string = previous();
                return new MolangAst.StringLiteralExpr(span(string), string.lexeme);
            }

            if (match(TokenKind.LEFT_PAREN)) {
                Token leftParen = previous();
                MolangAst.Expr expression = parseExpression();
                Token rightParen = consume(TokenKind.RIGHT_PAREN, "Expected ')' after grouped expression.");
                return new MolangAst.GroupingExpr(SourceSpan.covering(span(leftParen), span(rightParen)), expression);
            }

            if (match(TokenKind.LEFT_BRACE)) {
                Token leftBrace = previous();
                ParseBlockResult block = parseStatements(TokenKind.RIGHT_BRACE);
                Token rightBrace = consume(TokenKind.RIGHT_BRACE, "Expected '}' after block expression.");
                return new MolangAst.BlockExpr(SourceSpan.covering(span(leftBrace), span(rightBrace)), block.statements);
            }

            throw error("Unexpected token: " + peek().kind);
        }

        private MolangAst.LoopExpr parseLoopControlForm(Token loopToken) {
            consume(TokenKind.LEFT_PAREN, "Expected '(' after loop.");
            MolangAst.Expr count = parseExpression();
            String countText = source.substring(count.span().startIndex(), count.span().stopIndexInclusive() + 1);
            consume(TokenKind.COMMA, "Expected ',' after loop iteration count.");
            MolangAst.BlockExpr body = parseBlockExpression();
            Token rightParen = consume(TokenKind.RIGHT_PAREN, "Expected ')' after loop body.");
            return new MolangAst.LoopExpr(SourceSpan.covering(span(loopToken), span(rightParen)), countText, body);
        }

        private MolangAst.ForEachExpr parseForEachControlForm(Token forEachToken) {
            consume(TokenKind.LEFT_PAREN, "Expected '(' after for_each.");
            MolangAst.Expr variable = parseExpression();
            consume(TokenKind.COMMA, "Expected ',' after for_each variable.");
            MolangAst.Expr collection = parseExpression();
            consume(TokenKind.COMMA, "Expected ',' after for_each collection.");
            MolangAst.BlockExpr body = parseBlockExpression();
            Token rightParen = consume(TokenKind.RIGHT_PAREN, "Expected ')' after for_each body.");
            return new MolangAst.ForEachExpr(SourceSpan.covering(span(forEachToken), span(rightParen)), variable, collection, body);
        }

        private MolangAst.BlockExpr parseBlockExpression() {
            Token leftBrace = consume(TokenKind.LEFT_BRACE, "Expected '{' before block body.");
            ParseBlockResult block = parseStatements(TokenKind.RIGHT_BRACE);
            Token rightBrace = consume(TokenKind.RIGHT_BRACE, "Expected '}' after block expression.");
            return new MolangAst.BlockExpr(SourceSpan.covering(span(leftBrace), span(rightBrace)), block.statements);
        }

        private Token consume(TokenKind expectedKind, String message) {
            if (check(expectedKind)) {
                return advance();
            }
            throw error(message);
        }

        private boolean match(TokenKind... kinds) {
            for (TokenKind kind : kinds) {
                if (check(kind)) {
                    advance();
                    return true;
                }
            }
            return false;
        }

        private boolean check(TokenKind kind) {
            return peek().kind == kind;
        }

        private Token advance() {
            if (!isAtEnd()) {
                position++;
            }
            return previous();
        }

        private boolean isAtEnd() {
            return peek().kind == TokenKind.EOF;
        }

        private Token peek() {
            return tokens.get(position);
        }

        private Token previous() {
            return tokens.get(position - 1);
        }

        private ParseException error(String message) {
            return new ParseException(message + " at index " + peek().startIndex);
        }

        private SourceSpan span(Token token) {
            return new SourceSpan(
                    token.startIndex,
                    token.stopIndexInclusive,
                    token.startLine,
                    token.startColumn,
                    token.endLine,
                    token.endColumnExclusive
            );
        }

        private record ParseBlockResult(List<MolangAst.Stmt> statements, boolean sawSemicolon) {
        }
    }

    private static final class Tokenizer {
        private final String source;
        private final int length;
        private int index;
        private int line;
        private int column;

        private Tokenizer(String source) {
            this.source = source;
            this.length = source.length();
            this.index = 0;
            this.line = 1;
            this.column = 0;
        }

        private List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (hasRemaining()) {
                char current = source.charAt(index);
                if (isWhitespace(current)) {
                    consumeWhitespace(current);
                    continue;
                }

                int tokenStartIndex = index;
                int tokenStartLine = line;
                int tokenStartColumn = column;

                if (isIdentifierStart(current)) {
                    tokens.add(readIdentifier(tokenStartIndex, tokenStartLine, tokenStartColumn));
                    continue;
                }
                if (isDigit(current)) {
                    tokens.add(readNumber(tokenStartIndex, tokenStartLine, tokenStartColumn));
                    continue;
                }
                if (current == '\'') {
                    tokens.add(readString(tokenStartIndex, tokenStartLine, tokenStartColumn));
                    continue;
                }

                Token token = readPunctuationOrOperator(tokenStartIndex, tokenStartLine, tokenStartColumn);
                tokens.add(token);
            }

            tokens.add(new Token(
                    TokenKind.EOF,
                    "",
                    length,
                    length,
                    line,
                    column,
                    line,
                    column
            ));
            return tokens;
        }

        private Token readIdentifier(int startIndex, int startLine, int startColumn) {
            int start = index;
            while (hasRemaining() && isIdentifierPart(source.charAt(index))) {
                index++;
                column++;
            }

            String text = source.substring(start, index);
            TokenKind kind = switch (text.toLowerCase(java.util.Locale.ROOT)) {
                case "return" -> TokenKind.RETURN;
                case "break" -> TokenKind.BREAK;
                case "continue" -> TokenKind.CONTINUE;
                default -> TokenKind.IDENTIFIER;
            };
            return new Token(kind, text, startIndex, index - 1, startLine, startColumn, line, column);
        }

        private Token readString(int startIndex, int startLine, int startColumn) {
            int start = index;
            index++;
            column++;
            while (hasRemaining() && source.charAt(index) != '\'') {
                index++;
                column++;
            }
            if (!hasRemaining()) {
                throw new ParseException("Unterminated string at index " + startIndex);
            }
            index++;
            column++;

            String text = source.substring(start, index);
            return new Token(TokenKind.STRING, text, startIndex, index - 1, startLine, startColumn, line, column);
        }

        private Token readNumber(int startIndex, int startLine, int startColumn) {
            int start = index;
            while (hasRemaining() && isDigit(source.charAt(index))) {
                index++;
                column++;
            }

            if (hasRemaining() && source.charAt(index) == '.') {
                index++;
                column++;
                while (hasRemaining() && isDigit(source.charAt(index))) {
                    index++;
                    column++;
                }
            }

            // Scientific notation: 1.8e-4, 1.8E+4, etc.
            if (hasRemaining() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                index++;
                column++;
                if (hasRemaining() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                    column++;
                }
                while (hasRemaining() && isDigit(source.charAt(index))) {
                    index++;
                    column++;
                }
            }

            String text = source.substring(start, index);
            return new Token(TokenKind.NUMBER, text, startIndex, index - 1, startLine, startColumn, line, column);
        }

        private Token readPunctuationOrOperator(int startIndex, int startLine, int startColumn) {
        if (match("&&")) {
                return token(TokenKind.AND_AND, "&&", startIndex, startLine, startColumn, 2);
            }
            if (match("||")) {
                return token(TokenKind.OR_OR, "||", startIndex, startLine, startColumn, 2);
            }
            if (match("??")) {
                return token(TokenKind.QUESTION_QUESTION, "??", startIndex, startLine, startColumn, 2);
            }
            if (match("->")) {
                return token(TokenKind.ARROW, "->", startIndex, startLine, startColumn, 2);
            }
            if (match("==")) {
                return token(TokenKind.EQUAL_EQUAL, "==", startIndex, startLine, startColumn, 2);
            }
            if (match("!=")) {
                return token(TokenKind.BANG_EQUAL, "!=", startIndex, startLine, startColumn, 2);
            }
            if (match("<=")) {
                return token(TokenKind.LESS_EQUAL, "<=", startIndex, startLine, startColumn, 2);
            }
            if (match(">=")) {
                return token(TokenKind.GREATER_EQUAL, ">=", startIndex, startLine, startColumn, 2);
            }

            char c = source.charAt(index);
            return switch (c) {
                case '(' -> token(TokenKind.LEFT_PAREN, "(", startIndex, startLine, startColumn, 1);
                case ')' -> token(TokenKind.RIGHT_PAREN, ")", startIndex, startLine, startColumn, 1);
                case '{' -> token(TokenKind.LEFT_BRACE, "{", startIndex, startLine, startColumn, 1);
                case '}' -> token(TokenKind.RIGHT_BRACE, "}", startIndex, startLine, startColumn, 1);
                case '[' -> token(TokenKind.LEFT_BRACKET, "[", startIndex, startLine, startColumn, 1);
                case ']' -> token(TokenKind.RIGHT_BRACKET, "]", startIndex, startLine, startColumn, 1);
                case ',' -> token(TokenKind.COMMA, ",", startIndex, startLine, startColumn, 1);
                case ';' -> token(TokenKind.SEMICOLON, ";", startIndex, startLine, startColumn, 1);
                case '.' -> token(TokenKind.DOT, ".", startIndex, startLine, startColumn, 1);
                case '+' -> token(TokenKind.PLUS, "+", startIndex, startLine, startColumn, 1);
                case '-' -> token(TokenKind.MINUS, "-", startIndex, startLine, startColumn, 1);
                case '*' -> token(TokenKind.STAR, "*", startIndex, startLine, startColumn, 1);
                case '/' -> token(TokenKind.SLASH, "/", startIndex, startLine, startColumn, 1);
                case '>' -> token(TokenKind.GREATER, ">", startIndex, startLine, startColumn, 1);
                case '<' -> token(TokenKind.LESS, "<", startIndex, startLine, startColumn, 1);
                case '=' -> token(TokenKind.EQUAL, "=", startIndex, startLine, startColumn, 1);
                case '!' -> token(TokenKind.BANG, "!", startIndex, startLine, startColumn, 1);
                case '?' -> token(TokenKind.QUESTION, "?", startIndex, startLine, startColumn, 1);
                case ':' -> token(TokenKind.COLON, ":", startIndex, startLine, startColumn, 1);
                default -> throw new ParseException("Unsupported character: '" + c + "' at index " + index);
            };
        }

        private Token token(TokenKind kind, String lexeme, int startIndex, int startLine, int startColumn, int consumedChars) {
            index += consumedChars;
            column += consumedChars;
            return new Token(kind, lexeme, startIndex, startIndex + consumedChars - 1, startLine, startColumn, line, column);
        }

        private boolean match(String text) {
            if (index + text.length() > length) {
                return false;
            }
            for (int i = 0; i < text.length(); i++) {
                if (source.charAt(index + i) != text.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        private void consumeWhitespace(char current) {
            index++;
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }

        private boolean hasRemaining() {
            return index < length;
        }

        private boolean isWhitespace(char c) {
            return c == ' ' || c == '\t' || c == '\r' || c == '\n';
        }

        private boolean isIdentifierStart(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
        }

        private boolean isIdentifierPart(char c) {
            return isIdentifierStart(c) || isDigit(c);
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }

    private enum TokenKind {
        IDENTIFIER,
        NUMBER,
        STRING,
        RETURN,
        BREAK,
        CONTINUE,
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACE,
        RIGHT_BRACE,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        COMMA,
        SEMICOLON,
        DOT,
        PLUS,
        MINUS,
        STAR,
        SLASH,
        GREATER,
        GREATER_EQUAL,
        LESS,
        LESS_EQUAL,
        EQUAL,
        EQUAL_EQUAL,
        BANG,
        BANG_EQUAL,
        AND_AND,
        OR_OR,
        QUESTION,
        QUESTION_QUESTION,
        COLON,
        ARROW,
        EOF
    }

    private record Token(
            TokenKind kind,
            String lexeme,
            int startIndex,
            int stopIndexInclusive,
            int startLine,
            int startColumn,
            int endLine,
            int endColumnExclusive
    ) {
    }

    private static final class ParseException extends RuntimeException {
        private ParseException(String message) {
            super(message);
        }
    }
}