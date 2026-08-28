package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MolangRuntimeSupport#resolveCall} 非零参解析缓存（shape cache，Opt14）契约：
 * <ul>
 *     <li>按 (名称, 逐参 STRING/NUMBER 形态, host 有无) 缓存变体选择结果；</li>
 *     <li>STRING 与 NUMBER 形态解析到各自重载，互不串扰；</li>
 *     <li>注册表重建/原地 addNode（epoch 自增）后缓存必须失效重解析；</li>
 *     <li>缺失结果同样被缓存，运行时注册后必须翻正。</li>
 * </ul>
 *
 * @author TT432
 */
class MolangRuntimeSupportShapeCacheTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void nonZeroArgResolvesAndCacheSurvivesRepeatCalls() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(DoubleMapping.class)));
        MolangScope scope = new MolangScope();
        MolangObject[] args = {MolangFloat.valueOf(2.5f)};
        assertEquals(5.0f, MolangRuntimeSupport.resolveCall(scope, "math.sc_double", args).asFloat());
        // 第二次命中缓存，结果一致
        assertEquals(5.0f, MolangRuntimeSupport.resolveCall(scope, "math.sc_double", args).asFloat());
    }

    @Test
    void stringAndNumberShapesResolveDistinctOverloads() {
        // 注册键 = (名称, publication signature)：同元数不同参数类型属冲突（后者不生效），
        // 故用不同元数区分重载——形态键 = 参数数 + 逐参 STRING 标记，两种调用形必须
        // 解析到各自变体且缓存互不串扰
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(NumberShapeMapping.class), entry(StringShapeMapping.class)));
        MolangScope scope = new MolangScope();
        assertEquals(1.0f, MolangRuntimeSupport.resolveCall(scope, "math.sc_shape",
                new MolangObject[]{MolangFloat.valueOf(0.5f)}).asFloat());
        assertEquals(2.0f, MolangRuntimeSupport.resolveCall(scope, "math.sc_shape",
                new MolangObject[]{MolangString.valueOf("a"), MolangString.valueOf("b")}).asFloat());
        // 反向再取：两种形态的缓存条目互不污染
        assertEquals(1.0f, MolangRuntimeSupport.resolveCall(scope, "math.sc_shape",
                new MolangObject[]{MolangFloat.valueOf(0.5f)}).asFloat());
    }

    @Test
    void registryRebuildInvalidatesCachedResolution() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(DoubleMapping.class)));
        MolangScope scope = new MolangScope();
        MolangObject[] args = {MolangFloat.valueOf(2.5f)};
        assertEquals(5.0f, MolangRuntimeSupport.resolveCall(scope, "math.sc_double", args).asFloat());

        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(TripleMapping.class)));
        assertEquals(7.5f, MolangRuntimeSupport.resolveCall(scope, "math.sc_double", args).asFloat());
    }

    @Test
    void runtimeAddNodeFixesCachedMissingResolution() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(DoubleMapping.class)));
        MolangScope scope = new MolangScope();
        // 缺失解析（null）进入缓存
        assertTrue(MolangRuntimeSupport.resolveCall(scope, "math.sc_late",
                new MolangObject[]{MolangFloat.valueOf(1.0f)}) instanceof MolangNull);

        // 运行时原地新增 → epoch 自增 → 缓存缺失必须失效
        MolangMappingRegistries.mappingTree().addNode("math",
                new MolangMappingTree.MolangClass(LateMapping.class, false));
        assertEquals(9.0f, MolangRuntimeSupport.resolveCall(scope, "math.sc_late",
                new MolangObject[]{MolangFloat.valueOf(4.5f)}).asFloat());
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("math")
    public static final class DoubleMapping {
        @MolangFunction("sc_double")
        public static float dbl(float v) {
            return v * 2.0f;
        }
    }

    @MolangMapping("math")
    public static final class TripleMapping {
        @MolangFunction("sc_double")
        public static float trp(float v) {
            return v * 3.0f;
        }
    }

    @MolangMapping("math")
    public static final class NumberShapeMapping {
        @MolangFunction("sc_shape")
        public static float byNumber(float v) {
            return 1.0f;
        }
    }

    @MolangMapping("math")
    public static final class StringShapeMapping {
        @MolangFunction("sc_shape")
        public static float byString(String a, String b) {
            return 2.0f;
        }
    }

    @MolangMapping("math")
    public static final class LateMapping {
        @MolangFunction("sc_late")
        public static float late(float v) {
            return v * 2.0f;
        }
    }
}
