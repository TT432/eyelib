//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.events;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
//?} else {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
//?}
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.event.EditorEvent;
import io.github.tt432.eyelib.snowstorm.event.EventList;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * EventList.vue（events 组的事件定义编辑器）的 LDLib2 复刻：
 * 每事件 header（排序握把 + Event ID rename 文本框 + Remove Event）+
 * 递归 {@link EventSubpartView}，末尾 Add Event（ListAddButton 形态）。
 *
 * <p>数据侧复用 {@link EventList}（rename/add/remove/move + registerEdit 已在模型内）。
 * JS 单一事实源是 {@code group.events}（Config.events 镜像）；Java 侧
 * {@code EventList.events} 与 {@code group.events} 为两个容器装同一批
 * {@link EditorEvent} 对象——本视图以 group.events 为外部事实源：
 * UI 操作经 model 执行后立即镜像回 group；每帧检测外部重建（import）
 * 则反向重载 model。rename/registerEdit 语义全在模型。</p>
 *
 * <p>排序：握把 MOUSE_DOWN 起拖，drag 更新按行高过半规则算 hover_index
 * （sort.js as-is），DRAG_END 调 {@code model.move}（JS sort 结束不 registerEdit）。
 * 拖拽中目标行顶/底画 2px accent 指示（.sort_before/.sort_after）。</p>
 */
public class EventListView extends UIElement {

    private static final Object DRAG_SORT = new Object();

    private final InputStructure.Group group;
    private final EventList model;
    private final UIElement rows = new UIElement();
    private final List<RowElement> rowElements = new ArrayList<>();

    private String lastSig = "";
    private int sortOriginal = -1;
    private int sortHover = -1;

