package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.registry.AttachableAssetRegistry;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.loader.LoaderParsingOps;
import io.github.tt432.eyelibutil.search.Searchable;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

@Slf4j
@ResourceLoader

/** @author TT432 */
@NullMarked
public class BrAttachableLoader extends BrResourcesLoader implements Searchable<BrClientEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrAttachableLoader.class);

    BrAttachableLoader() {
        super("attachables", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, BrClientEntity> parsedAttachables = LoaderParsingOps.parseAndTranslate(
                object,
                BrClientEntity.ATTACHABLE_CODEC,
                (sourceLocation, entity) -> new ResourceLocation(entity.identifier()),
                LOGGER,
                "entity"
        );
        AttachableAssetRegistry.replaceAttachables(parsedAttachables.values());
    }

    @Override
    public Stream<Map.Entry<String, BrClientEntity>> search(String searchStr) {
        return AttachableManager.readPort().getAllData().entrySet().stream()
                .filter(entry -> StringUtils.contains(entry.getKey(), searchStr))
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()));
    }
}
