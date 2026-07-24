package io.github.tt432.eyelib.debug.benchmark;

/**
 * Fixed-capacity, allocation-free storage for low-frequency resource samples.
 *
 * <p>GC count and pause are cumulative process values at the sample timestamp.
 * Process CPU load is stored exactly as supplied (normally a value in
 * {@code [0, 1]}, or a caller-defined unavailable sentinel).</p>
 */
public final class ResourceSampleBuffer {
    private final long[] timestampsNs;
    private final long[] heapUsedBytes;
    private final long[] heapCommittedBytes;
    private final long[] gcCounts;
    private final long[] gcPauseMs;
    private final double[] processCpuLoads;
    private int size;
    private boolean overflow;

    /**
     * Creates a buffer with exactly {@code capacity} sample slots.
     *
     * @throws IllegalArgumentException when {@code capacity} is negative
     */
    public ResourceSampleBuffer(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be non-negative");
        }
        timestampsNs = new long[capacity];
        heapUsedBytes = new long[capacity];
        heapCommittedBytes = new long[capacity];
        gcCounts = new long[capacity];
        gcPauseMs = new long[capacity];
        processCpuLoads = new double[capacity];
    }

    /**
     * Appends one resource sample without allocating.
     *
     * @return {@code true} when appended; {@code false} when full
     */
    public boolean record(long timestampNs, long heapUsedBytes, long heapCommittedBytes,
                          long gcCount, long gcPauseMs, double processCpuLoad) {
        if (size == timestampsNs.length) {
            overflow = true;
            return false;
        }

        int index = size;
        timestampsNs[index] = timestampNs;
        this.heapUsedBytes[index] = heapUsedBytes;
        this.heapCommittedBytes[index] = heapCommittedBytes;
        gcCounts[index] = gcCount;
        this.gcPauseMs[index] = gcPauseMs;
        processCpuLoads[index] = processCpuLoad;
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

    public long heapUsedBytes(int index) {
        checkIndex(index);
        return heapUsedBytes[index];
    }

    public long heapCommittedBytes(int index) {
        checkIndex(index);
        return heapCommittedBytes[index];
    }

    public long gcCount(int index) {
        checkIndex(index);
        return gcCounts[index];
    }

    public long gcPauseMs(int index) {
        checkIndex(index);
        return gcPauseMs[index];
    }

    public double processCpuLoad(int index) {
        checkIndex(index);
        return processCpuLoads[index];
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}
