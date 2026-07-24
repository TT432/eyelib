package io.github.tt432.eyelib.debug.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Report-stage statistics for frame samples. */
public final class BenchmarkStatistics {
    public static final String STABILITY_FLAT = "flat";
    public static final String STABILITY_SLOWDOWN = "slowdown";
    public static final String STABILITY_WARMUP = "warmup";
    public static final String STABILITY_INSUFFICIENT_DATA = "insufficient-data";

    private static final long NS_PER_SECOND = 1_000_000_000L;
    private static final double NS_PER_MS = 1_000_000.0;
    private static final long WINDOW_NS = 5L * NS_PER_SECOND;
    private static final double MAD_SCALE = 1.4826;
    private static final double MIN_MEANINGFUL_CHANGE_MS = 0.05;

    private BenchmarkStatistics() {
    }

    /**
     * Summarizes only samples whose phase is {@link BenchmarkPhase#MEASURE}.
     *
     * <p>Percentiles use the linearly interpolated quantile definition with
     * zero-based rank {@code p * (n - 1)} after ascending sort. A fractional
     * rank is interpolated between its two neighboring values. Consequently,
     * P50 of an even-sized sample is the mean of its two center values. An
     * empty input has percentile {@code 0}; a singleton has that sample's
     * value for every percentile. All public floating-point results are finite:
     * empty data, a non-positive first-to-last duration, and division by a
     * zero P99 produce {@code 0}, never NaN or infinity.</p>
     *
     * <p>Throughput is {@code frameCount / (lastTimestamp - firstTimestamp)};
     * it is not an average of per-frame instantaneous FPS. Budget and hitch
     * thresholds use strict {@code interval > threshold} comparisons.</p>
     */
    public static Summary summarize(FrameSampleBuffer samples) {
        int frameCount = 0;
        for (int i = 0; i < samples.size(); i++) {
            if (samples.phaseCode(i) == BenchmarkPhase.MEASURE.code()) {
                frameCount++;
            }
        }

        if (frameCount == 0) {
            return new Summary(0, 0, 0, 0, 0,
                    Percentiles.ZERO, Percentiles.ZERO, 0,
                    BudgetStatistics.empty(30), BudgetStatistics.empty(60), BudgetStatistics.empty(120),
                    List.of(), 0, STABILITY_INSUFFICIENT_DATA, 0, STABILITY_INSUFFICIENT_DATA);
        }

        long[] timestampsNs = new long[frameCount];
        long[] frameIntervalsNs = new long[frameCount];
        long[] renderWorkNs = new long[frameCount];
        int outputIndex = 0;
        for (int i = 0; i < samples.size(); i++) {
            if (samples.phaseCode(i) != BenchmarkPhase.MEASURE.code()) {
                continue;
            }
            timestampsNs[outputIndex] = samples.timestampNs(i);
            frameIntervalsNs[outputIndex] = samples.frameIntervalNs(i);
            renderWorkNs[outputIndex] = samples.renderWorkNs(i);
            outputIndex++;
        }

        long firstTimestampNs = timestampsNs[0];
        long lastTimestampNs = timestampsNs[frameCount - 1];
        long durationNs = lastTimestampNs - firstTimestampNs;
        double durationSeconds = durationNs > 0 ? durationNs / (double) NS_PER_SECOND : 0;
        double throughputFps = durationSeconds > 0 ? frameCount / durationSeconds : 0;

        Percentiles frameIntervalPercentilesMs = percentilesMs(frameIntervalsNs);
        Percentiles renderWorkPercentilesMs = percentilesMs(renderWorkNs);
        double p99DerivedFps = frameIntervalPercentilesMs.p99Ms() > 0
                ? 1000.0 / frameIntervalPercentilesMs.p99Ms()
                : 0;

        List<WindowStatistics> windows = windows(timestampsNs, frameIntervalsNs);
        double slopeMsPerMinute = theilSenSlopeMsPerMinute(windows, WindowStatistics::frameIntervalP50Ms);
        String stability = classifyStability(windows, slopeMsPerMinute, WindowStatistics::frameIntervalP50Ms);
        double tailSlopeMsPerMinute = theilSenSlopeMsPerMinute(windows, WindowStatistics::frameIntervalP99Ms);
        String tailStability = classifyStability(windows, tailSlopeMsPerMinute, WindowStatistics::frameIntervalP99Ms);

        return new Summary(frameCount, firstTimestampNs, lastTimestampNs,
                finiteOrZero(durationSeconds), finiteOrZero(throughputFps),
                frameIntervalPercentilesMs, renderWorkPercentilesMs, finiteOrZero(p99DerivedFps),
                budgetStatistics(frameIntervalsNs, 30),
                budgetStatistics(frameIntervalsNs, 60),
                budgetStatistics(frameIntervalsNs, 120),
                windows, finiteOrZero(slopeMsPerMinute), stability,
                finiteOrZero(tailSlopeMsPerMinute), tailStability);
    }

