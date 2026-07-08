package io.github.tt432.eyelib.bridge.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资源重载监听器的版本适配基类（ACL）。
 *
 * <p>直接接触 MC 的 {@link SimplePreparableReloadListener} API 与
 * {@code ResourceLocation}/{@code Identifier}（26.1 重命名）类型，Stonecutter 条件化在此合法
 * （bridge 即版本差异的栖息地）。子类（application 层）实现稳定的
 * {@link #applyJson(Map, ResourceManager, ProfilerFiller)}，键统一为字符串
 * （{@code ResourceLocation#toString()} → {@code "namespace:path"}），无需任何条件化注释。
 *
 * @author TT432
 */
//? if <26.1 {
public abstract class SimpleJsonWithSuffixResourceReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
//?} else {
public abstract class SimpleJsonWithSuffixResourceReloadListener extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
//?}
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Gson gson;
    private final String directory;
    private final String suffix;

    public SimpleJsonWithSuffixResourceReloadListener(Gson gson, String directory, String suffix) {
        this.gson = gson;
        this.directory = directory;
        this.suffix = suffix;
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    //? if <26.1 {
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
    //?} else {
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
    //?}
        //? if <26.1 {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        //?} else {
        Map<Identifier, JsonElement> map = new HashMap<>();
        //?}
        scanDirectory(resourceManager, this.directory, this.suffix, this.gson, map);
        return map;
    }

    /**
     * MC 版本特定的 apply 入口：将 {@code ResourceLocation}/{@code Identifier} 键归一为字符串，
     * 委托给稳定的 {@link #applyJson}，使 application 子类与版本无关。
     */
    //? if <26.1 {
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
    //?} else {
    @Override
    protected void apply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
    //?}
        LinkedHashMap<String, JsonElement> stable = new LinkedHashMap<>();
        map.forEach((key, value) -> stable.put(key.toString(), value));
        applyJson(stable, resourceManager, profiler);
    }

    /**
     * 稳定的资源应用回调（application 子类实现）。键为 {@code "namespace:path"} 字符串。
     */
    protected abstract void applyJson(Map<String, JsonElement> source, ResourceManager resourceManager, ProfilerFiller profiler);

    //? if <26.1 {
    public static void scanDirectory(ResourceManager resourceManager, String name, String suffix, Gson gson, Map<ResourceLocation, JsonElement> output) {
    //?} else {
    public static void scanDirectory(ResourceManager resourceManager, String name, String suffix, Gson gson, Map<Identifier, JsonElement> output) {
    //?}
        FileToIdConverter filetoidconverter = new FileToIdConverter(name, "." + suffix);

        //? if <26.1 {
        for (Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(resourceManager)
        //?} else {
        for (Map.Entry<Identifier, Resource> entry : filetoidconverter.listMatchingResources(resourceManager)
        //?}
                                                                            .entrySet()) {
            //? if <26.1 {
            ResourceLocation resourcelocation = entry.getKey();
            //?} else {
            Identifier resourcelocation = entry.getKey();
            //?}
            //? if <26.1 {
            ResourceLocation resourcelocation1 = filetoidconverter.fileToId(resourcelocation);
            //?} else {
            Identifier resourcelocation1 = filetoidconverter.fileToId(resourcelocation);
            //?}

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement jsonelement = gson.fromJson(reader, JsonElement.class);
                JsonElement jsonelement1 = output.put(resourcelocation1, jsonelement);
                if (jsonelement1 != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourcelocation1);
                }
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                LOGGER.error("Couldn't parse data file {} from {}", resourcelocation1, resourcelocation, jsonparseexception);
            }
        }
    }

    //? if <26.1 {
    protected ResourceLocation getPreparedPath(ResourceLocation rl) {
    //?} else {
    protected Identifier getPreparedPath(Identifier rl) {
    //?}
        return rl.withPath(this.directory + "/" + rl.getPath() + ".json");
    }
}
