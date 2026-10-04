package io.github.tt432.eyelib.importer.model.bbmodel;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneAnimationSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class BbModelAnimationsTest {
    private static BBModel parseModel(String animationsJson) {
        return parseModel("[]", animationsJson);
    }

    private static BBModel parseModel(String outlinerJson, String animationsJson) {
        String json = """
                {
                  "meta": {"format_version": "4.5", "model_format": "bedrock", "box_uv": false},
                  "name": "test",
                  "model_identifier": "",
                  "visible_box": [1, 1, 1],
                  "resolution": {"width": 16, "height": 16},
                  "elements": [],
                  "outliner": %s,
                  "textures": [],
                  "animations": %s
                }
                """.formatted(outlinerJson, animationsJson);
        return TestCodecUtil.unwrap(BBModel.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
    }

    private static float constantValue(BrBoneAnimationSchema bone, String channel, float time, int axis) {
        var frames = switch (channel) {
            case "position" -> bone.position();
            case "rotation" -> bone.rotation();
            default -> bone.scale();
        };
        BrBoneKeyFrameSchema frame = frames.get(time);
        var point = frame.dataPoints().get(0);
        MolangValue value = switch (axis) {
            case 0 -> point.x();
            case 1 -> point.y();
            default -> point.z();
        };
        return value.constantValueOrNull().asFloat();
    }

    @Test
    void convertsCoordinateConventionAndLoopType() {
        BBModel model = parseModel("""
                [
                  {
                    "name": "据枪",
                    "loop": "loop",
                    "length": 0.5,
                    "override": false,
                    "anim_time_update": "",
                    "blend_weight": "",
                    "start_delay": "",
                    "loop_delay": "",
                    "animators": {
                      "uuid-a": {
                        "name": "arm",
                        "type": "bone",
                        "keyframes": [
                          {"channel": "position", "time": 0.0, "data_points": [{"x": 1.0, "y": 2.0, "z": 3.0}]},
                          {"channel": "rotation", "time": 0.0, "data_points": [{"x": 10.0, "y": 20.0, "z": 30.0}]},
                          {"channel": "scale", "time": 0.0, "data_points": [{"x": 4.0, "y": 5.0, "z": 6.0}]},
                          {"channel": "position", "time": 0.5, "data_points": [{"x": "query.move", "y": 0.0, "z": 0.0}]}
                        ]
                      }
                    }
                  }
                ]
                """);

        BrAnimationSet set = BbModelAnimations.toAnimationSet(model, "test");

        BrAnimationEntrySchema entry = set.animations().get("test.据枪");
        assertEquals(BrLoopType.LOOP, entry.loop());
        assertEquals(0.5F, entry.animationLength());
        // 空串延迟字段回落 schema 默认值
        assertSame(MolangValue.ONE, entry.blendWeight());
        assertSame(MolangValue.ZERO, entry.startDelay());
        assertEquals("query.anim_time + query.delta_time", entry.animTimeUpdate().toString());

        BrBoneAnimationSchema arm = entry.bones().get("arm");
        // position：仅 x 取反（显示空间→基岩剪辑空间，与 Blockbench 基岩导出同约定）
        assertEquals(-1.0F, constantValue(arm, "position", 0F, 0));
        assertEquals(2.0F, constantValue(arm, "position", 0F, 1));
        assertEquals(3.0F, constantValue(arm, "position", 0F, 2));
        // rotation：x/y 取反
        assertEquals(-10.0F, constantValue(arm, "rotation", 0F, 0));
        assertEquals(-20.0F, constantValue(arm, "rotation", 0F, 1));
        assertEquals(30.0F, constantValue(arm, "rotation", 0F, 2));
        // scale：不取反
        assertEquals(4.0F, constantValue(arm, "scale", 0F, 0));
        // molang 表达式在取反通道包裹 -(…)
        assertEquals("-(query.move)", arm.position().get(0.5F).dataPoints().get(0).x().toString());
    }

    @Test
    void mapsLoopModesAndSkipsNonBoneChannels() {
        BBModel model = parseModel("""
                [
                  {"name": "a", "loop": "once", "animators": {}},
                  {"name": "b", "loop": "hold", "animators": {}},
                  {
                    "name": "c",
                    "animators": {
                      "uuid-e": {
                        "name": "effect",
                        "type": "effect",
                        "keyframes": [{"channel": "effect", "time": 0.0, "data_points": [{"x": 1.0}]}]
                      }
                    }
                  }
                ]
                """);

        BrAnimationSet set = BbModelAnimations.toAnimationSet(model, "test");

        assertEquals(BrLoopType.ONCE, set.animations().get("test.a").loop());
        assertEquals(BrLoopType.HOLD_ON_LAST_FRAME, set.animations().get("test.b").loop());
        // 非 position/rotation/scale 通道不产生骨骼条目
        assertTrue(set.animations().get("test.c").bones().isEmpty());
    }

    @Test
    void subtractsBindRotationFromAbsoluteRotationKeyframes() {
        BBModel model = parseModel("""
                [
                  {
                    "uuid": "g1", "name": "arm", "origin": [0, 0, 0], "rotation": [10, 20, 30],
                    "isOpen": true, "export": true, "locked": false, "visibility": true,
                    "mirror_uv": false, "color": 0, "autouv": 0, "children": []
                  },
                  {
                    "uuid": "g2", "name": "plain", "origin": [0, 0, 0],
                    "isOpen": true, "export": true, "locked": false, "visibility": true,
                    "mirror_uv": false, "color": 0, "autouv": 0, "children": []
                  }
                ]
                """, """
                [
                  {
                    "name": "据枪",
                    "loop": "loop",
                    "animators": {
                      "uuid-a": {
                        "name": "arm",
                        "type": "bone",
                        "keyframes": [
                          {"channel": "rotation", "time": 0.0, "data_points": [{"x": 40.0, "y": 50.0, "z": 60.0}]},
                          {"channel": "rotation", "time": 0.5, "data_points": [{"x": "query.a", "y": 0.0, "z": 0.0}]},
                          {"channel": "position", "time": 0.0, "data_points": [{"x": 1.0, "y": 2.0, "z": 3.0}]}
                        ]
                      },
                      "uuid-b": {
                        "name": "plain",
                        "type": "bone",
                        "keyframes": [
                          {"channel": "rotation", "time": 0.0, "data_points": [{"x": 1.0, "y": 2.0, "z": 3.0}]}
                        ]
                      }
                    }
                  }
                ]
                """);

        BrAnimationEntrySchema entry = BbModelAnimations.toAnimationSet(model, "test").animations().get("test.据枪");

        BrBoneAnimationSchema arm = entry.bones().get("arm");
        // 绝对关键帧先减 bind 旋转再按约定取反：-(40-10)=-30、-(50-20)=-30、60-30=30
        // （运行时 bind + (kf − bind) = kf，与 Blockbench 绝对语义一致）
        assertEquals(-30.0F, constantValue(arm, "rotation", 0F, 0));
        assertEquals(-30.0F, constantValue(arm, "rotation", 0F, 1));
        assertEquals(30.0F, constantValue(arm, "rotation", 0F, 2));
        // position 通道不做 bind 减法（bind 位置恒零向量）
        assertEquals(-1.0F, constantValue(arm, "position", 0F, 0));
        assertEquals(2.0F, constantValue(arm, "position", 0F, 1));
        assertEquals(3.0F, constantValue(arm, "position", 0F, 2));
        // molang 表达式先包 (raw) - (bind) 再在取反通道包 -(…)
        assertEquals("-((query.a) - (10.0))", arm.rotation().get(0.5F).dataPoints().get(0).x().toString());

        // 无 rest 旋转的骨骼行为不变：x/y 取反、z 透传
        BrBoneAnimationSchema plain = entry.bones().get("plain");
        assertEquals(-1.0F, constantValue(plain, "rotation", 0F, 0));
        assertEquals(-2.0F, constantValue(plain, "rotation", 0F, 1));
        assertEquals(3.0F, constantValue(plain, "rotation", 0F, 2));
    }
}