    private static Percentiles percentilesMs(long[] valuesNs) {
        if (valuesNs.length == 0) {
            return Percentiles.ZERO;
        }
        long[] sorted = Arrays.copyOf(valuesNs, valuesNs.length);
        Arrays.sort(sorted);
        return new Percentiles(
                percentileSorted(sorted, 0.5) / NS_PER_MS,
                percentileSorted(sorted, 0.95) / NS_PER_MS,
                percentileSorted(sorted, 0.99) / NS_PER_MS,
                percentileSorted(sorted, 0.999) / NS_PER_MS);
    }

    private static double percentileSorted(long[] sorted, double percentile) {
        if (sorted.length == 0) {
            return 0;
        }
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = percentile * (sorted.length - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) {
            return sorted[lower];
        }
        double fraction = rank - lower;
        return sorted[lower] + (sorted[upper] - (double) sorted[lower]) * fraction;
    }

    private static BudgetStatistics budgetStatistics(long[] intervalsNs, int targetFps) {
        double budgetNs = NS_PER_SECOND / (double) targetFps;
        int missCount = 0;
        int hitch2xCount = 0;
        int hitch3xCount = 0;
        int longestConsecutiveMiss = 0;
        int currentConsecutiveMiss = 0;

        for (long intervalNs : intervalsNs) {
            if (intervalNs > budgetNs) {
                missCount++;
                currentConsecutiveMiss++;
                longestConsecutiveMiss = Math.max(longestConsecutiveMiss, currentConsecutiveMiss);
            } else {
                currentConsecutiveMiss = 0;
            }
            if (intervalNs > budgetNs * 2) {
                hitch2xCount++;
            }
            if (intervalNs > budgetNs * 3) {
                hitch3xCount++;
            }
        }

        return new BudgetStatistics(targetFps, budgetNs / NS_PER_MS, missCount,
                missCount / (double) intervalsNs.length, hitch2xCount, hitch3xCount,
                longestConsecutiveMiss);
    }

    private static List<WindowStatistics> windows(long[] timestampsNs, long[] intervalsNs) {
        List<WindowStatistics> result = new ArrayList<>();
        long epochNs = timestampsNs[0];
        long currentBucket = 0;
        int firstIndex = 0;

        for (int i = 1; i <= timestampsNs.length; i++) {
            long bucket = i == timestampsNs.length
                    ? Long.MIN_VALUE
                    : bucketIndex(epochNs, timestampsNs[i]);
            if (bucket == currentBucket) {
                continue;
            }

            long windowStartNs = epochNs + currentBucket * WINDOW_NS;
            long windowEndNs = windowStartNs + WINDOW_NS;
            long[] sortedWindowIntervals = Arrays.copyOfRange(intervalsNs, firstIndex, i);
            Arrays.sort(sortedWindowIntervals);
            result.add(new WindowStatistics(windowStartNs, windowEndNs, i - firstIndex,
                    percentileSorted(sortedWindowIntervals, 0.5) / NS_PER_MS,
                    percentileSorted(sortedWindowIntervals, 0.99) / NS_PER_MS));

            if (i < timestampsNs.length) {
                firstIndex = i;
                currentBucket = bucket;
            }
        }
        return List.copyOf(result);
    }

    private static long bucketIndex(long epochNs, long timestampNs) {
        if (timestampNs <= epochNs) {
            return 0;
        }
        return (timestampNs - epochNs) / WINDOW_NS;
    }

    private static double theilSenSlopeMsPerMinute(List<WindowStatistics> windows,
                                                   ToDoubleFunction<WindowStatistics> windowValue) {
        int pairCount = windows.size() * (windows.size() - 1) / 2;
        if (pairCount == 0) {
            return 0;
        }
        double[] slopes = new double[pairCount];
        int index = 0;
        for (int i = 0; i < windows.size() - 1; i++) {
            double firstTimeNs = windows.get(i).midpointTimestampNs();
            for (int j = i + 1; j < windows.size(); j++) {
                double elapsedMinutes = (windows.get(j).midpointTimestampNs() - firstTimeNs)
                        / (60.0 * NS_PER_SECOND);
                if (elapsedMinutes > 0) {
                    slopes[index++] = (windowValue.applyAsDouble(windows.get(j))
                            - windowValue.applyAsDouble(windows.get(i))) / elapsedMinutes;
                }
            }
        }
        if (index == 0) {
            return 0;
        }
        Arrays.sort(slopes, 0, index);
        return medianSorted(slopes, index);
    }

