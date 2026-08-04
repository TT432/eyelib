//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodePortWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodeWidget;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.nodegraph.InlineLiteral;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortType;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 端口行内联编辑器（UE 引脚行内编辑器，用户决策 2026-08-03）：
 * 带默认值的输入端口不再在节点内容区产生独立配置行，而是在<b>端口行内</b>
 * 嵌入一个小编辑器（圆点 + 标签 + 编辑器一行）。
 *
 * <p>布局：编辑器挂在 {@code ports} 组内（在 input/output 组之后，反向 z 序优先收事件），
 * x = 标签尾，右缘 = 节点宽 - output 组宽（首行 out 端口占右侧，编辑器不越过它）。
 * 连线 → 编辑器禁用 + 灰罩（与旧堆叠常量行同规则）；断线恢复。
 *
 * <p>{@link NodeWidget#reloadWidget()} 重建 ports 组后必须由
 * {@link EvmNodeWidget#reloadWidget()} 重新调用 {@link #embed}。
 */
final class EvmInlinePortFields {
    private EvmInlinePortFields() {
    }

    /** 连线禁用罩面色。 */
    private static final int COLOR_DISABLED_MASK = 0x90000000;

    /**
     * 给节点全部「有默认值且非 EXEC/SLOT」的输入端口嵌入行内编辑器。
     * 调用前提：widget 刚 reloadWidget 完（ports 组为全新内容，无需判重）。
     */
    static void embed(NodeWidget widget, EvmNode node) {
        NodeType type = node.nodeType();
        if (type == null) {
            return;
        }
        WidgetGroup ports = widget.getPorts();
        if (ports.widgets.size() < 2 || !(ports.widgets.get(0) instanceof WidgetGroup inputGroup)) {
            return;
        }
        WidgetGroup outputGroup = ports.widgets.get(1) instanceof WidgetGroup og ? og : null;
        int outWidth = outputGroup == null || outputGroup.widgets.isEmpty() ? 0 : outputGroup.getSizeWidth();
        int nodeWidth = widget.getSizeWidth();
        var font = Minecraft.getInstance().font;

        NodeInstance instance = new NodeInstance(node.evmUid, node.nodeTypeId, 0, 0, node.options, node.constants);
        Map<String, PortDef> defs = new LinkedHashMap<>();
        for (PortDef def : type.inputsOf(instance, node.resolver)) {
            defs.put(def.id(), def);
        }

        int row = 0;
        for (Widget w : inputGroup.widgets) {
            if (w instanceof NodePortWidget portWidget) {
                PortDef def = defs.get(portWidget.port.portData.identifier);
                if (def != null && def.type() != PortType.EXEC && def.type() != PortType.SLOT
                        && def.defaultValue().isPresent()) {
                    int fieldX = 18 + font.width(def.id()) + 4;
                    int fieldW = nodeWidth - outWidth - fieldX - 3;
                    if (fieldW > 12) {
                        // 挂到 NodeWidget（无布局管理，坐标生效）；ports 组有 HORIZONTAL 布局，
                        // 直接挂会被重排到 output 组右侧、飞出节点外（实测踩过）
                        widget.addWidget(new InlineField(fieldX, 15 + row * 15 + 2, fieldW, 11, node, def));
                    }
                }
                row++;
            }
        }
    }

    /** 行内编辑器：灰底 + 按端口类型的输入控件（float/int 数字框、bool 开关、string 文本框）。 */
    private static final class InlineField extends WidgetGroup {
        private final EvmNode node;
        private final PortDef def;
        private final Widget input;

        InlineField(int x, int y, int w, int h, EvmNode node, PortDef def) {
            super(x, y, w, h);
            this.node = node;
            this.def = def;
            JsonElement defValue = def.defaultValue().orElseThrow();
            setBackground(ColorPattern.T_GRAY.rectTexture().setRadius(3));
            input = switch (def.type()) {
                case INT -> {
                    var field = new TextFieldWidget(2, 1, w - 4, h - 2,
                            () -> String.valueOf(current(defValue).getAsInt()), this::writeInt);
                    field.setBordered(false);
                    field.setNumbersOnly(Integer.MIN_VALUE, Integer.MAX_VALUE);
                    field.setClientSideWidget();
                    yield field;
                }
                case BOOL -> {
                    var sw = new SwitchWidget(2, 0, 10, 10, (cd, pressed) -> writeBool(pressed));
                    sw.setTexture(
                            new ColorBorderTexture(-1, -1).setRadius(5),
                            new GuiTextureGroup(new ColorBorderTexture(-1, -1).setRadius(5),
                                    new ColorRectTexture(-1).setRadius(5).scale(0.5f)));
                    sw.setPressed(boolOf(current(defValue)));
                    yield sw;
                }
                case FLOAT -> {
                    var field = new TextFieldWidget(2, 1, w - 4, h - 2,
                            () -> String.valueOf(current(defValue).getAsFloat()), this::writeFloat);
                    field.setBordered(false);
                    field.setNumbersOnly(-Float.MAX_VALUE, Float.MAX_VALUE);
                    field.setClientSideWidget();
                    yield field;
                }
                case ANY -> {
                    // ANY 端口行内字面值智能解析（规格 §2.8）："5"→int、"1.5"→float、
                    // "true"→bool、其余→string；set_var.value 直接敲值即得数字而非字符串
                    var field = new TextFieldWidget(2, 1, w - 4, h - 2,
                            () -> InlineLiteral.toText(current(defValue)), this::writeAny);
                    field.setBordered(false);
                    field.setClientSideWidget();
                    yield field;
                }
                default -> {
                    var field = new TextFieldWidget(2, 1, w - 4, h - 2,
                            () -> current(defValue).getAsString(), this::writeString);
                    field.setBordered(false);
                    field.setClientSideWidget();
                    yield field;
                }
            };
            addWidget(input);
        }

        // ---------- 值读写（constants 映射，端口 id 为键） ----------

        private JsonElement current(JsonElement defValue) {
            return node.constants.getOrDefault(def.id(), defValue);
        }

        private void writeInt(String text) {
            try {
                node.constants.put(def.id(), new JsonPrimitive(Integer.parseInt(text.trim())));
            } catch (NumberFormatException ignored) {
                // 输入中间态（空/-）不写回，供应商下一帧回显旧值
            }
        }

        private void writeFloat(String text) {
            try {
                node.constants.put(def.id(), new JsonPrimitive(Float.parseFloat(text.trim())));
            } catch (NumberFormatException ignored) {
                // 同上
            }
        }

        /** ANY 写回：智能解析字面值（{@link InlineLiteral#parse} 不会抛）。 */
        private void writeAny(String text) {
            node.constants.put(def.id(), InlineLiteral.parse(text));
        }

        private void writeBool(boolean pressed) {
            node.constants.put(def.id(), new JsonPrimitive(pressed));
        }

        private void writeString(String text) {
            node.constants.put(def.id(), new JsonPrimitive(text));
        }

        /** BOOL 读取：兼容数字（0/1）与布尔字面量两种存储形态。 */
        private static boolean boolOf(JsonElement e) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            return p.isBoolean() ? p.getAsBoolean() : p.getAsFloat() != 0;
        }

        // ---------- 连线禁用（锁死规避：只切内层输入控件，不切自身——外层按 active 门控 updateScreen） ----------

        private boolean isWired() {
            NodePort port = node.getPort("in", def.id());
            return port != null && !port.getEdges().isEmpty();
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            boolean wired = isWired();
            if (input.isActive() == wired) {
                input.setActive(!wired);
            }
        }

        @Override
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            if (isWired()) {
                DrawerHelper.drawSolidRect(graphics, 0, 0, getSizeWidth(), getSizeHeight(),
                        COLOR_DISABLED_MASK);
            }
        }
    }
}
//?}
