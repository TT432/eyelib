package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.model.bedrock.UnBakedBrModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockBrModelLoader implements IGeometryLoader<UnBakedBrModel>, ResourceManagerReloadListener {
    @Getter
    private static BlockBrModelLoader instance;

    @SubscribeEvent
    public static void onEvent(ModelEvent.RegisterGeometryLoaders event) {
        instance = new BlockBrModelLoader();
        event.register(new ResourceLocation(Eyelib.MOD_ID, "bedrock_model"), instance);
    }

    ResourceManager resourceManager;

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.resourceManager = pResourceManager;
    }

    @Override
    public UnBakedBrModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new UnBakedBrModel(BrModel.parse("$dummy", jsonObject));
    }
}
