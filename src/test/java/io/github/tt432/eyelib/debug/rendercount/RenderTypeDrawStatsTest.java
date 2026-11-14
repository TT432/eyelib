package io.github.tt432.eyelib.debug.rendercount;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RenderTypeDrawStats} 的帧滚动与快照契约：
 * F3 必须显示完整上一帧，且帧切换不能把上一帧数据带进新帧。
 *
 * @author TT432
 */
class RenderTypeDrawStatsTest {

    @BeforeEach
    void reset() {
        RenderTypeDrawStats.reset();
    }

    @Test
    void emptyBeforeAnyFrame() {
        assertTrue(RenderTypeDrawStats.snapshot().toDebugLines().isEmpty());
    }

    @Test
    void snapshotReadsLastCompletedFrame() {
        RenderTypeDrawStats.record("entity_solid");
        RenderTypeDrawStats.record("entity_solid");
        RenderTypeDrawStats.record("entity_cutout");

        RenderTypeDrawStats.beginFrame();

        RenderTypeDrawStats.Snapshot snapshot = RenderTypeDrawStats.snapshot();
        assertEquals(3, snapshot.totalDraws());
        assertEquals(2, snapshot.renderTypeCount());
        assertEquals(List.of(
                "RenderType draws: 3/frame, 2 types",
                "  entity_solid: 2",
                "  entity_cutout: 1"
        ), snapshot.toDebugLines());
    }

    @Test
    void frameRolloverDoesNotLeakPreviousFrame() {
        RenderTypeDrawStats.record("old");
        RenderTypeDrawStats.beginFrame();

        RenderTypeDrawStats.record("new");
        RenderTypeDrawStats.beginFrame();

        RenderTypeDrawStats.Snapshot snapshot = RenderTypeDrawStats.snapshot();
        assertEquals(1, snapshot.totalDraws());
        assertEquals("new", snapshot.lines().get(0).name());
    }

    @Test
    void rowsAreCappedAndRemainderIsReported() {
        for (int i = 0; i < 15; i++) {
            RenderTypeDrawStats.record("type_" + i);
        }

        RenderTypeDrawStats.beginFrame();

        RenderTypeDrawStats.Snapshot snapshot = RenderTypeDrawStats.snapshot();
        assertEquals(15, snapshot.totalDraws());
        assertEquals(12, snapshot.lines().size());
        assertEquals(3, snapshot.hiddenTypeCount());
        assertEquals(14, snapshot.toDebugLines().size(), "总一行 + 12 行明细 + 1 行折叠");
    }

    @Test
    void tiesAreOrderedByNameForStableOutput() {
        RenderTypeDrawStats.record("b");
        RenderTypeDrawStats.record("a");

        RenderTypeDrawStats.beginFrame();

        assertEquals(List.of("a", "b"), RenderTypeDrawStats.snapshot().lines().stream()
                .map(RenderTypeDrawStats.Line::name)
                .toList());
    }
}
