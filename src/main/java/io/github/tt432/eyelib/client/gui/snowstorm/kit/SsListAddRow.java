package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import net.minecraft.network.chat.Component;

/**
 * Snowstorm 列表添加行（#add_curve_button as-is）：1px dashed bar 边框 + 居中 ＋、
 * hover dark 底。
 */
public final class SsListAddRow {

    private SsListAddRow() {
    }

    public static UIElement of(com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener onClick) {
        UIElement row = new UIElement().layout(l -> l
                .widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                .alignItems(AlignItems.CENTER)
                .marginAll(2));
        Button add = SsButton.ghost("＋", onClick);
        add.layout(l -> l.widthPercent(100).heightPercent(100));
        row.addChild(add);
        // dashed 边框 LDLib2 无等价：不画边框（偏离记录）
        return row;
    }
}
//?}
