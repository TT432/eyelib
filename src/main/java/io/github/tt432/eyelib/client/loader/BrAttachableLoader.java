package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.entity.BrClientEntityScripts;
import io.github.tt432.eyelib.util.search.Searchable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author TT432
 */
@Slf4j
@Getter
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class BrAttachableLoader extends BrResourcesLoader implements Searchable<BrClientEntity> {
    public static final BrAttachableLoader INSTANCE = new BrAttachableLoader();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrClientEntity> attachables = new HashMap<>();

    public BrClientEntity get(ResourceLocation id) {
        return attachables.get(id);
    }

    private BrAttachableLoader() {
        super("attachables", "json");
    }

    public static final Codec<BrClientEntity> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            RecordCodecBuilder.<BrClientEntity>create(ins1 -> ins1.group(
                    RecordCodecBuilder.<BrClientEntity>create(ins2 -> ins2.group(
                            Codec.STRING.fieldOf("identifier").forGetter(BrClientEntity::identifier),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("materials", Map.of()).forGetter(BrClientEntity::materials),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING.xmap(s -> s + ".png", s -> s.substring(0, s.length() - ".png".length()))).optionalFieldOf("textures", Map.of()).forGetter(BrClientEntity::textures),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("geometry", Map.of()).forGetter(BrClientEntity::geometry),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("animations", Map.of()).forGetter(BrClientEntity::animations),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("particle_effects", Map.of()).forGetter(BrClientEntity::particle_effects),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("sound_effects", Map.of()).forGetter(BrClientEntity::sound_effects),
                            Codec.STRING.listOf().optionalFieldOf("render_controllers", java.util.List.of()).forGetter(BrClientEntity::render_controllers),
                            BrClientEntityScripts.CODEC.optionalFieldOf("scripts").forGetter(BrClientEntity::scripts)
                    ).apply(ins2, BrClientEntity::new)).fieldOf("description").forGetter(o -> o)
            ).apply(ins1, o -> o)).fieldOf("minecraft:attachable").forGetter(o -> o)
    ).apply(ins, o -> o));

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        attachables.clear();

        for (var entry : object.entrySet()) {
            ResourceLocation key = entry.getKey();

            try {
                BrClientEntity entity = CODEC.parse(JsonOps.INSTANCE, entry.getValue().getAsJsonObject()).getOrThrow();
                attachables.put(ResourceLocation.parse(entity.identifier()), entity);
            } catch (Exception e) {
                log.error("can't load entity {}", key, e);
            }
        }
    }

    @Override
    public Stream<Map.Entry<String, BrClientEntity>> search(String searchStr) {
        return attachables.entrySet().stream()
                .filter(entry -> StringUtils.contains(entry.getKey().toString(), searchStr))
                .map(entry -> Map.entry(entry.getKey().toString(), entry.getValue()));
    }
}
