package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MolangRuntimeSupport#resolveMemberAccess} query/math 快路径契约（Opt16）：
 * <ul>
 *     <li>链上无覆盖时 query.* 直接走绑定解析，scope cache 不被触碰；</li>
 *     <li>scope 覆盖写入（含 parent 链、struct 根）置位后遮蔽语义保持；</li>
 *     <li>覆盖位判定只认 query/math 根，其余根与非根前缀不置位。</li>
 * </ul>
 *
 * @author TT432
 */
class MolangRuntimeSupportMemberFastPathTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void queryRootSkipsScopeLookupWhenNoOverride() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(FastPathMapping.class)));
        MolangScope scope = new MolangScope();

        assertEquals(5.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.mfp_value").asFloat());
        // 快路径实证：scope cache 未被写入任何遮蔽键，覆盖位保持清零
        assertEquals(0, scope.getCacheSize());
        assertFalse(scope.hasRootOverrideChain(MolangScope.ROOT_OVERRIDE_QUERY));
        assertFalse(scope.hasRootOverrideChain(MolangScope.ROOT_OVERRIDE_MATH));
    }

    @Test
    void queryOverrideSetsBitAndKeepsShadowing() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(FastPathMapping.class)));
        MolangScope scope = new MolangScope();

        scope.set("query.mfp_value", 9.0f);
        assertTrue(scope.hasRootOverrideChain(MolangScope.ROOT_OVERRIDE_QUERY));
        // 遮蔽语义：快路径不得越过 scope 覆盖
        assertEquals(9.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.mfp_value").asFloat());
    }

    @Test
    void parentChainOverrideShadowsThroughFastPath() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(FastPathMapping.class)));
        MolangScope parent = new MolangScope();
        MolangScope child = new MolangScope();
        child.setParent(parent);

        parent.set("query.mfp_value", 7.0f);
        // parent 链置位 → child 快路径让位 → 遮蔽值生效
        assertTrue(child.hasRootOverrideChain(MolangScope.ROOT_OVERRIDE_QUERY));
        assertEquals(7.0f, MolangRuntimeSupport.resolveMemberAccess(child, "query.mfp_value").asFloat());
    }

    @Test
    void structRootWriteSetsBitAndResolvesThroughScope() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(FastPathMapping.class)));
        MolangScope scope = new MolangScope();

        // 三段名写入 struct 根（putTracked(rootKey="query.mfp_value")）也必须置位
        scope.set("query.mfp_value.inner", 3.0f);
        assertTrue(scope.hasRootOverrideChain(MolangScope.ROOT_OVERRIDE_QUERY));
        assertEquals(3.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.mfp_value.inner").asFloat());
    }

    @Test
    void tempAndVariableWritesDoNotSetBits() {
        MolangScope scope = new MolangScope();
        scope.set("temp.a", 1.0f);
        scope.set("variable.b", 2.0f);
        scope.set("context.c", 3.0f);
        assertFalse(scope.hasRootOverrideChain(MolangScope.ROOT_OVERRIDE_QUERY));
        assertFalse(scope.hasRootOverrideChain(MolangScope.ROOT_OVERRIDE_MATH));
    }

    @Test
    void rootOverrideBitOfMatchesOnlyQueryAndMathRoots() {
        assertEquals(MolangScope.ROOT_OVERRIDE_QUERY, MolangScope.rootOverrideBitOf("query.x"));
        assertEquals(MolangScope.ROOT_OVERRIDE_QUERY, MolangScope.rootOverrideBitOf("query"));
        assertEquals(MolangScope.ROOT_OVERRIDE_QUERY, MolangScope.rootOverrideBitOf("query.a.b"));
        assertEquals(MolangScope.ROOT_OVERRIDE_MATH, MolangScope.rootOverrideBitOf("math.sin"));
        assertEquals(MolangScope.ROOT_OVERRIDE_MATH, MolangScope.rootOverrideBitOf("math"));
        assertEquals(0, MolangScope.rootOverrideBitOf("context.item_slot"));
        assertEquals(0, MolangScope.rootOverrideBitOf("variable.x"));
        assertEquals(0, MolangScope.rootOverrideBitOf("temp.x"));
        assertEquals(0, MolangScope.rootOverrideBitOf("queryx.x"));
        assertEquals(0, MolangScope.rootOverrideBitOf("mathx.x"));
        assertEquals(0, MolangScope.rootOverrideBitOf("qu"));
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class FastPathMapping {
        @MolangFunction("mfp_value")
        public static float value() {
            return 5.0f;
        }
    }
}
