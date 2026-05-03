package io.github.tt432.eyelib.client.instrument;

import io.github.tt432.eyelib.client.instrument.collector.MolangCompileCacheObserver;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression;
import io.github.tt432.eyelibmolang.compiler.cache.MolangCompileCache;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstrumentMolangIntegrationTest {

    private static CompiledMolangExpression constantExpression(float value) {
        return new CompiledMolangExpression() {
            @Override
            public MolangObject evaluate(MolangScope scope) {
                return MolangFloat.valueOf(value);
            }

            @Override
            public String sourceExpression() {
                return String.valueOf(value);
            }

            @Override
            public Set<String> requiredHostRoles() {
                return Set.of();
            }
        };
    }

    @Test
    void cacheSizeIncreasesWithUniqueCompilations() {
        MolangCompileCache cache = new MolangCompileCache();
        MolangCompileCacheObserver observer = new MolangCompileCacheObserver(cache);

        assertEquals(0, observer.currentSize(), "Cache should start empty");

        for (int i = 0; i < 50; i++) {
            int value = i;
            cache.getOrCompile("expr_" + value, () -> constantExpression(value));
        }

        assertEquals(50, observer.currentSize(), "50 unique compilations should produce 50 cache entries");
    }

    @Test
    void cacheDeduplicatesRepeatedKeys() {
        MolangCompileCache cache = new MolangCompileCache();
        MolangCompileCacheObserver observer = new MolangCompileCacheObserver(cache);

        for (int i = 0; i < 100; i++) {
            cache.getOrCompile("same_expression", () -> constantExpression(1.0f));
        }

        assertEquals(1, observer.currentSize(), "Repeated key should only produce 1 cache entry");
    }

    @Test
    void observerReportsCorrectSourceMetadata() {
        MolangCompileCache cache = new MolangCompileCache();
        MolangCompileCacheObserver observer = new MolangCompileCacheObserver(cache);

        assertEquals("MolangCompileCache", observer.source());
        assertEquals("cache_entries", observer.metricName());
        assertEquals("count", observer.metricUnit());
    }

    @Test
    void cacheSizeMatchesActualCacheEntries() {
        MolangCompileCache cache = new MolangCompileCache();
        MolangCompileCacheObserver observer = new MolangCompileCacheObserver(cache);

        cache.getOrCompile("a", () -> constantExpression(1));
        cache.getOrCompile("b", () -> constantExpression(2));
        cache.getOrCompile("c", () -> constantExpression(3));

        assertEquals(cache.size(), observer.currentSize(), "Observer size should match cache.size()");
    }
}
