package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link VariableDisplayGrouping} 行序与缩进深度验证。
 *
 * @author TT432
 */
class VariableDisplayGroupingTest {

    /** 测试条目：名字 + 是否 object。 */
    private record V(String name, boolean object) {
    }

    private static List<VariableDisplayGrouping.Row<V>> group(V... vars) {
        return VariableDisplayGrouping.group(List.of(vars), V::name, V::object);
    }

    private static void assertRow(VariableDisplayGrouping.Row<V> row, String name, int depth) {
        assertEquals(name, row.item().name());
        assertEquals(depth, row.depth());
    }

    @Test
    void objectMembersFollowParentIndented() {
        // 截图布局：object 父行 + 成员行缩进一级、保持声明顺序
        List<VariableDisplayGrouping.Row<V>> rows = group(
                new V("qpptaw", true),
                new V("speed", false),
                new V("qpptaw.r", false),
                new V("qpptaw.x", false),
                new V("qpptaw.y", false),
                new V("qpptaw.z", false));
        assertEquals(6, rows.size());
        assertRow(rows.get(0), "qpptaw", 0);
        assertRow(rows.get(1), "qpptaw.r", 1);
        assertRow(rows.get(2), "qpptaw.x", 1);
        assertRow(rows.get(3), "qpptaw.y", 1);
        assertRow(rows.get(4), "qpptaw.z", 1);
        assertRow(rows.get(5), "speed", 0);
    }

    @Test
    void membersDeclaredBeforeParentStillGroup() {
        List<VariableDisplayGrouping.Row<V>> rows = group(
                new V("qpptaw.x", false),
                new V("qpptaw", true));
        assertEquals(2, rows.size());
        assertRow(rows.get(0), "qpptaw", 0);
        assertRow(rows.get(1), "qpptaw.x", 1);
    }

    @Test
    void orphanDottedNameStaysFlat() {
        // 无 object 父声明的点分名字不缩进、保持原位
        List<VariableDisplayGrouping.Row<V>> rows = group(
                new V("a", false),
                new V("foo.bar", false),
                new V("b", false));
        assertEquals(3, rows.size());
        assertRow(rows.get(0), "a", 0);
        assertRow(rows.get(1), "foo.bar", 0);
        assertRow(rows.get(2), "b", 0);
    }

    @Test
    void nestedObjectsNestDepth() {
        List<VariableDisplayGrouping.Row<V>> rows = group(
                new V("a", true),
                new V("a.b", true),
                new V("a.b.c", false),
                new V("a.d", false));
        assertEquals(4, rows.size());
        assertRow(rows.get(0), "a", 0);
        assertRow(rows.get(1), "a.b", 1);
        assertRow(rows.get(2), "a.b.c", 2);
        assertRow(rows.get(3), "a.d", 1);
    }

    @Test
    void nonObjectParentDoesNotGroup() {
        // 父名存在但不是 object → 不分组
        List<VariableDisplayGrouping.Row<V>> rows = group(
                new V("foo", false),
                new V("foo.bar", false));
        assertEquals(2, rows.size());
        assertRow(rows.get(0), "foo", 0);
        assertRow(rows.get(1), "foo.bar", 0);
    }
}
