package io.github.tt432.eyelibpreprocessing.particle.flipbook;

import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleFlipbookSummaryOpsTest {
    @Test
    void summarizeProducesConstantRangesForInlineFlipbook() {
        ParticleFlipbookSummary summary = ParticleFlipbookSummaryOps.summarize(
                "particles/witchspell.json",
                particleWithFlipbook("64", "72", "8", "8", "-8", "0", "10", "8", true, false)
        ).orElseThrow();

        assertFalse(summary.dynamic());
        assertFalse(summary.invalid());
        assertTrue(summary.lifetimeDependent());
        assertFalse(summary.loop());
        assertEquals(0, summary.frameRange().orElseThrow().minInclusive());
        assertEquals(7, summary.frameRange().orElseThrow().maxInclusive());
        assertEquals(8F, summary.xUvRange().orElseThrow().minInclusive());
        assertEquals(64F, summary.xUvRange().orElseThrow().maxInclusive());
        assertEquals(72F, summary.yUvRange().orElseThrow().minInclusive());
        assertEquals(72F, summary.yUvRange().orElseThrow().maxInclusive());
    }

    @Test
    void summarizeMarksDynamicWhenFlipbookUsesNonConstantMolang() {
        ParticleFlipbookSummary summary = ParticleFlipbookSummaryOps.summarize(
                "particles/dynamic.json",
                particleWithFlipbook("64", "72", "8", "8", "-8", "0", "variable.fps", "8", false, false)
        ).orElseThrow();

        assertTrue(summary.dynamic());
        assertFalse(summary.invalid());
        assertTrue(summary.frameRange().isEmpty());
        assertTrue(summary.xUvRange().isEmpty());
        assertTrue(summary.yUvRange().isEmpty());
    }

    @Test
    void summarizeMarksInvalidWhenMaxFrameIsNonPositive() {
        ParticleFlipbookSummary summary = ParticleFlipbookSummaryOps.summarize(
                "particles/invalid-max.json",
                particleWithFlipbook("64", "72", "8", "8", "-8", "0", "10", "0", true, false)
        ).orElseThrow();

        assertFalse(summary.dynamic());
        assertTrue(summary.invalid());
        assertTrue(summary.frameRange().isEmpty());
    }

    @Test
    void summarizeMarksInvalidWhenNonLifetimeFlipbookHasNonPositiveFps() {
        ParticleFlipbookSummary summary = ParticleFlipbookSummaryOps.summarize(
                "particles/invalid-fps.json",
                particleWithFlipbook("64", "72", "8", "8", "-8", "0", "0", "8", false, false)
        ).orElseThrow();

        assertFalse(summary.dynamic());
        assertTrue(summary.invalid());
        assertTrue(summary.frameRange().isEmpty());
    }

    @Test
    void summarizeSupportsFoldableArithmeticAndMathExpressions() {
        ParticleFlipbookSummary summary = ParticleFlipbookSummaryOps.summarize(
                "particles/folded.json",
                particleWithFlipbook("32 + 32", "64 + 8", "8", "8", "math.abs(-8)", "0", "4 + 6", "2 * 4", false, true)
        ).orElseThrow();

        assertFalse(summary.dynamic());
        assertFalse(summary.invalid());
        assertTrue(summary.loop());
        assertEquals(0, summary.frameRange().orElseThrow().minInclusive());
        assertEquals(7, summary.frameRange().orElseThrow().maxInclusive());
        assertEquals(64F, summary.xUvRange().orElseThrow().minInclusive());
        assertEquals(120F, summary.xUvRange().orElseThrow().maxInclusive());
        assertEquals(72F, summary.yUvRange().orElseThrow().minInclusive());
        assertEquals(72F, summary.yUvRange().orElseThrow().maxInclusive());
    }

    @Test
    void summarizeMarksDynamicWhenFoldableAnalysisFailsForRuntimeVariable() {
        ParticleFlipbookSummary summary = ParticleFlipbookSummaryOps.summarize(
                "particles/runtime-variable.json",
                particleWithFlipbook("64", "72", "8", "8", "-8", "0", "variable.fps", "2 + 6", false, false)
        ).orElseThrow();

        assertTrue(summary.dynamic());
        assertFalse(summary.invalid());
    }

    @Test
    void summarizeMarksInvalidWhenFoldedExpressionsResolveToNonPositiveValues() {
        ParticleFlipbookSummary invalidMaxFrame = ParticleFlipbookSummaryOps.summarize(
                "particles/folded-invalid-max.json",
                particleWithFlipbook("64", "72", "8", "8", "-8", "0", "10", "2 - 2", true, false)
        ).orElseThrow();
        ParticleFlipbookSummary invalidFps = ParticleFlipbookSummaryOps.summarize(
                "particles/folded-invalid-fps.json",
                particleWithFlipbook("64", "72", "8", "8", "-8", "0", "math.abs(0)", "8", false, false)
        ).orElseThrow();

        assertTrue(invalidMaxFrame.invalid());
        assertTrue(invalidFps.invalid());
    }

    private static BrParticle particleWithFlipbook(
            String baseX,
            String baseY,
            String sizeX,
            String sizeY,
            String stepX,
            String stepY,
            String framesPerSecond,
            String maxFrame,
            boolean stretchToLifetime,
            boolean loop
    ) {
        LinkedHashMap<String, BedrockResourceValue> components = new LinkedHashMap<>();
        components.put("minecraft:particle_appearance_billboard", object(
                entry("size", array(number("1"), number("1"))),
                entry("facing_camera_mode", string("lookat_xyz")),
                entry("uv", object(
                        entry("texture_width", number("128")),
                        entry("texture_height", number("128")),
                        entry("flipbook", object(
                                entry("base_UV", array(scalar(baseX), scalar(baseY))),
                                entry("size_UV", array(scalar(sizeX), scalar(sizeY))),
                                entry("step_UV", array(scalar(stepX), scalar(stepY))),
                                entry("frames_per_second", scalar(framesPerSecond)),
                                entry("max_frame", scalar(maxFrame)),
                                entry("stretch_to_lifetime", new BedrockResourceValue.BooleanValue(stretchToLifetime)),
                                entry("loop", new BedrockResourceValue.BooleanValue(loop))
                        ))
                ))
        ));

        return new BrParticle(
                "1.10.0",
                new BrParticle.ParticleEffect(
                        new BrParticle.Description(
                                "sample:test_particle",
                                new BrParticle.BasicRenderParameters(
                                        "particles_alpha",
                                        "textures/particle/particles"
                                )
                        ),
                        Map.of(),
                        new BrParticle.Events(),
                        components
                )
        );
    }

    private static Map.Entry<String, BedrockResourceValue> entry(String key, BedrockResourceValue value) {
        return Map.entry(key, value);
    }

    @SafeVarargs
    private static BedrockResourceValue.ObjectValue object(Map.Entry<String, BedrockResourceValue>... entries) {
        LinkedHashMap<String, BedrockResourceValue> values = new LinkedHashMap<>();
        for (Map.Entry<String, BedrockResourceValue> entry : entries) {
            values.put(entry.getKey(), entry.getValue());
        }
        return new BedrockResourceValue.ObjectValue(values);
    }

    private static BedrockResourceValue.ArrayValue array(BedrockResourceValue... values) {
        return new BedrockResourceValue.ArrayValue(java.util.List.of(values));
    }

    private static BedrockResourceValue scalar(String value) {
        try {
            return number(value);
        } catch (NumberFormatException ignored) {
            return string(value);
        }
    }

    private static BedrockResourceValue.NumberValue number(String value) {
        return new BedrockResourceValue.NumberValue(new BigDecimal(value));
    }

    private static BedrockResourceValue.StringValue string(String value) {
        return new BedrockResourceValue.StringValue(value);
    }
}
