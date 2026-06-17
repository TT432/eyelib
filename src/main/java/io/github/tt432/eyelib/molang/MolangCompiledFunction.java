package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
/**
 * 已编译的 Molang 函数接口。
 *
 * @author TT432
 */
@FunctionalInterface
public interface MolangCompiledFunction {
    MolangCompiledFunction NULL = scope -> MolangNull.INSTANCE;

    MolangObject apply(MolangScope scope);
}