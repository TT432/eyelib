package io.github.tt432.eyelib.debug.benchmark;

/**
 * Sampling phases written to {@link FrameSampleBuffer}.
 *
 * <p>The explicit byte code is part of the raw-sample contract and must not be
 * derived from {@link #ordinal()}.</p>
 */
public enum BenchmarkPhase {
    WARMUP((byte) 0),
    MEASURE((byte) 1);

    private final byte code;

    BenchmarkPhase(byte code) {
        this.code = code;
    }

    /** Returns the stable, compact code stored in the sample buffer. */
    public byte code() {
        return code;
    }

    /**
     * Resolves a stored phase code.
     *
     * @throws IllegalArgumentException when {@code code} is not a known phase
     */
    public static BenchmarkPhase fromCode(byte code) {
        return switch (code) {
            case 0 -> WARMUP;
            case 1 -> MEASURE;
            default -> throw new IllegalArgumentException("Unknown benchmark phase code: " + code);
        };
    }
}
