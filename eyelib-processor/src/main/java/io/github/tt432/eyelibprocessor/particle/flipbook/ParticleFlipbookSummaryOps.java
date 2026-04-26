package io.github.tt432.eyelibprocessor.particle.flipbook;

import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibmolang.compiler.MolangConstantExpressionEvaluator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleFlipbookSummaryOps {
    public static Optional<ParticleFlipbookSummary> summarize(String sourcePath, BrParticle particle) {
        return particle.particleEffect().billboardFlipbook().map(flipbook -> summarize(sourcePath, particle, flipbook));
    }

    public static Map<String, ParticleFlipbookSummary> summarizeAll(Map<String, BrParticle> particleFiles) {
        LinkedHashMap<String, ParticleFlipbookSummary> result = new LinkedHashMap<>();
        particleFiles.forEach((sourcePath, particle) -> summarize(sourcePath, particle)
                .ifPresent(summary -> result.put(sourcePath, summary)));
        return Map.copyOf(result);
    }

    private static ParticleFlipbookSummary summarize(String sourcePath, BrParticle particle, BrParticle.BillboardFlipbook flipbook) {
        ArrayList<String> diagnostics = new ArrayList<>();

        Optional<Float> baseX = parseConstant(flipbook.baseUV().x());
        Optional<Float> baseY = parseConstant(flipbook.baseUV().y());
        Optional<Float> stepX = parseConstant(flipbook.stepUV().x());
        Optional<Float> stepY = parseConstant(flipbook.stepUV().y());
        Optional<Float> maxFrame = parseConstant(flipbook.maxFrame());

        boolean dynamic = baseX.isEmpty() || baseY.isEmpty() || stepX.isEmpty() || stepY.isEmpty() || maxFrame.isEmpty();
        if (!flipbook.stretchToLifetime() && parseConstant(flipbook.framesPerSecond()).isEmpty()) {
            dynamic = true;
        }
        if (dynamic) {
            diagnostics.add("Flipbook uses non-constant Molang input; numeric UV/frame summary was not produced.");
            return new ParticleFlipbookSummary(
                    sourcePath,
                    particle.particleEffect().description().identifier(),
                    true,
                    false,
                    flipbook.stretchToLifetime(),
                    flipbook.loop(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    diagnostics
            );
        }

        int frameCount = (int) Math.floor(maxFrame.orElseThrow());
        if (frameCount <= 0) {
            diagnostics.add("Flipbook max_frame must resolve to a value greater than zero.");
            return new ParticleFlipbookSummary(
                    sourcePath,
                    particle.particleEffect().description().identifier(),
                    false,
                    true,
                    flipbook.stretchToLifetime(),
                    flipbook.loop(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    diagnostics
            );
        }

        float fps = parseConstant(flipbook.framesPerSecond()).orElse(0F);
        if (!flipbook.stretchToLifetime() && fps <= 0F) {
            diagnostics.add("Flipbook frames_per_second must resolve to a value greater than zero when stretch_to_lifetime is false.");
            return new ParticleFlipbookSummary(
                    sourcePath,
                    particle.particleEffect().description().identifier(),
                    false,
                    true,
                    false,
                    flipbook.loop(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    diagnostics
            );
        }

        float minX = baseX.orElseThrow();
        float maxX = baseX.orElseThrow();
        float minY = baseY.orElseThrow();
        float maxY = baseY.orElseThrow();

        for (int frame = 1; frame < frameCount; frame++) {
            float frameX = baseX.orElseThrow() + stepX.orElseThrow() * frame;
            float frameY = baseY.orElseThrow() + stepY.orElseThrow() * frame;
            minX = Math.min(minX, frameX);
            maxX = Math.max(maxX, frameX);
            minY = Math.min(minY, frameY);
            maxY = Math.max(maxY, frameY);
        }

        return new ParticleFlipbookSummary(
                sourcePath,
                particle.particleEffect().description().identifier(),
                false,
                false,
                flipbook.stretchToLifetime(),
                flipbook.loop(),
                Optional.of(new ParticleFlipbookSummary.IntRange(0, frameCount - 1)),
                Optional.of(new ParticleFlipbookSummary.FloatRange(minX, maxX)),
                Optional.of(new ParticleFlipbookSummary.FloatRange(minY, maxY)),
                diagnostics
        );
    }

    private static Optional<Float> parseConstant(String expression) {
        return MolangConstantExpressionEvaluator.tryEvaluateNumber(expression);
    }
}
