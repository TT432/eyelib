package io.github.tt432.eyelib.bridge.client.sound.adapter;
//? if <26.1 {

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.sounds.SoundEvent;
//? if <1.20.6 {
import net.minecraftforge.event.AddPackFindersEvent;
//?} else {
import net.neoforged.neoforge.event.AddPackFindersEvent;
//?}

/**
 * addon 音效播放桥：注册 {@link AddonSoundPack}（required 客户端资源包，mod 总线
 * AddPackFindersEvent），addon 替换后局部重建 SoundManager（{@link #triggerSoundReload}），
 * 预览播放走 variable-range SoundEvent（无需 SoundEvent 注册表条目）。
 *
 * @author TT432
 */
public final class AddonSoundBridge {
    private AddonSoundBridge() {
    }

    /** AddPackFindersEvent 监听：把 addon 音效包挂进客户端资源扫描。 */
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        //? if <1.20.6 {
        event.addRepositorySource(consumer -> consumer.accept(Pack.readMetaAndCreate(
                AddonSoundPack.PACK_ID,
                Component.literal("eyelib addon sounds"),
                true,
                id -> new AddonSoundPack(),
                PackType.CLIENT_RESOURCES,
                Pack.Position.TOP,
                PackSource.DEFAULT)));
        //?} else {
        // 1.21 的 ResourcesSupplier 双抽象方法（openPrimary/openFull），不能写 lambda
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(
                    net.minecraft.server.packs.PackLocationInfo info) {
                return new AddonSoundPack();
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(
                    net.minecraft.server.packs.PackLocationInfo info,
                    Pack.Metadata metadata) {
                return new AddonSoundPack();
            }
        };
        event.addRepositorySource(consumer -> consumer.accept(Pack.readMetaAndCreate(
                new net.minecraft.server.packs.PackLocationInfo(
                        AddonSoundPack.PACK_ID,
                        Component.literal("eyelib addon sounds"),
                        PackSource.DEFAULT,
                        java.util.Optional.empty()),
                resources,
                PackType.CLIENT_RESOURCES,
                new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, true))));
        //?}
    }

    /**
     * 预览播放音效 id（bedrock 音效表值）。事件 RL = eyelibaddon 命名空间 +
     * {@link AddonSoundPack#toEventKey} 映射（"ns:path" → "ns/path"，与合成同口径）。
     * 未定义/无 .ogg 的 id 在此已被 MISSING_REFERENCE 通道标红，这里静默忽略。
     */
    public static void playPreview(String soundId) {
        ResourceLocation location = new ResourceLocation(
                AddonSoundPack.NAMESPACE, AddonSoundPack.toEventKey(soundId));
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(location), 1.0F));
    }

    /**
     * addon 资产替换后调用：重建音效注册表使新合成的 sounds.json 生效。
     * 注意 SoundManager 无参 reload() 只重启 SoundEngine（1.20.1 字节码实证）——
     * 重读 sounds.json 必须走监听式 reload（prepare + apply）。
     */
    public static void triggerSoundReload() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return; // 单测/headless 环境无客户端实例
        }
        minecraft.execute(() -> minecraft.getSoundManager().reload(
                NOP_BARRIER, minecraft.getResourceManager(),
                NOP_PROFILER, NOP_PROFILER, Runnable::run, Runnable::run));
    }

    private static final net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier NOP_BARRIER =
            new net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier() {
                @Override
                public <T> java.util.concurrent.CompletableFuture<T> wait(T value) {
                    return java.util.concurrent.CompletableFuture.completedFuture(value);
                }
            };

    private static final net.minecraft.util.profiling.ProfilerFiller NOP_PROFILER =
            new net.minecraft.util.profiling.ProfilerFiller() {
                @Override public void startTick() { }
                @Override public void endTick() { }
                @Override public void push(String name) { }
                @Override public void push(java.util.function.Supplier<String> name) { }
                @Override public void pop() { }
                @Override public void popPush(String name) { }
                @Override public void popPush(java.util.function.Supplier<String> name) { }
                @Override public void markForCharting(net.minecraft.util.profiling.metrics.MetricCategory category) { }
                @Override public void incrementCounter(String name, int amount) { }
                @Override public void incrementCounter(java.util.function.Supplier<String> name, int amount) { }
            };
}
//?}
