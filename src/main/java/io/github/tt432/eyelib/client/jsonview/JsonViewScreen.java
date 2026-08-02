package io.github.tt432.eyelib.client.jsonview;

import io.github.tt432.eyelib.bridge.ui.UiPort;
import io.github.tt432.eyelib.ui.UIButton;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UITextField;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Entity/ClientEntity JSON 查看器屏幕。
 *
 * <p>顶部为「客户端实体 / 行为实体」两个切换 tab；左侧为可过滤的实体 id 列表
 * （{@link IdListPanel}，点击选中、选中高亮）；右侧为选中实体定义的格式化 JSON
 * （{@link JsonTextPanel}，带轻量语法着色）。
 *
 * <p>渲染与事件处理均走 {@code ui} 端口（MC 无关），仅 {@link #open()} 触碰 MC。
 *
 * @author TT432
 */
public final class JsonViewScreen implements UIScreen {
    private static final int COLOR_BG = 0xC0101015;
    private static final int COLOR_PANEL_BG = 0x80000000;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int MARGIN = 10;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;
    private static final String EMPTY_HINT = "无定义";

    private enum Tab {
        CLIENT("客户端实体"),
        BEHAVIOR("行为实体");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final EntityJsonService service = new EntityJsonService();

    private @Nullable UIScreenContext ctx;
    private @Nullable IdListPanel idPanel;
    private @Nullable JsonTextPanel jsonPanel;
    private @Nullable UIButton clientTabButton;
    private @Nullable UIButton behaviorTabButton;

    private Tab tab = Tab.CLIENT;
    private @Nullable String selectedId;

    // 布局缓存（onInit 时计算，onRender 复用）
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int jsonX;
    private int jsonY;
    private int jsonW;
    private int jsonH;

    /**
     * 打开 JSON 查看器屏幕。
     */
    public static void open() {
        Minecraft.getInstance().setScreen(UiPort.wrap(new JsonViewScreen()));
    }

    @Override
    public void onInit(UIScreenContext ctx) {
        this.ctx = ctx;

        clientTabButton = ctx.addButton(Tab.CLIENT.label,
                MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT, () -> selectTab(Tab.CLIENT));
        behaviorTabButton = ctx.addButton(Tab.BEHAVIOR.label,
                MARGIN + BUTTON_WIDTH + 6, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT, () -> selectTab(Tab.BEHAVIOR));

        int inputHeight = Math.round(ctx.fontHeight() / 0.614F);
        int top = MARGIN + BUTTON_HEIGHT + 6;
        int leftWidth = listWidth(ctx);

        UITextField filterField = ctx.addTextField(MARGIN, top, leftWidth, inputHeight);
        filterField.setMaxLength(64);
        filterField.setBordered(true);
        filterField.setHint("过滤…");
        filterField.setCanLoseFocus(true);
        filterField.setResponder(this::onFilterEdited);

        listX = MARGIN;
        listY = top + inputHeight + 4;
        listW = leftWidth;
        listH = ctx.height() - MARGIN - listY;
        idPanel = ctx.addWidget(new IdListPanel(listX, listY, listW, listH,
                ctx.fontHeight() + 4, this::onIdSelected));

        jsonX = listX + listW + MARGIN;
        jsonY = top;
        jsonW = ctx.width() - MARGIN - jsonX;
        jsonH = ctx.height() - MARGIN - jsonY;
        jsonPanel = ctx.addWidget(new JsonTextPanel(jsonX, jsonY, jsonW, jsonH, ctx.fontHeight() + 2));

        reloadList();
        updateTabButtons();
        refreshJson();
    }

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UIScreenContext context = ctx;
        if (context == null) {
            return;
        }
        gfx.fill(0, 0, context.width(), context.height(), COLOR_BG);
        gfx.fill(listX, listY, listX + listW, listY + listH, COLOR_PANEL_BG);
        gfx.fill(jsonX, jsonY, jsonX + jsonW, jsonY + jsonH, COLOR_PANEL_BG);

        String title = tab.label + (selectedId != null ? " : " + selectedId : "");
        int titleX = MARGIN + (BUTTON_WIDTH + 6) * 2 + 10;
        int titleY = MARGIN + Math.max(0, (BUTTON_HEIGHT - context.fontHeight()) / 2);
        gfx.drawText(title, titleX, titleY, COLOR_TITLE);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        IdListPanel list = idPanel;
        if (list != null && list.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        JsonTextPanel json = jsonPanel;
        return json != null && json.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        IdListPanel list = idPanel;
        if (list != null && list.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        JsonTextPanel json = jsonPanel;
        return json != null && json.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int listWidth(UIScreenContext ctx) {
        return Math.min(220, Math.max(120, ctx.width() / 4));
    }

    private void selectTab(Tab tab) {
        if (this.tab == tab) {
            return;
        }
        this.tab = tab;
        selectedId = null;
        reloadList();
        updateTabButtons();
        refreshJson();
    }

    private void reloadList() {
        IdListPanel panel = idPanel;
        if (panel == null) {
            return;
        }
        panel.setIds(tab == Tab.CLIENT ? service.clientEntityIds() : service.behaviorEntityIds());
    }

    private void updateTabButtons() {
        UIButton client = clientTabButton;
        UIButton behavior = behaviorTabButton;
        if (client != null) {
            client.setActive(tab != Tab.CLIENT);
        }
        if (behavior != null) {
            behavior.setActive(tab != Tab.BEHAVIOR);
        }
    }

    private void onFilterEdited(String input) {
        IdListPanel panel = idPanel;
        if (panel != null) {
            panel.setFilter(input);
        }
    }

    private void onIdSelected(String id) {
        selectedId = id;
        refreshJson();
    }

    /**
     * 按当前选中 id 重新编码 JSON 并刷新右侧面板；无选中或编码失败时显示「无定义」。
     */
    private void refreshJson() {
        JsonTextPanel panel = jsonPanel;
        if (panel == null) {
            return;
        }
        List<String> lines = List.of(EMPTY_HINT);
        String id = selectedId;
        if (id != null) {
            Optional<String> json = tab == Tab.CLIENT
                    ? service.clientEntityJson(id)
                    : service.behaviorEntityJson(id);
            if (json.isPresent()) {
                lines = Arrays.asList(json.get().split("\n"));
            }
        }
        panel.setLines(lines);
    }
}
