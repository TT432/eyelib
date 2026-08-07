package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * LDLib2 {@code SelectorConfigurator} 的懒候选变体（动机见 {@link LazySelector}）。
 * 行为与原版一致，仅候选 UI 推迟到首次展开下拉时构建。
 */
public final class EvmSelectorConfigurator<T> extends ValueConfigurator<T> {
    public final LazySelector<T> selector;

    public EvmSelectorConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate,
                                   @NonNull T defaultValue, boolean forceUpdate,
                                   List<T> candidates, Function<T, String> mapping) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
        inlineContainer.addChild(selector = new LazySelector<>());
        selector.setCandidates(candidates);
        selector.setCandidateUIProvider(candidate -> new Label()
                .textStyle(style -> style
                        .textWrap(TextWrap.HOVER_ROLL)
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .setText(candidate == null ? "---" : mapping.apply(candidate))
                .setOverflowVisible(false));
        selector.setSelected(value, false);
        selector.setOnValueChanged(this::updateValueActively);
    }

    @Override
    protected void onValueUpdatePassively(T newValue) {
        if (newValue == null) {
            newValue = defaultValue;
        }
        if (newValue != null && newValue.equals(value)) {
            return;
        }
        super.onValueUpdatePassively(newValue);
        selector.setSelected(newValue, false);
    }
}
//?}
