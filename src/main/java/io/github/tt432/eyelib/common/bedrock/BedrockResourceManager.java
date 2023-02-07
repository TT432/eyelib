package io.github.tt432.eyelib.common.bedrock;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Animation;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.common.bedrock.model.pojo.Converter;
import io.github.tt432.eyelib.common.bedrock.model.pojo.FormatVersion;
import io.github.tt432.eyelib.common.bedrock.model.pojo.RawGeoModel;
import io.github.tt432.eyelib.common.bedrock.model.tree.GeoBuilder;
import io.github.tt432.eyelib.common.bedrock.model.tree.RawGeometryTree;
import io.github.tt432.eyelib.util.JsonUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.util.Lazy;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BedrockResourceManager {
    private static final Lazy<BedrockResourceManager> INSTANCE = Lazy.of(BedrockResourceManager::new);

    public static BedrockResourceManager getInstance() {
        return INSTANCE.get();
    }

    private Map<ResourceLocation, Animation> animations = Collections.emptyMap();
    private Map<ResourceLocation, GeoModel> geoModels = Collections.emptyMap();

    public Map<ResourceLocation, Animation> getAnimations() {
        if (!Eyelib.hasInitialized)
            throw new RuntimeException("GeckoLib was never initialized! Please read the documentation!");

        return animations;
    }

    public Map<ResourceLocation, GeoModel> getGeoModels() {
        if (!Eyelib.hasInitialized)
            throw new RuntimeException("GeckoLib was never initialized! Please read the documentation!");

        return geoModels;
    }

    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        Map<ResourceLocation, Animation> animations = new Object2ObjectOpenHashMap<>();
        Map<ResourceLocation, GeoModel> geoModels = new Object2ObjectOpenHashMap<>();

        return CompletableFuture.allOf(
                        loadResources(backgroundExecutor, resourceManager, "geo/animations",
                                animation -> loadAnimation(animation, resourceManager), animations::put),
                        loadResources(backgroundExecutor, resourceManager, "geo/models",
                                resource -> loadModel(resourceManager, resource), geoModels::put))
                .thenCompose(stage::wait).thenAcceptAsync(empty -> {
                    this.animations = animations;
                    this.geoModels = geoModels;
                }, gameExecutor);
    }

    public GeoModel loadModel(ResourceManager resourceManager, ResourceLocation location) {
        try {
            // Deserialize from json into basic json objects, bones are still stored as a
            // flat list
            RawGeoModel rawModel = Converter
                    .fromJsonString(getResourceAsString(location, resourceManager));
            if (rawModel.getFormatVersion() != FormatVersion.VERSION_1_12_0) {
                throw new EyelibLoadingException(location, "Wrong geometry json version, expected 1.12.0");
            }

            // Parse the flat list of bones into a raw hierarchical tree of "BoneGroup"s
            RawGeometryTree rawGeometryTree = RawGeometryTree.parseHierarchy(rawModel);

            // Build the quads and cubes from the raw tree into a built and ready to be
            // rendered GeoModel
            return GeoBuilder.getGeoBuilder(location.getNamespace()).constructGeoModel(rawGeometryTree);
        } catch (Exception e) {
            Eyelib.LOGGER.error(String.format("Error parsing %S", location), e);
            throw (new RuntimeException(e));
        }
    }

    public Animation loadAnimation(ResourceLocation rl, ResourceManager manager) {
        return JsonUtils.normal.fromJson(getResourceAsString(rl, manager), Animation.class);
    }

    public static String getResourceAsString(ResourceLocation location, ResourceManager manager) {
        try (InputStream inputStream = manager.getResource(location).getInputStream()) {
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (Exception e) {
            String message = "Couldn't load " + location;
            Eyelib.LOGGER.error(message, e);
            throw new RuntimeException(new FileNotFoundException(location.toString()));
        }
    }

    private static <T> CompletableFuture<Void> loadResources(Executor executor, ResourceManager resourceManager,
                                                             String type, Function<ResourceLocation, T> loader,
                                                             BiConsumer<ResourceLocation, T> map) {
        return CompletableFuture
                .supplyAsync(() -> resourceManager.listResources(type, fileName -> fileName.endsWith(".json")),
                        executor)
                .thenApplyAsync(resources -> {
                    Map<ResourceLocation, CompletableFuture<T>> tasks = new Object2ObjectOpenHashMap<>();

                    for (ResourceLocation resource : resources) {
                        CompletableFuture<T> existing = tasks.put(resource,
                                CompletableFuture.supplyAsync(() -> loader.apply(resource), executor));
                        if (existing != null) {// Possibly if this matters, the last one will win
                            System.err.println("Duplicate resource for " + resource);
                            existing.cancel(false);
                        }
                    }

                    return tasks;
                }, executor).thenAcceptAsync(tasks -> {
                    for (Entry<ResourceLocation, CompletableFuture<T>> entry : tasks.entrySet()) {
                        map.accept(entry.getKey(), entry.getValue().join());
                    }
                }, executor);
    }
}
