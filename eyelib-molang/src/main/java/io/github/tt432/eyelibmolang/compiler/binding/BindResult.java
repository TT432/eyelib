package io.github.tt432.eyelibmolang.compiler.binding;

import java.util.List;

public record BindResult(
        BoundMolang.BoundExprSet root,
        List<BindDiagnostic> diagnostics,
        List<BindDeferredNote> deferredNotes
) {
    public BindResult {
        diagnostics = List.copyOf(diagnostics);
        deferredNotes = List.copyOf(deferredNotes);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == BindDiagnostic.Severity.ERROR);
    }
}
