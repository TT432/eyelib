package io.github.tt432.eyelib.client.scratch;

import io.github.tt432.eyelib.molang.scratch.ScratchBlock;
import io.github.tt432.eyelib.molang.scratch.ScratchKind;
import io.github.tt432.eyelib.molang.scratch.ScratchShape;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ScratchLayout} 布局规则单测：尺寸规则、形状动态判定、C 形高度合成、
 * 语句首元素对齐凹口右缘。
 *
 * @author TT432
 */
class ScratchLayoutTest {
    /** 等宽度量：每字符 6px。 */
    private static final ToIntFunction<String> FONT = s -> s.length() * 6;

    private static ScratchLayout.Node layout(ScratchBlock block) {
        return ScratchLayout.layout(block, FONT);
    }

    @Test
    void pillNumHasReporterHeightAndMinWidth() {
        ScratchLayout.Node node = layout(ScratchBlock.of(ScratchKind.NUM));
        assertEquals(ScratchShape.PILL, node.shape);
        assertEquals(ScratchTheme.REPORTER_H, node.h);
        assertTrue(node.w >= ScratchTheme.MIN_W_REPORTER, "w 不小于最小 reporter 宽");
        assertTrue(node.w >= node.h, "pill w >= h（两端半圆）");
        ScratchLayout.Item first = node.items.get(0);
        assertInstanceOf(ScratchLayout.InputItem.class, first);
        assertEquals(ScratchTheme.PAD_IN_ROUND, first.x(), "pill 首元素从圆角 padding 开始");
    }

    @Test
    void boolBinaryLeavesTipSpace() {
        ScratchBlock less = ScratchBlock.of(ScratchKind.BINARY);
        less.setField("op", "<");
        ScratchLayout.Node node = layout(less);
        assertEquals(ScratchShape.BOOL, node.shape, "比较运算动态判为 BOOL");
        assertTrue(node.w >= node.h, "六边形 w >= h");
        ScratchLayout.Item first = node.items.get(0);
        assertTrue(first.x() >= node.h / 2, "首元素让出左尖端: x=" + first.x() + " h/2=" + node.h / 2);
    }

    @Test
    void plusBinaryStaysPill() {
        ScratchBlock plus = ScratchBlock.of(ScratchKind.BINARY);
        plus.setField("op", "+");
        ScratchLayout.Node node = layout(plus);
        assertEquals(ScratchShape.PILL, node.shape);
        // 元素序列：socket op socket
        assertEquals(3, node.items.size());
        assertInstanceOf(ScratchLayout.SocketItem.class, node.items.get(0));
        assertInstanceOf(ScratchLayout.OpItem.class, node.items.get(1));
        assertInstanceOf(ScratchLayout.SocketItem.class, node.items.get(2));
    }

    @Test
    void stackFirstItemAlignsNotchRightEdge() {
        ScratchLayout.Node node = layout(ScratchBlock.of(ScratchKind.STMT_ASSIGN));
        assertEquals(ScratchShape.STACK, node.shape);
        assertEquals(ScratchLayout.STACK_TEXT_X, node.items.get(0).x(),
                "语句首元素对齐凹口右缘（官方 cmw）");
        assertTrue(node.h >= ScratchTheme.FIRST_LINE_H, "首行含 padding");
        assertEquals(node.h + ScratchTheme.NOTCH_DEPTH, node.totalHeight(), "渲染总高含底凸榫");
    }

    @Test
    void emptyLoopHeightIsHeadPlusMouthPlusLip() {
        ScratchLayout.Node node = layout(ScratchBlock.of(ScratchKind.CTRL_LOOP));
        assertEquals(ScratchShape.C, node.shape);
        assertEquals(node.rowH + ScratchTheme.EMPTY_MOUTH_H + ScratchTheme.LIP_LINE_H, node.h,
                "空 C 形高 = 头行 + 空腔高 + 下唇");
        assertTrue(node.w >= ScratchTheme.MIN_W_C);
        assertEquals(ScratchTheme.INSET, node.bodyX, "内腔左缘 = 内缩");
    }

