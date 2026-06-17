package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.NullMarked;

/**
 * 已编译的 Molang 函数接口。
 *
 * @author TT432
 */
@NullMarked
@FunctionalInterface
public interface MolangCompiledFunction {
    MolangCompiledFunction NULL = scope -> MolangNull.INSTANCE;

    MolangObject apply(MolangScope scope);
}