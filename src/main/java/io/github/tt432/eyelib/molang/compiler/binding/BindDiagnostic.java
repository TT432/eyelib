package io.github.tt432.eyelib.molang.compiler.binding;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.SourceSpan;
/**
 * @author TT432
 */
public record BindDiagnostic(
        SourceSpan span,
        Severity severity,
        String code,
        String message
) {
    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }
}