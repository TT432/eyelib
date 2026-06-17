package io.github.tt432.eyelib.material.render;

import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 BrMaterialResolver → BrRenderStateFactory 整条纯逻辑链路的正确性。
 * <p>
 * Oracle：BrRenderState 的 transparency/cull/depth 字段必须符合 Mojang 材质规范。
 * 这条链路完全在 domain 模块内，零 MC 依赖，可在纯 JUnit 中运行。
 * <p>
 * 映射关系（来自 BrRenderTypeFactory.toPortPass，已在此验证）：
 * <ul>
 *   <li>BrRenderState.Transparency.NONE → PortRenderPass.SOLID</li>
 *   <li>BrRenderState.Transparency.BLEND → PortRenderPass.TRANSLUCENT</li>
 *   <li>BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE → PortRenderPass.TRANSLUCENT_EMISSIVE</li>
 *   <li>BrRenderState.Transparency.ALPHA_TEST → PortRenderPass.ALPHA_TEST</li>
 *   <li>BrRenderState.Transparency.ADDITIVE → PortRenderPass.ADDITIVE</li>
 *   <li>BrRenderState.cull=false → disableCulling=true</li>
 * </ul>
 *
 * @author TT432
 */
@NullMarked
class BrRenderStateSpecTest {

    // === 辅助构造 ===

    private static BrMaterialEntry entry(String key, BrMaterialEntry.Defines defines, BrMaterialEntry.States states) {
        return entry(key, defines, states, BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha);
    }

