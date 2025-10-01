package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.client.model.UnBakedBrModel;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BlockBrModelLoader implements IGeometryLoader<UnBakedBrModel> {
    @SubscribeEvent
    public static void onEvent(ModelEvent.RegisterGeometryLoaders event) {
        event.register("bedrock_model", new BlockBrModelLoader());
    }

    @Override
    public @NotNull UnBakedBrModel read(@NotNull JsonObject jsonObject, @NotNull JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new UnBakedBrModel(BrModel.parse(jsonObject).models().get(0));
    }
}
