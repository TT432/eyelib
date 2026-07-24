package io.github.tt432.eyelib.debug.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BenchmarkStatisticsTest {
    private static final long SECOND_NS = 1_000_000_000L;
    private static final long MS_NS = 1_000_000L;

    @Test
    void summaryUsesOnlyMeasureSamplesAndComputesElapsedThroughput() {
        FrameSampleBuffer buffer = new FrameSampleBuffer(4);
        buffer.record(0, 1_000 * MS_NS, 800 * MS_NS, 1, BenchmarkPhase.WARMUP);
        buffer.record(SECOND_NS, 10 * MS_NS, 2 * MS_NS, 1, BenchmarkPhase.MEASURE);
        buffer.record(SECOND_NS + SECOND_NS / 2, 20 * MS_NS, 4 * MS_NS, 1, BenchmarkPhase.MEASURE);
        buffer.record(2 * SECOND_NS, 2_000 * MS_NS, 900 * MS_NS, 1, BenchmarkPhase.WARMUP);

        BenchmarkStatistics.Summary summary = BenchmarkStatistics.summarize(buffer);

        assertEquals(2, summary.frameCount());
        assertEquals(0.5, summary.measurementDurationSeconds(), 0.000001);
        assertEquals(4.0, summary.throughputFps(), 0.000001,
                "throughput is frame count divided by first-to-last elapsed time");
        assertNotEquals((100.0 + 50.0) / 2.0, summary.throughputFps(), 0.000001,
                "throughput must not average instantaneous FPS");
        assertEquals(15.0, summary.frameIntervalPercentilesMs().p50Ms(), 0.000001);
        assertEquals(3.0, summary.renderWorkPercentilesMs().p50Ms(), 0.000001);
    }

    @Test
    void percentilesUseLinearInterpolationInFrameTimeDomain() {
        FrameSampleBuffer buffer = new FrameSampleBuffer(5);
        for (int i = 1; i <= 5; i++) {
            buffer.record((i - 1L) * SECOND_NS, i * MS_NS, i * 2L * MS_NS,
                    1, BenchmarkPhase.MEASURE);
        }

        BenchmarkStatistics.Summary summary = BenchmarkStatistics.summarize(buffer);
        BenchmarkStatistics.Percentiles intervals = summary.frameIntervalPercentilesMs();

        assertEquals(3.0, intervals.p50Ms(), 0.000001);
        assertEquals(4.8, intervals.p95Ms(), 0.000001);
        assertEquals(4.96, intervals.p99Ms(), 0.000001);
        assertEquals(4.996, intervals.p999Ms(), 0.000001);
        assertEquals(1000.0 / 4.96, summary.p99DerivedFps(), 0.000001);
    }

    @Test
    void budgetAndHitchThresholdsAreStrictlyGreaterThan() {
        long[] intervals = {
                33_333_333L,
                33_333_334L,
                66_666_666L,
                66_666_667L,
                100_000_000L,
                100_000_001L
        };
        BenchmarkStatistics.BudgetStatistics budget = summarizeIntervals(intervals).fps30Budget();

        assertEquals(5, budget.missCount());
        assertEquals(3, budget.hitch2xCount());
        assertEquals(1, budget.hitch3xCount(), "exactly 3x budget is not a strict hitch miss");
        assertEquals(5, budget.longestConsecutiveMiss());
        assertEquals(5.0 / 6.0, budget.missRatio(), 0.000001);
    }

    @Test
    void longestConsecutiveMissResetsAfterAnOnBudgetFrame() {
        long[] intervals = {
                40 * MS_NS, 40 * MS_NS, 10 * MS_NS,
                40 * MS_NS, 40 * MS_NS, 40 * MS_NS
        };

        assertEquals(3, summarizeIntervals(intervals).fps30Budget().longestConsecutiveMiss());
    }

    @Test
    void fiveSecondTheilSenSlopeIsRobustToOneOutlier() {
        BenchmarkStatistics.Summary summary = summarizeWindowP50Values(
                10, 11, 12, 100, 14, 15, 16);

        assertEquals(7, summary.fiveSecondWindows().size());
        assertEquals(100.0, summary.fiveSecondWindows().get(3).frameIntervalP50Ms(), 0.000001);
        assertEquals(12.0, summary.windowP50TheilSenSlopeMsPerMinute(), 0.000001,
                "the single outlier must not move the median pairwise slope");
    }

    @Test
    void sustainedFrameTimeIncreaseIsClassifiedAsSlowdown() {
        BenchmarkStatistics.Summary summary = summarizeWindowP50Values(
                10, 11, 12, 13, 14, 15, 16, 17);

        assertEquals(12.0, summary.windowP50TheilSenSlopeMsPerMinute(), 0.000001);
        assertEquals(BenchmarkStatistics.STABILITY_SLOWDOWN, summary.stabilityClassification());
    }

    @Test
    void flatWindowMedianWithRisingWindowTailIsClassifiedAsTailSlowdown() {
        BenchmarkStatistics.Summary summary = summarizeWindowTailValues(20, 40, 60, 80, 100, 120, 140, 160);

        assertEquals(8, summary.fiveSecondWindows().size());
        assertEquals(10.0, summary.fiveSecondWindows().get(0).frameIntervalP50Ms(), 0.000001);
        assertEquals(160.0, summary.fiveSecondWindows().get(7).frameIntervalP99Ms(), 0.000001);
        assertEquals(BenchmarkStatistics.STABILITY_FLAT, summary.stabilityClassification(),
                "window P50 stays constant, so the median classification stays flat");
        assertEquals(BenchmarkStatistics.STABILITY_SLOWDOWN, summary.tailStabilityClassification(),
                "rising window P99 must surface as a tail slowdown, not be masked by the flat median");
    }

    @Test
    void flatWindowMedianAndFlatWindowTailAreBothClassifiedAsFlat() {
        BenchmarkStatistics.Summary summary = summarizeWindowTailValues(30, 30, 30, 30, 30, 30, 30, 30);

        assertEquals(BenchmarkStatistics.STABILITY_FLAT, summary.stabilityClassification());
        assertEquals(BenchmarkStatistics.STABILITY_FLAT, summary.tailStabilityClassification());
    }

    @Test
    void tooFewWindowsYieldInsufficientDataForBothClassifications() {
        BenchmarkStatistics.Summary summary = summarizeWindowTailValues(20, 60);

        assertEquals(2, summary.fiveSecondWindows().size());
        assertEquals(BenchmarkStatistics.STABILITY_INSUFFICIENT_DATA, summary.stabilityClassification());
        assertEquals(BenchmarkStatistics.STABILITY_INSUFFICIENT_DATA, summary.tailStabilityClassification());
    }

    @Test
    void emptyAndSingletonSummariesNeverExposeNonFiniteNumbers() {
        BenchmarkStatistics.Summary empty = BenchmarkStatistics.summarize(new FrameSampleBuffer(0));
        assertEquals(0, empty.frameCount());
        assertEquals(BenchmarkStatistics.STABILITY_INSUFFICIENT_DATA, empty.stabilityClassification());
        assertFiniteSummaryValues(empty);

        FrameSampleBuffer singletonBuffer = new FrameSampleBuffer(1);
        singletonBuffer.record(42, 10 * MS_NS, 4 * MS_NS, 1, BenchmarkPhase.MEASURE);
        BenchmarkStatistics.Summary singleton = BenchmarkStatistics.summarize(singletonBuffer);
        assertEquals(0.0, singleton.throughputFps());
        assertEquals(10.0, singleton.frameIntervalPercentilesMs().p99Ms());
        assertFiniteSummaryValues(singleton);
    }

    private static BenchmarkStatistics.Summary summarizeIntervals(long[] intervalsNs) {
        FrameSampleBuffer buffer = new FrameSampleBuffer(intervalsNs.length);
        for (int i = 0; i < intervalsNs.length; i++) {
            buffer.record(i * SECOND_NS, intervalsNs[i], intervalsNs[i] / 2,
                    1, BenchmarkPhase.MEASURE);
        }
        return BenchmarkStatistics.summarize(buffer);
    }

    private static BenchmarkStatistics.Summary summarizeWindowP50Values(long... valuesMs) {
        FrameSampleBuffer buffer = new FrameSampleBuffer(valuesMs.length);
        for (int i = 0; i < valuesMs.length; i++) {
            buffer.record(i * 5L * SECOND_NS, valuesMs[i] * MS_NS, valuesMs[i] * MS_NS,
                    1, BenchmarkPhase.MEASURE);
        }
        return BenchmarkStatistics.summarize(buffer);
    }

    private static BenchmarkStatistics.Summary summarizeWindowTailValues(long... tailValuesMs) {
        int framesPerWindow = 100;
        int tailFramesPerWindow = 10;
        FrameSampleBuffer buffer = new FrameSampleBuffer(tailValuesMs.length * framesPerWindow);
        for (int w = 0; w < tailValuesMs.length; w++) {
            for (int f = 0; f < framesPerWindow; f++) {
                long intervalNs = f < framesPerWindow - tailFramesPerWindow
                        ? 10 * MS_NS
                        : tailValuesMs[w] * MS_NS;
                buffer.record(w * 5L * SECOND_NS + f * (SECOND_NS / 20), intervalNs, intervalNs,
                        1, BenchmarkPhase.MEASURE);
            }
        }
        return BenchmarkStatistics.summarize(buffer);
    }

    private static void assertFiniteSummaryValues(BenchmarkStatistics.Summary summary) {
        assertFalse(Double.isNaN(summary.measurementDurationSeconds()));
        assertFalse(Double.isInfinite(summary.measurementDurationSeconds()));
        assertFalse(Double.isNaN(summary.throughputFps()));
        assertFalse(Double.isInfinite(summary.throughputFps()));
        assertFalse(Double.isNaN(summary.p99DerivedFps()));
        assertFalse(Double.isInfinite(summary.p99DerivedFps()));
        assertFalse(Double.isNaN(summary.windowP50TheilSenSlopeMsPerMinute()));
        assertFalse(Double.isInfinite(summary.windowP50TheilSenSlopeMsPerMinute()));
        assertFalse(Double.isNaN(summary.windowP99TheilSenSlopeMsPerMinute()));
        assertFalse(Double.isInfinite(summary.windowP99TheilSenSlopeMsPerMinute()));
    }
}
