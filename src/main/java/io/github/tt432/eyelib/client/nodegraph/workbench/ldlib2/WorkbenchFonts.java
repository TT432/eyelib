package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
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
 * + {@code GuiGraphicsExtractor#text}；1.20.1 移植版（2.2.27）的 LDLibFonts
 * 无 font()/drawText 入口，同走 vanilla 字体 + {@code GuiGraphics#drawString}。
 *
 * <p>三个 {@code draw} 重载的 graphics 参数类型本身即版本专属类型，故连签名一起
 * 用 {@code //?} 分支（import 无法两版共存的类型一律全限定名内联）。
 */
final class WorkbenchFonts {
    private WorkbenchFonts() {
    }

    static Font font() {
        //? if modern {
        return Minecraft.getInstance().font;
        //?} else if legacy {
        // LDLib2 1.20.1 移植版（2.2.27）的 LDLibFonts 没有 font()/drawText 入口（2.2.28+ 才加入），
        // 退化为 vanilla 字体——徽标是调试文本，字体 advance 差异不影响布局正确性。
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
    //?} else if legacy {
    /** 在 (x, y) 处绘制单行文本（不带阴影）。 */
    static void draw(net.minecraft.client.gui.GuiGraphics graphics, String text, float x, float y, int color) {
        graphics.drawString(font(), Component.literal(text), Math.round(x), Math.round(y), color, false);
    }
    //?} else {
    /** 在 (x, y) 处绘制单行文本（不带阴影）。 */
    static void draw(net.minecraft.client.gui.GuiGraphics graphics, String text, float x, float y, int color) {
        LDLibFonts.drawText(graphics, font(), Component.literal(text), x, y, color, false);
    }
    //?}
}
//?}
