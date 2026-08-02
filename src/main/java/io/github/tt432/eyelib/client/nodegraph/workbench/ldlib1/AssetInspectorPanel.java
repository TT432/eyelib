//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.client.jsonview.EntityJsonService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 资产检查器侧栏（规格 §W1，LDLib1 薄壳）：分类（客户端实体/行为实体/渲染控制器）
 * + id 过滤列表 + 着色 JSON 文本显示。数据源 {@link EntityJsonService}（与独立
 * 服务）。行为实体 JSON 仅查看（规格：不作图导入）。
 *
 * <p>默认收起由编辑器根容器控制（visible/active 开关）；本面板只负责内容。
 */
public final class AssetInspectorPanel extends WidgetGroup {
    private enum Category {
        CLIENT_ENTITY,
        BEHAVIOR_ENTITY,
        RENDER_CONTROLLER
    }

    private final EntityJsonService service = new EntityJsonService();
    private final ListRowView idList;
    private final ScrollableTextView jsonView;

    private Category category = Category.CLIENT_ENTITY;
    private String filter = "";
    private List<List<JsonColors.Segment>> jsonLines = List.of();

    public AssetInspectorPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
        // 先标记客户端 widget：子 widget 在 addWidget 时继承（TextFieldWidget 的
        // textResponder 仅在 clientSide 时本地回调，否则走网络包）
        setClientSideWidget();
        setBackground(new ColorRectTexture(0xF0141414));

        int inner = width - 8;
        int buttonWidth = (inner - 8) / 3;
        addWidget(new ButtonWidget(4, 4, buttonWidth, 14, new TextTexture("客户端实体"),
                cd -> switchCategory(Category.CLIENT_ENTITY)));
        addWidget(new ButtonWidget(8 + buttonWidth, 4, buttonWidth, 14, new TextTexture("行为实体"),
                cd -> switchCategory(Category.BEHAVIOR_ENTITY)));
        addWidget(new ButtonWidget(12 + buttonWidth * 2, 4, buttonWidth, 14, new TextTexture("渲染控制器"),
                cd -> switchCategory(Category.RENDER_CONTROLLER)));

        TextFieldWidget filterField = new TextFieldWidget(4, 22, inner, 14, null,
                text -> filter = text == null ? "" : text);
        filterField.setHoverTooltips("过滤 id");
        addWidget(filterField);

        int listHeight = Math.max(60, (height - 48) / 3);
        idList = new ListRowView(4, 40, inner, listHeight, this::rows, row -> select(row.id()));
        addWidget(idList);

        jsonView = new ScrollableTextView(4, 44 + listHeight, inner,
                Math.max(20, height - listHeight - 48), () -> jsonLines);
        addWidget(jsonView);
    }

    private List<ListRowView.Row> rows() {
        List<String> ids = switch (category) {
            case CLIENT_ENTITY -> service.clientEntityIds();
            case BEHAVIOR_ENTITY -> service.behaviorEntityIds();
            case RENDER_CONTROLLER -> service.renderControllerIds();
        };
        String needle = filter.toLowerCase(Locale.ROOT);
        return ids.stream()
                .filter(id -> needle.isEmpty() || id.toLowerCase(Locale.ROOT).contains(needle))
                .map(id -> new ListRowView.Row(id, id))
                .toList();
    }

    private void switchCategory(Category newCategory) {
        category = newCategory;
        idList.setSelectedId(null);
        jsonLines = List.of();
        jsonView.resetScroll();
    }

    private void select(String id) {
        Optional<String> json = switch (category) {
            case CLIENT_ENTITY -> service.clientEntityJson(id);
            case BEHAVIOR_ENTITY -> service.behaviorEntityJson(id);
            case RENDER_CONTROLLER -> service.renderControllerJson(id);
        };
        jsonLines = json.<List<List<JsonColors.Segment>>>map(JsonColors::toLines)
                .orElseGet(() -> List.of(List.of(
                        new JsonColors.Segment("（定义缺失或编码失败，详见日志）", MolangTypeColors.ERROR))));
        jsonView.resetScroll();
    }
}
//?}
