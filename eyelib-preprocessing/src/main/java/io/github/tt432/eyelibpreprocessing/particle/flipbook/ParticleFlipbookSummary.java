package io.github.tt432.eyelibpreprocessing.particle.flipbook;

import java.util.List;
import java.util.Optional;

public record ParticleFlipbookSummary(
        String sourcePath,
        String identifier,
        boolean dynamic,
        boolean invalid,
        boolean lifetimeDependent,
        boolean loop,
        Optional<IntRange> frameRange,
        Optional<FloatRange> xUvRange,
        Optional<FloatRange> yUvRange,
        List<String> diagnostics
) {
    public ParticleFlipbookSummary {
        frameRange = frameRange == null ? Optional.empty() : frameRange;
        xUvRange = xUvRange == null ? Optional.empty() : xUvRange;
        yUvRange = yUvRange == null ? Optional.empty() : yUvRange;
        diagnostics = List.copyOf(diagnostics);
    }

    public record IntRange(int minInclusive, int maxInclusive) {
    }

    public record FloatRange(float minInclusive, float maxInclusive) {
    }
}
