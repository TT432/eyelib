package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import lombok.extern.slf4j.Slf4j;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
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
    //? if <26.1 {
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
    //?} else {
    protected void apply(Map<Identifier, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
    //?}
        //? if <26.1 {
        Map<ResourceLocation, BrClientEntity> parsedEntities = LoaderParsingOps.parseAndTranslate(
        //?} else {
        Map<Identifier, BrClientEntity> parsedEntities = LoaderParsingOps.parseAndTranslate(
        //?}
                object,
                BrClientEntity.CODEC,
                //? if <1.20.6 {
                (sourceLocation, entity) -> new ResourceLocation(entity.identifier()),
                //?} elif <26.1 {
                (sourceLocation, entity) -> ResourceLocation.parse(entity.identifier()),
                //?} else {
                (sourceLocation, entity) -> Identifier.parse(entity.identifier()),

                //?}
                LOGGER,
                "entity"
        );

        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        parsedEntities.values().forEach(entity -> flattened.put(entity.identifier(), entity));
        ClientEntityManager.INSTANCE.replaceAll(flattened);
    }
}
