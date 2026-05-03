package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression;
import io.github.tt432.eyelibmolang.compiler.cache.MolangCompileCache;
import io.github.tt432.eyelibmolang.compiler.cache.MolangDiskCache;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CacheSizeObserver} implementations.
 */
class CacheSizeObserverTest {

    @Test
    void molangCompileCacheObserverReportsInitialSizeZero() {
        // Given
        var cache = new MolangCompileCache();
        var observer = new MolangCompileCacheObserver(cache);

        // Then
        assertEquals(0, observer.currentSize());
    }

    @Test
    void molangCompileCacheObserverReportsSizeAfterCompile() {
        // Given
        var cache = new MolangCompileCache();
        var observer = new MolangCompileCacheObserver(cache);

        // When
        cache.getOrCompile("expr1", () -> new CompiledMolangExpression() {
            @Override
            public MolangObject evaluate(MolangScope scope) {
                return MolangFloat.valueOf(1.0f);
            }

            @Override
            public String sourceExpression() {
                return "1.0";
            }

            @Override
            public Set<String> requiredHostRoles() {
                return Set.of();
            }
        });

        // Then
        assertEquals(1, observer.currentSize());
    }

    @Test
    void molangCompileCacheObserverReportsSizeAfterMultipleCompiles() {
        // Given
        var cache = new MolangCompileCache();
        var observer = new MolangCompileCacheObserver(cache);

        // When
        CompiledMolangExpression expr1 = molangExpression();
        CompiledMolangExpression expr2 = molangExpression();

        cache.getOrCompile("a", () -> expr1);
        cache.getOrCompile("b", () -> expr2);

        // Then
        assertEquals(2, observer.currentSize());
    }

    @Test
    void molangCompileCacheObserverSourceAndMetric() {
        // Given
        var cache = new MolangCompileCache();
        var observer = new MolangCompileCacheObserver(cache);

        // Then
        assertEquals("MolangCompileCache", observer.source());
        assertEquals("cache_entries", observer.metricName());
        assertEquals("count", observer.metricUnit());
    }

    // --- ManagerStorageObserver tests ---

    @Test
    void managerStorageObserverWithNonEmptyData() {
        // Given
        var data = Map.of("k1", "v1", "k2", "v2");
        var observer = new ManagerStorageObserver(() -> data, "testManager");

        // Then
        assertEquals("testManager", observer.source());
        assertEquals("allocation_size", observer.metricName());
        assertEquals("count", observer.metricUnit());
        assertEquals(2, observer.currentSize());
    }

    @Test
    void managerStorageObserverWithEmptyData() {
        // Given
        var observer = new ManagerStorageObserver(Map::of, "emptyManager");

        // Then
        assertEquals(0, observer.currentSize());
    }

    // --- BrParticleObserver ---

    @Test
    void brParticleObserverReturnsExpectedMetadata() {
        var observer = new BrParticleObserver();
        assertEquals("BrParticleRenderManager", observer.source());
        assertEquals("total_particles", observer.metricName());
        assertEquals("count", observer.metricUnit());
    }

    @Test
    void brParticleObserverCurrentSizeIsNonNegative() {
        var observer = new BrParticleObserver();
        int size = observer.currentSize();
        assertTrue(size >= 0, "currentSize should be >= 0, got: " + size);
    }

    // --- helpers ---

    private static CompiledMolangExpression molangExpression() {
        return new CompiledMolangExpression() {
            @Override
            public MolangObject evaluate(MolangScope scope) {
                return MolangFloat.valueOf(1.0f);
            }

            @Override
            public String sourceExpression() {
                return "1.0";
            }

            @Override
            public Set<String> requiredHostRoles() {
                return Set.of();
            }
        };
    }

    // ================================================================
    // RenderHelperDfsModelsObserver tests
    // ================================================================

    @Test
    void renderHelperDfsModelsObserverSourceAndMetric() {
        var observer = new RenderHelperDfsModelsObserver();
        assertEquals("RenderHelper", observer.source());
        assertEquals("dfs_models", observer.metricName());
        assertEquals("count", observer.metricUnit());
    }

    @Test
    void renderHelperDfsModelsObserverCurrentSizeNonNegative() {
        var observer = new RenderHelperDfsModelsObserver();
        org.junit.jupiter.api.Assertions.assertTrue(observer.currentSize() >= 0);
    }

    // ================================================================
    // MolangValueConstantPoolObserver tests
    // ================================================================

    @Test
    void molangValueConstantPoolObserverSourceAndMetric() {
        var observer = new MolangValueConstantPoolObserver();
        assertEquals("MolangValue", observer.source());
        assertEquals("constant_pool_entries", observer.metricName());
        assertEquals("count", observer.metricUnit());
    }

    @Test
    void molangValueConstantPoolObserverPopulated() {
        // Populate the constant pool with distinct values
        MolangValue.getConstant(100.0f);
        MolangValue.getConstant(200.0f);
        MolangValue.getConstant(300.0f);

        var observer = new MolangValueConstantPoolObserver();
        org.junit.jupiter.api.Assertions.assertTrue(observer.currentSize() > 0);
    }

