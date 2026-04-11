package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.registry.ClientEntityAssetRegistry;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public class BrClientEntityLoader extends BrResourcesLoader {
    public static final BrClientEntityLoader INSTANCE = new BrClientEntityLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrClientEntityLoader.class);

    private BrClientEntityLoader() {
        super("entity", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, BrClientEntity> parsedEntities = LoaderParsingOps.parseAndTranslate(
                object,
                BrClientEntity.CODEC,
                (sourceLocation, entity) -> new ResourceLocation(entity.identifier()),
                LOGGER,
                "entity"
        );

        ClientEntityAssetRegistry.replaceClientEntities(parsedEntities.values());
    }
}
