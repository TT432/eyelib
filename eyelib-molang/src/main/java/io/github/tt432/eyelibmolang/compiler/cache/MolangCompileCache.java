package io.github.tt432.eyelibmolang.compiler.cache;

import io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MolangCompileCache {
    private final Map<String, CompiledMolangExpression> cache = new ConcurrentHashMap<>();

    public CompiledMolangExpression getOrCompile(String key, Supplier<CompiledMolangExpression> supplier) {
        return cache.computeIfAbsent(key, k -> supplier.get());
    }
}