    public EventListView(InputStructure.Group group) {
        this.group = group;
        this.model = new EventList(EditorRuntime.Config);
        syncModelFromGroup();
        // #event_list：margin-block 12px
        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).marginVertical(12));
        rows.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
        addChild(rows);
        // list-add-button：Add Event（虚线框 + Plus 20px，SsListAddRow as-is）
        addChild(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsListAddRow.of(e -> {
            model.addEvent();
            afterModelOp();
        }));
        rebuildRows();
    }

    // ==================================================================
    // group.events ⇄ model.events 同步
    // ==================================================================

    private void syncModelFromGroup() {
        model.events.clear();
        for (Object o : group.events) {
            model.events.add((EditorEvent) o);
        }
    }

    private void mirrorToGroup() {
        group.events.clear();
        group.events.addAll(model.events);
    }

    private static boolean sameContent(List<?> a, List<?> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) return false;
        }
        return true;
    }

    /** model 变更后的统一收尾：镜像回 group + 重建行。 */
    private void afterModelOp() {
        mirrorToGroup();
        lastSig = sig();
        rebuildRows();
    }

    private String sig() {
        StringBuilder sb = new StringBuilder();
        for (EditorEvent entry : model.events) {
            sb.append(System.identityHashCode(entry)).append(',');
        }
        return sb.toString();
    }

    //? if <26.1 {
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        syncFrame();
    }
    //?} else {
    @Override
    protected void drawBackgroundAdditional(IGUIContext context) {
        syncFrame();
    }
    //?}

    private void syncFrame() {
        // 外部重建（import 回填 group.events）→ 反向重载
        if (!sameContent(group.events, model.events)) {
            syncModelFromGroup();
            lastSig = sig();
            rebuildRows();
            return;
        }
        if (!sig().equals(lastSig)) {
            lastSig = sig();
            rebuildRows();
        }
    }

    // ==================================================================
    // 行构建
    // ==================================================================

    private void rebuildRows() {
        rows.clearAllChildren();
        rowElements.clear();
        for (int i = 0; i < model.events.size(); i++) {
            EditorEvent entry = model.events.get(i);
            RowElement row = new RowElement();
            // .event：padding 20px 6px；border-top 2px bar（首项透明 + padding-top 4 + margin-top 0）
            boolean first = i == 0;
            row.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN)
                    .paddingHorizontal(6)
                    .paddingTop(first ? 4 : 20)
                    .paddingBottom(20));
            if (!first) {
                UIElement topBorder = new UIElement().layout(l -> l.widthPercent(100).height(2)
                        .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE).left(0).top(0));
                topBorder.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));
                row.addChild(topBorder);
            }

            // .event_header_bar：flex gap 4px、bar 底、padding 5px（→ 高 = 5*2 + 30 = 40）
            UIElement header = new UIElement();
            header.layout(l -> l.widthPercent(100).height(40).flexDirection(FlexDirection.ROW).gapAll(4)
                    .paddingAll(5).alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
            header.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));
            Button grip = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                    "grip-vertical", 20, e -> {
                    });
            grip.layout(l -> l.width(24).heightPercent(100));
            grip.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                if (e.button != 0) return;
                sortOriginal = rowIndex(row);
                sortHover = sortOriginal;
                grip.startDrag(DRAG_SORT, null);
            });
            grip.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onSortDragUpdate);
            grip.addEventListener(UIEvents.DRAG_END, this::onSortDragEnd);
            header.addChild(grip);

            // .event_header_bar > label：min-width 80px、padding 4px、右对齐
            TextElement idLabel = new TextElement();
            idLabel.setText(Component.literal("Event ID"));
            idLabel.textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT)
                    .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.RIGHT));
            idLabel.layout(l -> l.minWidth(80).heightPercent(100).paddingAll(4));
            header.addChild(idLabel);

            // rename：JS @input 逐键 renameEvent（删旧键插新键 quirk 在模型）
            TextField rename = new TextField();
            io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(rename, io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.TEXT);
            rename.setText(entry.id, false);
            rename.setTextResponder(text -> model.renameEvent(entry, text));
            rename.layout(l -> l.flex(1).heightPercent(100));
            header.addChild(rename);

            Button remove = new Button();
            remove.setText(Component.literal("Remove Event"))
                    .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                    .buttonStyle(s -> s
                            .baseTexture(IGuiTexture.EMPTY)
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                    .setOnClick(e -> {
                        model.removeEvent(entry);
                        afterModelOp();
                    });
            remove.layout(l -> l.heightPercent(100));
            header.addChild(remove);
            row.addChild(header);

            row.addChild(new EventSubpartView(entry.event));
            rows.addChild(row);
            rowElements.add(row);
        }
        refreshSortMarkers();
    }

    private int rowIndex(RowElement row) {
        return rowElements.indexOf(row);
    }

    // ==================================================================
    // 排序（sort.js as-is；结束不 registerEdit，domain 口径）
    // ==================================================================

    private void onSortDragUpdate(UIEvent e) {
        if (e.dragHandler == null || e.dragHandler.getDraggingObject() != DRAG_SORT || sortOriginal < 0) return;
        int hover = rowElements.size();
        for (int i = 0; i < rowElements.size(); i++) {
            RowElement row = rowElements.get(i);
            float top = row.getPositionY();
            float height = row.getSizeHeight();
            if (e.y >= top && e.y < top + height) {
                hover = (e.y - top > height / 2) ? i + 1 : i;
                break;
            }
        }
        if (hover != sortHover) {
            sortHover = hover;
            refreshSortMarkers();
        }
    }

    private void onSortDragEnd(UIEvent e) {
        if (e.dragHandler == null || e.dragHandler.getDraggingObject() != DRAG_SORT || sortOriginal < 0) return;
        if (sortHover >= 0 && sortHover != sortOriginal) {
            model.move(sortOriginal, sortHover);
            sortOriginal = sortHover = -1;
            afterModelOp();
            return;
        }
        sortOriginal = sortHover = -1;
        refreshSortMarkers();
    }

    /** .sort_before（目标行顶 2px accent）/ .sort_after（末尾行底）。 */
    private void refreshSortMarkers() {
        for (int i = 0; i < rowElements.size(); i++) {
            RowElement row = rowElements.get(i);
            row.marker = 0;
            if (sortOriginal >= 0) {
                if (i == sortHover) row.marker = 1;
                else if (sortHover == rowElements.size() && i == rowElements.size() - 1) row.marker = 2;
            }
        }
    }

    /** 排序指示行（marker：0 无 / 1 顶线 / 2 底线）。 */
    private static final class RowElement extends UIElement {
        int marker;

        //? if <26.1 {
        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            draw(guiContext::drawTexture);
        }
        //?} else {
        @Override
        protected void drawBackgroundAdditional(IGUIContext context) {
            draw(context::drawTexture);
        }
        //?}

        private void draw(RowSink sink) {
            if (marker == 0) return;
            float x = getPositionX(), y = getPositionY(), w = getSizeWidth(), h = getSizeHeight();
            sink.rect(new ColorRectTexture(SnowstormTheme.ACCENT), x, marker == 1 ? y : y + h - 2, w, 2);
        }

        private interface RowSink {
            void rect(IGuiTexture tex, float x, float y, float w, float h);
        }
    }
}
//?}
