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
@ClientSmoke(description = "摄像机 Molang 与 Bedrock 骨骼朝向回归", priority = 1, delayTicks = 100)
public class CameraMolangSmoke {
    public CameraMolangSmoke() {
        Minecraft mc = Minecraft.getInstance();
        CameraType original = mc.options.getCameraType();
        boolean shoulderAvailable = net.minecraftforge.fml.ModList.get().isLoaded("shouldersurfing");
        int modes = shoulderAvailable ? 4 : 3;
        int[] frames = {0};
        int[] verified = {0};
        Throwable[] failure = {null};
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        java.util.function.Consumer<RenderLevelStageEvent> listener = event -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || failure[0] != null) return;
            int frame = frames[0]++;
            int mode = frame / 12;
            if (mode >= modes) return;
            try {
                if (frame % 12 == 6) {
                    verify(mc, mode > 0);
                    verified[0]++;
                    org.slf4j.LoggerFactory.getLogger(CameraMolangSmoke.class).info("Camera Molang mode {} passed", mode);
                }
                if (frame % 12 == 11 && mode + 1 < modes) {
                    if (mode == 0) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    else if (mode == 1) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    else enableShoulderSurfing();
                }
            } catch (Throwable error) {
                failure[0] = error;
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        ClientSmokeVisualHooks.set(m -> {}, image -> {
            try {
                if (failure[0] != null) throw new AssertionError("摄像机查询验证失败", failure[0]);
                if (verified[0] != modes) throw new AssertionError("摄像机模式验证不足: " + verified[0] + "/" + modes);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
                mc.options.setCameraType(original);
            }
        });
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
