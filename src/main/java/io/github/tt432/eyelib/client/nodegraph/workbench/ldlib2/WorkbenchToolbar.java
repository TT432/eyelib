package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

/**
 * 工作台顶部工具条（规格 §W1/W3 入口）：「导入」「资产」「规范化」「自动布局」按钮。
 * 「资产」是左侧栏的折叠开关，按当前可见性显示按下态。
 * 「自动布局」对当前显示图重跑 GraphLayout（自动布局此前只在导入时跑，见 AutoLayoutApplier）。
 * 「变量」「调试」开关在变量表面板头（规格 nodegraph-variable-table §2.7）。
 */
final class WorkbenchToolbar extends UIElement {
    WorkbenchToolbar(Runnable onImport, Runnable onToggleAssets, Runnable onNormalize, Runnable onAutoLayout) {
        layout(layout -> layout
                .widthPercent(100)
                .height(22)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4)
                .paddingHorizontal(4)
                .paddingVertical(2));
        WorkbenchWidgets.panelBackground(this);

        addChildren(
                toolButton("导入", onImport),
                toolButton("资产", onToggleAssets),
                toolButton("规范化", onNormalize),
                toolButton("自动布局", 52, onAutoLayout));
    }

    private static Button toolButton(String text, Runnable onClick) {
        return toolButton(text, 40, onClick);
    }

    private static Button toolButton(String text, int width, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.literal(text));
        button.textStyle(style -> style.fontSize(9));
        button.setOnClick(event -> onClick.run());
        button.layout(layout -> layout.width(width).heightPercent(100));
        return button;
    }
}
//?}
