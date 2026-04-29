package io.github.tt432.eyelibmolang.compiler.cache;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.compiler.BoundMolangCompilerInput;
import io.github.tt432.eyelibmolang.compiler.BoundMolangEvaluator;
import io.github.tt432.eyelibmolang.compiler.CompileContext;
import io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression;
import io.github.tt432.eyelibmolang.compiler.MolangBytecodeEmitter;
import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MolangDiskCacheTest {
    private static final int COMPILER_VERSION = 1;

    @TempDir
    Path tempDir;

    private MolangDiskCache diskCache;
    private String registryRef;

    @BeforeEach
    void setUp() {
        diskCache = new MolangDiskCache(tempDir);
        registryRef = MolangMappingTree.INSTANCE.registryVersionRef().value();
    }

    @Test
    void writeReadRoundTrip() throws Exception {
        String expr = "1+2";
        byte[] expected = emitBytecode(expr);

        diskCache.write(expected, expr, registryRef, COMPILER_VERSION);

        byte[] actual = diskCache.read(expr, registryRef, COMPILER_VERSION);
        assertNotNull(actual);
        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    void readMissingFileReturnsNull() throws Exception {
        assertNull(diskCache.read("missing_expr", registryRef, COMPILER_VERSION));
    }

    @Test
    void writeTwiceOverwrites() throws Exception {
        String expr = "1+2";
        byte[] first = new byte[]{1, 2, 3};
        byte[] second = new byte[]{9, 8, 7, 6};

        diskCache.write(first, expr, registryRef, COMPILER_VERSION);
        diskCache.write(second, expr, registryRef, COMPILER_VERSION);

        byte[] actual = diskCache.read(expr, registryRef, COMPILER_VERSION);
        assertNotNull(actual);
        assertArrayEquals(second, actual);
    }

    @Test
    void corruptedMagicReturnsNull() throws Exception {
        String expr = "1+2";
        byte[] bytes = emitBytecode(expr);
        diskCache.write(bytes, expr, registryRef, COMPILER_VERSION);

        Path cacheFile = cacheFileFor(expr);
        byte[] all = Files.readAllBytes(cacheFile);
        all[0] = 0x00;
        Files.write(cacheFile, all, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        assertNull(diskCache.read(expr, registryRef, COMPILER_VERSION));
    }

    @Test
    void truncatedFileReturnsNull() throws Exception {
        String expr = "1+2";
        byte[] bytes = emitBytecode(expr);
        diskCache.write(bytes, expr, registryRef, COMPILER_VERSION);

        Path cacheFile = cacheFileFor(expr);
        Files.write(cacheFile, new byte[]{0x4D, 0x4F, 0x4C, 0x43, 0x00}, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        assertNull(diskCache.read(expr, registryRef, COMPILER_VERSION));
    }

    @Test
    void checksumMismatchReturnsNull() throws Exception {
        String expr = "1+2";
        byte[] bytes = emitBytecode(expr);
        diskCache.write(bytes, expr, registryRef, COMPILER_VERSION);

        Path cacheFile = cacheFileFor(expr);
        byte[] all = Files.readAllBytes(cacheFile);
        int classDataTailIndex = all.length - Integer.BYTES - 1;
        all[classDataTailIndex] = (byte) (all[classDataTailIndex] ^ 0x01);
        Files.write(cacheFile, all, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        assertNull(diskCache.read(expr, registryRef, COMPILER_VERSION));
    }

    @Test
    void compilerVersionMismatchReturnsNull() throws Exception {
        String expr = "1+2";
        byte[] bytes = emitBytecode(expr);
        diskCache.write(bytes, expr, registryRef, 1);

        assertNull(diskCache.read(expr, registryRef, 2));
    }

    @Test
    void registryVersionRefMismatchReturnsNull() throws Exception {
        String expr = "1+2";
        byte[] bytes = emitBytecode(expr);
        diskCache.write(bytes, expr, "abc", COMPILER_VERSION);

        assertNull(diskCache.read(expr, "def", COMPILER_VERSION));
    }

    @Test
    void l1MemoryHitNoDiskIo() {
        MolangCompileCache cache = new MolangCompileCache(MolangMappingTree.INSTANCE, tempDir);
        AtomicInteger supplierCalls = new AtomicInteger(0);

        CompiledMolangExpression first = cache.getOrCompile("1+2", () -> {
            supplierCalls.incrementAndGet();
            return buildEvaluator("1+2");
        });
        CompiledMolangExpression second = cache.getOrCompile("1+2", () -> {
            supplierCalls.incrementAndGet();
            return buildEvaluator("1+2");
        });

        assertSame(first, second);
        assertEquals(1, supplierCalls.get());
    }

    @Test
    void l2DiskHitOnFreshCache() throws Exception {
        String expr = "1+2";
        byte[] bytes = emitBytecode(expr);
        diskCache.write(bytes, expr, registryRef, COMPILER_VERSION);

        MolangCompileCache freshCache = new MolangCompileCache(MolangMappingTree.INSTANCE, tempDir);
        AtomicInteger supplierCalls = new AtomicInteger(0);

        CompiledMolangExpression result = freshCache.getOrCompile(expr, () -> {
            supplierCalls.incrementAndGet();
            fail("Supplier should not be called on L2 hit");
            return buildEvaluator(expr);
        });

        assertEquals(0, supplierCalls.get(), "Supplier should not be called on L2 hit");
        assertEquals(3.0F, result.evaluate(new MolangScope()).asFloat(), 0.0001F);
    }

    @Test
    void evaluatorFallbackNotWrittenToDisk() {
        String expr = "1+2";
        MolangCompileCache cache = new MolangCompileCache(MolangMappingTree.INSTANCE, tempDir);

        CompiledMolangExpression result = cache.getOrCompile(expr, () -> buildEvaluator(expr));

        assertNotNull(result);
        assertTrue(result instanceof BoundMolangEvaluator);
        assertFalse(Files.exists(cacheFileFor(expr)));
    }

    @Test
    void concurrentReadWrite() throws Exception {
        int workers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                int index = i;
                tasks.add(() -> {
                    String expr = "expr_" + index;
                    byte[] data = new byte[]{(byte) index, (byte) (index + 1), (byte) (index + 2)};
                    diskCache.write(data, expr, registryRef, COMPILER_VERSION);
                    byte[] readBack = diskCache.read(expr, registryRef, COMPILER_VERSION);
                    assertNotNull(readBack);
                    assertArrayEquals(data, readBack);
                    return null;
                });
            }

            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Path cacheFileFor(String expr) {
        return tempDir.resolve(MolangDiskCache.computeFileName(expr) + ".molcache");
    }

    private static byte[] emitBytecode(String expression) {
        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE
                .parseExprSetAst(expression)
                .orElseThrow(() -> new AssertionError("Expected parser to accept expression: " + expression));

        BindResult bindResult = new MolangBinder().bind(ast);
        assertTrue(bindResult.diagnostics().isEmpty(), "Expected bind diagnostics to be empty for expression: " + expression);

        return MolangBytecodeEmitter.emit(new BoundMolangCompilerInput(
                expression,
                bindResult.root(),
                CompileContext.defaults()
        ));
    }

    private static BoundMolangEvaluator buildEvaluator(String expression) {
        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE
                .parseExprSetAst(expression)
                .orElseThrow(() -> new AssertionError("Expected parser to accept expression: " + expression));

        BindResult bindResult = new MolangBinder().bind(ast);
        assertTrue(bindResult.diagnostics().isEmpty(), "Expected bind diagnostics to be empty for expression: " + expression);

        return new BoundMolangEvaluator(expression, bindResult.root(), MolangMappingTree.INSTANCE);
    }
}
