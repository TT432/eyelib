package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.util.search.Searchable;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;


/**
 * 加载 attachables 目录下的附着物 JSON 文件。
 *
 * @author TT432
 */
@Slf4j
@ResourceLoader
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
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        parsedAttachables.values().forEach(attachable -> flattened.put(attachable.identifier(), attachable));
        AttachableManager.INSTANCE.replaceAll(flattened);
    }

    @Override
    public Stream<Map.Entry<String, BrClientEntity>> search(String searchStr) {
        return AttachableManager.INSTANCE.getAllData().entrySet().stream()
                                .filter(entry -> StringUtils.contains(entry.getKey(), searchStr))
                                .map(entry -> Map.entry(entry.getKey(), entry.getValue()));
    }
}
