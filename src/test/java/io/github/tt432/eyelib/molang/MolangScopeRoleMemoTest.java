package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MolangScope} 的 HostContext 角色 memo 契约（Opt14）：
 * <ul>
 *     <li>memo 命中与三步解析（精确→roleStore isInstance 扫描→classStore 回退）结果一致；</li>
 *     <li>任一 store 的 put/remove（两族四个方法）必须使 memo 失效（纪元机制）；</li>
 *     <li>「解析为空」也必须被 memo（NULL_HOST_MARKER），且后续 put 能翻正。</li>
 * </ul>
 *
 * @author TT432
 */
class MolangScopeRoleMemoTest {

    private static final HostRole<ArrayList<?>> ARRAYLIST_ROLE = HostRole.of("memo_al", cast());
    private static final HostRole<List<?>> LIST_ROLE = HostRole.of("memo_list", castList());

    @SuppressWarnings("unchecked")
    private static Class<ArrayList<?>> cast() {
        return (Class<ArrayList<?>>) (Class<?>) ArrayList.class;
    }

    @SuppressWarnings("unchecked")
    private static Class<List<?>> castList() {
        return (Class<List<?>>) (Class<?>) List.class;
    }

    @Test
    void exactRoleHitMemoizedAndConsistent() {
        MolangScope scope = new MolangScope();
        ArrayList<String> value = new ArrayList<>();
        scope.getHostContext().put(ARRAYLIST_ROLE, value);
        assertEquals(Optional.of(value), scope.getHostContext().get(ARRAYLIST_ROLE));
        // 第二次命中 memo，结果必须一致
        assertEquals(Optional.of(value), scope.getHostContext().get(ARRAYLIST_ROLE));
    }

    @Test
    void crossRoleIsInstanceFallbackMemoized() {
        MolangScope scope = new MolangScope();
        ArrayList<String> value = new ArrayList<>();
        // 以具体类角色存入，以父类型角色取出：第 2 步 isInstance 扫描命中
        scope.getHostContext().put(ARRAYLIST_ROLE, value);
        assertEquals(Optional.of(value), scope.getHostContext().get(LIST_ROLE));
        // memo 后再取仍一致
        assertEquals(Optional.of(value), scope.getHostContext().get(LIST_ROLE));
    }

    @Test
    void roleRemoveInvalidatesMemo() {
        MolangScope scope = new MolangScope();
        ArrayList<String> value = new ArrayList<>();
        scope.getHostContext().put(ARRAYLIST_ROLE, value);
        assertEquals(Optional.of(value), scope.getHostContext().get(ARRAYLIST_ROLE));

        scope.getHostContext().remove(ARRAYLIST_ROLE);
        assertTrue(scope.getHostContext().get(ARRAYLIST_ROLE).isEmpty());

        // 重新放入 → 跨角色取出并 memo 正结果 → 再移除 → 跨角色 memo 也必须失效
        scope.getHostContext().put(ARRAYLIST_ROLE, value);
        assertEquals(Optional.of(value), scope.getHostContext().get(LIST_ROLE));
        scope.getHostContext().remove(ARRAYLIST_ROLE);
        assertTrue(scope.getHostContext().get(LIST_ROLE).isEmpty());
    }

    @Test
    void classStoreMutationInvalidatesRoleMemo() {
        MolangScope scope = new MolangScope();
        // 空解析被 memo（NULL_HOST_MARKER）
        assertTrue(scope.getHostContext().get(LIST_ROLE).isEmpty());

        // class 族 put 与 role 解析第 3 步耦合——必须使 role memo 失效
        ArrayList<String> value = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Class<ArrayList<?>> clazz = (Class<ArrayList<?>>) (Class<?>) ArrayList.class;
        scope.getHostContext().put(clazz, value);
        assertEquals(Optional.of(value), scope.getHostContext().get(LIST_ROLE));

        // class 族 remove 同理
        scope.getHostContext().remove(clazz);
        assertTrue(scope.getHostContext().get(LIST_ROLE).isEmpty());
    }

    @Test
    void singleThreadedScopeSameContract() {
        MolangScope scope = MolangScope.singleThreaded();
        ArrayList<String> value = new ArrayList<>();
        assertTrue(scope.getHostContext().get(ARRAYLIST_ROLE).isEmpty()); // memo 空
        scope.getHostContext().put(ARRAYLIST_ROLE, value);
        assertEquals(Optional.of(value), scope.getHostContext().get(ARRAYLIST_ROLE));
        assertEquals(Optional.of(value), scope.getHostContext().get(LIST_ROLE)); // 跨角色
        scope.getHostContext().remove(ARRAYLIST_ROLE);
        assertTrue(scope.getHostContext().get(ARRAYLIST_ROLE).isEmpty());
        assertTrue(scope.getHostContext().get(LIST_ROLE).isEmpty());
    }
}
