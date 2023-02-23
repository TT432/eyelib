package io.github.tt432.eyelib.common;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.util.FileToIdConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.stream.Stream;

/**
 * @author DustW
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AnimationTextureLoader extends TextureAtlasHolder {
    public static AnimationTextureLoader INSTANCE;
    FileToIdConverter converter = new FileToIdConverter("textures/animatable", ".png");

    private AnimationTextureLoader(TextureManager pTextureManager) {
        super(pTextureManager, new ResourceLocation(Eyelib.MOD_ID, "textures/atlas/eyelib.png"), "animatable");
    }

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        if (INSTANCE == null) {
            INSTANCE = new AnimationTextureLoader(Minecraft.getInstance().getTextureManager());
        }

        event.registerReloadListener(INSTANCE);
    }

    @Override
    protected Stream<ResourceLocation> getResourcesToLoad() {
        return converter.listMatchingResources(Minecraft.getInstance().getResourceManager())
                .stream()
                .map(converter::fileToId);
    }

    public TextureAtlasSprite get(ResourceLocation id) {
        ResourceLocation fixName = fixName(id);

        if (fixName != null)
            return getSprite(fixName);

        return getSprite(id);
    }

    ResourceLocation fixName(ResourceLocation id) {
        if (!isMissing(getSprite(id))) {
            return id;

        } else {
            String path = id.getPath();
            ResourceLocation sub = new ResourceLocation(id.getNamespace(),
                    path.substring(0, path.length() - /* .png */ 4));

            if (!isMissing(getSprite(sub))) {
                return sub;
            } else if (!isMissing(getSprite(converter.idToFile(id)))) {
                return converter.idToFile(id);
            }
        }

        return null;
    }

    public boolean has(ResourceLocation id) {
        return fixName(id) != null;
    }

    public boolean isMissing(TextureAtlasSprite sprite) {
        return sprite.getName().equals(MissingTextureAtlasSprite.getLocation());
    }
}
