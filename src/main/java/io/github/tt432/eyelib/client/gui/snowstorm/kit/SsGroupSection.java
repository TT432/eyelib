package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Snowstorm 输入组区块（Sidebar.vue .input_group as-is）：h4 头（label 灰字 +
 * 可选 help「?」右浮 + 折叠/展开切换）+ 内容容器 + 组间 1px border 分隔线。
 *
 * <p>用法：{@code SsGroupSection section = SsGroupSection.of("Label", helpAction, foldAction);}
 * 内容 {@code section.content()} 加子元素；折叠状态由调用方持有（domain group._folded）。
 */
public final class SsGroupSection extends UIElement {

    private final UIElement content;

    private SsGroupSection(String label, boolean folded,
                           com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener @Nullable [] helpAndFold) {
        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));

        // h4 行：label + 可选 help
        UIElement header = new UIElement().layout(l -> l
                .widthPercent(100)
                .height(SsMetrics.GROUP_HEADER_PADDING * 2 + 12)
                .flexDirection(FlexDirection.ROW)
                .paddingHorizontal(SsMetrics.GROUP_HEADER_PADDING_LEFT)
                .paddingVertical(2));
        Button fold = SsButton.ghost((folded ? "▸ " : "▾ ") + label, e -> {
            if (helpAndFold != null && helpAndFold[1] != null) helpAndFold[1].handleEvent(e);
        });
        fold.layout(l -> l.flex(1).heightPercent(100));
        header.addChild(fold);
        if (helpAndFold != null && helpAndFold[0] != null) {
            Button help = SsButton.ghost("?", helpAndFold[0]);
            help.layout(l -> l.width(14).heightPercent(100));
            header.addChild(help);
        }
        addChild(header);

        content = new UIElement().layout(l -> l
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .paddingLeft(SsMetrics.GROUP_BODY_PADDING)
                .paddingRight(2)
                .paddingVertical(SsMetrics.GROUP_BODY_PADDING));
        if (!folded) {
            addChild(content);
        } else {
            // 折叠指示条（.input_group_folded_indicator as-is）
            Button indicator = SsButton.ghost("...", e -> {
                if (helpAndFold != null && helpAndFold[1] != null) helpAndFold[1].handleEvent(e);
            });
            indicator.layout(l -> l.widthPercent(100).height(SsMetrics.FOLDED_INDICATOR_HEIGHT));
            addChild(indicator);
        }

        // 组间分隔线（.input_group:not(:last-of-type) border-bottom 1px border）
        UIElement divider = new UIElement().layout(l -> l.widthPercent(100).height(1));
        divider.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BORDER)));
        addChild(divider);
    }

    public UIElement content() {
        return content;
    }

    /**
     * @param helpAction null = 无 help 钮；foldAction null = 不可折叠
     */
    public static SsGroupSection of(String label, boolean folded,
                                    com.lowdragmc.lowdraglib2.gui.ui.event.@Nullable UIEventListener helpAction,
                                    com.lowdragmc.lowdraglib2.gui.ui.event.@Nullable UIEventListener foldAction) {
        @SuppressWarnings("unchecked")
        com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener[] pair =
                new com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener[]{helpAction, foldAction};
        return new SsGroupSection(label, folded, pair);
    }
}
//?}
