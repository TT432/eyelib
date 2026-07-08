package io.github.tt432.eyelib.bridge.client.loader;

import io.github.tt432.eyelib.bridge.ApplicationLifecyclePort;
import io.github.tt432.eyelib.bridge.client.render.texture.adapter.NativeImageIO;
import io.github.tt432.eyelib.bridge.event.adapter.TextureChangedEvent;
import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.importer.addon.BedrockAddonLoader;
import io.github.tt432.eyelib.importer.addon.BedrockAddonWarning;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.particle.loading.ParticleResourcePublication;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
//? if <26.1 {
import net.minecraft.server.packs.resources.ResourceManager;
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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 扫描 resourcepacks/ 目录自动加载资源包侧 Bedrock 附加包资产。
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
        return CompletableFuture.supplyAsync(this::loadAllAddons, backgroundExecutor)
                                .thenCompose(barrier::wait)
                                .thenAcceptAsync(addons -> {
                                    for (BedrockAddon addon : addons) {
                                        bridgeAndPublish(addon);
                                    }
                                }, gameExecutor);
    }
    //?} else {
    @Override
    public CompletableFuture<Void> reload(
            PreparableReloadListener.SharedState currentReload,
            Executor taskExecutor,
            PreparableReloadListener.PreparationBarrier preparationBarrier,
            Executor reloadExecutor) {
        return CompletableFuture.supplyAsync(this::loadAllAddons, reloadExecutor)
                                .thenCompose(preparationBarrier::wait)
                                .thenAcceptAsync(addons -> {
                                    for (BedrockAddon addon : addons) {
                                        bridgeAndPublish(addon);
                                    }
                                }, Minecraft.getInstance());
    }
    //?}

    private List<BedrockAddon> loadAllAddons() {
        var addons = new ArrayList<BedrockAddon>();
        Path resourcepacksDir = Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks");
        if (!Files.isDirectory(resourcepacksDir)) {
            return addons;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(resourcepacksDir, this::isAddonFormat)) {
            for (Path addonFile : stream) {
                BedrockAddon addon = loadOne(addonFile);
                if (addon != null) {
                    addons.add(addon);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan resourcepacks/ for addon packs", e);
        }
        return addons;
    }

    private boolean isAddonFormat(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return Files.isRegularFile(path) && (name.endsWith(".mcpack") || name.endsWith(".mcaddon"));
    }

    @Nullable
    private BedrockAddon loadOne(Path addonFile) {
        LOGGER.info("Loading Bedrock addon from resourcepacks/: {}", addonFile.getFileName());
        try {
            BedrockAddon addon = BedrockAddonLoader.load(addonFile);
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
                addon.aggregate().resourcePack().particleFiles(), LOGGER);
        uploadAddonTextures(addon.aggregate().textures());
    }

    private void uploadAddonTextures(Map<String, ImportedImageData> textures) {
        if (textures.isEmpty()) {
            return;
        }
        textures.forEach((relativePath, imageData) -> {
            try {
                NativeImageIO.upload(relativePath.toLowerCase(Locale.ROOT),
                                     NativeImageIO.fromImportedImageData(imageData));
            } catch (RuntimeException e) {
                LOGGER.error("Failed to upload addon texture: {}", relativePath, e);
            }
        });
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


