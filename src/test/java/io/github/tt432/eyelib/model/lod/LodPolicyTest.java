package io.github.tt432.eyelib.model.lod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LodPolicyTest {
    @Test
    void zeroIntensityAlwaysKeepsFullDetail() {
        assertEquals(LodLevel.FULL, LodPolicy.select(LodLevel.LOW, 0F, 0F));
        assertEquals(LodLevel.FULL, LodPolicy.select(LodLevel.MEDIUM, 0.01F, Float.NaN));
    }

    @Test
    void selectsThreeLevelsFromProjectedHeight() {
        assertEquals(LodLevel.FULL, LodPolicy.select(LodLevel.FULL, 0.1F, 1F));
        assertEquals(LodLevel.MEDIUM, LodPolicy.select(LodLevel.FULL, 0.05F, 1F));
        assertEquals(LodLevel.LOW, LodPolicy.select(LodLevel.FULL, 0.01F, 1F));
    }

    @Test
    void demotionUsesHysteresisButPromotionIsImmediate() {
        assertEquals(LodLevel.FULL, LodPolicy.select(LodLevel.FULL, 0.07F, 1F));
        assertEquals(LodLevel.MEDIUM, LodPolicy.select(LodLevel.FULL, 0.067F, 1F));
        assertEquals(LodLevel.FULL, LodPolicy.select(LodLevel.MEDIUM, 0.08F, 1F));

        assertEquals(LodLevel.MEDIUM, LodPolicy.select(LodLevel.MEDIUM, 0.022F, 1F));
        assertEquals(LodLevel.LOW, LodPolicy.select(LodLevel.MEDIUM, 0.02F, 1F));
        assertEquals(LodLevel.MEDIUM, LodPolicy.select(LodLevel.LOW, 0.025F, 1F));
    }

    @Test
    void boneThresholdUsesProjectedPixelSize() {
        LodRuntimeState state = new LodRuntimeState();
        state.setPreview(1F, 10F);

        assertFalse(state.shouldRenderBone(0.3F));
        assertTrue(state.shouldRenderBone(0.4F));
    }

    @Test
    void intensityIsClampedAndFinite() {
        assertEquals(0F, LodPolicy.normalizeIntensity(Float.NaN));
        assertEquals(0F, LodPolicy.normalizeIntensity(-1F));
        assertEquals(1F, LodPolicy.normalizeIntensity(2F));
        assertEquals(1F, LodPolicy.normalizeIntensity(Float.POSITIVE_INFINITY));
    }
}
