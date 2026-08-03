//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.editor.configurator.BooleanConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.StringConfigurator;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 连线感知的端口内联常量配置器（用户决策 2026-08-03：连线 → 禁用编辑 + 变灰）。
 *
 * <p>背景：输入端口同时有「连线」与「内联常量」两条取值途径，codegen 顺序是
 * 连线 > 内联常量 > 端口默认值——连线存在时内联值不参与生成，保持可编辑会误导
 * （UE 蓝图同款：连线后默认值输入禁用）。每帧经 {@link #wired} 同步：
 * 连线 → 内层输入控件 {@code setActive(false)}（交互被外层 WidgetGroup 的
 * active 门控阻断）+ 灰色罩面；断线自动恢复。注意不能把配置器自身置 inactive——
 * 外层按 active 门控 updateScreen，禁用自身会永久锁死（updateScreen 不再被调用）。
 *
 * <p>ldlib2 侧无需对应物：LDLib2 {@code PortConstantEditorElement} 内建
 * 「端口 connected 时移除常量编辑器」。
 */
final class WiredConstantConfigurators {
    private WiredConstantConfigurators() {
    }

    /** 禁用罩面色（半透明黑灰）。 */
    private static final int COLOR_DISABLED_MASK = 0x90000000;

    /** 三个实现共用的罩面逻辑（active 切换只能做在内层输入控件上——外层 WidgetGroup
     * 按 active 门控 updateScreen，禁用自身会永久锁死无法恢复）。 */
    private interface WiredAware {
        BooleanSupplier wired();

        /** 每帧：内层控件与连线态反向同步（配置器自身保持 active 以便持续 tick）。 */
        default void syncInner(Widget inner) {
            boolean isWired = wired().getAsBoolean();
            if (inner.isActive() == isWired) {
                inner.setActive(!isWired);
            }
        }

        /** 连线时罩灰色（画在自身内容之后）。 */
        default void drawMask(GuiGraphics graphics, Widget self) {
            if (wired().getAsBoolean()) {
                DrawerHelper.drawSolidRect(graphics, 0, 0, self.getSizeWidth(), self.getSizeHeight(),
                        COLOR_DISABLED_MASK);
            }
        }
    }

    /** Number 常量（float/int 端口）。 */
    static final class WiredNumber extends NumberConfigurator implements WiredAware {
        private final BooleanSupplier wired;

        WiredNumber(String name, Supplier<Number> supplier, Consumer<Number> consumer,
                    Number defaultValue, boolean forceUpdate, BooleanSupplier wired) {
            super(name, supplier, consumer, defaultValue, forceUpdate);
            this.wired = wired;
        }

        @Override
        public BooleanSupplier wired() {
            return wired;
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            syncInner(textFieldWidget);
        }

        @Override
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            drawMask(graphics, this);
        }
    }

    /** Boolean 常量（bool 端口）。 */
    static final class WiredBoolean extends BooleanConfigurator implements WiredAware {
        private final BooleanSupplier wired;

        WiredBoolean(String name, Supplier<Boolean> supplier, Consumer<Boolean> consumer,
                     boolean defaultValue, boolean forceUpdate, BooleanSupplier wired) {
            super(name, supplier, consumer, defaultValue, forceUpdate);
            this.wired = wired;
        }

        @Override
        public BooleanSupplier wired() {
            return wired;
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            syncInner(switchWidget);
        }

        @Override
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            drawMask(graphics, this);
        }
    }

    /** String 常量（string/ref 端口）。 */
    static final class WiredString extends StringConfigurator implements WiredAware {
        private final BooleanSupplier wired;

        WiredString(String name, Supplier<String> supplier, Consumer<String> consumer,
                    String defaultValue, boolean forceUpdate, BooleanSupplier wired) {
            super(name, supplier, consumer, defaultValue, forceUpdate);
            this.wired = wired;
        }

        @Override
        public BooleanSupplier wired() {
            return wired;
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            syncInner(textFieldWidget);
        }

        @Override
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            drawMask(graphics, this);
        }
    }
}
//?}
