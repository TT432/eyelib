package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangCompiledFunction;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangObject;

import java.util.Set;

public interface CompiledMolangExpression extends MolangCompiledFunction {
    MolangObject evaluate(MolangScope scope);

    String sourceExpression();

    Set<String> requiredHostRoles();

    @Override
    default MolangObject apply(MolangScope scope) {
        return evaluate(scope);
    }
}
