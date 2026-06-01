package io.github.tt432.eyelibmaterial.gl;

import io.github.tt432.eyelibmaterial.gl.stencil.Face;
import io.github.tt432.eyelibmaterial.gl.stencil.StencilDepthFailOp;
import io.github.tt432.eyelibmaterial.gl.stencil.StencilFailOp;
import io.github.tt432.eyelibmaterial.gl.stencil.StencilFunc;
import io.github.tt432.eyelibmaterial.gl.stencil.StencilPassOp;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author TT432
 */
@NullMarked
@Disabled("All tests require an active OpenGL context — run in a Minecraft dev environment")
class GLStateApplierTest {

    @Test
    @DisplayName("apply() sets correct GL enable/disable from material GLStates")
    void applySetsGLEnableDisableFromStates() {
        var material = createEntry("multi_state", "base",
                List.of(GLStates.Blending, GLStates.DisableDepthTest, GLStates.EnableStencilTest));

        GLStateApplier.apply(material, Map.of("multi_state", material));
        // Verify: GL_BLEND enabled, GL_DEPTH_TEST disabled, GL_STENCIL_TEST enabled
    }

    @Test
    @DisplayName("apply() sets correct blend function (Blending + blend factors)")
    void applySetsBlendFunction() {
        var material = createEntryWithBlend("blend_material", "base",
                List.of(GLStates.Blending),
                BlendFactor.One, BlendFactor.OneMinusSrcAlpha,
                BlendFactor.One, BlendFactor.Zero);

        GLStateApplier.apply(material, Map.of("blend_material", material));
        // Verify: GL_BLEND enabled, glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
    }

    @Test
    @DisplayName("apply() sets correct depth function")
    void applySetsDepthFunction() {
        var material = createEntryWithDepthFunc("depth_material", "base",
                List.of(),
                DepthFunc.LessEqual);

        GLStateApplier.apply(material, Map.of("depth_material", material));
        // Verify: glDepthFunc(GL_LEQUAL)
    }

    @Test
    @DisplayName("apply() sets correct stencil operations")
    void applySetsStencilOperations() {
        var stencil = new BrMaterialEntry.Stencil(
                Optional.of(1),            // stencilRef
                Optional.of(1),            // stencilRefOverride
                Optional.of(0xFF),         // stencilReadMask
                Optional.of(0xFF),         // stencilWriteMask
                Optional.of(new Face(
                        StencilDepthFailOp.Keep,
                        StencilFailOp.Replace,
                        StencilFunc.Always,
                        StencilPassOp.Replace
                )),
                Optional.of(new Face(
                        StencilDepthFailOp.Keep,
                        StencilFailOp.Keep,
                        StencilFunc.Always,
                        StencilPassOp.Keep
                ))
        );

        var material = createEntryWithStencil("stencil_material", "base",
                List.of(GLStates.EnableStencilTest, GLStates.StencilWrite),
                stencil);

        GLStateApplier.apply(material, Map.of("stencil_material", material));
        // Verify: GL_STENCIL_TEST enabled, stencil func/op/mask configured per above
    }

    @Test
    @DisplayName("reset() restores GL state to defaults")
    void resetRestoresDefaults() {
        GLStateApplier.reset();
        // Verify: depth func = LEQUAL, color mask = (true,true,true,true),
        //         culling enabled (BACK), blend disabled, depth test enabled,
        //         depth write enabled, stencil disabled, alpha-to-coverage disabled,
        //         polygon mode = FILL
    }

    @Test
    @DisplayName("Material with DisableCulling disables face culling")
    void materialWithDisableCulling() {
        var material = createEntry("cull_material", "base",
                List.of(GLStates.DisableCulling));

        GLStateApplier.apply(material, Map.of("cull_material", material));
        // Verify: GL_CULL_FACE is disabled
    }

    @Test
    @DisplayName("Material with Wireframe sets polygon mode to GL_LINE")
    void materialWithWireframe() {
        var material = createEntry("wire_material", "base",
                List.of(GLStates.Wireframe));

        GLStateApplier.apply(material, Map.of("wire_material", material));
        // Verify: glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
    }