    private static String classifyStability(List<WindowStatistics> windows, double slopeMsPerMinute,
                                            ToDoubleFunction<WindowStatistics> windowValue) {
        if (windows.size() < 4) {
            return STABILITY_INSUFFICIENT_DATA;
        }

        double firstTimeNs = windows.get(0).midpointTimestampNs();
        double lastTimeNs = windows.get(windows.size() - 1).midpointTimestampNs();
        double durationMinutes = (lastTimeNs - firstTimeNs) / (60.0 * NS_PER_SECOND);
        if (durationMinutes <= 0) {
            return STABILITY_INSUFFICIENT_DATA;
        }

        double[] intercepts = new double[windows.size()];
        for (int i = 0; i < windows.size(); i++) {
            double elapsedMinutes = (windows.get(i).midpointTimestampNs() - firstTimeNs)
                    / (60.0 * NS_PER_SECOND);
            intercepts[i] = windowValue.applyAsDouble(windows.get(i)) - slopeMsPerMinute * elapsedMinutes;
        }
        Arrays.sort(intercepts);
        double intercept = medianSorted(intercepts, intercepts.length);

        double[] absoluteResiduals = new double[windows.size()];
        for (int i = 0; i < windows.size(); i++) {
            double elapsedMinutes = (windows.get(i).midpointTimestampNs() - firstTimeNs)
                    / (60.0 * NS_PER_SECOND);
            double expected = intercept + slopeMsPerMinute * elapsedMinutes;
            absoluteResiduals[i] = Math.abs(windowValue.applyAsDouble(windows.get(i)) - expected);
        }
        Arrays.sort(absoluteResiduals);
        double noiseMadMs = medianSorted(absoluteResiduals, absoluteResiduals.length);
        double requiredChangeMs = Math.max(MIN_MEANINGFUL_CHANGE_MS, 3 * MAD_SCALE * noiseMadMs);

        int edgeCount = Math.max(2, windows.size() / 3);
        double earlyMedianMs = edgeMedian(windows, 0, edgeCount, windowValue);
        double lateMedianMs = edgeMedian(windows, windows.size() - edgeCount, windows.size(), windowValue);
        double robustOverallChangeMs = lateMedianMs - earlyMedianMs;
        double fittedOverallChangeMs = slopeMsPerMinute * durationMinutes;

        if (fittedOverallChangeMs > requiredChangeMs && robustOverallChangeMs > requiredChangeMs) {
            return STABILITY_SLOWDOWN;
        }
        if (fittedOverallChangeMs < -requiredChangeMs && robustOverallChangeMs < -requiredChangeMs) {
            return STABILITY_WARMUP;
        }
        return STABILITY_FLAT;
    }

    private static double edgeMedian(List<WindowStatistics> windows, int fromIndex, int toIndex,
                                     ToDoubleFunction<WindowStatistics> windowValue) {
        double[] values = new double[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; i++) {
            values[i - fromIndex] = windowValue.applyAsDouble(windows.get(i));
        }
        Arrays.sort(values);
        return medianSorted(values, values.length);
    }

    private static double medianSorted(double[] sorted, int length) {
        int middle = length / 2;
        if ((length & 1) == 1) {
            return sorted[middle];
        }
        return (sorted[middle - 1] + sorted[middle]) / 2;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0;
    }

    /** Frame-time percentiles, all explicitly expressed in milliseconds. */
    public record Percentiles(double p50Ms, double p95Ms, double p99Ms, double p999Ms) {
        private static final Percentiles ZERO = new Percentiles(0, 0, 0, 0);
    }

    /** Strict budget and hitch counts for one target FPS. */
    public record BudgetStatistics(int targetFps, double budgetMs, int missCount, double missRatio,
                                   int hitch2xCount, int hitch3xCount, int longestConsecutiveMiss) {
        private static BudgetStatistics empty(int targetFps) {
            return new BudgetStatistics(targetFps, 1000.0 / targetFps, 0, 0, 0, 0, 0);
        }
    }

    /** Statistics for one non-empty, fixed, half-open five-second timestamp window. */
    public record WindowStatistics(long windowStartTimestampNs, long windowEndTimestampNs,
                                   int frameCount, double frameIntervalP50Ms,
                                   double frameIntervalP99Ms) {
        private double midpointTimestampNs() {
            return windowStartTimestampNs + (windowEndTimestampNs - windowStartTimestampNs) / 2.0;
        }
    }

    /**
     * Immutable summary whose units are encoded in each time-related accessor name.
     *
     * <p>{@code stabilityClassification} tracks the windowed-P50 trend while
     * {@code tailStabilityClassification} independently tracks the windowed-P99 trend,
     * so a flat median can never mask a deteriorating tail.</p>
     */
    public record Summary(int frameCount, long firstTimestampNs, long lastTimestampNs,
                          double measurementDurationSeconds, double throughputFps,
                          Percentiles frameIntervalPercentilesMs,
                          Percentiles renderWorkPercentilesMs, double p99DerivedFps,
                          BudgetStatistics fps30Budget, BudgetStatistics fps60Budget,
                          BudgetStatistics fps120Budget, List<WindowStatistics> fiveSecondWindows,
                          double windowP50TheilSenSlopeMsPerMinute,
                          String stabilityClassification,
                          double windowP99TheilSenSlopeMsPerMinute,
                          String tailStabilityClassification) {
        public Summary {
            fiveSecondWindows = List.copyOf(fiveSecondWindows);
        }
    }
}
