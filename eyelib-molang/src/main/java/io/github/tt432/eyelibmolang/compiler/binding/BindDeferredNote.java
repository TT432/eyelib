package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;

public record BindDeferredNote(
        SourceSpan span,
        Reason reason,
        String sourceFamily
) {
    public enum Reason {
        UNSUPPORTED_IN_THIS_SLICE
    }
}
