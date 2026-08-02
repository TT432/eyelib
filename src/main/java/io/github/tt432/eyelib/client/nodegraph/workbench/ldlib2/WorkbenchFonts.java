package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * 文本绘制/测量的版本分歧收敛点（UDF）：
 * 1.21.1 走 LDLib2 的 SDF 字体管线（{@link LDLibFonts#font()} + 带缓存的
 * {@link LDLibFonts#drawText}，graphics 为 vanilla {@code GuiGraphics}）；
 * 26.1 移除了这两个入口，且渲染上下文改为 {@code GuiGraphicsExtractor}
 * （IGUIContext 体系），TextElement 直接用 {@code Minecraft.getInstance().font}
 * + {@code GuiGraphicsExtractor#text}。两版字体 advance 一致（LDLibFonts javadoc 保证）。
 *
 * <p>两个 {@code draw} 重载的 graphics 参数类型本身即版本专属类型，故连签名一起
 * 用 {@code //?} 分支（import 无法两版共存的类型一律全限定名内联）。
 */
final class WorkbenchFonts {
    private WorkbenchFonts() {
    }

    static Font font() {
        //? if modern {
        return Minecraft.getInstance().font;
        //?} else {
        return LDLibFonts.font();
        //?}
    }

    static int width(String text) {
        return font().width(text);
    }

    //? if modern {
    /** 在 (x, y) 处绘制单行文本（不带阴影）。 */
    static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics, String text, float x, float y, int color) {
        graphics.text(font(), Component.literal(text).getVisualOrderText(), Math.round(x), Math.round(y), color, false);
    }
    //?} else {
    /** 在 (x, y) 处绘制单行文本（不带阴影）。 */
    static void draw(net.minecraft.client.gui.GuiGraphics graphics, String text, float x, float y, int color) {
        LDLibFonts.drawText(graphics, font(), Component.literal(text), x, y, color, false);
    }
    //?}
}
//?}
