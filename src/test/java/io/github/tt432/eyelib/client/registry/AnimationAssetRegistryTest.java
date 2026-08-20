package io.github.tt432.eyelib.client.registry;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 多源暂存回归：mod 基线（BrAnimationLoader/BrAnimationControllerLoader）、bedrock addon 桥、
 * GUI 导入、节点图构建各占一个来源槽位，任何来源的空暂存/再暂存不得抹掉其他来源。
 *
 * @author TT432
 */
class AnimationAssetRegistryTest {
    @AfterEach
    void tearDown() {
        AnimationAssetRegistry.resetStaging();
    }

    /** 未选中 addon 包时桥发布空暂存，mod 基线动画与控制器必须保留。 */
    @Test
    void emptyAddonStagingKeepsModBaselineEntries() {
        AnimationAssetRegistry.stageAnimations("br-animation-loader",
                Map.of("animations", animation("animation.test.idle")));
        AnimationAssetRegistry.stageControllers("br-animation-controller-loader",
                Map.of("controllers", controllers("controller.animation.test")));

        AnimationAssetRegistry.stageAnimations("bedrock-addon", Map.of());
        AnimationAssetRegistry.stageControllers("bedrock-addon", Map.of());

        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(AnimationLookup.get("controller.animation.test"));
    }

    /** 同一来源再暂存只替换自己的贡献，其他来源的条目不受影响。 */
    @Test
    void restagingSourceRemovesOnlyItsOwnEntries() {
        AnimationAssetRegistry.stageAnimations("br-animation-loader",
                Map.of("animations", animation("animation.test.stale")));
        AnimationAssetRegistry.stageAnimations("bedrock-addon",
                Map.of("bedrock-addon", animation("animation.test.addon")));

        AnimationAssetRegistry.stageAnimations("br-animation-loader",
                Map.of("animations", animation("animation.test.fresh")));

        assertNull(AnimationLookup.get("animation.test.stale"));
        assertNotNull(AnimationLookup.get("animation.test.fresh"));
        assertNotNull(AnimationLookup.get("animation.test.addon"));
    }

    /** 同名 id 冲突时最近一次暂存的来源胜出；再暂存回原来源则恢复。 */
    @Test
    void laterStagedSourceWinsOnIdConflict() {
        BrAnimation mod = animation("animation.test.shared");
        AnimationAssetRegistry.stageAnimations("br-animation-loader", Map.of("animations", mod));
        Object modEntry = mod.animations().get("animation.test.shared");

        BrAnimation addon = animation("animation.test.shared");
        AnimationAssetRegistry.stageAnimations("bedrock-addon", Map.of("bedrock-addon", addon));
        Object addonEntry = addon.animations().get("animation.test.shared");

        assertSame(addonEntry, AnimationLookup.get("animation.test.shared"));

        AnimationAssetRegistry.stageAnimations("br-animation-loader", Map.of("animations", mod));
        assertSame(modEntry, AnimationLookup.get("animation.test.shared"));
    }

    private static BrAnimation animation(String id) {
        return TestCodecUtil.unwrap(BrAnimation.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "animations": {
                    "%s": {}
                  }
                }
                """.formatted(id))));
    }

    private static BrAnimationControllers controllers(String id) {
        return TestCodecUtil.unwrap(BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "animation_controllers": {
                    "%s": {
                      "initial_state": "default",
                      "states": {
                        "default": {}
                      }
                    }
                  }
                }
                """.formatted(id))));
    }
}
