package io.github.tt432.eyelib.bridge.particle.adapter;

import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.Emitter;
import io.github.tt432.eyelib.wintersky.Scene;
import io.github.tt432.eyelib.wintersky.three.Object3D;
import io.github.tt432.eyelib.wintersky.three.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * wintersky Scene 生命周期与 spawn 入口（ADR-0035 §3.1）。
 *
 * <ul>
 *   <li>持有 {@link Scene} 单例，注入 fetchTexture/fetchParticleFile 钩子（D5）；</li>
 *   <li>渲染帧驱动 tick（D4）：真实帧 dt 累积，按全局 tick_rate 固定步长消费，暂停冻结；</li>
 *   <li>音效事件经 emitter 的 {@code play_sound} 监听出域到 MC 音效（D6）；</li>
 *   <li>一次性发射器粒子耗尽后自动 {@link Emitter#delete()}，LoggingOut 全量重建。</li>
 * </ul>
 */
public final class WinterskySceneManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinterskySceneManager.class);

    private WinterskySceneManager() {
    }

    /** 单帧最多消费的固定步数（防卡顿后死亡螺旋）。 */
    private static final int MAX_STEPS_PER_FRAME = 8;

    private static Scene scene = createScene();
    /** 相机替身：updateFacingRotation 用，每渲染帧回填位置/旋转。 */
    private static final Object3D cameraProxy = new Object3D();
    /** 已挂 play_sound 监听的 emitter（弱键，emitter delete 后自动回收）。 */
    private static final Set<Emitter> soundWired = java.util.Collections.newSetFromMap(new WeakHashMap<>());

    private static double accumulator = 0;
    private static long lastTickNanos = -1;

    private static Scene createScene() {
        Scene.Options options = new Scene.Options();
        options.fetchTexture = WinterskySceneManager::fetchTexture;
        options.fetchParticleFile = WinterskyParticleFileLoader::fetchParticleFile;
        return new Scene(options);
    }

    public static Scene scene() {
        return scene;
    }

    static Object3D cameraProxy() {
        return cameraProxy;
    }

    /**
     * 在世界坐标 spawn 一个粒子效果。
     *
     * @return true 表示 emitter 已创建并 start
     */
    public static boolean spawn(String identifier, double x, double y, double z) {
        Map<String, Object> json = WinterskyParticleFileLoader.jsonFor(identifier);
        if (json == null) {
            LOGGER.warn("[wintersky] 粒子未找到: {}", identifier);
            return false;
        }
        try {
            Emitter emitter = new Emitter(scene, json, new Emitter.Options());
            emitter.global_space.position.set(x, y, z);
            emitter.start();
            wireSound(emitter);
            return true;
        } catch (Exception e) {
            LOGGER.warn("[wintersky] 粒子创建失败 {}: {}", identifier, e.toString());
            return false;
        }
    }

    /** 清空场景（含全部 emitter），重建 Scene 与缓存。 */
    public static void clear() {
        for (Emitter emitter : new ArrayList<>(scene.emitters)) {
            emitter.delete();
        }
        scene = createScene();
        soundWired.clear();
        accumulator = 0;
        lastTickNanos = -1;
        WinterskyParticleFileLoader.invalidate();
        WinterskyRenderTypes.clearCache();
    }

    /** 渲染帧驱动 tick（RenderTick START 调用）；暂停时冻结。 */
    public static void onRenderTickStart() {
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        if (minecraft.isPaused() || minecraft.level == null) {
            lastTickNanos = now;
            return;
        }
        if (lastTickNanos < 0) {
            lastTickNanos = now;
            return;
        }
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        if (dt <= 0) {
            return;
        }
        // 单帧 dt 上限：窗口拖拽/断帧后不追帧
        accumulator += Math.min(dt, 0.1);
        double step = 1.0 / scene.global_options.tick_rate;
        int steps = Math.min((int) (accumulator / step), MAX_STEPS_PER_FRAME);
        accumulator = Math.min(accumulator - steps * step, step * MAX_STEPS_PER_FRAME);
        for (int i = 0; i < steps; i++) {
            tickRoots();
        }
    }

    /** 固定步长 tick：仅根发射器（子发射器经父发射器递归 tick，与 Node oracle 一致）。 */
    private static void tickRoots() {
        for (Emitter emitter : new ArrayList<>(scene.emitters)) {
            if (emitter.parent_emitter == null) {
                emitter.tick();
            }
        }
        for (Emitter emitter : new ArrayList<>(scene.emitters)) {
            if (!soundWired.contains(emitter)) {
                wireSound(emitter);
            }
            if (isFinished(emitter)) {
                emitter.delete();
            }
        }
    }

    /** 一次性发射器终结判定：不会自动重启（非 looping/expression）且粒子与子发射器均已清空。 */
    private static boolean isFinished(Emitter emitter) {
        if (emitter.enabled || !emitter.particles.isEmpty() || !emitter.child_emitters.isEmpty()) {
            return false;
        }
        String mode = emitter.config.emitter_lifetime_mode;
        boolean restarts = "expression".equals(mode)
                || "looping".equals(emitter.loop_mode)
                || ("auto".equals(emitter.loop_mode) && "looping".equals(mode));
        return !restarts;
    }

    private static void wireSound(Emitter emitter) {
        if (!soundWired.add(emitter)) {
            return;
        }
        emitter.on("play_sound", data -> {
            if (!(data instanceof Map<?, ?> map)) {
                return;
            }
            Object sound = map.get("sound_effect");
            if (sound == null) {
                return;
            }
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            ResourceLocation soundId = ResourceLocation.tryParse(sound.toString());
            if (soundId == null) {
                return;
            }
            Vector3 position = new Vector3();
            emitter.getActiveSpace().getWorldPosition(position);
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.containsKey(soundId)
                    ? BuiltInRegistries.SOUND_EVENT.get(soundId)
                    : SoundEvent.createVariableRangeEvent(soundId);
            level.playLocalSound(position.x, position.y, position.z, event,
                    SoundSource.AMBIENT, 1.0F, 1.0F, false);
        });
    }

    /**
     * {@code Scene.fetchTexture} 默认实现（D5）：Bedrock 纹理路径
     * （如 {@code textures/particle/flame_atlas}）映射 ResourceManager 内
     * {@code <ns>:<path>.png}；minecraft 命名空间优先，找不到返回 null
     * （Config 回退内置纹理/missing 占位图，as-is 行为）。
     */
    private static @Nullable Object fetchTexture(Config config) {
        String path = config.particle_texture_path;
        if (path.isEmpty()) {
            return null;
        }
        String png = path.endsWith(".png") ? path : path + ".png";
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        ResourceLocation vanilla = ResourceLocation.tryBuild("minecraft", png);
        if (vanilla != null && manager.getResource(vanilla).isPresent()) {
            return vanilla.toString();
        }
        for (String namespace : manager.getNamespaces()) {
            ResourceLocation candidate = ResourceLocation.tryBuild(namespace, png);
            if (candidate != null && manager.getResource(candidate).isPresent()) {
                return candidate.toString();
            }
        }
        // 附加包纹理（BedrockAddonAutoLoader 选中包 / 预览直扫桥接）：TextureManagerMixin 按 path 命中
        if (io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry.get(png) != null) {
            return "minecraft:" + png;
        }
        return null;
    }

    /** 由渲染器在渲染前回填相机替身（位置 + MC 相机四元数转 three 约定）。 */
    static void updateCameraProxy(Vec3 cameraPos, org.joml.Quaternionf mcRotation) {
        cameraProxy.position.set(cameraPos.x, cameraPos.y, cameraPos.z);
        // MC 相机恒等旋转看向 +Z（南），three 相机默认看向 -Z：q_proxy = q_mc * rotY(π)
        org.joml.Quaternionf proxy = new org.joml.Quaternionf(mcRotation)
                .mul(new org.joml.Quaternionf().rotateY((float) Math.PI));
        cameraProxy.quaternion.set(proxy.x, proxy.y, proxy.z, proxy.w);
    }
}
