package io.github.tt432.eyelibmolang;

import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface MolangCompiledFunction {
    MolangCompiledFunction NULL = scope -> MolangNull.INSTANCE;

    MolangObject apply(@NotNull MolangScope scope);
}
