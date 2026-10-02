package io.github.tt432.eyelib.client.gui.snowstorm.help;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.client.gui.snowstorm.kit.SsButton;
import io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIcon;
import io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton;
import io.github.tt432.eyelib.snowstorm.help.HelpData;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Snowstorm HelpPanel.vue as-is 移植（LDLib2）：右浮文档面板（宽 482、top 32、bottom 33、
 * 1px border、INTERFACE 底），内容 = 类目总览（Documentation 列表）或单页
 * （h1 标题 + HelpText 行 + HelpInputList）。
 *
 * <p>偏离：HTML 行降级为纯文本（MC 无 HTML 渲染）；链接点击经 {@link #setOnOpenLink}
 * 接缝（未接线时打日志）。input_list 的 HelpInputList 图标类型徽标按
 * HelpInputList.vue labels/as-is。
 */
public final class HelpPanelView extends UIElement {

    /** HelpPanel.vue: width 482px。 */
    public static final int WIDTH = 482;
    /** code 行说明色（HelpText.vue code color: #50cca7）。 */
    private static final int CODE_COLOR = 0xFF50CCA7;

    /** HelpPanel.vue data: category_key（'' = 总览）。 */
    private String categoryKey = "";
    /** HelpPanel.vue data: page_key。 */
    private String pageKey = "";

    private final ScrollerView content;
    private final UIElement header;
    private @Nullable Consumer<String> onOpenLink;
    private @Nullable Runnable onClose;

    public HelpPanelView() {
        // HelpPanel.vue：width 482px、max-width 100%
        layout(layout -> layout.width(WIDTH).maxWidthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN));
        style(style -> style.backgroundTexture(new com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup(
                new ColorRectTexture(SnowstormTheme.INTERFACE),
                new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1, SnowstormTheme.BORDER))));

        // .help_header：close X（22px）+ back 按钮（h29，有页面时显示）
        header = new UIElement().layout(l -> l.widthPercent(100).height(30)
                .flexDirection(FlexDirection.ROW)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        addChild(header);
        content = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsScroller.plain(new ScrollerView());
        content.layout(l -> l.widthPercent(100).flex(1));
        content.viewContainer.layout(l -> l.paddingAll(14));
        addChild(content);

        rebuild();
    }

    public HelpPanelView setOnOpenLink(@Nullable Consumer<String> value) {
        onOpenLink = value;
        return this;
    }

    public HelpPanelView setOnClose(@Nullable Runnable value) {
        onClose = value;
        return this;
    }

    /** HelpPanel.vue openPage(category_key, page_key)。 */
    public void openPage(String categoryKey, String pageKey) {
        this.categoryKey = categoryKey == null ? "" : categoryKey;
        this.pageKey = pageKey == null ? "" : pageKey;
        rebuild();
    }

    private void rebuild() {
        rebuildHeader();
        content.clearAllScrollViewChildren();
        if (categoryKey.isEmpty()) {
            buildOverview();
        } else {
            buildPage();
        }
    }

    private void rebuildHeader() {
        header.clearAllChildren();
        Button close = SsIconButton.ghost("x", 22, event -> {
            if (onClose != null) onClose.run();
        });
        close.layout(l -> l.width(30).height(30).marginAll(4));
        header.addChild(close);
        if (!categoryKey.isEmpty()) {
            // .back_button：ChevronLeft 20 + "Back to overview"、h29
            Button back = SsButton.ghost("< Back to overview", event -> openPage("", ""));
            back.layout(l -> l.height(29).paddingLeft(2).paddingRight(8));
            header.addChild(back);
        }
    }

    /** 总览：h1 Documentation + 类目/页面双层列表。 */
    private void buildOverview() {
        content.addScrollViewChild(h1("Documentation"));
        for (Map.Entry<String, HelpData.Category> cat : HelpData.data().entrySet()) {
            TextElement catTitle = text(cat.getValue().title().toUpperCase(java.util.Locale.ROOT),
                    SnowstormTheme.TEXT_GRAYED, 10);
            catTitle.layout(l -> l.widthPercent(100).paddingLeft(7).paddingVertical(3));
            content.addScrollViewChild(catTitle);
            for (Map.Entry<String, HelpData.Page> page : cat.getValue().pages().entrySet()) {
                String catKey = cat.getKey();
                String pageKey = page.getKey();
                // li.clickable：hover 高亮 + bar 底
                Button item = SsButton.ghost(page.getValue().title(),
                        event -> openPage(catKey, pageKey));
                item.textStyle(s -> s.textAlignHorizontal(Horizontal.LEFT));
                item.layout(l -> l.widthPercent(100).paddingLeft(25).paddingVertical(3));
                content.addScrollViewChild(item);
            }
        }
    }

    /** 单页：h1 标题 + HelpText 行 + HelpInputList。 */
    private void buildPage() {
        HelpData.Page page = HelpData.page(categoryKey, pageKey);
        if (page == null) {
            content.addScrollViewChild(text("Page not found: " + categoryKey + "/" + pageKey,
                    SnowstormTheme.TEXT_GRAYED, 10));
            return;
        }
        content.addScrollViewChild(h1(page.title()));
        for (HelpData.Line line : page.lines()) {
            addLine(content, line);
        }
        if (page.inputs() != null) {
            addInputList(content, page.inputs());
        }
    }

    private void addLine(ScrollerView parent, HelpData.Line line) {
        if (line instanceof HelpData.Line.Text t) {
            TextElement p = text(t.text(), SnowstormTheme.TEXT, 10);
            p.layout(l -> l.widthPercent(100).marginBottom(12));
            parent.addScrollViewChild(p);
        } else if (line instanceof HelpData.Line.Code c) {
            // p.code_line：code（dark 底 #50cca7、宽 208）+ text
            UIElement row = new UIElement().layout(l -> l.widthPercent(100)
                    .flexDirection(FlexDirection.ROW).marginBottom(2));
            TextElement code = text(c.code(), CODE_COLOR, 9);
            code.layout(l -> l.width(208).paddingHorizontal(8).paddingVertical(4).marginRight(8));
            code.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.DARK)));
            TextElement desc = text(c.text(), SnowstormTheme.TEXT, 10);
            desc.layout(l -> l.flex(1));
            row.addChildren(code, desc);
            parent.addScrollViewChild(row);
        } else if (line instanceof HelpData.Line.Heading h) {
            int fontSize = switch (h.level()) {
                case 2 -> 13;
                case 3 -> 11;
                default -> 10;
            };
            TextElement heading = text(h.text(), SnowstormTheme.TEXT, fontSize);
            heading.layout(l -> l.widthPercent(100)
                    .marginTop(h.level() == 2 ? 22 : 15)
                    .marginBottom(h.level() == 2 ? 6 : 5));
            parent.addScrollViewChild(heading);
        } else if (line instanceof HelpData.Line.Link link) {
            Button a = SsButton.ghost(link.text(), event -> {
                if (onOpenLink != null) {
                    onOpenLink.accept(link.href());
                } else {
                    System.out.println("[snowstorm] open link: " + link.href());
                }
            });
            a.textStyle(s -> s.textColor(SnowstormTheme.ACCENT).textAlignHorizontal(Horizontal.LEFT));
            a.layout(l -> l.widthPercent(100).marginBottom(12));
            parent.addScrollViewChild(a);
        } else if (line instanceof HelpData.Line.Html html) {
            // MC 无 HTML 渲染：降级纯文本（剥离 tag）
            TextElement p = text(html.content().replaceAll("<[^>]+>", ""), SnowstormTheme.TEXT, 10);
            p.layout(l -> l.widthPercent(100).marginBottom(12));
            parent.addScrollViewChild(p);
        } else if (line instanceof HelpData.Line.InputList list) {
            addInputList(parent, list.inputs());
        }
    }

    /** HelpInputList.vue as-is：h2（label）+ input_info_bar（molang）+ info 段落 + text。 */
    private void addInputList(ScrollerView parent, com.google.gson.JsonObject inputs) {
        for (Map.Entry<String, com.google.gson.JsonElement> e : inputs.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            com.google.gson.JsonObject input = e.getValue().getAsJsonObject();
            String type = input.has("type") ? input.get("type").getAsString() : "";
            String label = input.has("label") ? input.get("label").getAsString() : e.getKey();

            TextElement h2 = text(label, SnowstormTheme.TEXT, 12);
            h2.layout(l -> l.widthPercent(100).marginTop(15).marginBottom(5));
            parent.addScrollViewChild(h2);

            if ("molang".equals(type)) {
                // .input_info_bar：dark 底 h24 radius4；类型徽标 accent 底
                UIElement bar = new UIElement().layout(l -> l.widthPercent(100).height(24)
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
                bar.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.DARK)));
                TextElement typeLabel = text(" Molang ", SnowstormTheme.BORDER, 9);
                typeLabel.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.ACCENT)));
                bar.addChild(typeLabel);
                if (input.has("context")) {
                    // input_context_color: emitter #e98989 / particle #f9da88 / spawned_emitter #db57ae
                    int contextColor = switch (input.get("context").getAsString()) {
                        case "particle" -> 0xFFF9DA88;
                        case "spawned_emitter" -> 0xFFDB57AE;
                        default -> 0xFFE98989;
                    };
                    String contextLabel = switch (input.get("context").getAsString()) {
                        case "particle" -> "Per Particle";
                        case "spawned_emitter" -> "Spawned Emitter Context";
                        case "curve" -> "Per Curve";
                        default -> "Per Emitter";
                    };
                    TextElement ctx = text(" " + contextLabel + " ", SnowstormTheme.BORDER, 9);
                    ctx.style(s -> s.backgroundTexture(new ColorRectTexture(contextColor)));
                    ctx.layout(l -> l.marginLeft(4));
                    bar.addChild(ctx);
                }
                bar.layout(l -> l.marginBottom(3));
                parent.addScrollViewChild(bar);
            }

            if (input.has("info") && !input.get("info").getAsString().isEmpty()) {
                TextElement info = text(input.get("info").getAsString(), SnowstormTheme.TEXT, 10);
                info.layout(l -> l.widthPercent(100).marginBottom(12)
                        .paddingLeft("select".equals(type) ? 30 : 0));
                parent.addScrollViewChild(info);
            }
        }
    }

    private static TextElement h1(String text) {
        TextElement h1 = text(text, SnowstormTheme.TEXT, 14);
        h1.layout(l -> l.widthPercent(100).marginBottom(12).paddingBottom(2));
        // h1 border-bottom 2px selection → 2px 底条
        return h1;
    }

    private static TextElement text(String text, int color, int fontSize) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(fontSize).textColor(color)
                .textAlignHorizontal(Horizontal.LEFT));
        return element;
    }
}
//?}
