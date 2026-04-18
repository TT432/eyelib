package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.util.search.Searchable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author TT432
 */
@Slf4j
@Getter
public class BrAttachableLoader extends BrResourcesLoader implements Searchable<BrClientEntity> {
    public static final BrAttachableLoader INSTANCE = new BrAttachableLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrAttachableLoader.class);

    private final Map<ResourceLocation, BrClientEntity> attachables = new HashMap<>();

    @Nullable
    public BrClientEntity get(ResourceLocation id) {
        return attachables.get(id);
    }

    private BrAttachableLoader() {
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
        attachables.clear();
        attachables.putAll(parsedAttachables);
    }

    @Override
    public Stream<Map.Entry<String, BrClientEntity>> search(String searchStr) {
        return attachables.entrySet().stream()
                .filter(entry -> StringUtils.contains(entry.getKey().toString(), searchStr))
                .map(entry -> Map.entry(entry.getKey().toString(), entry.getValue()));
    }
}

