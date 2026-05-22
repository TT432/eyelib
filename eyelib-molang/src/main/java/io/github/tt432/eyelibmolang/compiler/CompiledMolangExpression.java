package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangCompiledFunction;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

/**
 * 已编译的 Molang 表达式，可直接求值。
 *
 * @author TT432
 */
@NullMarked
public interface CompiledMolangExpression extends MolangCompiledFunction {
    MolangObject evaluate(MolangScope scope);

    String sourceExpression();

    Set<String> requiredHostRoles();

    @Override
    default MolangObject apply(MolangScope scope) {
        return evaluate(scope);
    }
}