package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangCompiledFunction;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import java.util.Set;

/**
 * 已编译的 Molang 表达式，可直接求值。
 *
 * @author TT432
 */
public interface CompiledMolangExpression extends MolangCompiledFunction {
    MolangObject evaluate(MolangScope scope);

    String sourceExpression();

    Set<String> requiredHostRoles();

    @Override
    default MolangObject apply(MolangScope scope) {
        return evaluate(scope);
    }
}