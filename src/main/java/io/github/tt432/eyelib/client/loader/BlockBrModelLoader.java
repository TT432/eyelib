package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.client.model.UnBakedBrModel;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(value = Dist.CLIENT,bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockBrModelLoader implements IGeometryLoader<UnBakedBrModel>, ResourceManagerReloadListener {
    @Getter
    private static BlockBrModelLoader instance;

    @SubscribeEvent
    public static void onEvent(ModelEvent.RegisterGeometryLoaders event) {
        instance = new BlockBrModelLoader();
        event.register("bedrock", instance);
    }

    ResourceManager resourceManager;

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.resourceManager = pResourceManager;
    }

    @Override
    public UnBakedBrModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new UnBakedBrModel( BrModel.parse("$dummy", jsonObject));
    }
}
