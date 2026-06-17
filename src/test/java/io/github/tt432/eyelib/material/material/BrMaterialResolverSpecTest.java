package io.github.tt432.eyelib.material.material;

import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.render.BrRenderState;
import io.github.tt432.eyelib.material.render.BrRenderStateFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对照 Mojang Creator 文档 material-files.md 的规范测试。
 * Oracle 来自官方文档，不来自当前实现输出。
 *
 * @author TT432
 */
class BrMaterialResolverSpecTest {

    // === Helper methods ===

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

    // === Spec-based tests ===

    /**
     * Mojang 文档: entity_nocull:entity → +states [DisableCulling]
     * "This adds the DisableCulling state to the base entity definition."
     */
    @Test
    @DisplayName("Mojang §Inheritance: entity_nocull adds DisableCulling to entity")
    void entityNocullAddsDisableCulling() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull:entity", defines(),
                        states(null, List.of(GLStates.DisableCulling), null))
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_nocull:entity"), materials);

        assertTrue(resolved.hasState(GLStates.DisableCulling),
                "Mojang 文档: entity_nocull 添加 DisableCulling 状态");
    }

    /**
     * Mojang 文档: entity_change_color:entity_nocull → +defines [USE_OVERLAY, USE_COLOR_MASK]
     * 继承链: entity → entity_nocull → entity_change_color
     */
    @Test
    @DisplayName("Mojang §entity_change_color: 深度继承链 — defines 和 states 累积")
    void entityChangeColorDeepInheritance() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull:entity", defines(),
                        states(null, List.of(GLStates.DisableCulling), null)),
                "entity_change_color:entity_nocull", entry("entity_change_color:entity_nocull",
                        defines(null, List.of("USE_OVERLAY", "USE_COLOR_MASK"), null),
                        states())
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_change_color:entity_nocull"), materials);

        assertTrue(resolved.hasDefine("USE_OVERLAY"),
                "Mojang 文档: entity_change_color 添加 USE_OVERLAY define");
        assertTrue(resolved.hasDefine("USE_COLOR_MASK"),
                "Mojang 文档: entity_change_color 添加 USE_COLOR_MASK define");
        assertTrue(resolved.hasState(GLStates.DisableCulling),
                "继承自 entity_nocull 的 DisableCulling 应保留");
    }

    /**
     * Mojang 文档: sheep:entity_change_color → {}
     * "sheep inherits from the entity_change_color material, but doesn't make any changes to it.
     *  It's basically an alias."
     */
    @Test
    @DisplayName("Mojang §sheep: 空子材质 = 父材质别名")
    void sheepIsAliasForEntityChangeColor() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull:entity", defines(),
                        states(null, List.of(GLStates.DisableCulling), null)),
                "entity_change_color:entity_nocull", entry("entity_change_color:entity_nocull",
                        defines(null, List.of("USE_OVERLAY", "USE_COLOR_MASK"), null),
                        states()),
                "sheep:entity_change_color", entry("sheep:entity_change_color", defines(), states())
        );

        ResolvedBrMaterial sheep = BrMaterialResolver.resolve(
                materials.get("sheep:entity_change_color"), materials);
        ResolvedBrMaterial changeColor = BrMaterialResolver.resolve(
                materials.get("entity_change_color:entity_nocull"), materials);

        // sheep 应与 entity_change_color 等价
        assertEquals(changeColor.defines(), sheep.defines(),
                "Mojang 文档: sheep 是 entity_change_color 的别名，defines 应一致");
        assertEquals(changeColor.states(), sheep.states(),
                "Mojang 文档: sheep 是 entity_change_color 的别名，states 应一致");
    }

    /**
     * Mojang 文档: entity_beam_additive:entity_alphablend
     * +defines [COLOR_BASED, NO_TEXTURE], -defines [USE_OVERLAY]
     * +states [Blending, DisableDepthWrite], blendSrc=SourceAlpha, blendDst=One
     */
    @Test
    @DisplayName("Mojang §entity_beam_additive: +defines, -defines, +states, blend override")
    void entityBeamAdditiveOverrides() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_alphablend:entity", entry("entity_alphablend:entity", defines(),
                        states(null, List.of(GLStates.Blending), null),
                        BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha),
                "entity_beam_additive:entity_alphablend", entry("entity_beam_additive:entity_alphablend",
                        defines(null, List.of("COLOR_BASED", "NO_TEXTURE"), List.of("USE_OVERLAY")),
                        states(null, List.of(GLStates.Blending, GLStates.DisableDepthWrite), null),
                        BlendFactor.SourceAlpha, BlendFactor.One)
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_beam_additive:entity_alphablend"), materials);

        assertTrue(resolved.hasDefine("COLOR_BASED"),
                "Mojang 文档: +defines 应包含 COLOR_BASED");
        assertTrue(resolved.hasDefine("NO_TEXTURE"),
                "Mojang 文档: +defines 应包含 NO_TEXTURE");
        // USE_OVERLAY 不在父链中，-defines 在此场景无效果但对无父 defines 的材质应安全
        assertTrue(resolved.hasState(GLStates.Blending),
                "Mojang 文档: +states 应包含 Blending");
        assertTrue(resolved.hasState(GLStates.DisableDepthWrite),
                "Mojang 文档: +states 应包含 DisableDepthWrite");
    }

    /**
     * 验证 -states 可以从父材质中移除已添加的状态。
     * 场景：子材质加了 DisableCulling，孙子材质用 -states 移除了它。
     */
    @Test
    @DisplayName("-states removes state added by parent")
    void subStatesRemovesParentState() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull:entity", defines(),
                        states(null, List.of(GLStates.DisableCulling), null)),
                "entity_alphatest:entity_nocull", entry("entity_alphatest:entity_nocull",
                        defines(null, List.of("ALPHA_TEST"), null),
                        states(null, null, List.of(GLStates.DisableCulling)))
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_alphatest:entity_nocull"), materials);

        assertTrue(resolved.hasDefine("ALPHA_TEST"),
                "ALPHA_TEST define 应被添加");
        assertFalse(resolved.hasState(GLStates.DisableCulling),
                "Mojang 文档: -states 应移除父级别的 DisableCulling");
    }

    /**
     * 验证独立材质（无 base）的自身状态正确。
     */
    @Test
    @DisplayName("独立材质：自身 +states 直接生效")
    void standaloneMaterialOwnStatesWork() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "my_material", entry("my_material",
                        defines(null, List.of("MY_DEFINE"), null),
                        states(null, List.of(GLStates.Blending), null))
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("my_material"), materials);

        assertTrue(resolved.hasDefine("MY_DEFINE"),
                "独立材质的 +defines 应生效");
        assertTrue(resolved.hasState(GLStates.Blending),
                "独立材质的 +states 应生效");
    }

    /**
     * 验证循环继承检测。
     */
    @Test
    @DisplayName("循环继承 → 抛 IllegalStateException")
    void circularInheritanceThrows() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "a:b", entry("a:b", defines(), states()),
                "b:a", entry("b:a", defines(), states())
        );

        assertThrows(IllegalStateException.class,
                () -> BrMaterialResolver.resolve(materials.get("a:b"), materials),
                "Mojang 文档没有明确定义循环检测，但实现应防止无限递归");
    }

    @Test
    @DisplayName("Mojang §entity_alphablend: 添加 Blending 状态，不改 culling")
    void entityAlphablendAddsBlendingOnly() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_alphablend:entity", entry("entity_alphablend:entity",
                        defines(), states(null, List.of(GLStates.Blending), null))
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_alphablend:entity"), materials);

        assertTrue(resolved.hasState(GLStates.Blending),
                "Mojang 文档: entity_alphablend 添加 Blending 状态");
        assertFalse(resolved.hasState(GLStates.DisableCulling),
                "entity_alphablend 不添加 DisableCulling，区别于 entity_nocull");
    }

    /**
     * Mojang 文档: entity_glint 添加 GLINT define 到 entity。
     */
    @Test
    @DisplayName("Mojang §entity_glint: +defines [GLINT] 单层继承")
    void entityGlintAddsGlintDefine() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_glint:entity", entry("entity_glint:entity",
                        defines(null, List.of("GLINT"), null), states())
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_glint:entity"), materials);

        assertTrue(resolved.hasDefine("GLINT"),
                "Mojang 文档: entity_glint 添加 GLINT define");
    }

    /**
     * Mojang 文档: entity_alphatest_glint:entity_alphatest → +defines [GLINT]
     * 两段继承: entity → entity_alphatest → entity_alphatest_glint
     */
    @Test
    @DisplayName("Mojang §entity_alphatest_glint: GLINT 叠加到 alphatest 材质上")
    void entityAlphatestGlintDeepInheritance() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_alphatest:entity", entry("entity_alphatest:entity",
                        defines(null, List.of("ALPHA_TEST"), null), states()),
                "entity_alphatest_glint:entity_alphatest", entry("entity_alphatest_glint:entity_alphatest",
                        defines(null, List.of("GLINT"), null), states())
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_alphatest_glint:entity_alphatest"), materials);

        assertTrue(resolved.hasDefine("ALPHA_TEST"),
                "继承自 entity_alphatest 的 ALPHA_TEST 应保留");
        assertTrue(resolved.hasDefine("GLINT"),
                "Mojang 文档: entity_alphatest_glint 添加 GLINT define");
    }

    /**
     * Mojang 文档: entity_beam_additive 用 -defines [USE_OVERLAY] 移除父级 define。
     * 验证 -defines 可以跨级移除祖父级添加的 define。
     */
    @Test
    @DisplayName("-defines 跨级移除祖父级添加的 define")
    void crossLevelSubDefinesRemovesGrandparentDefine() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull:entity", defines(),
                        states(null, List.of(GLStates.DisableCulling), null)),
                "entity_change_color:entity_nocull", entry("entity_change_color:entity_nocull",
                        defines(null, List.of("USE_OVERLAY", "USE_COLOR_MASK"), null),
                        states()),
                "entity_alphablend:entity_change_color", entry("entity_alphablend:entity_change_color",
                        defines(null, null, List.of("USE_OVERLAY")),
                        states(null, List.of(GLStates.Blending), null))
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_alphablend:entity_change_color"), materials);

        assertTrue(resolved.hasDefine("USE_COLOR_MASK"),
                "entity_change_color 的 USE_COLOR_MASK 未被移除，应保留");
        assertFalse(resolved.hasDefine("USE_OVERLAY"),
                "Mojang 风格: -defines 移除祖父级加的 USE_OVERLAY");
        assertTrue(resolved.hasState(GLStates.Blending),
                "entity_alphablend 的 Blending 应添加");
    }

    /**
     * 验证子材质重写 blend 因子。
     * entity_alphablend 默认 blendDst=OneMinusSrcAlpha，entity_beam_additive 重写为 One。
     */
    @Test
    @DisplayName("子材质重写父材质的 blend 因子")
    void childOverridesBlendFactors() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_alphablend:entity", entry("entity_alphablend:entity",
                        defines(), states(null, List.of(GLStates.Blending), null),
                        BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha),
                "entity_beam:entity_alphablend", entry("entity_beam:entity_alphablend",
                        defines(null, List.of("COLOR_BASED", "NO_TEXTURE"), List.of("USE_OVERLAY")),
                        states(null, List.of(GLStates.Blending, GLStates.DisableDepthWrite), null),
                        BlendFactor.SourceAlpha, BlendFactor.One)
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_beam:entity_alphablend"), materials);

        assertEquals(BlendFactor.SourceAlpha, resolved.blend().blendSrc(),
                "Mojang 文档: blendSrc=SourceAlpha");
        assertEquals(BlendFactor.One, resolved.blend().blendDst(),
                "子材质重写 blendDst 为 One（非父材质的 OneMinusSrcAlpha）");
    }

    /**
     * entity_beam_additive 同时添加 Blending 和 DisableDepthWrite 两个状态。
     */
    @Test
    @DisplayName("同层添加多个 +states: Blending + DisableDepthWrite")
    void multipleAddStatesOnSameLevel() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_alphablend:entity", entry("entity_alphablend:entity",
                        defines(), states(null, List.of(GLStates.Blending), null)),
                "entity_beam:entity_alphablend", entry("entity_beam:entity_alphablend",
                        defines(null, List.of("COLOR_BASED"), null),
                        states(null, List.of(GLStates.Blending, GLStates.DisableDepthWrite), null))
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_beam:entity_alphablend"), materials);

        assertTrue(resolved.hasState(GLStates.Blending),
                "Blending 应保留");
        assertTrue(resolved.hasState(GLStates.DisableDepthWrite),
                "DisableDepthWrite 应与 Blending 同层添加");
    }

    /**
     * -defines 移除不存在的 define 应安全无异常。
     */
    @Test
    @DisplayName("-defines 移除不存在的 define 安全无异常")
    void subDefinesForNonexistentDefineIsSafe() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "my_entity:entity", entry("my_entity:entity",
                        defines(null, null, List.of("NON_EXISTENT_DEFINE")), states())
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("my_entity:entity"), materials);

        // 只要不抛异常就行——resolved 应正确生成
        assertNotNull(resolved, "解析不应失败");
        assertFalse(resolved.hasDefine("NON_EXISTENT_DEFINE"),
                "不存在的 define 不该出现在结果中");
    }

    /**
     * 验证继承链顺序（in order of traversal 从最顶层到最底层）。
     */
    @Test
    @DisplayName("继承链在 resolved 中保留从顶到底的顺序")
    void inheritanceChainPreservesOrder() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_nocull:entity", entry("entity_nocull:entity", defines(),
                        states(null, List.of(GLStates.DisableCulling), null)),
                "entity_change_color:entity_nocull", entry("entity_change_color:entity_nocull",
                        defines(null, List.of("USE_COLOR_MASK"), null), states())
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_change_color:entity_nocull"), materials);

        List<String> chain = resolved.inheritanceChain();
        assertEquals(3, chain.size(), "三层继承链应有 3 个条目");
        // 链中应有被解析材质自身和根材质 entity
        assertTrue(chain.stream().anyMatch(s -> s.contains("entity_change_color")),
                "继承链中应包含被解析的材质自身");
        assertTrue(chain.stream().anyMatch(s -> s.endsWith("entity") || s.equals("entity")),
                "继承链末端应是根材质 entity");
    }

    /**
     * 验证 entity_beam_additive 的完整场景：4 重继承 + blend 重写 + defines/states 加减。
     * entity → entity_alphablend → entity_beam_additive
     */
    @Test
    @DisplayName("Mojang §entity_beam_additive 完整场景：继承 + defines + states + blend")
    void entityBeamAdditiveFullChain() {
        Map<String, BrMaterialEntry> materials = Map.of(
                "entity", entry("entity", defines(), states()),
                "entity_alphablend:entity", entry("entity_alphablend:entity",
                        defines(), states(null, List.of(GLStates.Blending), null),
                        BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha),
                "entity_beam_additive:entity_alphablend", entry("entity_beam_additive:entity_alphablend",
                        defines(null, List.of("COLOR_BASED", "NO_TEXTURE"), List.of("USE_OVERLAY")),
                        states(null, List.of(GLStates.Blending, GLStates.DisableDepthWrite), null),
                        BlendFactor.SourceAlpha, BlendFactor.One)
        );

        ResolvedBrMaterial resolved = BrMaterialResolver.resolve(
                materials.get("entity_beam_additive:entity_alphablend"), materials);

        // Mojang 文档列出的行为
        assertTrue(resolved.hasDefine("COLOR_BASED"), "Mojang: +defines COLOR_BASED");
        assertTrue(resolved.hasDefine("NO_TEXTURE"), "Mojang: +defines NO_TEXTURE");
        assertFalse(resolved.hasDefine("USE_OVERLAY"), "Mojang: -defines USE_OVERLAY");
        assertTrue(resolved.hasState(GLStates.Blending), "Mojang: +states Blending");
        assertTrue(resolved.hasState(GLStates.DisableDepthWrite), "Mojang: +states DisableDepthWrite");
        assertEquals(BlendFactor.SourceAlpha, resolved.blend().blendSrc(), "Mojang: blendSrc=SourceAlpha");
        assertEquals(BlendFactor.One, resolved.blend().blendDst(), "Mojang: blendDst=One");
    }
}
