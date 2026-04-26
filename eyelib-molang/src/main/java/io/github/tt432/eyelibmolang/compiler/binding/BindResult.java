package io.github.tt432.eyelibmolang.compiler.binding;

import io.github.tt432.eyelibmolang.compiler.binding.link.MolangQueryBindLinkContract;
import io.github.tt432.eyelibmolang.compiler.binding.link.MolangCallableBindLinkContract;

import java.util.List;

public record BindResult(
        BoundMolang.BoundExprSet root,
        List<BindDiagnostic> diagnostics,
        List<BindDeferredNote> deferredNotes,
        List<MolangQueryBindLinkContract.QueryBindLinkRequest> queryBindLinkRequests,
        List<MolangCallableBindLinkContract.CallableBindLinkRequest> callableBindLinkRequests
) {
    public BindResult {
        diagnostics = List.copyOf(diagnostics);
        deferredNotes = List.copyOf(deferredNotes);
        queryBindLinkRequests = List.copyOf(queryBindLinkRequests);
        callableBindLinkRequests = List.copyOf(callableBindLinkRequests);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == BindDiagnostic.Severity.ERROR);
    }
}
