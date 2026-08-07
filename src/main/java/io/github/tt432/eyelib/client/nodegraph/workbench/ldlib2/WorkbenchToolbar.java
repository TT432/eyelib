package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

/**
 * 工作台顶部工具条（规格 §W1/W3 入口）：「导入」「资产」「变量」「调试」「规范化」按钮。
 * 「资产」「变量」「调试」同时是左右侧栏的折叠开关，按当前可见性显示按下态。
 */
final class WorkbenchToolbar extends UIElement {
    WorkbenchToolbar(Runnable onImport, Runnable onToggleAssets, Runnable onToggleVariables,
                     Runnable onToggleDebug, Runnable onNormalize) {
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
                toolButton("变量", onToggleVariables),
                toolButton("调试", onToggleDebug),
                toolButton("规范化", onNormalize));
    }

    private static Button toolButton(String text, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.literal(text));
        button.textStyle(style -> style.fontSize(9));
        button.setOnClick(event -> onClick.run());
        button.layout(layout -> layout.width(40).heightPercent(100));
        return button;
    }
}
//?}