    private static BrMaterialEntry entry(String key, BrMaterialEntry.Defines defines, BrMaterialEntry.States states,
                                         BlendFactor blendSrc, BlendFactor blendDst) {
        String name, base;
        int colon = key.indexOf(':');
        if (colon >= 0) {
            name = key.substring(0, colon);
            base = key.substring(colon + 1);
        } else {
            name = key;
            base = "";
        }
        return new BrMaterialEntry(
                base, name,
                Optional.empty(), Optional.empty(),
                defines,
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                states,
                Optional.empty(),
                new BrMaterialEntry.Blend(
                        Optional.of(blendSrc), Optional.of(blendDst),
                        Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                            Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                List.of()
        );
    }

    private static BrMaterialEntry.Defines defines() {
        return defines(null, null, null);
    }

    private static BrMaterialEntry.Defines defines(List<String> base, List<String> add, List<String> sub) {
        return new BrMaterialEntry.Defines(
                Optional.ofNullable(base), Optional.ofNullable(add), Optional.ofNullable(sub));
    }

    private static BrMaterialEntry.States states() {
        return states(null, null, null);
    }

    private static BrMaterialEntry.States states(List<GLStates> base, List<GLStates> add, List<GLStates> sub) {
        return new BrMaterialEntry.States(
                Optional.ofNullable(base), Optional.ofNullable(add), Optional.ofNullable(sub));
    }

    private static BrRenderState resolve(Map<String, BrMaterialEntry> materials, String key) {
        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(materials.get(key), materials);
        return BrRenderStateFactory.from(resolved);
    }

    // === S1: entity → SOLID ===

    @Test
    @DisplayName("Render §entity: 无 Blending 无 ALPHA_TEST → NONE + cull=true")
    void entityIsSolidWithCull() {
        BrRenderState state = resolve(
                Map.of("entity", entry("entity", defines(), states())),
                "entity");

        assertEquals(BrRenderState.Transparency.NONE, state.transparency(),
                     "entity 无 Blending 无 ALPHA_TEST → Transparency.NONE");
        assertTrue(state.cull(), "entity 无 DisableCulling → cull=true");
        assertTrue(state.isSolid(), "NONE + writeDepth → isSolid=true");
    }

    // === S2: entity_alphablend → TRANSLUCENT ===

    @Test
    @DisplayName("Render §entity_alphablend: +states[Blending] → BLEND + cull=true")
    void entityAlphablendIsBlend() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_alphablend:entity", entry("entity_alphablend:entity",
                                                          defines(), states(null, List.of(GLStates.Blending), null))
                ),
                "entity_alphablend:entity");

        assertEquals(BrRenderState.Transparency.BLEND, state.transparency(),
                     "Blending → Transparency.BLEND");
        assertTrue(state.cull(), "无 DisableCulling → cull=true");
        assertFalse(state.isSolid(), "BLEND → isSolid=false");
    }

    // === S3: 真实 entity_nocull → SOLID（仅 DisableCulling，无 ALPHA_TEST）===

    @Test
    @DisplayName("Render §entity_nocull (real): +states[DisableCulling] only → NONE + cull=false")
    void realEntityNocullIsSolidNoCull() {
        // Bedrock .mcpack 中 entity_nocull:entity 只有 +states[DisableCulling]
        // 不包含 ALPHA_TEST define。ALPHA_TEST 由 render controller 运行时选择材质
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_nocull:entity", entry("entity_nocull:entity",
                                                      defines(), // 无 ALPHA_TEST
                                                      states(null, List.of(GLStates.DisableCulling), null))
                ),
                "entity_nocull:entity");

        assertEquals(BrRenderState.Transparency.NONE, state.transparency(),
                     "真实 entity_nocull 无 ALPHA_TEST 无 Blending → Transparency.NONE");
        assertFalse(state.cull(), "DisableCulling → cull=false");
    }

    // === S3b: 假想材质（ALPHA_TEST + DisableCulling）→ 测试继承链逻辑 ===

    @Test
    @DisplayName("Render §hypothetical: +defines[ALPHA_TEST] +states[DisableCulling] → ALPHA_TEST + cull=false")
    void hypotheticalAlphaTestNoCull() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_nocull:entity", entry("entity_nocull:entity",
                                                      defines(null, List.of("ALPHA_TEST"), null),
                                                      states(null, List.of(GLStates.DisableCulling), null))
                ),
                "entity_nocull:entity");

        assertEquals(BrRenderState.Transparency.ALPHA_TEST, state.transparency(),
                     "ALPHA_TEST define → Transparency.ALPHA_TEST");
        assertFalse(state.cull(), "DisableCulling → cull=false");
    }

    // === S4: entity_beam_additive → ADDITIVE, cull=false, noDepthWrite ===

    @Test
    @DisplayName("Render §entity_beam_additive: blendDst=One + DisableCulling + DisableDepthWrite → ADDITIVE + cull=false")
    void entityBeamAdditiveIsAdditiveNoCullNoDepth() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_alphablend:entity", entry("entity_alphablend:entity",
                                                          defines(), states(null, List.of(GLStates.Blending), null),
                                                          BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha),
                        "entity_beam_additive:entity_alphablend",
                        entry("entity_beam_additive:entity_alphablend",
                              defines(null, List.of("COLOR_BASED", "NO_TEXTURE"), null),
                              states(null, List.of(GLStates.Blending, GLStates.DisableCulling, GLStates.DisableDepthWrite), null),
                              BlendFactor.SourceAlpha, BlendFactor.One)
                ),
                "entity_beam_additive:entity_alphablend");

        assertEquals(BrRenderState.Transparency.ADDITIVE, state.transparency(),
                     "Blending + blendDst=One → isAdditive=true → Transparency.ADDITIVE");
        assertFalse(state.cull(), "DisableCulling → cull=false");
        assertFalse(state.writeMask().writeDepth(),
                    "DisableDepthWrite → writeDepth=false");
        assertEquals(BrRenderState.SurfaceClass.ADDITIVE, state.surfaceClass(),
                     "ADDITIVE transparency 应对应 ADDITIVE surfaceClass");
    }

    // === S5: entity_alphatest → ALPHA_TEST, cull=true ===

    @Test
    @DisplayName("Render §entity_alphatest: +defines[ALPHA_TEST] → ALPHA_TEST + cull=true")
    void entityAlphatestIsAlphaTest() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_alphatest:entity", entry("entity_alphatest:entity",
                                                         defines(null, List.of("ALPHA_TEST"), null), states())
                ),
                "entity_alphatest:entity");

        assertEquals(BrRenderState.Transparency.ALPHA_TEST, state.transparency(),
                     "ALPHA_TEST define → Transparency.ALPHA_TEST");
        assertTrue(state.cull(), "无 DisableCulling → cull=true");
        assertEquals(BrRenderState.SurfaceClass.CUTOUT, state.surfaceClass(),
                     "ALPHA_TEST + no emissive → SurfaceClass.CUTOUT");
    }

    // === S6: USE_EMISSIVE → EMISSIVE_CUTOUT ===

    @Test
    @DisplayName("Render §emissive: +defines[USE_EMISSIVE, ALPHA_TEST] → EMISSIVE_CUTOUT")
    void emissiveAlphaTestIsEmissiveCutout() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "emissive:entity", entry("emissive:entity",
                                                 defines(null, List.of("USE_EMISSIVE", "ALPHA_TEST"), null), states())
                ),
                "emissive:entity");

        assertEquals(BrRenderState.SurfaceClass.EMISSIVE_CUTOUT, state.surfaceClass(),
                     "ALPHA_TEST + USE_EMISSIVE → SurfaceClass.EMISSIVE_CUTOUT (区别于 CUTOUT)");
    }

    // === S7: GLINT → SurfaceClass.GLINT ===

    @Test
    @DisplayName("Render §entity_glint: +defines[GLINT] → SurfaceClass.GLINT")
    void glintIsGlintSurface() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_glint:entity", entry("entity_glint:entity",
                                                     defines(null, List.of("GLINT"), null), states())
                ),
                "entity_glint:entity");

        assertEquals(BrRenderState.SurfaceClass.GLINT, state.surfaceClass(),
                     "GLINT define → SurfaceClass.GLINT（优先级最高）");
    }

    // === S8: standalone material → NONE ===

    @Test
    @DisplayName("Render §独立材质无特殊 defines/states → NONE + cull=true")
    void standaloneIsSolid() {
        BrRenderState state = resolve(
                Map.of("my_material", entry("my_material", defines(), states())),
                "my_material");

        assertEquals(BrRenderState.Transparency.NONE, state.transparency());
        assertTrue(state.cull());
        assertTrue(state.isSolid());
    }

    // === S9: needsCustomRenderType 检测 ===

    @Test
    @DisplayName("Render §entity_nocull (real): 仅 DisableCulling → needsCustomRenderType=true（因 cull 改变即触发）")
    void entityNocullNeedsCustomRenderType() {
        // 真实 entity_nocull：无 ALPHA_TEST，仅 DisableCulling
        // BrRenderState.needsCustomRenderType() 在 cull 改变时不触发 custom
        // 但 writeMask/stencil/blend 等无变化 → 预期 false
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_nocull:entity", entry("entity_nocull:entity",
                                                      defines(),
                                                      states(null, List.of(GLStates.DisableCulling), null))
                ),
                "entity_nocull:entity");

        // DisableCulling 只改变 cull → needsCustomRenderType=false
        assertFalse(state.needsCustomRenderType(),
                    "entity_nocull 仅改 cull → needsCustomRenderType=false（cull 改变不触发 custom RT）");
    }

    @Test
    @DisplayName("Render §entity_beam_additive 需要 custom render type（因 blendSrc/Dst 非默认）")
    void entityBeamAdditiveNeedsCustom() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_alphablend:entity", entry("entity_alphablend:entity",
                                                          defines(), states(null, List.of(GLStates.Blending), null),
                                                          BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha),
                        "entity_beam_additive:entity_alphablend",
                        entry("entity_beam_additive:entity_alphablend",
                              defines(null, List.of("COLOR_BASED"), null),
                              states(null, List.of(GLStates.Blending, GLStates.DisableCulling, GLStates.DisableDepthWrite), null),
                              BlendFactor.SourceAlpha, BlendFactor.One)
                ),
                "entity_beam_additive:entity_alphablend");

        assertTrue(state.needsCustomRenderType(),
                   "entity_beam_additive blendDst=One（非默认 blend）→ needsCustomRenderType=true");
    }

    @Test
    @DisplayName("Render §shader 字段不单独触发 custom render type")
    void shaderFieldsDoNotRequireCustomRenderType() {
        ResolvedBrMaterial material = new ResolvedBrMaterial(
                "entity",
                List.of("entity"),
                Optional.of("eyelibmaterial:shaders/render.vert"),
                Optional.of("eyelibmaterial:shaders/render.frag"),
                Set.of(),
                Set.of(),
                List.of(),
                Optional.empty(),
                ResolvedBrMaterial.BlendState.DEFAULT,
                ResolvedBrMaterial.StencilState.DEFAULT,
                List.of()
        );

        BrRenderState state = BrRenderStateFactory.from(material);

        assertTrue(state.customShader());
        assertFalse(state.needsCustomRenderType(),
                    "仅存在 Bedrock shader 字段时仍应复用普通实体 RenderType");
    }

    @Test
    @DisplayName("Render §charged_creeper: One/One Blending → ADDITIVE + custom render type")
    void chargedCreeperUsesAdditiveCustomRenderType() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_static:entity", entry("entity_static:entity", defines(), states()),
                        "charged_creeper:entity_static", entry("charged_creeper:entity_static",
                                                               defines(null, List.of("USE_UV_ANIM", "ALPHA_TEST"), null),
                                                               states(null, List.of(GLStates.Blending, GLStates.DisableCulling), null),
                                                               BlendFactor.One, BlendFactor.One)
                ),
                "charged_creeper:entity_static");

        assertEquals(BrRenderState.Transparency.ADDITIVE, state.transparency());
        assertFalse(state.cull());
        assertTrue(state.blend().isPresent());
        assertEquals(BlendFactor.One, state.blend().orElseThrow().blendSrc());
        assertEquals(BlendFactor.One, state.blend().orElseThrow().blendDst());
        assertTrue(state.needsCustomRenderType(),
                   "charged_creeper 的 One/One 混合必须保留为自定义 RenderType");
    }

    @Test
    @DisplayName("Render §warden_bioluminescent_layer: USE_EMISSIVE + Blending → TRANSLUCENT_EMISSIVE")
    void wardenBioluminescentLayerUsesTranslucentEmissiveSurface() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "entity_alphablend:entity", entry("entity_alphablend:entity",
                                                          defines(), states(null, List.of(GLStates.Blending), null),
                                                          BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha),
                        "warden_bioluminescent_layer:entity_alphablend",
                        entry("warden_bioluminescent_layer:entity_alphablend",
                              defines(null, List.of("USE_EMISSIVE"), null),
                              states(null, List.of(GLStates.Blending), null),
                              BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha)
                ),
                "warden_bioluminescent_layer:entity_alphablend");

        assertEquals(BrRenderState.Transparency.BLEND, state.transparency());
        assertEquals(BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE, state.surfaceClass());
    }

    // === S10: DisableColorWrite → writeColor=false ===

    @Test
    @DisplayName("Render §DisableColorWrite: +states[DisableColorWrite] → writeColor=false + writeDepth=true")
    void disableColorWrite() {
        BrRenderState state = resolve(
                Map.of(
                        "entity", entry("entity", defines(), states()),
                        "no_color:entity", entry("no_color:entity",
                                                 defines(), states(null, List.of(GLStates.DisableColorWrite), null))
                ),
                "no_color:entity");

        assertFalse(state.writeMask().writeColor(),
                    "DisableColorWrite → writeMask.writeColor=false");
        assertTrue(state.writeMask().writeDepth(),
                   "无 DisableDepthWrite → writeMask.writeDepth=true（默认）");
    }
}
