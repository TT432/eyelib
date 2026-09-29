package io.github.tt432.eyelib.snowstorm.event;

import java.util.List;

/**
 * sort.js 拖拽排序的模型侧提取：只保留 mouseup 时对列表的实际重排逻辑
 * （DOM hover 计算属于 UI，不移植）。
 *
 * <p>JS as-is：</p>
 * <pre>
 * if (hover_index != original_index &amp;&amp; hover_index &gt;= 0 &amp;&amp; hover_index &lt;= list.length) {
 *     let original_entry = list.splice(original_index, 1)[0];
 *     if (original_index &lt; hover_index) hover_index--;
 *     list.splice(hover_index, 0, original_entry);
 * }
 * </pre>
 */
public final class Sort {

    private Sort() {
    }

    /**
     * 把 {@code originalIndex} 处的元素移动到 hover 位置。
     *
     * @param hoverIndex JS hover_index：目标元素索引；等于 list.size() 表示移到末尾。
     * @return 是否发生了移动（JS 条件不满足时列表不变）
     */
    public static <T> boolean move(List<T> list, int originalIndex, int hoverIndex) {
        if (hoverIndex != originalIndex && hoverIndex >= 0 && hoverIndex <= list.size()) {
            T originalEntry = list.remove(originalIndex);
            if (originalIndex < hoverIndex) hoverIndex--;
            list.add(hoverIndex, originalEntry);
            return true;
        }
        return false;
    }

    /** JS {@code node.matches(':hover')} 循环的越界守卫：{@code if (i == node_list.length+1) return;}。 */
    public static boolean isAbortIndex(int hoverIndex, int nodeCount) {
        return hoverIndex == nodeCount + 1;
    }
}
