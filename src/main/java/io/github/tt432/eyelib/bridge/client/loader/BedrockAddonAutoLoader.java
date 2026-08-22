package io.github.tt432.eyelib.bridge.client.loader;

import io.github.tt432.eyelib.bridge.ApplicationLifecyclePort;
import io.github.tt432.eyelib.bridge.client.loader.adapter.BedrockPackResources;
import io.github.tt432.eyelib.bridge.client.render.texture.adapter.NativeImageIO;
import io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry;
import io.github.tt432.eyelib.bridge.event.adapter.TextureChangedEvent;
import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.importer.addon.BedrockAddonLoader;
import io.github.tt432.eyelib.importer.addon.BedrockAddonPack;
import io.github.tt432.eyelib.importer.addon.BedrockAddonWarning;
import io.github.tt432.eyelib.importer.addon.BedrockPackSetting;
import io.github.tt432.eyelib.importer.addon.BedrockPackSettingsService;
import io.github.tt432.eyelib.importer.addon.BedrockPackSettingsStore;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.particle.loading.ParticleResourcePublication;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
//? if <26.1 {
import net.minecraft.util.profiling.ProfilerFiller;
//?}
//? if <1.20.6 {
import net.minecraftforge.common.MinecraftForge;
//?} else {
import net.neoforged.neoforge.common.NeoForge;
//?}
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 资源重载时发布「原版包管理选中的」Bedrock 附加包资产。
 * <p>
 * 包发现/启用/排序归 {@code PackRepository}（{@link BedrockAddonPackFinder} 把
 * resourcepacks/ 下 .mcpack/.mcaddon 注册为可选资源包）；本监听器只从
 * {@code ResourceManager.listPacks()} 枚举选中的 {@link BedrockPackResources}，
 * 按优先级顺序合并后一次性桥接发布——未选中任何包时发布空视图以卸载旧内容。
 *
 * @author TT432
 */
final class BedrockAddonAutoLoader implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockAddonAutoLoader.class);

    //? if <26.1 {
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> loadSelectedAddons(resourceManager), backgroundExecutor)
                                .thenCompose(barrier::wait)
                                .thenAcceptAsync(this::bridgeAndPublish, gameExecutor);
    }
    //?} else {
    @Override
    public CompletableFuture<Void> reload(
            PreparableReloadListener.SharedState currentReload,
            Executor taskExecutor,
            PreparableReloadListener.PreparationBarrier preparationBarrier,
            Executor reloadExecutor) {
        return CompletableFuture.supplyAsync(() -> loadSelectedAddons(currentReload.resourceManager()), reloadExecutor)
                                .thenCompose(preparationBarrier::wait)
                                .thenAcceptAsync(this::bridgeAndPublish, Minecraft.getInstance());
    }
    //?}

    /**
     * 枚举原版包管理选中的 Bedrock 附加包（{@link BedrockPackResources} 实例，
     * {@code listPacks()} 顺序 = 优先级底→顶，靠后覆盖靠前），解析后按
     * {@link BedrockAddon#merge} 合并为单一视图。未选中任何包时返回全空 addon
     * （发布侧据此卸载上一轮内容）。
     */
    private BedrockAddon loadSelectedAddons(ResourceManager resourceManager) {
        BedrockPackSettingsStore.ensureInitialized(Minecraft.getInstance().gameDirectory.toPath());
        var addons = new ArrayList<BedrockAddon>();
        // 设置目录的包键 = 文件名（与 vanilla pack id "file/<name>"、store 键一致；
        // 不能用 BedrockAddonPack.sourceName——zip 加载剥了扩展名）
        var activeSettings = new ArrayList<BedrockPackSettingsService.ActivePack>();
        resourceManager.listPacks()
                .filter(BedrockPackResources.class::isInstance)
                .map(BedrockPackResources.class::cast)
                .forEach(pack -> {
                    BedrockAddon addon = loadOne(pack.sourceFile());
                    if (addon != null) {
                        addons.add(addon);
                        String packKey = pack.sourceFile().getFileName().toString();
                        for (BedrockAddonPack resourcePack : addon.resourcePacks()) {
                            List<BedrockPackSetting> settings =
                                    BedrockPackSetting.parseList(resourcePack.manifest().settings());
                            if (!settings.isEmpty()) {
                                activeSettings.add(new BedrockPackSettingsService.ActivePack(packKey, settings));
                            }
                        }
                    }
                });
        BedrockPackSettingsService.updateActivePacks(activeSettings);
        return BedrockAddon.merge(addons);
    }

    @Nullable
    private BedrockAddon loadOne(Path addonFile) {
        LOGGER.info("Loading Bedrock addon from resourcepacks/: {}", addonFile.getFileName());
        try {
            String packKey = addonFile.getFileName().toString();
            BedrockAddon addon = BedrockAddonLoader.load(
                    addonFile, BedrockPackSettingsStore.subpackOverride(packKey).orElse(null));
            logWarnings(addon);
            return addon;
        } catch (Exception e) {
            LOGGER.error("Failed to load Bedrock addon: {}", addonFile.getFileName(), e);
            return null;
        }
    }

    private void bridgeAndPublish(BedrockAddon addon) {
        ApplicationLifecyclePort port = ApplicationLifecyclePort.get();
        if (port != null) port.onAddonParsed(addon);
        ParticleResourcePublication.replaceFromSchemas(
                "bedrock-addon", addon.aggregate().resourcePack().particleFiles(), LOGGER);
        uploadAddonTextures(addon.aggregate().textures());
    }

    private void uploadAddonTextures(Map<String, ImportedImageData> textures) {
        // 先清后传：包被禁用/移除时陈旧纹理必须退场；合并视图外的键不再残留
        java.util.Set<String> previousKeys = AddonTextureRegistry.keys();
        AddonTextureRegistry.clear();
        // 注册到 AddonTextureRegistry，由 TextureManagerMixin 在 getTexture() 中按需创建 DynamicTexture。
        // .tga 路径自动归一化为 .png，使 MC 原版纹理加载机制能透明加载 .tga。
        textures.forEach((relativePath, imageData) -> {
            AddonTextureRegistry.put(relativePath.toLowerCase(Locale.ROOT), imageData);
        });
        // vanilla 重载对 DynamicTexture 是 no-op（DynamicTexture.load 空实现，1.20.1 反编译实证），
        // TextureManagerMixin 又在 byPath 命中时短路——必须主动驱逐 byPath 中的 addon 基图
        // （新旧键并集）与 clamped/_color_mask 派生纹理，下一次 getTexture 才按新数据重建。
        java.util.Set<String> stalePaths = new java.util.HashSet<>(previousKeys);
        stalePaths.addAll(AddonTextureRegistry.keys());
        NativeImageIO.clearColorMaskCache();
        NativeImageIO.evictTexturesMatching(path -> stalePaths.contains(path)
                || NativeImageIO.isEyelibDerivedTexturePath(path));
        //? if <1.20.6 {
        MinecraftForge.EVENT_BUS.post(new TextureChangedEvent());
        //?} else {
        NeoForge.EVENT_BUS.post(new TextureChangedEvent());
        //?}
    }

    private void logWarnings(BedrockAddon addon) {
        for (BedrockAddonWarning warning : addon.warnings()) {
            LOGGER.warn("[{}] {}: {}", warning.packSource(), warning.code(), warning.message());
        }
    }
}


