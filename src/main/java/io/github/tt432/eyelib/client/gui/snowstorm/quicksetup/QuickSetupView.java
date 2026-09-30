package io.github.tt432.eyelib.client.gui.snowstorm.quicksetup;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.QuickSetupPresets;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;

/**
 * QuickSetup.vue 视图 as-is（P5-D）：四组预设（Shape &amp; Motion / Timing / Physics / Sprite）
 * 选项格 + speed/amount/particle_lifetime 滑杆 + lighting/random_rotation 开关 +
 * sprite 选择网格（7 图，底部帧裁剪 = CSS object-position:bottom 静态态）。
 *
 * <p>数据层全部走 {@link QuickSetupPresets}（as-is 单例状态）；构造时调用
 * {@link QuickSetupPresets#mount()}（JS mounted() 语义）。</p>
 *
 * <p>偏离：lucide 图标用文字占位；选项列表 overflow-x:auto 改 flexWrap（LDLib 行溢出处理）；
 * 滑杆 step（0.5/1/0.1）经回调取整实现；sprite 缩略图 hover 帧动画未移植。</p>
 */
public final class QuickSetupView extends UIElement {

    /** sprite 帧数（竖直 strip；1 = 单帧）。 */
    private static final int[][] SPRITE_FRAMES = {
            // {width, frames}；id 与下表 SPRITE_IDS 平行
            {16, 1}, {16, 1}, {16, 1}, {16, 8}, {8, 8}, {16, 4}, {16, 8},
    };
    private static final String[] SPRITE_IDS = {"ball", "dirt", "leaves", "smoke", "dust", "sparkle", "magic"};
    private static final String[] SPRITE_LABELS = {"Ball", "Dirt", "Leaves", "Smoke", "Dust", "Sparkle", "Magic"};

    private boolean showSpriteLicense;

