//? if >=1.20.1 {
package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 变量表对象成员布局（截图布局：object 父行 + 点分成员行缩进跟随）。
 *
 * <p>纯函数、UI 无关：输入声明序列（名字 + 是否 object），输出行序列（条目 + 缩进深度）。
 * 规则：
 * <ul>
 *   <li>object 类型声明是父；名字以 {@code 父名.} 为前缀的声明是其成员（最长前缀父优先，
 *       多级嵌套按深度累进缩进）；</li>
 *   <li>成员不单独出现在顶层序——由父行带出，紧跟父行之后（成员间保持声明顺序）；</li>
 *   <li>无 object 父的点分名字（孤儿成员）保持原位置平铺，不缩进；</li>
 *   <li>改名父对象不级联改成员名（扁平名既有语义），成员随即变孤儿平铺。</li>
 * </ul>
 */
public final class VariableDisplayGrouping {
    private VariableDisplayGrouping() {
    }

    /** 一行：条目 + 缩进深度（0 = 顶层）。 */
    public record Row<T>(T item, int depth) {
    }

    /**
     * 计算显示行序。
     *
     * @param items    声明（显示前的过滤结果，顺序 = 顶层基准序）
     * @param nameOf   取名字
     * @param isObject 是否 object 类型（父资格）
     */
    public static <T> List<Row<T>> group(List<T> items, Function<T, String> nameOf, Predicate<T> isObject) {
        Map<String, T> objectByName = new HashMap<>();
        for (T item : items) {
            if (isObject.test(item)) {
                objectByName.put(nameOf.apply(item), item);
            }
        }
        List<Row<T>> rows = new ArrayList<>();
        Set<T> emitted = new HashSet<>();
        for (T item : items) {
            if (emitted.contains(item) || parentOf(nameOf.apply(item), objectByName) != null) {
                continue; // 成员由父行带出
            }
            rows.add(new Row<>(item, 0));
            emitted.add(item);
            emitMembers(item, 1, items, nameOf, objectByName, emitted, rows);
        }
        return List.copyOf(rows);
    }

    /** 递归带出 parent 的直接成员（成员的成员按深度累进）。 */
    private static <T> void emitMembers(T parent, int depth, List<T> items, Function<T, String> nameOf,
                                        Map<String, T> objectByName, Set<T> emitted, List<Row<T>> rows) {
        String prefix = nameOf.apply(parent) + ".";
        for (T item : items) {
            if (emitted.contains(item)) {
                continue;
            }
            String name = nameOf.apply(item);
            // 直接成员：最长前缀父即本 parent（等价于 name 以 prefix 开头且去掉该前缀后
            // 剩余段不再以其它 object 名为前缀——由 parentOf 最长匹配保证）
            if (!name.startsWith(prefix) || parentOf(name, objectByName) != parent) {
                continue;
            }
            rows.add(new Row<>(item, depth));
            emitted.add(item);
            if (objectByName.containsKey(name)) {
                emitMembers(item, depth + 1, items, nameOf, objectByName, emitted, rows);
            }
        }
    }

    /** 名字的最长点分前缀 object 父（无 → null）。 */
    private static <T> T parentOf(String name, Map<String, T> objectByName) {
        for (int i = name.lastIndexOf('.'); i > 0; i = name.lastIndexOf('.', i - 1)) {
            T parent = objectByName.get(name.substring(0, i));
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }
}
//?}
