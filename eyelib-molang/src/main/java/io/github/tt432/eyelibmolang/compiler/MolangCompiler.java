package io.github.tt432.eyelibmolang.compiler;

public interface MolangCompiler {
    CompiledMolangExpression compile(String expression, CompileContext ctx);
}
