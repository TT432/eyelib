package io.github.tt432.eyelib.molang.compiler.binding;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.SourceSpan;
/**
 * @author TT432
 */
public record BindDeferredNote(
        SourceSpan span,
        Reason reason,
        String sourceFamily
) {
    public enum Reason {
        UNSUPPORTED_IN_THIS_SLICE,
        HOST_SHAPE_DEPENDENT,
        QUERY_VARIANT_SELECTION_DEPENDENT,
        COMPATIBILITY_POLICY_DEPENDENT,
        DIAGNOSTICS_OVERLAY_OWNED_FOLLOWUP
    }
}