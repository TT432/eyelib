package io.github.tt432.eyelib.molang.compiler.frontend.ast;

import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
public record SourceSpan(
        int startIndex,
        int stopIndexInclusive,
        int startLine,
        int startColumn,
        int endLine,
        int endColumnExclusive
) {
    public static SourceSpan unknown() {
        return new SourceSpan(-1, -1, -1, -1, -1, -1);
    }

    public static SourceSpan covering(@Nullable SourceSpan start, @Nullable SourceSpan end) {
        if (start == null) {
            return end == null ? unknown() : end;
        }
        if (end == null) {
            return start;
        }
        return new SourceSpan(
                start.startIndex(),
                end.stopIndexInclusive(),
                start.startLine(),
                start.startColumn(),
                end.endLine(),
                end.endColumnExclusive()
        );
    }
}