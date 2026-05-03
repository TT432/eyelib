package io.github.tt432.eyelibmolang.compiler.cache;

import io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MolangCompileCache {
    private static final int COMPILER_VERSION = 1;
    private static final int MAX_L1_SIZE = 1000;

    private final Map<String, CompiledMolangExpression> cache = new ConcurrentHashMap<>();

    // Required for staleness detection at lookup time
    private volatile MolangMappingTree mappingTree;

    /** No-arg constructor: disk cache disabled (backward compatible). */
    public MolangCompileCache() {
        this(null, null);
    }

    /**
     * Constructor keeping historical cacheDirectory parameter for compatibility.
     *
     * @param mappingTree    the Molang mapping tree for staleness detection; may be null
     * @param cacheDirectory unused
     */
    public MolangCompileCache(MolangMappingTree mappingTree, Path cacheDirectory) {
        this.mappingTree = mappingTree;
    }

    /**
     * Returns the number of cached expressions in L1 memory cache.
     * For telemetry only, not for cache control.
     */
    public int size() {
        return cache.size();
    }

    public CompiledMolangExpression getOrCompile(String key, Supplier<CompiledMolangExpression> supplier) {
        // Build composite key incorporating registry version ref for staleness detection
        String currentRegistryRef = mappingTree != null
                ? mappingTree.registryVersionRef().value()
                : null;
        String cacheKey = currentRegistryRef != null
                ? key + "#" + currentRegistryRef
                : key;

        // Size-bound eviction: remove ~25% of entries when threshold is exceeded
        if (cache.size() >= MAX_L1_SIZE) {
            int toRemove = MAX_L1_SIZE / 4;
            var it = cache.keySet().iterator();
            for (int i = 0; i < toRemove && it.hasNext(); i++) {
                it.next();
                it.remove();
            }
        }

        return cache.computeIfAbsent(cacheKey, k -> supplier.get());
    }
}
