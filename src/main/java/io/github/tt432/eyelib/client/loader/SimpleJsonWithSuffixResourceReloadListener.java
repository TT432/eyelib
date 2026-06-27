package io.github.tt432.eyelib.client.loader;

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
import java.util.Map;

/**
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