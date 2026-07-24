package io.github.tt432.eyelib.debug.benchmark;

/**
 * Fixed-capacity, allocation-free frame sample storage.
 *
 * <p>All primitive arrays are allocated by the constructor. A failed
 * {@link #record(long, long, long, int, BenchmarkPhase)} call never changes a
 * previously recorded sample. This class is intentionally not thread-safe;
 * the render thread owns it.</p>
 */
public final class FrameSampleBuffer {
    /** Value callers may record when the rendered entity count is unavailable. */
    public static final int ENTITY_COUNT_UNAVAILABLE = -1;

    private final long[] timestampsNs;
    private final long[] frameIntervalsNs;
    private final long[] renderWorkNs;
    private final int[] renderedEntityCounts;
    private final byte[] phases;
    private int size;
    private boolean overflow;

    /**
     * Creates a buffer with exactly {@code capacity} sample slots.
     *
     * @throws IllegalArgumentException when {@code capacity} is negative
     */
    public FrameSampleBuffer(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be non-negative");
        }
        timestampsNs = new long[capacity];
        frameIntervalsNs = new long[capacity];
        renderWorkNs = new long[capacity];
        renderedEntityCounts = new int[capacity];
        phases = new byte[capacity];
    }

    /**
     * Appends one sample without allocating.
     *
     * @return {@code true} when appended; {@code false} when full
     * @throws NullPointerException when {@code phase} is {@code null}
     */
    public boolean record(long timestampNs, long frameIntervalNs, long renderWorkNs,
                          int renderedEntityCount, BenchmarkPhase phase) {
        if (size == timestampsNs.length) {
            overflow = true;
            return false;
        }
        if (phase == null) {
            throw new NullPointerException("phase");
        }

        int index = size;
        timestampsNs[index] = timestampNs;
        frameIntervalsNs[index] = frameIntervalNs;
        this.renderWorkNs[index] = renderWorkNs;
        renderedEntityCounts[index] = renderedEntityCount;
        phases[index] = phase.code();
        size = index + 1;
        return true;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return timestampsNs.length;
    }

    /** Returns whether at least one append was rejected because the buffer was full. */
    public boolean overflow() {
        return overflow;
    }

    public long timestampNs(int index) {
        checkIndex(index);
        return timestampsNs[index];
    }

    public long frameIntervalNs(int index) {
        checkIndex(index);
        return frameIntervalsNs[index];
    }

    public long renderWorkNs(int index) {
        checkIndex(index);
        return renderWorkNs[index];
    }

    public int renderedEntityCount(int index) {
        checkIndex(index);
        return renderedEntityCounts[index];
    }

    public byte phaseCode(int index) {
        checkIndex(index);
        return phases[index];
    }

    public BenchmarkPhase phase(int index) {
        return BenchmarkPhase.fromCode(phaseCode(index));
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}
