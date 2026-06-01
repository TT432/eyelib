package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.binding.BindDiagnosticsMode;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

/**
 * @author TT432
 */
@NullMarked
public record CompileContext(
        MolangMappingTree mappingTree,
        BindDiagnosticsMode diagnosticsMode,
        Set<String> availableHostRoles
) {
    public static CompileContext defaults() {
        return new CompileContext(MolangMappingTree.INSTANCE, BindDiagnosticsMode.NORMAL, Set.of());
    }
}