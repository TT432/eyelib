package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.compiler.binding.BindDiagnosticsMode;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import java.util.Set;

/**
 * @author TT432
 */
public record CompileContext(
        MolangMappingTree mappingTree,
        BindDiagnosticsMode diagnosticsMode,
        Set<String> availableHostRoles
) {
    public static CompileContext defaults() {
        return new CompileContext(MolangMappingTree.INSTANCE, BindDiagnosticsMode.NORMAL, Set.of());
    }
}