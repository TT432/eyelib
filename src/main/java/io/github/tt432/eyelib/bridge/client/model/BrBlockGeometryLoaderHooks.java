package io.github.tt432.eyelib.bridge.client.model;

//? if <26.1 {
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
//?}

/**
 * 注册 {@link BrBlockGeometryLoader}（{@code eyelib:bedrock}）。
 * 26.1 渲染管线重写后未适配，该版本不注册。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class BrBlockGeometryLoaderHooks {
    private BrBlockGeometryLoaderHooks() {
    }

    //? if <1.20.6 {
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("bedrock", new BrBlockGeometryLoader());
    }
    //?} else {
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath("eyelib", "bedrock"), new BrBlockGeometryLoader());
    }
    //?}
}
//?}
