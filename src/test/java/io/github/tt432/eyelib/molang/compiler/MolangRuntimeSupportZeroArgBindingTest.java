package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MolangRuntimeSupport} 零参绑定缓存契约：
 * <ul>
 *     <li>注册表变更（epoch 自增）后缓存绑定必须重解析，不得返回陈旧结果；</li>
 *     <li>scope 覆盖（脚本赋值）始终优先于缓存绑定；</li>
 *     <li>host 有无两种槽位的变体选择相互独立（FULL/MINIMAL 不串扰）。</li>
 * </ul>
 *
 * @author TT432
 */
class MolangRuntimeSupportZeroArgBindingTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void registryMutationInvalidatesCachedBinding() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionOneMapping.class)));
        MolangScope scope = new MolangScope();
        assertEquals(1.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_version").asFloat());

        // 二次解析命中缓存（结果一致是前提，不是断言目标）
        assertEquals(1.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_version").asFloat());

        // 注册表整体重建 → epoch 变更 → 绑定必须重解析为新实现
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionTwoMapping.class)));
        assertEquals(2.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_version").asFloat());
    }

    @Test
    void scopeOverrideWinsOverCachedBinding() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionOneMapping.class)));
        MolangScope scope = new MolangScope();
        assertEquals(1.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_version").asFloat());

        scope.set("query.zab_version", 42.0f);
        assertEquals(42.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_version").asFloat());
    }

    @Test
    void hostPresenceSelectsIndependentVariantSlots() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(HostSpecificMapping.class),
                entry(HostFallbackMapping.class)
        ));

        // 无 host → MINIMAL 槽：host 变体被过滤，回退变体生效
        MolangScope guest = new MolangScope();
        assertEquals(-1.0f, MolangRuntimeSupport.resolveMemberAccess(guest, "query.zab_host").asFloat());

        // 有 host → FULL 槽：高 specificity 的 host 变体生效，且拿到 receiver
        MolangScope hosted = new MolangScope();
        hosted.getHostContext().put(HostRoles.HOST_PRESENCE_MARKER, new Object());
        hosted.getHostContext().put(ZabHost.class, new ZabHost(7.5f));
        assertEquals(7.5f, MolangRuntimeSupport.resolveMemberAccess(hosted, "query.zab_host").asFloat());

        // 再次回到无 host scope：MINIMAL 槽不受 FULL 槽缓存污染
        assertEquals(-1.0f, MolangRuntimeSupport.resolveMemberAccess(guest, "query.zab_host").asFloat());
    }

    @Test
    void zeroArgResolveCallUsesCachedBinding() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionOneMapping.class)));
        MolangScope scope = new MolangScope();
        MolangObject viaCall = MolangRuntimeSupport.resolveCall(scope, "query.zab_version", new MolangObject[0]);
        assertEquals(1.0f, viaCall.asFloat());

        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionTwoMapping.class)));
        assertEquals(2.0f, MolangRuntimeSupport.resolveCall(scope, "query.zab_version", new MolangObject[0]).asFloat());
    }

    @Test
    void unresolvedNameReturnsNullObject() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionOneMapping.class)));
        MolangScope scope = new MolangScope();
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_missing") instanceof MolangNull);
    }
    @Test
    void runtimeAddNodeResolvesPreviouslyMissingFunction() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionOneMapping.class)));
        MolangScope scope = new MolangScope();
        // 函数未注册时求值：NONE 绑定进入零参缓存
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_abc") instanceof MolangNull);
        assertTrue(MolangRuntimeSupport.resolveCall(scope, "query.zab_abc", new MolangObject[0]) instanceof MolangNull);

        // 运行时原地新增（同一树实例 addNode）→ epoch 自增 → 缓存 NONE 必须失效重解析
        MolangMappingRegistries.mappingTree().addNode("query",
                new MolangMappingTree.MolangClass(RuntimeAddedMapping.class, false));
        assertEquals(3.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_abc").asFloat());
        assertEquals(3.0f, MolangRuntimeSupport.resolveCall(scope, "query.zab_abc", new MolangObject[0]).asFloat());
    }

    @Test
    void compiledExpressionPicksUpRuntimeAddedFunction() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionOneMapping.class)));
        MolangScope scope = new MolangScope();
        // 编译发生在函数注册之前：字节码对 query.zab_abc() 发出运行时 resolveCall（ldc 名称），
        // 不做编译期常量折叠——同一份编译产物在注册后必须解析到新实现
        CompiledMolangExpression compiled = new MolangCompilerImpl()
                .compile("query.zab_abc()", CompileContext.defaults());
        assertTrue(compiled.evaluate(scope) instanceof MolangNull);

        MolangMappingRegistries.mappingTree().addNode("query",
                new MolangMappingTree.MolangClass(RuntimeAddedMapping.class, false));
        assertEquals(3.0f, compiled.evaluate(scope).asFloat());
    }

    @Test
    void registryRebuildWithoutFunctionTurnsCachedBindingIntoNull() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(VersionOneMapping.class), entry(RuntimeAddedMapping.class)));
        MolangScope scope = new MolangScope();
        assertEquals(3.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_abc").asFloat());

        // 重建注册表移除该函数（mod 更新删除场景）→ 缓存 METHOD 绑定必须失效
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VersionOneMapping.class)));
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.zab_abc") instanceof MolangNull);
        assertTrue(MolangRuntimeSupport.resolveCall(scope, "query.zab_abc", new MolangObject[0]) instanceof MolangNull);
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class VersionOneMapping {
        @MolangFunction("zab_version")
        public static float version() {
            return 1.0f;
        }
    }

    @MolangMapping("query")
    public static final class VersionTwoMapping {
        @MolangFunction("zab_version")
        public static float version() {
            return 2.0f;
        }
    }

    @MolangMapping("query")
    public static final class HostSpecificMapping {
        @MolangFunction(value = "zab_host", specificity = 100)
        public static float hostSpecific(
                @MolangFunction.Role(MolangFunction.ParameterRole.RECEIVER) ZabHost host
        ) {
            return host.offset();
        }
    }

    @MolangMapping("query")
    public static final class HostFallbackMapping {
        // varargs 形态：与 host 变体 publication signature 区分（同签名变体属注册冲突，
        // 契约同 MolangQueryVariantSelectionMatrixContractTest 的 DefaultRoleFallbackVariantMapping）
        @MolangFunction(value = "zab_host", specificity = Integer.MIN_VALUE)
        public static float hostFallback(float... values) {
            return -1.0f;
        }
    }

    public record ZabHost(float offset) {
    }
    @MolangMapping("query")
    public static final class RuntimeAddedMapping {
        @MolangFunction("zab_abc")
        public static float abc() {
            return 3.0f;
        }
    }
}
