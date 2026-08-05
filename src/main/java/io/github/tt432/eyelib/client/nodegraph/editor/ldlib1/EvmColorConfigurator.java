//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ColorConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.HsbColorWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * LDLib1 {@link ColorConfigurator} 的取色弹窗修补：原实现在非 LDLib Editor 环境下
 * 把弹窗按色板的「父级相对坐标」加进 mainGroup（y ≈ -108 → 屏外，点击色板看起来
 * 无反应），且 {@link HsbColorWidget} 未启用 alpha 滑条（本项目颜色值为 #AARRGGBB）。
 *
 * <p>子类仅替换色板按钮的点击回调：弹窗宿主沿 parent 链爬根组、位置取鼠标处
 * （节点控件坐标是画布坐标系，非屏幕坐标），并打开 alpha 通道编辑。其余行为
 * （右键复制/粘贴菜单、拖拽换色）保持 LDLib 原样。
 */
public final class EvmColorConfigurator extends ColorConfigurator {

    private static final int DIALOG_SIZE = 110;

    public EvmColorConfigurator(String name, Supplier<Number> supplier, Consumer<Number> onUpdate,
                                Number defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
    }

    @Override
    public void init(int width) {
        super.init(width);
        for (Widget widget : widgets) {
            if (widget instanceof ButtonWidget button) {
                button.setOnPressCallback(this::openColorDialog);
            }
        }
    }

    /** 按鼠标位置弹取色窗（夹取屏内；节点在画布坐标系下，控件绝对坐标不是屏幕坐标）。 */
    private void openColorDialog(ClickData clickData) {
        // 节点内容区的配置器不经 ModularUI.setGui 挂载（gui == null，LDLib 原实现在
        // 此环境必 NPE/弹窗出屏），沿 parent 链爬到根 WidgetGroup 作为弹窗宿主。
        WidgetGroup root = null;
        for (Widget w = this; w != null; w = w.getParent()) {
            if (w instanceof WidgetGroup group) {
                root = group;
            }
        }
        if (root == null) {
            return;
        }
        var window = Minecraft.getInstance().getWindow();
        int mouseX = (int) (Minecraft.getInstance().mouseHandler.xpos()
                * window.getGuiScaledWidth() / window.getScreenWidth());
        int mouseY = (int) (Minecraft.getInstance().mouseHandler.ypos()
                * window.getGuiScaledHeight() / window.getScreenHeight());
        int x = Math.max(0, Math.min(mouseX, window.getGuiScaledWidth() - DIALOG_SIZE));
        int y = Math.max(0, Math.min(mouseY, window.getGuiScaledHeight() - DIALOG_SIZE));
        DialogWidget dialog = new DialogWidget(x, y, DIALOG_SIZE, DIALOG_SIZE);
        dialog.setClickClose(true);
        dialog.setBackground(new GuiTextureGroup(
                ColorPattern.BLACK.rectTexture(), ColorPattern.T_WHITE.borderTexture(-1)));
        dialog.addWidget(new HsbColorWidget(5, 5, 100, 100)
                .setOnChanged(this::onDialogColorChanged)
                .setColorSupplier(() -> value.intValue())
                .setColor(value.intValue())
                .setShowAlpha(true));
        root.addWidget(dialog);
    }

    /** 取色窗变更回写（同 LDLib 原回调语义：直接改值 + updateValue + 刷新色板）。 */
    private void onDialogColorChanged(int newColor) {
        value = newColor;
        updateValue();
        image.setImage(commonColor(newColor));
    }

    /** 色板材质（同 LDLib ColorConfigurator.getCommonColor，该方法为 private 无法复用）。 */
    private static IGuiTexture commonColor(int color) {
        return new GuiTextureGroup(
                new ColorRectTexture(color).setRadius(5),
                new ColorBorderTexture(ColorPattern.WHITE.color, -1).setRadius(5));
    }
}
//?}
