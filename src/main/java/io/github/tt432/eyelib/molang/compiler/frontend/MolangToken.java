package io.github.tt432.eyelib.molang.compiler.frontend;

/**
 * molang 词法单元（{@link MolangTokenizer} 产物；lexeme 为原文切片，span 供诊断定位）。
 */
public record MolangToken(
        MolangTokenKind kind,
        String lexeme,
        int startIndex,
        int stopIndexInclusive,
        int startLine,
        int startColumn,
        int endLine,
        int endColumnExclusive
) {
}
