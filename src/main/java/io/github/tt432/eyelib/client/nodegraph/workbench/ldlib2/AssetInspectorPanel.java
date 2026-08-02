package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.jsonview.EntityJsonService;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 资产检查器侧栏（规格 §W1，语义与 ldlib1 工作台对齐）：
 * 三个分类（客户端实体 / 行为实体 / 渲染控制器，数据全部来自 {@link EntityJsonService}）
 * + 过滤框 + id 列表 + 着色 JSON 文本（{@link JsonSyntax} 五色，同原 JsonTextPanel 规则）。
 * 行为实体仅查看（规格：行为 components/events 超出图语言表达域，不作为图导入）。
 */
final class AssetInspectorPanel extends UIElement {
    /** 侧栏展开宽度。 */
    static final int WIDTH = 240;

    private enum Category {
        CLIENT_ENTITY("客户端实体"),
        BEHAVIOR_ENTITY("行为实体"),
        RENDER_CONTROLLER("渲染控制器");

        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private final EntityJsonService service = new EntityJsonService();
    private final ScrollerView idList;
    private final ScrollerView jsonView;

    private Category category = Category.CLIENT_ENTITY;
    private String filter = "";
    private @Nullable String selectedId;

    AssetInspectorPanel() {
        layout(layout -> layout
                .width(WIDTH)
                .heightPercent(100)
                .paddingAll(4)
                .gapAll(2));
        WorkbenchWidgets.panelBackground(this);

        // 分类行
        UIElement categoryRow = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(16)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));
        for (Category c : Category.values()) {
            Button button = new Button();
            button.setText(Component.literal(c.label));
            button.textStyle(style -> style.fontSize(8));
            button.setOnClick(event -> selectCategory(c));
            button.layout(layout -> layout.flex(1).heightPercent(100));
            categoryRow.addChild(button);
        }

        TextField filterField = new TextField();
        filterField.textFieldStyle(style -> style.placeholder(Component.literal("过滤…")));
        filterField.setTextResponder(text -> {
            filter = text;
            refreshIdList();
        });
        filterField.layout(layout -> layout.widthPercent(100).height(14));

        idList = new ScrollerView();
        idList.layout(layout -> layout.widthPercent(100).height(110));

        jsonView = new ScrollerView();
        jsonView.scrollerStyle(style -> style.mode(ScrollerMode.BOTH));
        jsonView.layout(layout -> layout.widthPercent(100).flex(1));

        addChildren(
                WorkbenchWidgets.sectionTitle("资产检查器"),
                categoryRow,
                filterField,
                idList,
                jsonView);

        refreshIdList();
    }

    private void selectCategory(Category newCategory) {
        if (category == newCategory) {
            return;
        }
        category = newCategory;
        selectedId = null;
        refreshIdList();
        showJson(null);
    }

    /** 当前分类的 id 列表（字母序，来自 service）。 */
    private List<String> idsOf(Category c) {
        return switch (c) {
            case CLIENT_ENTITY -> service.clientEntityIds();
            case BEHAVIOR_ENTITY -> service.behaviorEntityIds();
            case RENDER_CONTROLLER -> service.renderControllerIds();
        };
    }

    private Optional<String> jsonOf(Category c, String id) {
        return switch (c) {
            case CLIENT_ENTITY -> service.clientEntityJson(id);
            case BEHAVIOR_ENTITY -> service.behaviorEntityJson(id);
            case RENDER_CONTROLLER -> service.renderControllerJson(id);
        };
    }

    private void refreshIdList() {
        String needle = filter.toLowerCase(Locale.ROOT);
        idList.clearAllScrollViewChildren();
        for (String id : idsOf(category)) {
            if (!needle.isEmpty() && !id.toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            idList.addScrollViewChild(WorkbenchWidgets.rowButton(
                    id, WorkbenchColors.TEXT, id.equals(selectedId), () -> onSelectId(id)));
        }
    }

    private void onSelectId(String id) {
        selectedId = id;
        refreshIdList();
        showJson(id);
    }

    /** 选中 id 的 JSON（按行着色）灌入文本区；null/缺失时显示提示。 */
    private void showJson(@Nullable String id) {
        jsonView.clearAllScrollViewChildren();
        if (id == null) {
            jsonView.addScrollViewChild(WorkbenchWidgets.textLine("（选择左侧条目查看 JSON）", WorkbenchColors.DIM));
            return;
        }
        Optional<String> json = jsonOf(category, id);
        if (json.isEmpty()) {
            jsonView.addScrollViewChild(WorkbenchWidgets.textLine("（JSON 编码失败，详见日志）", WorkbenchColors.ERROR));
            return;
        }
        for (String line : json.get().split("\n")) {
            TextElement lineElement = new TextElement();
            lineElement.setText(JsonSyntax.highlight(line));
            lineElement.textStyle(style -> style.fontSize(9).textWrap(TextWrap.NONE).adaptiveWidth(true));
            lineElement.layout(layout -> layout.height(10));
            jsonView.addScrollViewChild(lineElement);
        }
    }
}
//?}
