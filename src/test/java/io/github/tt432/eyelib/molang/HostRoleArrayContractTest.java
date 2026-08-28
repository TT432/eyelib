package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt18-D HostRole 驻留 + HostContext 数组索引契约：
 * of() 同参恒同实例（序号稠密）、scope 角色存取/移除/幂等写/类型回退语义不变。
 */
class HostRoleArrayContractTest {

    @SuppressWarnings("unchecked")
    private static Class<ArrayList<?>> arrayListType() {
        return (Class<ArrayList<?>>) (Class<?>) ArrayList.class;
    }

    @SuppressWarnings("unchecked")
    private static Class<List<?>> listType() {
        return (Class<List<?>>) (Class<?>) List.class;
    }

    @Test
    void ofInternsSameNameTypePair() {
        HostRole<ArrayList<?>> a1 = HostRole.of("opt18_al", arrayListType());
        HostRole<ArrayList<?>> a2 = HostRole.of("opt18_al", arrayListType());
        assertSame(a1, a2);
        assertEquals(a1.id(), a2.id());

        HostRole<List<?>> list = HostRole.of("opt18_al", listType());
        // 同 name 不同 type → 不同实例不同序号（注册键 = (名称, 类型)，见 Opt14 注册语义）
        assertTrue(!list.equals(a1));
        assertTrue(list.id() != a1.id());
    }

    @Test
    void roleStorePutGetRemoveSemanticsPreserved() {
        MolangScope scope = new MolangScope();
        HostRole<String> role = HostRole.of("opt18_str", String.class);

        assertTrue(scope.getHostContext().get(role).isEmpty());
        scope.getHostContext().put(role, "hello");
        assertEquals(Optional.of("hello"), scope.getHostContext().get(role));
        scope.getHostContext().remove(role);
        assertTrue(scope.getHostContext().get(role).isEmpty());
    }

    @Test
    void isInstanceFallbackScanWorksWithArraySlots() {
        MolangScope scope = new MolangScope();
        // 以 ArrayList 角色存入，按 List 角色读取 → isInstance 扫描命中
        HostRole<ArrayList<?>> storeRole = HostRole.of("opt18_scan", arrayListType());
        HostRole<List<?>> queryRole = HostRole.of("opt18_scan_query", listType());
        ArrayList<String> value = new ArrayList<>();
        scope.getHostContext().put(storeRole, value);
        Optional<List<?>> found = scope.getHostContext().get(queryRole);
        assertTrue(found.isPresent());
        assertSame(value, found.get());
    }

    @Test
    void idempotentPutDoesNotInvalidateMemo() {
        MolangScope scope = new MolangScope();
        HostRole<String> role = HostRole.of("opt18_idem", String.class);
        scope.getHostContext().put(role, "v");
        assertEquals(Optional.of("v"), scope.getHostContext().get(role)); // memo 填充
        scope.getHostContext().put(role, "v"); // 同实例幂等写：不动纪元
        assertEquals(Optional.of("v"), scope.getHostContext().get(role));
        scope.getHostContext().put(role, "v2"); // 真实变更
        assertEquals(Optional.of("v2"), scope.getHostContext().get(role));
    }

    @Test
    void highIdRolesGrowSlotsBeyondInitialCapacity() {
        MolangScope scope = new MolangScope();
        // 大量新角色把 id 推过初始容量 32，验证扩容路径
        List<HostRole<String>> roles = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            HostRole<String> role = HostRole.of("opt18_grow_" + i, String.class);
            roles.add(role);
            scope.getHostContext().put(role, "v" + i);
        }
        for (int i = 0; i < 64; i++) {
            assertEquals(Optional.of("v" + i), scope.getHostContext().get(roles.get(i)));
        }
    }
}
