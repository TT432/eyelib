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
        // ListAddButton.vue：宽 auto、padding 2px、图标 20px、1px dashed bar 边框 → 高 ≈26
        UIElement row = new UIElement().layout(l -> l
                .widthPercent(100)
                .height(26)
                .flexDirection(FlexDirection.ROW)
                .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                .alignItems(AlignItems.CENTER)
                .marginAll(2));
        Button add = new Button();
        add.setText(Component.empty());
        // dashed 边框 LDLib2 无等价：ColorBorderTexture 实线近似（偏离记录）
        add.buttonStyle(s -> s
                        .baseTexture(new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1, SnowstormTheme.BAR))
                        .hoverTexture(new com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup(
                                new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(SnowstormTheme.DARK),
                                new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1, SnowstormTheme.BAR)))
                        .pressedTexture(new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(SnowstormTheme.DARK)))
                .setOnClick(onClick);
        add.layout(l -> l.widthPercent(100).heightPercent(100)
                .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                .alignItems(AlignItems.CENTER));
        add.addChild(SsIcon.of("plus", 20));
        row.addChild(add);
        return row;
    }
}
//?}
