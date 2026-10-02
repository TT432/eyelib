//? if <1.20.6 {
package io.github.tt432.eyelib.smoke;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.clientsmoke.runtime.ClientSmokeVisualHooks;
import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.molang.MolangValue;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

/** 在真实相机更新后验证动画表达式、越肩偏移和跨 ±180° 身体旋转。 */
@ClientSmoke(description = "摄像机 Molang 与 Bedrock 骨骼朝向回归", priority = 1, delayTicks = 300)
public class CameraMolangSmoke {
    public CameraMolangSmoke() {
        Minecraft mc = Minecraft.getInstance();
        CameraType original = mc.options.getCameraType();
        boolean shoulderAvailable = net.minecraftforge.fml.ModList.get().isLoaded("shouldersurfing");
        int modes = shoulderAvailable ? 4 : 3;
        float[] pitches = {0, -89.9F, -90, 89.9F, 90};
        float originalPitch = mc.player == null ? 0 : mc.player.getXRot();
        int[] frames = {0};
        int[] verified = {0};
        Throwable[] failure = {null};
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        java.util.function.Consumer<RenderLevelStageEvent> listener = event -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || failure[0] != null) return;
            int frame = frames[0]++;
            int scenario = frame / 12;
            int mode = scenario / pitches.length;
            if (mode >= modes) return;
            try {
                if (frame % 12 == 0) {
                    if (mc.player == null) throw new AssertionError("缺少玩家");
                    mc.options.setCameraType(mode == 0 ? CameraType.FIRST_PERSON : mode == 2
                            ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
                    mc.player.setXRot(pitches[scenario % pitches.length]);
                    mc.player.xRotO = mc.player.getXRot();
                    if (mode == 3) {
                        enableShoulderSurfing();
                        setShoulderPitch(mc.player.getXRot());
                    }
                }
                if (frame % 12 == 6) {
                    verify(mc, mode > 0);
                    verifyScreenAligned(mc);
                    if (scenario % pitches.length > 0 && Math.abs(mc.gameRenderer.getMainCamera().getXRot()) < 89) {
                        throw new AssertionError("极限俯仰测试未移动实际镜头");
                    }
                    verified[0]++;
                    org.slf4j.LoggerFactory.getLogger(CameraMolangSmoke.class).info("Camera Molang scenario {} passed, pitch={}", scenario, mc.gameRenderer.getMainCamera().getXRot());
                }
            } catch (Throwable error) {
                failure[0] = error;
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        ClientSmokeVisualHooks.set(m -> {}, image -> {
            try {
                if (failure[0] != null) throw new AssertionError("摄像机查询验证失败", failure[0]);
                if (verified[0] != modes * pitches.length) throw new AssertionError("摄像机模式验证不足: " + verified[0]);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
                mc.options.setCameraType(original);
                if (mc.player != null) { mc.player.setXRot(originalPitch); mc.player.xRotO = originalPitch; }
            }
        });
    }

    private static void setShoulderPitch(float pitch) throws ReflectiveOperationException {
        Object instance = Class.forName("com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing").getMethod("getInstance").invoke(null);
        Object camera = Class.forName("com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing").getMethod("getCamera").invoke(instance);
        Class.forName("com.github.exopandora.shouldersurfing.api.client.IShoulderSurfingCamera").getMethod("setXRot", float.class).invoke(camera, pitch);
    }

    private static void verifyScreenAligned(Minecraft mc) {
        if (mc.level == null) throw new AssertionError("缺少世界");
        // VFX 载体的眼高为 0.009；不能只用普通盔甲架测试相机靠近脚底的情况。
        var carrier = new ArmorStand(mc.level, 0, 0, 0) {
            @Override
            protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
                return .009F;
            }
        };
        var data = RenderData.getComponent(carrier);
        data.ensureOwner(carrier);
        var scope = data.requireScope();
        scope.set("variable.partial_tick", .5F);
        var camera = mc.gameRenderer.getMainCamera();
        for (int axis = 0; axis < 2; axis++) {
            float value = new MolangValue("query.camera_rotation(" + axis + ")").eval(scope);
            float expected = axis == 0 ? camera.getXRot() : camera.getYRot();
            if (Math.abs(Mth.wrapDegrees(value - expected)) > .001F) throw new AssertionError("查询未读取实际镜头旋转");
        }
        var animation = BrAnimationEntry.codec("animation.screen_aligned_smoke").parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"loop":true,"animation_length":1,"bones":{"billboard":{"rotation":[
                  "-query.camera_rotation(0)", "query.camera_rotation(1) + 180 - query.body_y_rotation", 0]}}}
                """)).result().orElseThrow();
        for (Vec3 offset : new Vec3[]{new Vec3(.043, .019, 0), new Vec3(0, -4, 0), new Vec3(0, 4, 0)}) {
            carrier.moveTo(camera.getPosition().subtract(offset));
            for (float yaw : new float[]{-179, 0, 179}) {
                carrier.yBodyRotO = yaw; carrier.yBodyRot = yaw;
                var runtime = new ModelRuntimeData();
                animation.tickAnimation(animation.createData(), Map.of(), scope, .5F, 1, runtime, new AnimationEffects(), () -> {});
                var r = runtime.getData(GlobalBoneIdHandler.get("billboard")).rotation;
                var normal = new Matrix4f().rotateY(-yaw * Mth.DEG_TO_RAD).rotateY(Mth.PI)
                        .rotateZYX(r.z, r.y, r.x).transformDirection(new Vector3f(0, 0, -1));
                var expected = new Vector3f(camera.getLookVector()).negate();
                if (normal.dot(expected) < .9999F) throw new AssertionError("屏幕对齐平面在极限俯仰时偏转");
            }
        }
    }

    private static void enableShoulderSurfing() throws ReflectiveOperationException {
        Class<?> api = Class.forName("com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing");
        Object instance = api.getMethod("getInstance").invoke(null);
        Class<?> service = Class.forName("com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing");
        Class<?> perspective = Class.forName("com.github.exopandora.shouldersurfing.api.model.Perspective");
        service.getMethod("changePerspective", perspective).invoke(instance, perspective.getField("SHOULDER_SURFING").get(null));
        if (!Boolean.TRUE.equals(service.getMethod("isShoulderSurfing").invoke(instance))) {
            throw new AssertionError("越肩视角未激活");
        }
    }

    private static void verify(Minecraft mc, boolean requireOffset) {
        if (mc.level == null || mc.getCameraEntity() == null) throw new AssertionError("缺少世界或相机宿主");
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (requireOffset && camera.distanceTo(mc.getCameraEntity().getEyePosition(1)) < .25) {
            throw new AssertionError("测试未产生实际第三人称镜头偏移");
        }
        var animation = BrAnimationEntry.codec("animation.camera_smoke").parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"loop":true,"animation_length":1,"bones":{"billboard":{"rotation":[
                  "query.rotation_to_camera(0)", "query.rotation_to_camera(1) - query.body_y_rotation", 0]}}}
                """)).result().orElseThrow();
        var carrier = new ArmorStand(mc.level, 0, 0, 0);
        var data = RenderData.getComponent(carrier);
        data.ensureOwner(carrier);
        var scope = data.requireScope();
        var body = new MolangValue("query.body_y_rotation");
        for (Vec3 offset : new Vec3[]{new Vec3(3, 2, 5), new Vec3(-4, -3, 2), new Vec3(1, 0, -4)}) {
            carrier.moveTo(camera.x - offset.x, camera.y - offset.y - carrier.getEyeHeight(), camera.z - offset.z, 0, 0);
            for (float[] yaw : new float[][]{{0, 0}, {179, -179}, {-179, 179}, {35, 80}}) {
                carrier.yBodyRotO = yaw[0]; carrier.yBodyRot = yaw[1];
                for (float partial : new float[]{0, .25F, .5F, .75F, 1}) {
                    scope.set("variable.partial_tick", partial);
                    float renderedBody = Mth.rotLerp(partial, yaw[0], yaw[1]);
                    if (Math.abs(Mth.wrapDegrees(body.eval(scope) - renderedBody)) > .001F) {
                        throw new AssertionError("身体查询与渲染朝向不一致");
                    }
                    var runtime = new ModelRuntimeData();
                    animation.tickAnimation(animation.createData(), Map.of(), scope, .5F, 1, runtime,
                            new AnimationEffects(), () -> {});
                    Vector3f rotation = runtime.getData(GlobalBoneIdHandler.get("billboard")).rotation;
                    // 模型根变换含 Y 轴 180°，基岩模型正面为 -Z；必须与实际绘制空间一致。
                    Vector3f normal = new Matrix4f().rotateY(-renderedBody * Mth.DEG_TO_RAD).rotateY(Mth.PI)
                            .rotateZYX(rotation.z, rotation.y, rotation.x).transformDirection(new Vector3f(0, 0, -1));
                    Vector3f expected = camera.subtract(carrier.getEyePosition(partial)).normalize().toVector3f();
                    if (normal.dot(expected) < .9999F) {
                        throw new AssertionError("动画平面未朝向实际相机: dot=" + normal.dot(expected)
                                + " rotation=" + rotation + " expected=" + expected);
                    }
                }
            }
        }
    }
}
//?}
