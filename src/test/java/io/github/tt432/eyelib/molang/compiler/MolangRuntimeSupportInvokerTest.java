package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MolangRuntimeSupport} 精确签名组合 invoker 契约：零参绑定的
 * （engine 槽 / receiver 槽 / 可见参数缺省 / varargs / 静态字段 / 异常归 Null）
 * 各形态经组合 invokeExact 路径的结果必须与旧泛型路径逐位一致。
 *
 * @author TT432
 */
class MolangRuntimeSupportInvokerTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void engineSlotReceivesScope() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(EngineSlotMapping.class)));
        MolangScope scope = new MolangScope();
        scope.set("variable.inv_marker", 11.0f);
        // engine 槽拿 scope 本体，可读 variable.*
        assertEquals(11.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_engine").asFloat());
        // 二次调用（缓存绑定 + 组合 invoker 命中）结果一致
        assertEquals(11.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_engine").asFloat());
        // 快路径必须真实生效（防静默回退：结果正确但走泛型路径）
        assertTrue(MolangRuntimeSupport.hasComposedInvoker("query.inv_engine", false));
    }

    @Test
    void receiverSlotRefetchedPerCall() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(ReceiverMapping.class)));
        MolangScope scope = new MolangScope();
        scope.getHostContext().put(InvHost.class, new InvHost(1.5f));
        assertEquals(1.5f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_receiver").asFloat());

        // host 值变化必须被下次调用看到（invoker 不得冻结 host 引用）
        scope.getHostContext().put(InvHost.class, new InvHost(2.5f));
        assertEquals(2.5f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_receiver").asFloat());
        assertTrue(MolangRuntimeSupport.hasComposedInvoker("query.inv_receiver", true));
    }

    @Test
    void requiredVisibleArgIsUnresolvableAsZeroArg() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VisibleArgMapping.class)));
        MolangScope scope = new MolangScope();
        // 既有语义（与 invoker 无关，旧路径同）：带必需可见参数的函数不匹配零参变体选择，
        // resolveMemberAccess / 零参 resolveCall 均归 MolangNull
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_visible") instanceof MolangNull);
        assertTrue(MolangRuntimeSupport.resolveCall(scope, "query.inv_visible", new MolangObject[0]) instanceof MolangNull);
        // 提供实参时正常解析（可见槽转换路径不受影响）
        assertEquals(105.0f, MolangRuntimeSupport.resolveCall(scope, "query.inv_visible",
                new MolangObject[]{io.github.tt432.eyelib.molang.type.MolangFloat.valueOf(5.0f)}).asFloat());
    }

    @Test
    void varargsOnlyFunctionGetsEmptyArray() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(VarargsMapping.class)));
        MolangScope scope = new MolangScope();
        assertEquals(0.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_varargs").asFloat());
        assertTrue(MolangRuntimeSupport.hasComposedInvoker("query.inv_varargs", false));
    }

    @Test
    void staticFieldBindingReadsCurrentValue() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(FieldMapping.class)));
        MolangScope scope = new MolangScope();
        assertEquals(3.5f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_field").asFloat());
        assertTrue(MolangRuntimeSupport.hasComposedInvoker("query.inv_field", false));
    }

    @Test
    void throwingFunctionYieldsNullObject() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(ThrowingMapping.class)));
        MolangScope scope = new MolangScope();
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_throws") instanceof MolangNull);
        // 经 resolveCall 零参快路径同样归 Null
        assertTrue(MolangRuntimeSupport.resolveCall(scope, "query.inv_throws", new MolangObject[0]) instanceof MolangNull);
    }

    @Test
    void mixedSlotsComposedTogether() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(MixedSlotMapping.class)));
        MolangScope scope = new MolangScope();
        scope.set("variable.inv_base", 2.0f);
        scope.getHostContext().put(InvHost.class, new InvHost(4.0f));
        // engine 槽 + receiver 槽 + 空 varargs 可见槽混合：2(scope) * 4(host) + 0 = 8
        assertEquals(8.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_mixed").asFloat());
        assertTrue(MolangRuntimeSupport.hasComposedInvoker("query.inv_mixed", true));
    }

    @Test
    void runtimeMutationStillInvalidatesInvoker() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(EngineSlotMapping.class)));
        MolangScope scope = new MolangScope();
        assertTrue(MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_late") instanceof MolangNull);

        // 运行时新增 → epoch 自增 → 组合 invoker 必须随之重解析
        MolangMappingRegistries.mappingTree().addNode("query",
                new MolangMappingTree.MolangClass(LateAddedMapping.class, false));
        assertEquals(9.0f, MolangRuntimeSupport.resolveMemberAccess(scope, "query.inv_late").asFloat());
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    public record InvHost(float scale) {
    }

    @MolangMapping("query")
    public static final class EngineSlotMapping {
        @MolangFunction("inv_engine")
        public static float engine(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return scope.get("variable.inv_marker").asFloat();
        }
    }

    @MolangMapping("query")
    public static final class ReceiverMapping {
        @MolangFunction("inv_receiver")
        public static float receiver(@MolangFunction.Role(MolangFunction.ParameterRole.RECEIVER) InvHost host) {
            return host.scale();
        }
    }

    @MolangMapping("query")
    public static final class VisibleArgMapping {
        @MolangFunction("inv_visible")
        public static float visible(float value) {
            return 100.0f + value;
        }
    }

    @MolangMapping("query")
    public static final class VarargsMapping {
        @MolangFunction("inv_varargs")
        public static float varargs(float... values) {
            return values.length;
        }
    }

    @MolangMapping("query")
    public static final class FieldMapping {
        // 字段映射无注解：MolangMappingTree 按 public 字段名直接登记（见 findField）
        public static final float inv_field = 3.5f;
    }

    @MolangMapping("query")
    public static final class ThrowingMapping {
        @MolangFunction("inv_throws")
        public static float throwsAlways() {
            throw new IllegalStateException("boom");
        }
    }

    @MolangMapping("query")
    public static final class MixedSlotMapping {
        @MolangFunction("inv_mixed")
        public static float mixed(
                @MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope,
                @MolangFunction.Role(MolangFunction.ParameterRole.RECEIVER) InvHost host,
                float... visible
        ) {
            // engine 槽 + receiver 槽 + 空 varargs 可见槽混合：2(scope) * 4(host) + 0(空 varargs) = 8
            float sum = 0;
            for (float v : visible) {
                sum += v;
            }
            return scope.get("variable.inv_base").asFloat() * host.scale() + sum;
        }
    }

    @MolangMapping("query")
    public static final class LateAddedMapping {
        @MolangFunction("inv_late")
        public static float late() {
            return 9.0f;
        }
    }
}
