package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.compiler.binding.BindDiagnosticsMode;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
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
        return new CompileContext(MolangMappingRegistries.mappingTree(), BindDiagnosticsMode.NORMAL, Set.of());
    }
}