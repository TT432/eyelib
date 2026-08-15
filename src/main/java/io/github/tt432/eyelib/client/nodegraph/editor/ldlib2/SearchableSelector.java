package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 打开后可打字筛选的 {@link Selector}（用户决策 2026-08-16：下拉框都需要点开后可打字筛选）。
 * 对话框顶部内嵌搜索框，{@link #show()} 自动聚焦；输入即按候选显示文本做不区分大小写
 * 子串过滤——只切换条目 display，不重建候选 UI（与 {@link LazySelector} 的懒构建兼容，
 * 重建路径经 {@link #setCandidates} / {@link #setCandidateUIProvider} 重新捕获条目）。
 * Enter 选中首个可见候选并关闭。筛选文本默认 {@code String.valueOf}，用
 * {@link #setSearchTextProvider} 与候选 UI 的显示名对齐。
 *
 * <p>适配层实现而非 fork 补丁：1.21.1/26.1.2 用上游 LDLib2，fork 只服务 1.20.1，
 * 在适配层做单端实现可让三版本行为一致（ADR-0028 同款判断）。
 */
public class SearchableSelector<T> extends Selector<T> {
    private final TextField searchField;
    private Function<T, String> searchTextProvider = value -> value == null ? "---" : String.valueOf(value);
    /** 与 {@link #getCandidates()} 平行的条目 UI（setupDialog 输出，index 一一对应）。 */
    private List<UIElement> itemUis = List.of();

    public SearchableSelector() {
        searchField = new TextField();
        searchField.textFieldStyle(style -> style.placeholder(Component.literal("筛选…")));
        searchField.layout(layout -> layout.widthPercent(100).height(12).marginAll(2));
        searchField.setTextResponder(this::applyFilter);
        searchField.addEventListener(UIEvents.KEY_DOWN, this::onSearchKeyDown);
        dialog.addChildAt(searchField, 0);
    }

    public SearchableSelector<T> setSearchTextProvider(Function<T, String> provider) {
        this.searchTextProvider = provider;
        return this;
    }

    @Override
    public SearchableSelector<T> setCandidates(List<T> candidates) {
        super.setCandidates(candidates);
        captureItems();
        return this;
    }

    @Override
    public SearchableSelector<T> setCandidateUIProvider(UIElementProvider<T> candidateUIProvider) {
        super.setCandidateUIProvider(candidateUIProvider);
        captureItems();
        return this;
    }

    @Override
    public void show() {
        super.show();
        searchField.setText("", false);
        applyFilter("");
        searchField.focus();
    }

    /** setupDialog 后条目铺进 listView（少量候选）或 scrollerView 容器（超 maxItemCount）。 */
    private void captureItems() {
        itemUis = !listView.getChildren().isEmpty()
                ? listView.getChildren()
                : scrollerView.viewContainer.getChildren();
    }

    private void applyFilter(String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        List<T> candidates = getCandidates();
        for (int i = 0; i < itemUis.size() && i < candidates.size(); i++) {
            itemUis.get(i).setDisplay(needle.isEmpty()
                    || searchTextProvider.apply(candidates.get(i)).toLowerCase(Locale.ROOT).contains(needle));
        }
    }

    private void onSearchKeyDown(UIEvent event) {
        if (event.keyCode == GLFW.GLFW_KEY_ENTER || event.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            int index = firstVisibleIndex();
            if (index >= 0) {
                setSelected(getCandidates().get(index));
                hide();
            }
        }
    }

    private int firstVisibleIndex() {
        List<T> candidates = getCandidates();
        int size = Math.min(itemUis.size(), candidates.size());
        for (int i = 0; i < size; i++) {
            if (itemUis.get(i).isDisplayed()) {
                return i;
            }
        }
        return -1;
    }
}
//?}
