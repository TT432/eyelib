package io.github.tt432.eyelib.molang.compiler.frontend;

/**
 * molang 词法单元类别（{@link MolangTokenizer} 产物）。
 */
public enum MolangTokenKind {
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
