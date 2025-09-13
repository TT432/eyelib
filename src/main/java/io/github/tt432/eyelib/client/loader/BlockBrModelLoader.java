package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.model.UnBakedBrModel;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.util.ResourceLocations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(value = Dist.CLIENT)
public class BlockBrModelLoader implements IGeometryLoader<UnBakedBrModel> {
    @SubscribeEvent
    public static void onEvent(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocations.of(Eyelib.MOD_ID, "bedrock_model"), new BlockBrModelLoader());
    }

    @Override
    public @NotNull UnBakedBrModel read(@NotNull JsonObject jsonObject, @NotNull JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new UnBakedBrModel(BrModel.parse(jsonObject).models().get(0));
    }
}
