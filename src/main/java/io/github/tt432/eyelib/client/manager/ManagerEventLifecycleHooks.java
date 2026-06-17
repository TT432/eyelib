package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.particle.MinecraftParticleRuntimeEnvironment;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.particle.client.ParticleSpawnRuntimeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/** @author TT432 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@NullMarked
public final class ManagerEventLifecycleHooks {
    private ManagerEventLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ManagerEventPublishBridge.install(new ForgeManagerEventPublisher());

        // 注入上下文 supplier，供 ParticleSpawnRuntimeAdapter.INSTANCE.spawn() 使用
        ParticleSpawnRuntimeAdapter.configure(
                () -> {
                    if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
                        return Optional.empty();
                    }
                    return Optional.of(new MinecraftParticleRuntimeEnvironment(Minecraft.getInstance().level));
                },
                () -> {
                    if (Minecraft.getInstance().player == null) {
                        return Optional.empty();
                    }
                    RenderData<?> data = DataAttachmentHelper.getOrCreate(
                            EyelibAttachableData.RENDER_DATA.get(), Minecraft.getInstance().player);
                    return Optional.ofNullable(data.getScope());
                }
        );
    }
}
