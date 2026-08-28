package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.type.MolangNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MemberSite 站点级单态分发契约（Opt17-A）：编译产物经站点字段求值，
 * 与 {@link MolangRuntimeSupport#resolveMemberAccess} 静态路径语义完全一致：
 * <ul>
 *     <li>绑定解析正确（含零参方法与字段）；</li>
 *     <li>运行时注册（epoch 变更）后站点重解析；</li>
 *     <li>scope 覆盖（含 struct 根写入置位）遮蔽保持。</li>
 * </ul>
 *
 * @author TT432
 */
class MolangMemberSiteContractTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void compiledSiteResolvesBinding() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(SiteMapping.class)));
        CompiledMolangExpression compiled = compile("query.ms_value + query.ms_sum(1, 2)");
        assertEquals(13.0f, compiled.evaluate(new MolangScope()).asFloat());
    }

    @Test
    void siteReResolvesAfterRuntimeRegistration() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(SiteMapping.class)));
        CompiledMolangExpression compiled = compile("query.ms_late");
        assertTrue(compiled.evaluate(new MolangScope()) instanceof MolangNull);

        // 运行时注册（mod 加载后补注册场景）→ 站点纪元守卫必须重解析
        MolangMappingRegistries.mappingTree().addNode("query",
                new MolangMappingTree.MolangClass(LateMapping.class, false));
        assertEquals(7.0f, compiled.evaluate(new MolangScope()).asFloat());
    }

    @Test
    void scopeOverrideShadowsSiteBinding() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(SiteMapping.class)));
        CompiledMolangExpression compiled = compile("query.ms_value");
        MolangScope scope = new MolangScope();
        assertEquals(10.0f, compiled.evaluate(scope).asFloat());

        scope.set("query.ms_value", 42.0f);
        assertEquals(42.0f, compiled.evaluate(scope).asFloat());
    }

    @Test
    void structRootWriteShadowsThroughSite() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(SiteMapping.class)));
        CompiledMolangExpression compiled = compile("query.ms_value.inner");
        MolangScope scope = new MolangScope();
        // 无绑定无覆盖 → null
        assertTrue(compiled.evaluate(scope) instanceof MolangNull);

        // struct 根写入（putTracked("query.ms_value", struct)）置位 → 站点点名走 scope 路径
        scope.set("query.ms_value.inner", 5.0f);
        assertEquals(5.0f, compiled.evaluate(scope).asFloat());
    }

    @Test
    void siteResultMatchesStaticPathAcrossScenarios() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(SiteMapping.class)));
        CompiledMolangExpression compiled = compile("query.ms_value");
        MolangScope plain = new MolangScope();
        assertEquals(
                MolangRuntimeSupport.resolveMemberAccess(plain, "query.ms_value").asFloat(),
                compiled.evaluate(plain).asFloat());

        MolangScope overridden = new MolangScope();
        overridden.set("query.ms_value", -3.0f);
        assertEquals(
                MolangRuntimeSupport.resolveMemberAccess(overridden, "query.ms_value").asFloat(),
                compiled.evaluate(overridden).asFloat());
    }

    private static CompiledMolangExpression compile(String expr) {
        return new MolangCompilerImpl().compile(expr, CompileContext.defaults());
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class SiteMapping {
        @MolangFunction("ms_value")
        public static float value() {
            return 10.0f;
        }

        @MolangFunction("ms_sum")
        public static float sum(float a, float b) {
            return a + b;
        }
    }

    @MolangMapping("query")
    public static final class LateMapping {
        @MolangFunction("ms_late")
        public static float late() {
            return 7.0f;
        }
    }
}
