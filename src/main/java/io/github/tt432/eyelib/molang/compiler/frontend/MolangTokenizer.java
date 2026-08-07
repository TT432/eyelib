package io.github.tt432.eyelib.molang.compiler.frontend;

import java.util.ArrayList;
import java.util.List;

/**
 * molang 词法分析器（自 {@code HandwrittenMolangAstParserFrontend} 抽取的公共 API，
 * 供 .emolang 解析等需要 token 级操作的场景复用——标识符/数字/字符串/运算符规则与
 * 编译器前端完全一致）。
 */
public final class MolangTokenizer {
    /** 词法分析：source → token 流（末尾附 EOF token）。语法错误抛 {@link MolangTokenizeException}。 */
    public static List<MolangToken> tokenize(String source) {
        return new MolangTokenizer(source).doTokenize();
    }

    /** 词法错误（未闭合字符串/不支持的字符）。 */
    public static final class MolangTokenizeException extends RuntimeException {
        public MolangTokenizeException(String message) {
            super(message);
        }
    }

    private final String source;
    private final int length;
    private int index;
    private int line;
    private int column;

    private MolangTokenizer(String source) {
        this.source = source;
        this.length = source.length();
        this.index = 0;
        this.line = 1;
        this.column = 0;
    }

    private List<MolangToken> doTokenize() {
        List<MolangToken> tokens = new ArrayList<>();
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
            if (isDigit(current)
                    || (current == '.' && index + 1 < length && isDigit(source.charAt(index + 1)))) {
                tokens.add(readNumber(tokenStartIndex, tokenStartLine, tokenStartColumn));
                continue;
            }
            if (current == '\'') {
                tokens.add(readString(tokenStartIndex, tokenStartLine, tokenStartColumn));
                continue;
            }

            MolangToken token = readPunctuationOrOperator(tokenStartIndex, tokenStartLine, tokenStartColumn);
            tokens.add(token);
        }

        tokens.add(new MolangToken(
                MolangTokenKind.EOF,
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

    private MolangToken readIdentifier(int startIndex, int startLine, int startColumn) {
        int start = index;
        while (hasRemaining() && isIdentifierPart(source.charAt(index))) {
            index++;
            column++;
        }

        String text = source.substring(start, index);
        MolangTokenKind kind = switch (text.toLowerCase(java.util.Locale.ROOT)) {
            case "return" -> MolangTokenKind.RETURN;
            case "break" -> MolangTokenKind.BREAK;
            case "continue" -> MolangTokenKind.CONTINUE;
            default -> MolangTokenKind.IDENTIFIER;
        };
        return new MolangToken(kind, text, startIndex, index - 1, startLine, startColumn, line, column);
    }

    private MolangToken readString(int startIndex, int startLine, int startColumn) {
        int start = index;
        index++;
        column++;
        while (hasRemaining() && source.charAt(index) != '\'') {
            index++;
            column++;
        }
        if (!hasRemaining()) {
            throw new MolangTokenizeException("Unterminated string at index " + startIndex);
        }
        index++;
        column++;

        String text = source.substring(start, index);
        return new MolangToken(MolangTokenKind.STRING, text, startIndex, index - 1, startLine, startColumn, line, column);
    }

    private MolangToken readNumber(int startIndex, int startLine, int startColumn) {
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

        // 科学计数法：1.8e-4、1.8E+4 等
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

        // 尾缀 f/F（在 vanilla .mcpack 数据中出现，无实际语义）
        if (hasRemaining() && (source.charAt(index) == 'f' || source.charAt(index) == 'F')) {
            index++;
            column++;
        }

        String text = source.substring(start, index);
        return new MolangToken(MolangTokenKind.NUMBER, text, startIndex, index - 1, startLine, startColumn, line, column);
    }

    private MolangToken readPunctuationOrOperator(int startIndex, int startLine, int startColumn) {
        if (match("&&")) {
            return token(MolangTokenKind.AND_AND, "&&", startIndex, startLine, startColumn, 2);
        }
        if (match("||")) {
            return token(MolangTokenKind.OR_OR, "||", startIndex, startLine, startColumn, 2);
        }
        if (match("??")) {
            return token(MolangTokenKind.QUESTION_QUESTION, "??", startIndex, startLine, startColumn, 2);
        }
        if (match("->")) {
            return token(MolangTokenKind.ARROW, "->", startIndex, startLine, startColumn, 2);
        }
        if (match("==")) {
            return token(MolangTokenKind.EQUAL_EQUAL, "==", startIndex, startLine, startColumn, 2);
        }
        if (match("!=")) {
            return token(MolangTokenKind.BANG_EQUAL, "!=", startIndex, startLine, startColumn, 2);
        }
        if (match("<=")) {
            return token(MolangTokenKind.LESS_EQUAL, "<=", startIndex, startLine, startColumn, 2);
        }
        if (match(">=")) {
            return token(MolangTokenKind.GREATER_EQUAL, ">=", startIndex, startLine, startColumn, 2);
        }

        char c = source.charAt(index);
        return switch (c) {
            case '(' -> token(MolangTokenKind.LEFT_PAREN, "(", startIndex, startLine, startColumn, 1);
            case ')' -> token(MolangTokenKind.RIGHT_PAREN, ")", startIndex, startLine, startColumn, 1);
            case '{' -> token(MolangTokenKind.LEFT_BRACE, "{", startIndex, startLine, startColumn, 1);
            case '}' -> token(MolangTokenKind.RIGHT_BRACE, "}", startIndex, startLine, startColumn, 1);
            case '[' -> token(MolangTokenKind.LEFT_BRACKET, "[", startIndex, startLine, startColumn, 1);
            case ']' -> token(MolangTokenKind.RIGHT_BRACKET, "]", startIndex, startLine, startColumn, 1);
            case ',' -> token(MolangTokenKind.COMMA, ",", startIndex, startLine, startColumn, 1);
            case ';' -> token(MolangTokenKind.SEMICOLON, ";", startIndex, startLine, startColumn, 1);
            case '.' -> token(MolangTokenKind.DOT, ".", startIndex, startLine, startColumn, 1);
            case '+' -> token(MolangTokenKind.PLUS, "+", startIndex, startLine, startColumn, 1);
            case '-' -> token(MolangTokenKind.MINUS, "-", startIndex, startLine, startColumn, 1);
            case '*' -> token(MolangTokenKind.STAR, "*", startIndex, startLine, startColumn, 1);
            case '/' -> token(MolangTokenKind.SLASH, "/", startIndex, startLine, startColumn, 1);
            case '>' -> token(MolangTokenKind.GREATER, ">", startIndex, startLine, startColumn, 1);
            case '<' -> token(MolangTokenKind.LESS, "<", startIndex, startLine, startColumn, 1);
            case '=' -> token(MolangTokenKind.EQUAL, "=", startIndex, startLine, startColumn, 1);
            case '!' -> token(MolangTokenKind.BANG, "!", startIndex, startLine, startColumn, 1);
            case '?' -> token(MolangTokenKind.QUESTION, "?", startIndex, startLine, startColumn, 1);
            case ':' -> token(MolangTokenKind.COLON, ":", startIndex, startLine, startColumn, 1);
            default -> throw new MolangTokenizeException("Unsupported character: '" + c + "' at index " + index);
        };
    }

    private MolangToken token(MolangTokenKind kind, String lexeme, int startIndex, int startLine, int startColumn, int consumedChars) {
        index += consumedChars;
        column += consumedChars;
        return new MolangToken(kind, lexeme, startIndex, startIndex + consumedChars - 1, startLine, startColumn, line, column);
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
