package io.github.tt432.eyelib.client.scratch;

import io.github.tt432.eyelib.molang.scratch.ScratchBlock;
import io.github.tt432.eyelib.molang.scratch.ScratchKind;
import io.github.tt432.eyelib.molang.scratch.ScratchShape;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Scratch 积木布局引擎：把 {@link ScratchBlock} 树量成不可变 {@link Node} 树
 * （尺寸 + 行内元素位置 + 子节点偏移），供渲染与命中测试共用。
 *
 * <p>尺寸规则忠实移植 scratchblocks {@code scratch3/blocks.js} 的 BlockView.draw：
 * 行内元素水平排列、间距 {@link ScratchTheme#GAP}；语句积木首元素对齐凹口右缘
 * （{@code cmw = 48 - pad} × 0.5）；BOOL 六边形左右各让出 {@code h/2} 尖端；
 * C 形总高 = 头行 + max(体高, {@link ScratchTheme#EMPTY_MOUTH_H}) + 下唇。
 *
 * <p>零 MC 依赖：字体度量经 {@code textWidth} 函数注入，可单测。
 *
 * @author TT432
 */
public final class ScratchLayout {
    private ScratchLayout() {}

    /** 语句积木首元素 x（官方 cmw = 48 - padding ≈ 凹口右缘）。 */
    public static final float STACK_TEXT_X = ScratchTheme.NOTCH_X + ScratchTheme.NOTCH_W - ScratchTheme.GAP;
    /** 空插槽占位宽。 */
    public static final float EMPTY_SOCKET_W = 16;
    /** 空插槽占位高。 */
    public static final float EMPTY_SOCKET_H = 12;
    /** 输入框高。 */
    public static final float INPUT_H = ScratchTheme.REPORTER_H - 4;
    /** CALL 变长按钮宽。 */
    public static final float VARARG_W = 10;

    // ===== 行内元素 =====

    /** 行内元素（x 相对块原点，渲染 y 由所在行垂直居中决定）。 */
    public sealed interface Item {
        float x();

        float w();
    }

    /** 静态文本。 */
    public record LabelItem(String text, float x, float w) implements Item {}

    /** 可点击循环的二元/一元运算符。 */
    public record OpItem(String op, boolean unary, float x, float w) implements Item {}

    /** 可编辑字段输入框。 */
    public record InputItem(String field, float x, float w) implements Item {}

    /** 表达式插槽（child 非空时子节点从 (x, rowY) 顶对齐绘制）。 */
    public record SocketItem(ScratchBlock.Socket socket, float x, float w) implements Item {}

    /** CALL 变长参数 +/- 按钮。 */
    public record VarArgItem(boolean add, float x, float w) implements Item {}

    // ===== 布局节点 =====

    /** 一块积木的布局结果（不可变；坐标相对块原点）。 */
    public static final class Node {
        public final ScratchBlock block;
        public final ScratchShape shape;
        /** 总宽。 */
        public float w;
        /** 体高（STACK/C 不含底凸榫深 {@link ScratchTheme#NOTCH_DEPTH}）。 */
        public float h;
        /** 头行高（行内元素区）。 */
        public float rowH;
        /** 行内元素（x 已分配）。 */
        public List<Item> items = List.of();
        /** socket → 子布局（仅含非空插槽）。 */
        public final Map<ScratchBlock.Socket, Node> socketKids = new IdentityHashMap<>();
        // ---- C 形 ----
        public float bodyX;
        public float bodyY;
        public float bodyW;
        public float bodyH;
        /** body 语句序列子布局（与 block.body() 平行）。 */
        public final List<Node> bodyKids = new ArrayList<>();

        Node(ScratchBlock block, ScratchShape shape) {
            this.block = block;
            this.shape = shape;
        }

        /** 渲染总高（含底凸榫）。 */
        public float totalHeight() {
            return (shape == ScratchShape.STACK || shape == ScratchShape.C) ? h + ScratchTheme.NOTCH_DEPTH : h;
        }
    }

    // ===== 布局 =====

    /**
     * 布局一块积木（递归子插槽与语句体）。
     *
     * @param textWidth 字体宽度度量
     */
    public static Node layout(ScratchBlock block, ToIntFunction<String> textWidth) {
        Node node = new Node(block, ScratchKind.shapeOf(block));
        boolean cForm = node.shape == ScratchShape.C;

        // 递归 socket 子布局
        for (ScratchBlock.Socket socket : block.sockets()) {
            ScratchBlock child = socket.child();
            if (child != null) {
                node.socketKids.put(socket, layout(child, textWidth));
            }
        }
        // 递归 body 子布局（语句栈：步进 = 体高）
        if (cForm) {
            float y = 0;
            float maxW = 0;
            for (ScratchBlock stmt : block.body()) {
                Node kid = layout(stmt, textWidth);
                node.bodyKids.add(kid);
                y += kid.h;
                maxW = Math.max(maxW, kid.w);
            }
            node.bodyX = ScratchTheme.INSET;
            node.bodyW = maxW;
            node.bodyH = y;
        }

        // 行高：socket 子积木撑高
        float contentH = 0;
        for (Node kid : node.socketKids.values()) {
            contentH = Math.max(contentH, kid.h);
        }
        float minRowH = switch (node.shape) {
            case PILL, BOOL -> ScratchTheme.REPORTER_H;
            case STACK, C -> ScratchTheme.FIRST_LINE_H;
        };
        boolean stmtRow = node.shape == ScratchShape.STACK || cForm;
        node.rowH = Math.max(minRowH, contentH + (stmtRow ? ScratchTheme.PAD_DEFAULT * 2 : 0));

        float h = node.rowH;
        if (cForm) {
            h = node.rowH + Math.max(node.bodyH, ScratchTheme.EMPTY_MOUTH_H) + ScratchTheme.LIP_LINE_H;
        }
        node.h = h;

        // 起始 x：PILL/BOOL 让出端部
        float startX = switch (node.shape) {
            case PILL -> ScratchTheme.PAD_IN_ROUND;
            case BOOL -> Math.max(h / 2, ScratchTheme.PAD_IN_BOOL);
            case STACK, C -> STACK_TEXT_X;
        };

        // 行内元素序列
        node.items = buildItems(block, node, textWidth, startX);

        // 总宽
        float itemsW = 0;
        if (!node.items.isEmpty()) {
            Item last = node.items.get(node.items.size() - 1);
            itemsW = last.x() + last.w();
        }
        float endPad = switch (node.shape) {
            case PILL -> ScratchTheme.PAD_IN_ROUND;
            case BOOL -> Math.max(h / 2, ScratchTheme.PAD_IN_BOOL);
            case STACK, C -> ScratchTheme.PAD_DEFAULT;
        };
        float w = itemsW + (node.items.isEmpty() ? startX * 2 : endPad);
        w = switch (node.shape) {
            case PILL -> Math.max(Math.max(w, ScratchTheme.MIN_W_REPORTER), h);
            case BOOL -> Math.max(w, h);
            case STACK -> Math.max(w, ScratchTheme.MIN_W_CMD);
            case C -> Math.max(Math.max(w, ScratchTheme.MIN_W_C),
                    ScratchTheme.INSET + node.bodyW + ScratchTheme.PAD_DEFAULT);
        };
        node.w = w;

        // C 形 body 区 y（头行之下）
        if (cForm) {
            node.bodyY = node.rowH;
        }
        return node;
    }

    /** 语句栈总高（步进 = 体高）。 */
    public static float stackHeight(List<Node> nodes) {
        float h = 0;
        for (Node n : nodes) {
            h += n.h;
        }
        return h;
    }

    private static List<Item> buildItems(ScratchBlock block, Node node,
                                         ToIntFunction<String> textWidth, float startX) {
        List<Item> items = new ArrayList<>();
        ScratchKind kind = block.kind();

        switch (kind) {
            case NUM, RAW -> addInput(items, block, "text", textWidth);
            case STR -> {
                addLabel(items, "\"", textWidth);
                addInput(items, block, "text", textWidth);
                addLabel(items, "\"", textWidth);
            }
            case THIS -> addLabel(items, "this", textWidth);
            case VAR -> addInput(items, block, "path", textWidth);
            case CALL -> {
                addInput(items, block, "name", textWidth);
                for (ScratchBlock.Socket socket : block.sockets()) {
                    addSocket(items, node, socket);
                }
                items.add(new VarArgItem(true, 0, VARARG_W));
                if (!block.sockets().isEmpty()) {
                    items.add(new VarArgItem(false, 0, VARARG_W));
                }
            }
            case BINARY -> {
                addSocket(items, node, block.requireSocket("left"));
                addOp(items, block.field("op"), false, textWidth);
                addSocket(items, node, block.requireSocket("right"));
            }
            case UNARY -> {
                addOp(items, block.field("op"), true, textWidth);
                addSocket(items, node, block.requireSocket("expr"));
            }
            case NULLCO -> {
                addSocket(items, node, block.requireSocket("left"));
                addLabel(items, "??", textWidth);
                addSocket(items, node, block.requireSocket("right"));
            }
            case TERNARY -> {
                addSocket(items, node, block.requireSocket("cond"));
                addLabel(items, "?", textWidth);
                addSocket(items, node, block.requireSocket("whenTrue"));
                addLabel(items, ":", textWidth);
                addSocket(items, node, block.requireSocket("whenFalse"));
            }
            case COND_BINARY -> {
                addSocket(items, node, block.requireSocket("cond"));
                addLabel(items, "?", textWidth);
                addSocket(items, node, block.requireSocket("whenFalse"));
            }
            case MEMBER -> {
                addSocket(items, node, block.requireSocket("owner"));
                addLabel(items, ".", textWidth);
                addInput(items, block, "member", textWidth);
            }
            case INDEX -> {
                addSocket(items, node, block.requireSocket("owner"));
                addLabel(items, "[", textWidth);
                addSocket(items, node, block.requireSocket("index"));
                addLabel(items, "]", textWidth);
            }
            case ARROW -> {
                addSocket(items, node, block.requireSocket("target"));
                addLabel(items, "->", textWidth);
                addInput(items, block, "owner", textWidth);
                addLabel(items, ".", textWidth);
                addInput(items, block, "member", textWidth);
            }
            case STMT_EXPR -> addSocket(items, node, block.requireSocket("expr"));
            case STMT_ASSIGN -> {
                addSocket(items, node, block.requireSocket("target"));
                addLabel(items, "=", textWidth);
                addSocket(items, node, block.requireSocket("value"));
            }
            case STMT_RETURN -> {
                addLabel(items, "return", textWidth);
                addSocket(items, node, block.requireSocket("value"));
            }
            case STMT_BREAK -> {
                addLabel(items, "break", textWidth);
                addSocket(items, node, block.requireSocket("value"));
            }
            case STMT_CONTINUE -> {
                addLabel(items, "continue", textWidth);
                addSocket(items, node, block.requireSocket("value"));
            }
            case CTRL_LOOP -> {
                addLabel(items, "loop", textWidth);
                addSocket(items, node, block.requireSocket("count"));
            }
            case CTRL_FOREACH -> {
                addLabel(items, "for_each", textWidth);
                addSocket(items, node, block.requireSocket("variable"));
                addLabel(items, "in", textWidth);
                addSocket(items, node, block.requireSocket("collection"));
            }
            case CTRL_BLOCK -> addLabel(items, "{", textWidth);
        }

        // 分配 x
        float x = startX;
        List<Item> placed = new ArrayList<>(items.size());
        for (Item item : items) {
            Item at;
            if (item instanceof LabelItem l) {
                at = new LabelItem(l.text(), x, l.w());
            } else if (item instanceof OpItem o) {
                at = new OpItem(o.op(), o.unary(), x, o.w());
            } else if (item instanceof InputItem in) {
                at = new InputItem(in.field(), x, in.w());
            } else if (item instanceof SocketItem s) {
                at = new SocketItem(s.socket(), x, s.w());
            } else if (item instanceof VarArgItem v) {
                at = new VarArgItem(v.add(), x, v.w());
            } else {
                throw new IllegalStateException("未知 Item 类型: " + item.getClass());
            }
            placed.add(at);
            x += at.w() + ScratchTheme.GAP;
        }
        return placed;
    }

    private static void addLabel(List<Item> items, String text, ToIntFunction<String> textWidth) {
        items.add(new LabelItem(text, 0, textWidth.applyAsInt(text)));
    }

    private static void addOp(List<Item> items, String op, boolean unary, ToIntFunction<String> textWidth) {
        items.add(new OpItem(op, unary, 0, Math.max(textWidth.applyAsInt(op), 6)));
    }

    private static void addInput(List<Item> items, ScratchBlock block, String field,
                                 ToIntFunction<String> textWidth) {
        float w = Math.max(ScratchTheme.MIN_W_INPUT,
                textWidth.applyAsInt(block.field(field)) + ScratchTheme.PAD_INPUT * 2);
        items.add(new InputItem(field, 0, w));
    }

    private static void addSocket(List<Item> items, Node node, ScratchBlock.Socket socket) {
        Node kid = node.socketKids.get(socket);
        items.add(new SocketItem(socket, 0, kid != null ? kid.w : EMPTY_SOCKET_W));
    }
}
