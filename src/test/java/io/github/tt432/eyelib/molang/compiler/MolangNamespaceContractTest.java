package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.mapping.api.MolangNamespaces;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态命名空间与运行期成员解析的接入契约。
 *
 * <p>顺序固定为：{@link MolangScope} 覆盖 → 动态命名空间 → 静态映射零参绑定；
 * 静态路径（{@link MolangRuntimeSupport#resolveMemberAccess}）与编译产物内联的
 * 站点路径（{@link MolangRuntimeSupport.MemberSite}）必须一致。
 *
 * @author TT432
 */
class MolangNamespaceContractTest {
    @BeforeEach
    void setUp() {
        MolangMappingRegistries.mappingTree().clear();
        MolangNamespaces.clear();
    }

    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
        MolangNamespaces.clear();
    }

    @Test
    void staticPathResolvesNamespaceMember() {
        MolangNamespaces.register("query.owner",
                (scope, path) -> "health".equals(path) ? MolangFloat.valueOf(9) : null);

        MolangScope scope = new MolangScope();
        assertEquals(9.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.owner.health").asFloat(), 0.0f);
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.owner.missing") instanceof MolangNull);
    }

    @Test
    void namespaceIsClosedLazilyPerAccess() {
        MolangNamespaces.Registration registration =
                MolangNamespaces.register("query.owner", (scope, path) -> MolangFloat.valueOf(9));

        MolangScope scope = new MolangScope();
        assertEquals(9.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.owner.health").asFloat(), 0.0f);

        registration.close();
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.owner.health") instanceof MolangNull);
    }

    @Test
    void scopeOverrideShadowsNamespace() {
        MolangNamespaces.register("query.owner", (scope, path) -> MolangFloat.valueOf(9));

        MolangScope scope = new MolangScope();
        assertEquals(9.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.owner.health").asFloat(), 0.0f);

        // struct 根写入（查询根覆盖位）→ scope 覆盖必须遮蔽命名空间
        scope.set("query.owner.health", 42.0f);
        assertEquals(42.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.owner.health").asFloat(), 0.0f);
    }

    @Test
    void namespaceWinsOverStaticZeroArgBinding() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(SiteMapping.class)));
        MolangNamespaces.register("query.ms_value", (scope, path) -> MolangFloat.valueOf(77));

        assertEquals(77.0f, MolangRuntimeSupport.resolveMemberAccess(new MolangScope(), "query.ms_value").asFloat(), 0.0f);
    }

    @Test
    void namespaceMissFallsThroughToStaticZeroArgBinding() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(SiteMapping.class)));
        MolangNamespaces.register("query", (scope, path) -> null);

        assertEquals(10.0f, MolangRuntimeSupport.resolveMemberAccess(new MolangScope(), "query.ms_value").asFloat(), 0.0f);
    }

    @Test
    void compiledSiteUsesNamespaceAndMatchesStaticPath() {
        MolangNamespaces.Registration registration =
                MolangNamespaces.register("query.owner", (scope, path) -> MolangFloat.valueOf(9));

        CompiledMolangExpression compiled = compile("query.owner.health");
        MolangScope plain = new MolangScope();
        assertEquals(
                MolangRuntimeSupport.resolveMemberAccess(plain, "query.owner.health").asFloat(),
                compiled.evaluate(plain).asFloat(),
                0.0f);
        assertEquals(9.0f, compiled.evaluate(plain).asFloat(), 0.0f);

        MolangScope overridden = new MolangScope();
        overridden.set("query.owner.health", -3.0f);
        assertEquals(
                MolangRuntimeSupport.resolveMemberAccess(overridden, "query.owner.health").asFloat(),
                compiled.evaluate(overridden).asFloat(),
                0.0f);

        registration.close();
        assertTrue(compiled.evaluate(new MolangScope()) instanceof MolangNull);
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
    }
}