    public QuickSetupView() {
        layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN));
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));
        // JS mounted()：QuickSetup.resetAll 替换 + lighting/random_rotation 从 Input 同步
        QuickSetupPresets.mount();
        rebuild();
    }

    /** 全量重建（选项点击后刷新选中态 + 滑杆值同步）。 */
    private void rebuild() {
        clearAllChildren();
        addChild(buildShapeGroup());
        addChild(buildTimingGroup());
        addChild(buildPhysicsGroup());
        addChild(buildSpriteGroup());
    }

    // ==================================================================
    // 四组（QuickSetup.vue template as-is）
    // ==================================================================

    private UIElement buildShapeGroup() {
        UIElement group = groupBlock("Shape & Motion");
        UIElement list = optionList();
        list.addChildren(
                optionCell("◯", "Sphere", "sphere".equals(QuickSetupPresets.getShape()),
                        () -> apply("shape", "sphere")),
                optionCell("☂", "Rain", "rain".equals(QuickSetupPresets.getShape()),
                        () -> apply("shape", "rain")),
                optionCell("◎", "Ring", "ring".equals(QuickSetupPresets.getShape()),
                        () -> apply("shape", "ring")),
                optionCell("∪", "Gravitate to Center", "gravitate".equals(QuickSetupPresets.getShape()),
                        () -> apply("shape", "gravitate")));
        group.addChild(list);
        group.addChild(sliderBar("Speed", 0, 20, 0.5,
                QuickSetupPresets.getSpeed(), QuickSetupPresets::setSpeed));
        return group;
    }

    private UIElement buildTimingGroup() {
        UIElement group = groupBlock("Timing");
        UIElement list = optionList();
        list.addChildren(
                optionCell("✸", "Burst", "burst".equals(QuickSetupPresets.getTiming()),
                        () -> apply("timing", "burst")),
                optionCell("⏱", "Steady", "steady".equals(QuickSetupPresets.getTiming()),
                        () -> apply("timing", "steady")));
        group.addChild(list);
        group.addChild(sliderBar("Amount", 1, 120, 1,
                QuickSetupPresets.getAmount(), QuickSetupPresets::setAmount));
        group.addChild(sliderBar("Particle Lifetime", 0.1, 10, 0.1,
                QuickSetupPresets.getParticleLifetime(), QuickSetupPresets::setParticleLifetime));
        return group;
    }

    private UIElement buildPhysicsGroup() {
        UIElement group = groupBlock("Physics");
        UIElement list = optionList();
        list.addChildren(
                optionCell("⊘", "None", "none".equals(QuickSetupPresets.getCollision()),
                        () -> apply("collision", "none")),
                optionCell("▣", "Solid", "solid".equals(QuickSetupPresets.getCollision()),
                        () -> apply("collision", "solid")),
                optionCell("☁", "Smoke", "smoke".equals(QuickSetupPresets.getCollision()),
                        () -> apply("collision", "smoke")),
                optionCell("◉", "Ball", "ball".equals(QuickSetupPresets.getCollision()),
                        () -> apply("collision", "ball")),
                optionCell("▤", "Paper", "paper".equals(QuickSetupPresets.getCollision()),
                        () -> apply("collision", "paper")));
        group.addChild(list);
        return group;
    }

    private UIElement buildSpriteGroup() {
        UIElement group = groupBlock("Sprite");

        // CC0 许可开关（CreativeCommons 图标 → © 文字占位）
        Button licenseToggle = new Button();
        licenseToggle.setText(Component.literal("©"))
                .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                .buttonStyle(style -> style
                        .baseTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(event -> {
                    showSpriteLicense = !showSpriteLicense;
                    rebuild();
                });
        licenseToggle.layout(layout -> layout
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(8).top(4).width(14).height(12));
        group.addChild(licenseToggle);
        if (showSpriteLicense) {
            TextElement license = text(
                    "All sprites listed below are copyright-free (CC0) and free to use and modify.",
                    SnowstormTheme.TEXT_GRAYED, 9);
            license.layout(layout -> layout.widthPercent(100).paddingHorizontal(12).paddingTop(2));
            group.addChild(license);
        }

        UIElement list = optionList();
        for (int i = 0; i < SPRITE_IDS.length; i++) {
            String id = SPRITE_IDS[i];
            list.addChild(spriteCell(id, SPRITE_LABELS[i], SPRITE_FRAMES[i],
                    id.equals(QuickSetupPresets.getSprite())));
        }
        group.addChild(list);

        // input_bar as-is：Random Rotation 在前，Glow in the dark 在后
        group.addChild(switchBar("Random Rotation", QuickSetupPresets.isRandomRotation(),
                QuickSetupPresets::setRandomRotation));
        group.addChild(switchBar("Glow in the dark", QuickSetupPresets.isLighting(),
                QuickSetupPresets::setLighting));
        return group;
    }

    // ==================================================================
    // 控件构造
    // ==================================================================

    private void apply(String key, String value) {
        QuickSetupPresets.set(key, value);
        rebuild();
    }

    private static UIElement groupBlock(String title) {
        UIElement group = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN));
        group.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));
        TextElement header = text(title, SnowstormTheme.TEXT_GRAYED, 10);
        header.layout(layout -> layout.widthPercent(100).height(12).paddingLeft(12).paddingTop(10));
        group.addChild(header);
        return group;
    }

    /** .preset_option_list（JS overflow-x:auto → flexWrap 偏离已记录）。 */
    private static UIElement optionList() {
        return new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .gapAll(2));
    }

    /** 预设选项格（80px 宽，选中 bg=--color-bar as-is；lucide 图标文字占位）。 */
    private static Button optionCell(String icon, String label, boolean selected, Runnable onClick) {
        Button cell = new Button();
        cell.setText(Component.literal(icon + "\n" + label))
                .textStyle(style -> style
                        .fontSize(9)
                        .textColor(selected ? SnowstormTheme.HIGHLIGHT : SnowstormTheme.TEXT))
                .buttonStyle(style -> style
                        .baseTexture(selected
                                ? new ColorRectTexture(SnowstormTheme.BAR)
                                : com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(event -> onClick.run());
        cell.layout(layout -> layout.width(80).height(30));
        return cell;
    }

    /** sprite 选项格：45×45 底部帧缩略图 + 标签。 */
    private static UIElement spriteCell(String id, String label, int[] shape, boolean selected) {
        Button cell = new Button();
        cell.buttonStyle(style -> style
                        .baseTexture(selected
                                ? new ColorRectTexture(SnowstormTheme.BAR)
                                : com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)));
        cell.layout(layout -> layout.width(80).height(64).paddingAll(8));

        String texture = ClasspathSpriteTextures.sprite(id);
        if (texture != null) {
            int w = shape[0];
            int frames = shape[1];
            SpriteTexture sprite = SpriteTexture.of(texture);
            if (frames > 1) {
                // CSS object-position:bottom 静态态：显示底部帧（hover 帧动画未移植）
                sprite.setSprite(0, w * (frames - 1), w, w);
            }
            UIElement image = new UIElement().layout(layout -> layout
                    .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                    .left(17).top(2).width(45).height(45));
            image.style(style -> style.backgroundTexture(sprite));
            cell.addChild(image);
        }
        TextElement text = text(label, selected ? SnowstormTheme.HIGHLIGHT : SnowstormTheme.TEXT, 9);
        text.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        text.layout(layout -> layout
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(0).right(0).bottom(2));
        cell.addChild(text);

        // 点击后重建由外层监听负责：此处直接内联（cell 无法回溯外层 rebuild，经静态回调）
        cell.setOnClick(event -> {
            QuickSetupPresets.set("sprite", id);
            // 重建选中态：父级 QuickSetupView 在下一轮 set 后由 apply() 刷新；
            // sprite 格走自身路径，直接重建父视图
            if (cell.getParent() != null && cell.getParent().getParent() instanceof QuickSetupView view) {
                view.rebuild();
            }
        });
        return cell;
    }

    /** .input_bar 滑杆行：label + range + range_number_label（46px as-is）。 */
    private static UIElement sliderBar(String label, double min, double max, double step,
                                       double current, java.util.function.DoubleConsumer onChange) {
        UIElement bar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(14)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .paddingHorizontal(14)
                .gapAll(8)
                .marginVertical(5));
        TextElement labelElement = text(label, SnowstormTheme.TEXT, 9);
        bar.addChild(labelElement);

        TextElement numberLabel = text(JsSemantics.toJsString(current), SnowstormTheme.TEXT, 9);
        numberLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        numberLabel.layout(layout -> layout.width(46));

        Slider slider = new Slider.Horizontal();
        slider.setRange((float) min, (float) max);
        slider.setValue((float) current, false);
        slider.setOnValueChanged(value -> {
            // JS input[type=range] step（0.5/1/0.1）：回调取整对齐
            double stepped = Math.round(value / step) * step;
            onChange.accept(stepped);
            numberLabel.setText(Component.literal(JsSemantics.toJsString(stepped)));
        });
        slider.layout(layout -> layout.flex(1).height(12));

        bar.addChildren(slider, numberLabel);
        return bar;
    }

    /** .input_bar 开关行（label + Switch）。 */
    private static UIElement switchBar(String label, boolean current,
                                       java.util.function.Consumer<Boolean> onChange) {
        UIElement bar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(14)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .paddingHorizontal(14)
                .gapAll(8)
                .marginVertical(5));
        TextElement labelElement = text(label, SnowstormTheme.TEXT, 9);
        labelElement.layout(layout -> layout.flex(1));
        Switch toggle = new Switch();
        toggle.setOn(current, false);
        toggle.setOnSwitchChanged(value -> onChange.accept(value));
        toggle.layout(layout -> layout.width(24).height(12));
        bar.addChildren(labelElement, toggle);
        return bar;
    }

    private static TextElement text(String text, int color, int fontSize) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(fontSize).textColor(color));
        return element;
    }
}
//?}
