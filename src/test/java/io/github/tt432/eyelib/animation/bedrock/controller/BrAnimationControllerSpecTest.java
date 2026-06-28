package io.github.tt432.eyelib.animation.bedrock.controller;

import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcParticleEffect;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对照 Mojang Creator 文档 AnimationRenderController.md 的规范测试。
 * Oracle 来自官方文档定义的 RC 状态机语义。
 *
 * @author TT432
 */
class BrAnimationControllerSpecTest {

    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
    }

    /** Mojang: initial_state 指定控制器的起始状态。不存在则回退到 "default"。 */
    @Test
    @DisplayName("Mojang §RC initial_state: 存在时使用指定状态")
    void initialStateMatchesSchemaDeclaration() {
        BrAcState idleState = mkState(Map.of("anim.idle", MolangValue.ONE));
        BrAnimationController controller = BrAnimationController.fromSchema("controller.test.idle",
                new BrAnimationControllerSchema("idle", Map.of("idle", idleState)));
        assertEquals(idleState.animations(), controller.initialState().animations());
    }

    @Test
    @DisplayName("Mojang §RC initial_state 缺失 → 回退 default")
    void missingInitialStateFallsBackToDefault() {
        BrAcState defaultState = mkState(Map.of("anim.walk", MolangValue.ONE));
        BrAnimationController controller = BrAnimationController.fromSchema("controller.test.missing",
                new BrAnimationControllerSchema("missing", Map.of("default", defaultState)));
        assertEquals(defaultState.animations(), controller.initialState().animations());
    }

    /** Mojang: blend_transition 控制动画间交叉淡入淡出时间（秒）。 */
    @Test
    @DisplayName("Mojang §RC blend_transition: 交叉淡入淡出时间")
    void blendTransitionTimeIsPreserved() {
        BrAcState state = mkState(Map.of("anim.run", MolangValue.ONE), 0.5f);
        assertEquals(0.5f, state.blendTransition(), 0.001f);
    }

    /** Mojang: blend_via_shortest_path 决定旋转是否使用最短插值路径。 */
    @Test
    @DisplayName("Mojang §RC blend_via_shortest_path: 标志位")
    void blendViaShortestPathFlag() {
        assertTrue(mkState(Map.of(), 0f, true).blendViaShortestPath());
        assertFalse(mkState(Map.of(), 0f, false).blendViaShortestPath());
    }

    /** Mojang: 每个 state 的 animations 是动画名→Molang 权重的映射。 */
    @Test
    @DisplayName("Mojang §RC animations: 动画→权重映射")
    void animationWeightsArePreserved() {
        BrAcState state = mkState(Map.of("anim.idle", MolangValue.ONE, "anim.walk", MolangValue.ZERO));
        assertEquals(2, state.animations().size());
        assertEquals(MolangValue.ONE, state.animations().get("anim.idle"));
    }

    /** Mojang: transitions 定义状态转换，key=目标状态，value=Molang 条件表达式。 */
    @Test
    @DisplayName("Mojang §RC transitions: 状态转换条件映射")
    void transitionsMapConditionsToStates() {
        BrAcState state = new BrAcState(
                Map.of("anim.walk", MolangValue.ONE),
                MolangValue.ZERO, MolangValue.ZERO,
                List.of(), List.of(),
                Map.of("running", MolangValue.getConstant(1.0f)),
                0F, false);

        assertTrue(state.transitions().containsKey("running"));
    }

    /** Mojang: particle_effects 在状态进入时触发粒子。 */
    @Test
    @DisplayName("Mojang §RC particle_effects: 状态进入时触发")
    void stateHasParticleEffectsList() {
        BrAcParticleEffect effect = new BrAcParticleEffect(
                Optional.of("minecraft:flame"), Optional.of("locator"),
                false, MolangValue.ZERO);
        BrAcState state = new BrAcState(
                Map.of("anim.idle", MolangValue.ONE),
                MolangValue.ZERO, MolangValue.ZERO,
                List.of(effect), List.of(), Map.of(), 0F, false);

        assertEquals(1, state.particleEffects().size());
        assertTrue(state.particleEffects().get(0).effect().isPresent());
        assertEquals("minecraft:flame", state.particleEffects().get(0).effect().get());
    }

    // === helpers ===

    private static BrAcState mkState(Map<String, MolangValue> anims) {
        return mkState(anims, 0f, false);
    }

    private static BrAcState mkState(Map<String, MolangValue> anims, float blendTime) {
        return mkState(anims, blendTime, false);
    }

    private static BrAcState mkState(Map<String, MolangValue> anims, float blendTime, boolean shortestPath) {
        return new BrAcState(anims, MolangValue.ZERO, MolangValue.ZERO,
                List.of(), List.of(), Map.of(), blendTime, shortestPath);
    }
}
