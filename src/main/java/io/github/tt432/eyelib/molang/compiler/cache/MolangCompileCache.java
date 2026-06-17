package io.github.tt432.eyelibmolang.compiler.cache;

import io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Molang 编译结果的 L1 内存缓存，支持过期检测和大小限制淘汰。
 *
 * @author TT432
 */
@NullMarked
public final class MolangCompileCache {
    private static final int COMPILER_VERSION = 2;
    private static final int MAX_L1_SIZE = 1000;

    private final Map<String, CompiledMolangExpression> cache = new ConcurrentHashMap<>();

    // 用于查找时检测过期
    private volatile MolangMappingTree mappingTree;

    public MolangCompileCache() {
        this(null, null);
    }

    /**
     * @param mappingTree    过期检测用的映射树，可为 null
     * @param cacheDirectory 未使用，保留仅为兼容
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
        // 构建包含注册表版本引用的复合键，用于过期检测
        String currentRegistryRef = mappingTree != null
                ? mappingTree.registryVersionRef().value()
                : null;
        String cacheKey = currentRegistryRef != null
                ? key + "#" + currentRegistryRef
                : key;

        // 大小限制淘汰：超出阈值时移除约 25% 的条目
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