    // ================================================================
    // MolangScopeCacheObserver tests
    // ================================================================

    @Test
    void molangScopeCacheObserverSourceAndMetric() {
        var scope = new MolangScope();
        var observer = new MolangScopeCacheObserver(scope);
        assertEquals("MolangScope", observer.source());
        assertEquals("scope_cache_entries", observer.metricName());
        assertEquals("count", observer.metricUnit());
    }

    @Test
    void molangScopeCacheObserverReportsSizeZeroInitially() {
        var scope = new MolangScope();
        var observer = new MolangScopeCacheObserver(scope);
        assertEquals(0, observer.currentSize());
    }

    @Test
    void molangScopeCacheObserverReportsSizeAfterSet() {
        var scope = new MolangScope();
        var observer = new MolangScopeCacheObserver(scope);

        scope.set("test", 1.0f);
        assertEquals(1, observer.currentSize());
    }

    // ================================================================
    // MolangDiskCacheObserver tests
    // ================================================================

    @Test
    void molangDiskCacheObserverReportsCorrectMetadata(@TempDir Path tempDir) {
        var diskCache = new MolangDiskCache(tempDir);
        var observer = new MolangDiskCacheObserver(diskCache);

        assertEquals("MolangDiskCache", observer.source());
        assertEquals("disk_cache_files", observer.metricName());
        assertEquals("files", observer.metricUnit());
    }

    @Test
    void molangDiskCacheObserverReportsEmptyDirectory(@TempDir Path tempDir) {
        var diskCache = new MolangDiskCache(tempDir);
        var observer = new MolangDiskCacheObserver(diskCache);

        assertEquals(0, observer.currentSize());
    }

    @Test
    void molangDiskCacheObserverReportsFileCount(@TempDir Path tempDir) throws IOException {
        var diskCache = new MolangDiskCache(tempDir);
        var observer = new MolangDiskCacheObserver(diskCache);

        Files.createFile(tempDir.resolve("file1.molcache"));
        Files.createFile(tempDir.resolve("file2.molcache"));
        Files.createFile(tempDir.resolve("file3.molcache"));

        assertEquals(3, observer.currentSize());
    }

    @Test
    void molangDiskCacheObserverIgnoresSubdirectories(@TempDir Path tempDir) throws IOException {
        var diskCache = new MolangDiskCache(tempDir);
        var observer = new MolangDiskCacheObserver(diskCache);

        Files.createFile(tempDir.resolve("cache.molcache"));
        Files.createDirectory(tempDir.resolve("subdir"));

        assertEquals(1, observer.currentSize());
    }

    @Test
    void molangDiskCacheObserverReturnsZeroForNonexistentDirectory() {
        var diskCache = new MolangDiskCache(Path.of("/nonexistent/path"));
        var observer = new MolangDiskCacheObserver(diskCache);

        assertEquals(0, observer.currentSize());
    }

    // ================================================================
    // Large-scale integration tests
    // ================================================================

    @Test
    void molangCompileCacheObserverTracks1000Entries() {
        // Given
        var cache = new MolangCompileCache();
        var observer = new MolangCompileCacheObserver(cache);

        // When — compile 1000 unique expressions
        for (int i = 0; i < 1000; i++) {
            cache.getOrCompile("expr_" + i, () -> molangExpression());
        }

        // Then
        assertTrue(observer.currentSize() >= 1000,
                "Expected at least 1000 entries, got: " + observer.currentSize());
    }

    @Test
    void molangCompileCacheDeduplicatesByKey() {
        // Given
        var cache = new MolangCompileCache();
        var observer = new MolangCompileCacheObserver(cache);

        // When — same key repeated 100 times
        for (int i = 0; i < 100; i++) {
            cache.getOrCompile("same_key", () -> molangExpression());
        }

        // Then — only one distinct entry
        assertEquals(1, observer.currentSize());
    }

    @Test
    void molangScopeCacheObserverTracksMultipleEntries() {
        // Given
        var scope = new MolangScope();
        var observer = new MolangScopeCacheObserver(scope);

        // Then — initially empty
        assertEquals(0, observer.currentSize());

        // When — add three entries
        scope.set("a", 1.0f);
        scope.set("b", 2.0f);
        scope.set("c", 3.0f);

        // Then — all three tracked
        assertEquals(3, observer.currentSize());

        // When — remove one entry
        scope.remove("a");

        // Then — size decreases
        assertEquals(2, observer.currentSize());
    }

    @Test
    void molangScopeCacheObserverRemoval() {
        // Given
        var scope = new MolangScope();
        var observer = new MolangScopeCacheObserver(scope);

        // When — add five entries
        scope.set("k1", 1.0f);
        scope.set("k2", 2.0f);
        scope.set("k3", 3.0f);
        scope.set("k4", 4.0f);
        scope.set("k5", 5.0f);

        // Then — all five tracked
        assertEquals(5, observer.currentSize());

        // When — remove two entries
        scope.remove("k1");
        scope.remove("k3");

        // Then — only three remain
        assertEquals(3, observer.currentSize());
    }
}
