package io.github.tt432.eyelibmaterial.material;

import io.github.tt432.eyelibmaterial.gl.BlendFactor;
import io.github.tt432.eyelibmaterial.gl.GLStates;
import io.github.tt432.eyelibmaterial.render.BrRenderState;
import io.github.tt432.eyelibmaterial.render.BrRenderStateFactory;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
class BrMaterialResolverTest {
    @Test
    void resolvesActionsAndStuffStyleAlphaTestCullOverride() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", "", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull", "entity", defines(), states(null, List.of(GLStates.DisableCulling), null)),
                "entity_alphatest:entity_nocull", entry("entity_alphatest", "entity_nocull", defines(null, List.of("ALPHA_TEST"), null), states()),
                "gmjckk:entity_alphatest", entry("gmjckk", "entity_alphatest", defines(), states(null, null, List.of(GLStates.DisableCulling)))
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(materials.get("gmjckk:entity_alphatest"), materials);
        BrRenderState state = BrRenderStateFactory.from(resolved);

        assertTrue(resolved.hasDefine("ALPHA_TEST"));
        assertFalse(resolved.hasState(GLStates.DisableCulling));
        assertTrue(state.cull());
        assertEquals(BrRenderState.Transparency.ALPHA_TEST, state.transparency());
    }

    @Test
    void resolvesEmissiveAlphaAsCutoutEmissiveWithoutBlending() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", "", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull", "entity", defines(), states(null, List.of(GLStates.DisableCulling), null)),
                "entity_emissive_alpha:entity_nocull", entry(
                        "entity_emissive_alpha",
                        "entity_nocull",
                        defines(null, List.of("ALPHA_TEST", "USE_EMISSIVE"), null),
                        states()
                )
        );

        BrRenderState state = BrRenderStateFactory.from(
                BrMaterialResolver.resolve(materials.get("entity_emissive_alpha:entity_nocull"), materials));

        assertFalse(state.cull());
        assertEquals(BrRenderState.Transparency.ALPHA_TEST, state.transparency());
        assertEquals(BrRenderState.SurfaceClass.EMISSIVE_CUTOUT, state.surfaceClass());
    }

    @Test
    void preservesNonDefaultBlendFactorsAsCustomRenderState() {
        BrMaterialEntry material = entry(
                "lqwvta",
                "entity_alphablend",
                defines(null, List.of("USE_EMISSIVE"), null),
                states(null, List.of(GLStates.Blending), null),
                new BrMaterialEntry.Blend(Optional.of(BlendFactor.One), Optional.of(BlendFactor.One), Optional.empty(), Optional.empty())
        );
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity_alphablend", entry("entity_alphablend", "", defines(), states(null, List.of(GLStates.Blending), null)),
                "lqwvta:entity_alphablend", material
        );

        BrRenderState state = BrRenderStateFactory.from(BrMaterialResolver.resolve(material, materials));

        assertEquals(BrRenderState.Transparency.ADDITIVE, state.transparency());
        assertTrue(state.needsCustomRenderType());
        assertEquals(BlendFactor.One, state.blend().orElseThrow().blendSrc());
        assertEquals(BlendFactor.One, state.blend().orElseThrow().blendDst());
    }

    private static BrMaterialEntry entry(
            String name,
            String base,
            BrMaterialEntry.Defines defines,
            BrMaterialEntry.States states
    ) {
        return entry(name, base, defines, states,
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    private static BrMaterialEntry entry(
            String name,
            String base,
            BrMaterialEntry.Defines defines,
            BrMaterialEntry.States states,
            BrMaterialEntry.Blend blend
    ) {
        return new BrMaterialEntry(
                base,
                name,
                Optional.empty(),
                Optional.empty(),
                defines,
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                states,
                Optional.empty(),
                blend,
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of()
        );
    }

    private static BrMaterialEntry.Defines defines() {
        return defines(null, null, null);
    }

    private static BrMaterialEntry.Defines defines(List<String> base, List<String> add, List<String> sub) {
        return new BrMaterialEntry.Defines(Optional.ofNullable(base), Optional.ofNullable(add), Optional.ofNullable(sub));
    }

    private static BrMaterialEntry.States states() {
        return states(null, null, null);
    }

    private static BrMaterialEntry.States states(List<GLStates> base, List<GLStates> add, List<GLStates> sub) {
        return new BrMaterialEntry.States(Optional.ofNullable(base), Optional.ofNullable(add), Optional.ofNullable(sub));
    }
}
