package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;

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
