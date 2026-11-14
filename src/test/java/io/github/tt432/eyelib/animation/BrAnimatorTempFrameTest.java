package io.github.tt432.eyelib.animation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * temp.* 的生命周期是整个实体动画帧，而非单个表达式。
 * Bedrock 实测（1.21.x 客户端原生帧日志，铁傀儡）：物理剪辑的 anim_time_update 读到的 t.x / t.z
 * 来自同帧更早的 pre_animation 与另一剪辑。帧开始时清空一次；帧外（如 render controller）每个表达式仍从空 temp 开始。
 */
@SuppressWarnings("PMD.LawOfDemeter")
class BrAnimatorTempFrameTest {
    private static final String TEMP_A = "t.a";

    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
    }

    @Test
    void scriptsInOneFrameShareTheirTemps() {
        AnimationComponent component = component();
        MolangScope scope = new MolangScope();

        BrAnimator.tickAnimation(component, scope, new AnimationEffects(), 0F,
                () -> new MolangValue("t.x = 3;").eval(scope));

        assertEquals(3F, new MolangValue("v.seen_x").eval(scope), "pre_animation 写的 t.x 到达剪辑");
        assertEquals(2F, new MolangValue("v.seen_w").eval(scope), "animate 权重写的 t.w 到达剪辑");
        assertEquals(5F, new MolangValue("v.seen_e").eval(scope), "控制器 on_entry 写的 t.e 到达剪辑");
        assertEquals(7F, new MolangValue("v.seen_z").eval(scope), "同状态更早剪辑写的 t.z 到达后续剪辑");
    }

    @Test
    void theNextFrameStartsWithoutTheLastFramesTemps() {
        AnimationComponent component = component();
        MolangScope scope = new MolangScope();

        BrAnimator.tickAnimation(component, scope, new AnimationEffects(), 0F,
                () -> new MolangValue("t.x = 3;").eval(scope));
        BrAnimator.tickAnimation(component, scope, new AnimationEffects(), 1F / 60,
                () -> new MolangValue("v.carried = t.x;").eval(scope));

        assertEquals(0F, new MolangValue("v.carried").eval(scope));
        assertEquals(0F, new MolangValue("v.seen_x").eval(scope));
        // on_entry 只在进入状态的那一帧执行，上一帧的 t.e 不跨帧
        assertEquals(0F, new MolangValue("v.seen_e").eval(scope));
        assertEquals(7F, new MolangValue("v.seen_z").eval(scope));
    }

    @Test
    void outsideAFrameEachExpressionStartsWithoutTemps() {
        AnimationComponent component = component();
        MolangScope scope = new MolangScope();

        BrAnimator.tickAnimation(component, scope, new AnimationEffects(), 0F,
                () -> new MolangValue("t.x = 3;").eval(scope));

        assertFalse(scope.tempVariablesHeld());
        assertEquals(0F, new MolangValue("t.x").eval(scope), "帧后的 render controller 看不到帧内 temp");
        assertEquals(5F, new MolangValue("t.y = 5; return t.y;").eval(scope));
        assertEquals(0F, new MolangValue("t.y").eval(scope));
    }

    @Test
    void nestedHoldsClearOnlyAtTheOutermost() {
        MolangScope scope = new MolangScope();
        new MolangValue("t.a = 1;").eval(scope);

        scope.holdTempVariables(); // 最外层：帧开始清空一次
        assertEquals(0F, new MolangValue(TEMP_A).eval(scope));
        new MolangValue("t.a = 2;").eval(scope);
        scope.holdTempVariables(); // 嵌套：不清空
        assertEquals(2F, new MolangValue(TEMP_A).eval(scope));
        scope.releaseTempVariables();
        assertTrue(scope.tempVariablesHeld());
        assertEquals(2F, new MolangValue(TEMP_A).eval(scope));
        scope.releaseTempVariables();
        assertFalse(scope.tempVariablesHeld());
        assertEquals(0F, new MolangValue(TEMP_A).eval(scope));
    }

    /**
     * animate 权重写 t.w 后播放控制器；状态 on_entry 写 t.e，状态内按序播放 writer（写 t.z）与 reader（把 temp 抄进 variable）。
     */
    private static AnimationComponent component() {
        BrAnimationEntry writer = clip("animation.test.temp_frame.writer", "t.z = 7; return 0;");
        BrAnimationEntry reader = clip("animation.test.temp_frame.reader",
                "v.seen_x = t.x; v.seen_w = t.w; v.seen_e = t.e; v.seen_z = t.z; return 0;");
        BrAnimationController controller = BrAnimationController.fromSchema("controller.test.temp_frame",
                TestCodecUtil.unwrap(BrAnimationControllerSchema.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                        {"initial_state":"default","states":{
                         "default":{"animations":["writer","reader"],"on_entry":"t.e = 5;"}}}
                        """))));
        AnimationRegistries.animation().put(controller.name(), controller);

        AnimationComponent component = new AnimationComponent();
        component.setup(Map.of("ctrl", controller.name(), "writer", writer.name(), "reader", reader.name()),
                Map.of("ctrl", new MolangValue("t.w = 2; return 1;")));
        return component;
    }

    private static BrAnimationEntry clip(String name, String timeUpdate) {
        BrAnimationEntry clip = TestCodecUtil.unwrap(BrAnimationEntry.codec(name).parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"loop\":true,\"animation_length\":1,\"anim_time_update\":\"" + timeUpdate
                        + "\",\"bones\":{\"temp_frame\":{\"rotation\":[10,0,0]}}}")));
        AnimationRegistries.animation().put(clip.name(), clip);
        return clip;
    }
}
