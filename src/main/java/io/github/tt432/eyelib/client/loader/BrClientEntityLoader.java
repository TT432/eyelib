package io.github.tt432.eyelib.client.loader;

import io.github.tt432.eyelib.bridge.client.loader.ResourceLoader;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * 加载 entity 目录下的客户端实体定义 JSON 文件。
 *
 * @author TT432
 */
@Slf4j
@ResourceLoader
public class BrClientEntityLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrClientEntityLoader.class);

    BrClientEntityLoader() {
        super("entity", "json");
    }

    @Override
    protected void applyJson(Map<String, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, BrClientEntity> parsedEntities = LoaderParsingOps.parseAndTranslate(
                object,
                BrClientEntity.CODEC,
                (sourceLocation, entity) -> entity.identifier(),
                LOGGER,
                "entity"
        );

        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        parsedEntities.values().forEach(entity -> flattened.put(entity.identifier(), entity));
        ClientEntityManager.INSTANCE.replaceAll(flattened);
    }
}
