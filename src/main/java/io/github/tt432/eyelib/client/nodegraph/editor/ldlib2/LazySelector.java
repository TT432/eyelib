package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import java.util.List;

/**
 * 候选 UI 懒构建的 {@link SearchableSelector}：LDLib2 的 {@code setCandidates} 会立即为每个候选
 * 创建 UIElement（含 Button 覆盖层），百级候选 × 节点级数量在 loadGraph 时是分钟级卡顿
 * （实证：悦灵 811 节点打开 35s+，jstack 停在 Selector.setupDialog）。此类把候选 UI
 * 构建推迟到首次展开下拉（{@link #show()}）；选中值预览不依赖候选 UI
 * （setValue 只走 candidateUIProvider），因此延迟构建不影响显示。
 */
final class LazySelector<T> extends SearchableSelector<T> {
    private List<T> pending = List.of();
    private boolean built;

    @Override
    public SearchableSelector<T> setCandidates(List<T> candidates) {
        if (built) {
            return super.setCandidates(candidates);
        }
        pending = candidates;
        return this;
    }

    @Override
    public void show() {
        if (!built) {
            built = true;
            super.setCandidates(pending);
        }
        super.show();
    }
}
//?}
