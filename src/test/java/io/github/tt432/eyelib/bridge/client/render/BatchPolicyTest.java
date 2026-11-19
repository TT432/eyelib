package io.github.tt432.eyelib.bridge.client.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BatchPolicy} 三个策略常量与 {@link BatchStats} 的值语义。
 *
 * @author TT432
 */
class BatchPolicyTest {

    @Test
    @DisplayName("DEFER 只推迟 flush：不分桶、保留 GPU 蒙皮")
    void deferPolicy() {
        assertFalse(BatchPolicy.DEFER.mergeDraws());
        assertTrue(BatchPolicy.DEFER.allowGpuSkinning());
        assertEquals(new BatchPolicy(false, true), BatchPolicy.DEFER);
    }

    @Test
    @DisplayName("MERGE 自建分桶并禁用 GPU 蒙皮")
    void mergePolicy() {
        assertTrue(BatchPolicy.MERGE.mergeDraws());
        assertFalse(BatchPolicy.MERGE.allowGpuSkinning());
        assertEquals(new BatchPolicy(true, false), BatchPolicy.MERGE);
    }

    @Test
    @DisplayName("MERGE_KEEP_SKINNING 自建分桶但保留 GPU 蒙皮")
    void mergeKeepSkinningPolicy() {
        assertTrue(BatchPolicy.MERGE_KEEP_SKINNING.mergeDraws());
        assertTrue(BatchPolicy.MERGE_KEEP_SKINNING.allowGpuSkinning());
        assertEquals(new BatchPolicy(true, true), BatchPolicy.MERGE_KEEP_SKINNING);
    }

    @Test
    @DisplayName("三个策略两两不等")
    void policiesAreDistinct() {
        assertNotEquals(BatchPolicy.DEFER, BatchPolicy.MERGE);
        assertNotEquals(BatchPolicy.DEFER, BatchPolicy.MERGE_KEEP_SKINNING);
        assertNotEquals(BatchPolicy.MERGE, BatchPolicy.MERGE_KEEP_SKINNING);
    }

    @Test
    @DisplayName("BatchStats 为纯值：组件按名取值、记录相等性按值")
    void batchStatsValueSemantics() {
        BatchStats stats = new BatchStats(4, 2, 3, 5, 1);
        assertEquals(4, stats.submits());
        assertEquals(2, stats.skinnedSubmits());
        assertEquals(3, stats.renderTypes());
        assertEquals(5, stats.draws());
        assertEquals(1, stats.vanillaEndBatches());
        assertEquals(new BatchStats(4, 2, 3, 5, 1), stats);
        assertNotEquals(new BatchStats(4, 2, 3, 5, 0), stats);
    }
}