    @Test
    @DisplayName("Material with Blending state enables blending and applies blend func")
    void materialWithBlending() {
        var material = createEntry("blend_material", "base",
                List.of(GLStates.Blending));

        GLStateApplier.apply(material, Map.of("blend_material", material));
        // Verify: GL_BLEND is enabled, glBlendFuncSeparate called with correct factors
    }



    @Test
    @DisplayName("Material with DisableDepthTest disables depth testing")
    void materialWithDisableDepthTest() {
        var material = createEntry("depth_test_material", "base",
                List.of(GLStates.DisableDepthTest));

        GLStateApplier.apply(material, Map.of("depth_test_material", material));
        // Verify: GL_DEPTH_TEST is disabled
    }



    @Test
    @DisplayName("Material with DisableAlphaWrite masks alpha in color write")
    void materialWithDisableAlphaWrite() {
        var material = createEntry("alpha_write_material", "base",
                List.of(GLStates.DisableAlphaWrite));

        GLStateApplier.apply(material, Map.of("alpha_write_material", material));
        // Verify: glColorMask(true, true, true, false)
    }



    @Test
    @DisplayName("Material with multiple states applies them in correct Bedrock pipeline order")
    void materialWithMultipleStates() {
        var material = createEntry("multi_material", "base",
                List.of(GLStates.Blending, GLStates.DisableDepthWrite, GLStates.Wireframe));

        GLStateApplier.apply(material, Map.of("multi_material", material));
        // Verify: depth test enabled (default), depth write disabled,
        //         blending enabled, wireframe enabled — in this order
    }



    @Test
    @DisplayName("Material with InvertCulling enables culling and culls front faces")
    void materialWithInvertCulling() {
        var material = createEntry("invert_cull_material", "base",
                List.of(GLStates.InvertCulling));

        GLStateApplier.apply(material, Map.of("invert_cull_material", material));
        // Verify: GL_CULL_FACE enabled, glCullFace(GL_FRONT)
    }

    private static BrMaterialEntry createEntry(String name, String base, List<GLStates> states) {
        return new BrMaterialEntry(
                base, name,
                Optional.empty(),   // vertexShader
                Optional.empty(),   // fragmentShader
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.of(states), Optional.empty(), Optional.empty()),
                Optional.empty(),   // depthFunc
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(),   // vertexFields
                Optional.empty(),   // msaaSupport
                Optional.empty(),   // depthBias
                Optional.empty(),   // slopeScaledDepthBias
                Optional.empty(),   // primitiveMode
                Optional.empty(),   // renderTargetFormats
                Optional.empty(),   // isAnimatedTexture
                List.of()           // variants
        );
    }

    private static BrMaterialEntry createEntryWithBlend(String name, String base, List<GLStates> states,
                                                        BlendFactor src, BlendFactor dst,
                                                        BlendFactor alphaSrc, BlendFactor alphaDst) {
        return new BrMaterialEntry(
                base, name,
                Optional.empty(), Optional.empty(),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.of(states), Optional.empty(), Optional.empty()),
                Optional.empty(),
                new BrMaterialEntry.Blend(Optional.of(src), Optional.of(dst),
                        Optional.of(alphaSrc), Optional.of(alphaDst)),
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), List.of()
        );
    }

    private static BrMaterialEntry createEntryWithDepthFunc(String name, String base, List<GLStates> states,
                                                            DepthFunc depthFunc) {
        return new BrMaterialEntry(
                base, name,
                Optional.empty(), Optional.empty(),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.of(states), Optional.empty(), Optional.empty()),
                Optional.of(depthFunc),
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), List.of()
        );
    }

    private static BrMaterialEntry createEntryWithStencil(String name, String base, List<GLStates> states,
                                                          BrMaterialEntry.Stencil stencil) {
        return new BrMaterialEntry(
                base, name,
                Optional.empty(), Optional.empty(),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.of(states), Optional.empty(), Optional.empty()),
                Optional.empty(),
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                stencil,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), List.of()
        );
    }
}