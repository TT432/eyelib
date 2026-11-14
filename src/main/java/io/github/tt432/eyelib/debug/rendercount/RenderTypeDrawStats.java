package io.github.tt432.eyelib.debug.rendercount;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * 每帧 RenderType 的 draw 调用计数，供 F3 调试条目读取。
 *
 * <p>帧边界由 {@link #beginFrame()} 标记：当前帧的计数滚动成「上一帧」，因此 F3 读到的是
 * 完整的一帧，而不是读取时刻的半帧。只在客户端渲染线程访问——计数注入点
 * （{@code RenderType.end} / {@code RenderType.draw}）与帧事件同线程。
 *
 * @author TT432
 */
public final class RenderTypeDrawStats {
    /** F3 最多列出的 RenderType 行数；超出的折叠成一行，避免挤占 F3 版面。 */
    private static final int MAX_ROWS = 12;

    private static Object2IntOpenHashMap<String> current = new Object2IntOpenHashMap<>();
    private static Object2IntOpenHashMap<String> previous = new Object2IntOpenHashMap<>();

    private RenderTypeDrawStats() {
    }

    /** 记一次 RenderType 的 draw 调用。 */
    public static void record(String renderTypeName) {
        current.addTo(renderTypeName, 1);
    }

    /** 帧边界：把当前帧计数滚动成「上一帧」，并清空当前帧。 */
    public static void beginFrame() {
        Object2IntOpenHashMap<String> finished = current;
        current = previous;
        current.clear();
        previous = finished;
    }

    /** 上一帧的只读快照，按 draw 次数降序（同次数按名字排序，保证显示稳定）。 */
    public static Snapshot snapshot() {
        if (previous.isEmpty()) {
            return Snapshot.EMPTY;
        }
        int total = 0;
        for (int draws : previous.values()) {
            total += draws;
        }
        List<Object2IntMap.Entry<String>> sorted = new ArrayList<>(previous.object2IntEntrySet());
        sorted.sort((a, b) -> {
            int byDraws = Integer.compare(b.getIntValue(), a.getIntValue());
            return byDraws != 0 ? byDraws : a.getKey().compareTo(b.getKey());
        });
        int rowCount = Math.min(sorted.size(), MAX_ROWS);
        List<Line> lines = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            Object2IntMap.Entry<String> entry = sorted.get(i);
            lines.add(new Line(entry.getKey(), entry.getIntValue()));
        }
        return new Snapshot(total, sorted.size(), lines, sorted.size() - rowCount);
    }

    /** 清空全部计数；测试与开发期重置用。 */
    public static void reset() {
        current.clear();
        previous.clear();
    }

    /**
     * 上一帧的绘制统计快照。
     *
     * @param totalDraws      上一帧的 draw 总次数
     * @param renderTypeCount 上一帧出现过的 RenderType 种类数
     * @param lines           按次数降序的前 {@value RenderTypeDrawStats#MAX_ROWS} 行
     * @param hiddenTypeCount 未列出的种类数
     */
    public record Snapshot(int totalDraws, int renderTypeCount, List<Line> lines, int hiddenTypeCount) {
        static final Snapshot EMPTY = new Snapshot(0, 0, List.of(), 0);

        /** 展开成 F3 显示行；上一帧没有任何绘制时返回空列表。 */
        public List<String> toDebugLines() {
            if (renderTypeCount == 0) {
                return List.of();
            }
            List<String> result = new ArrayList<>(lines.size() + 2);
            result.add("RenderType draws: " + totalDraws + "/frame, " + renderTypeCount + " types");
            for (Line line : lines) {
                result.add("  " + line.name() + ": " + line.draws());
            }
            if (hiddenTypeCount > 0) {
                result.add("  (+" + hiddenTypeCount + " more)");
            }
            return result;
        }
    }

    /** 单个 RenderType 的 draw 次数。 */
    public record Line(String name, int draws) {
    }
}
