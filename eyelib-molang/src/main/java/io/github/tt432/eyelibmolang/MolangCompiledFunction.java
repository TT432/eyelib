package io.github.tt432.eyelibmolang;

import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
@FunctionalInterface
public interface MolangCompiledFunction {
    MolangCompiledFunction NULL = scope -> MolangNull.INSTANCE;

    MolangObject apply(MolangScope scope);
}