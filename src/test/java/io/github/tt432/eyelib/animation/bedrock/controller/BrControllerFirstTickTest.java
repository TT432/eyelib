package io.github.tt432.eyelib.animation.bedrock.controller;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 控制器首次 tick（尚无当前状态）只进入初始状态，transitions 从下一帧起判定；首次 tick 晚于实体首帧时同样如此。
 * Bedrock 实测（1.21.x 客户端原生帧日志，铁傀儡）：一个每帧在两状态间翻转、on_exit 写 {@code v.x = q.anim_time} 的控制器，
 * 在其首次 tick 的帧上不执行 on_exit，下一帧才执行。
 *
 * @author TT432
 */
class BrControllerFirstTickTest {
    private static final float FRAME = 1F / 60;

    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
    }

    @Test
    void transitionsWaitForTheTickAfterTheInitialState() {
        BrAnimationController controller = register("controller.test.first_tick", """
                {"initial_state":"default","states":{
                 "default":{"transitions":[{"variant":"v.pick == 2"}]},
                 "variant":{"on_entry":"v.variant_at = v.frame;"}}}
                """);
        BrAnimationController.Data data = controller.createData();
        MolangScope scope = new MolangScope();
        scope.set("variable.pick", 2); // 首次 tick 前已满足转移条件（如 initialize 已选定）
        scope.set("variable.variant_at", -1);
        Map<String, String> aliases = Map.of();

        tick(controller, data, scope, aliases, 0);
        assertEquals(-1F, new MolangValue("v.variant_at").eval(scope), "首次 tick 只进入初始状态");
        tick(controller, data, scope, aliases, 1);
        assertEquals(1F, new MolangValue("v.variant_at").eval(scope), "下一帧才判定 transitions");
    }

    @Test
    void aNestedControllerWaitsOnTheActorsFirstFrameToo() {
        register("controller.test.first_tick.child", """
                {"initial_state":"default","states":{
                 "default":{"transitions":[{"variant":"v.pick == 2"}]},
                 "variant":{"on_entry":"v.variant_at = v.frame;"}}}
                """);
        BrAnimationController parent = register("controller.test.first_tick.parent", """
                {"initial_state":"default","states":{"default":{"animations":["child"]}}}
                """);
        BrAnimationController.Data data = parent.createData();
        MolangScope scope = new MolangScope();
        scope.set("variable.pick", 2);
        scope.set("variable.variant_at", -1);
        Map<String, String> aliases = Map.of("child", "controller.test.first_tick.child");

        tick(parent, data, scope, aliases, 0);
        assertEquals(-1F, new MolangValue("v.variant_at").eval(scope));
        tick(parent, data, scope, aliases, 1);
        assertEquals(1F, new MolangValue("v.variant_at").eval(scope));
    }

    @Test
    void aControllerFirstTickedLaterWaitsOnThatTick() {
        // 子控制器每帧在 a/b 间翻转；父控制器第 3 帧才进入播放它的状态，即子控制器首次 tick 在第 3 帧
        register("controller.test.first_tick.flipper", """
                {"initial_state":"a","states":{
                 "a":{"transitions":[{"b":"1"}],"on_exit":"v.exits = v.exits + 1; v.exit_frame = v.frame;"},
                 "b":{"transitions":[{"a":"1"}],"on_exit":"v.exits = v.exits + 1; v.exit_frame = v.frame;"}}}
                """);
        BrAnimationController parent = register("controller.test.first_tick.gate", """
                {"initial_state":"wait","states":{
                 "wait":{"transitions":[{"go":"v.frame >= 3"}]},
                 "go":{"animations":["flipper"]}}}
                """);
        BrAnimationController.Data data = parent.createData();
        MolangScope scope = new MolangScope();
        scope.set("variable.exits", 0);
        scope.set("variable.exit_frame", -1);
        Map<String, String> aliases = Map.of("flipper", "controller.test.first_tick.flipper");

        for (int frame = 0; frame < 3; frame++) {
            tick(parent, data, scope, aliases, frame);
        }
        tick(parent, data, scope, aliases, 3);
        assertEquals(0F, new MolangValue("v.exits").eval(scope), "子控制器首次 tick 只进入初始状态");
        assertEquals(-1F, new MolangValue("v.exit_frame").eval(scope));
        tick(parent, data, scope, aliases, 4);
        assertEquals(1F, new MolangValue("v.exits").eval(scope));
        assertEquals(4F, new MolangValue("v.exit_frame").eval(scope), "首个 on_exit 在下一帧执行");
        tick(parent, data, scope, aliases, 5);
        assertEquals(2F, new MolangValue("v.exits").eval(scope));
    }

    private static BrAnimationController register(String name, String json) {
        BrAnimationController controller = BrAnimationController.fromSchema(name,
                TestCodecUtil.unwrap(BrAnimationControllerSchema.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))));
        AnimationRegistries.animation().put(controller.name(), controller);
        return controller;
    }

    private static void tick(BrAnimationController controller, BrAnimationController.Data data, MolangScope scope,
                             Map<String, String> aliases, int frame) {
        scope.set("variable.frame", frame);
        controller.tickAnimation(data, aliases, scope, frame * FRAME, 1F, new ModelRuntimeData(), new AnimationEffects(), () -> {
        });
    }
}
