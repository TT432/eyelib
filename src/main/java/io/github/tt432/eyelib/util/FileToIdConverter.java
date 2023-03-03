package io.github.tt432.eyelib.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Collection;

/**
 * @author DustW
 */
public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String prefix, String extension) {
        this.prefix = prefix;
        this.extension = extension;
    }

    public static FileToIdConverter json(String p_248754_) {
        return new FileToIdConverter(p_248754_, ".json");
    }

    public static ResourceLocation withPath(ResourceLocation rl, String path) {
        return new ResourceLocation(rl.getNamespace(), path);
    }

    public ResourceLocation idToFile(ResourceLocation id) {
        return withPath(id, this.prefix + "/" + id.getPath() + this.extension);
    }

    public ResourceLocation fileToId(ResourceLocation file) {
        String s = file.getPath();
        return withPath(file, s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
    }

    public Collection<ResourceLocation> listMatchingResources(ResourceManager manager) {
        return manager.listResources(this.prefix, s -> s.endsWith(this.extension));
    }
}