    @Test
    void loopBodyContributesHeightAndWidth() {
        ScratchBlock loop = ScratchBlock.of(ScratchKind.CTRL_LOOP);
        ScratchBlock ret = ScratchBlock.of(ScratchKind.STMT_RETURN);
        loop.body().add(ret);
        ScratchLayout.Node node = layout(loop);
        ScratchLayout.Node kid = node.bodyKids.get(0);
        assertEquals(kid.h, node.bodyH, "体高 = 子语句体高");
        assertEquals(node.rowH + kid.h + ScratchTheme.LIP_LINE_H, node.h, "总高含体高");
        assertTrue(node.w >= ScratchTheme.INSET + kid.w, "总宽覆盖体宽");
        assertEquals(node.rowH, node.bodyY, "体区在头行之下");
    }

    @Test
    void socketChildExpandsRowHeight() {
        ScratchBlock assign = ScratchBlock.of(ScratchKind.STMT_ASSIGN);
        // value 插槽放一个 C 形？C 是语句——表达式插槽只放表达式。用嵌套表达式撑高：
        // 多层嵌套 pill 不会比 REPORTER_H 高；行高撑高的来源是 socket 子积木本身高 > 行高。
        // 表达式积木高恒 = rowH >= REPORTER_H，pill 行高被子积木撑高：子积木 h = 自身 rowH。
        // 用 BOOL 套 PILL 套 BOOL 不会超 REPORTER_H。故验证：行高 = max(REPORTER_H, 子高)。
        ScratchBlock ternary = ScratchBlock.of(ScratchKind.TERNARY);
        ternary.requireSocket("cond").setChild(ScratchBlock.of(ScratchKind.NUM));
        ScratchLayout.Node node = layout(ternary);
        ScratchLayout.Node kid = node.socketKids.get(ternary.requireSocket("cond"));
        assertEquals(Math.max(ScratchTheme.REPORTER_H, kid.h), node.h);
        assertTrue(node.h >= kid.h, "行高至少容纳子积木");
        // socket 子 x = SocketItem.x
        ScratchLayout.SocketItem slot = (ScratchLayout.SocketItem) node.items.get(0);
        assertEquals(kid.w, slot.w(), "非空插槽占位宽 = 子积木宽");
    }

    @Test
    void emptySocketUsesPlaceholderWidth() {
        ScratchLayout.Node node = layout(ScratchBlock.of(ScratchKind.STMT_RETURN));
        ScratchLayout.SocketItem slot = (ScratchLayout.SocketItem) node.items.get(1);
        assertEquals(ScratchLayout.EMPTY_SOCKET_W, slot.w(), "空插槽占位宽");
    }

    @Test
    void callHasVarArgButtons() {
        ScratchBlock call = ScratchBlock.of(ScratchKind.CALL);
        call.addSocket("arg0");
        ScratchLayout.Node node = layout(call);
        long varArgs = node.items.stream()
                .filter(i -> i instanceof ScratchLayout.VarArgItem).count();
        assertEquals(2, varArgs, "有参数时 +/- 都在");
        ScratchLayout.Node bare = layout(ScratchBlock.of(ScratchKind.CALL));
        long bareVarArgs = bare.items.stream()
                .filter(i -> i instanceof ScratchLayout.VarArgItem).count();
        assertEquals(1, bareVarArgs, "无参数时仅 +");
    }

    @Test
    void stackHeightSumsBodyHeights() {
        List<ScratchLayout.Node> nodes = List.of(
                layout(ScratchBlock.of(ScratchKind.STMT_BREAK)),
                layout(ScratchBlock.of(ScratchKind.STMT_CONTINUE)));
        assertEquals(nodes.get(0).h + nodes.get(1).h, ScratchLayout.stackHeight(nodes));
    }
}
