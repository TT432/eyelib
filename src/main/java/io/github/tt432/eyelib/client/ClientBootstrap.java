package io.github.tt432.eyelib.client;

import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.client.gui.GuiHookPort;
import io.github.tt432.eyelib.bridge.ui.UiPort;
import io.github.tt432.eyelib.capability.AttachableDataTypes;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.gui.ModelPreviewScreen;
import io.github.tt432.eyelib.client.gui.manager.AnimationView;
import io.github.tt432.eyelib.client.gui.manager.EyelibManagerScreen;
import io.github.tt432.eyelib.client.particle.MinecraftParticleRuntimeEnvironment;
import io.github.tt432.eyelib.client.render.EntityRenderOrchestrator;
import io.github.tt432.eyelib.particle.ParticleSpawnRuntimeAdapter;
import net.minecraft.client.Minecraft;

import java.util.Optional;

/**
 * 客户端 bootstrap，由 bridge 层反射加载以触发 application 层各类 Port 注册与适配器注入。
 *
 * @author TT432
 */
public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void wire() {
        EntityRenderOrchestrator.wirePorts();
        GuiHookPort.setModelPreviewScreenHook(ModelPreviewScreen::new);
        GuiHookPort.setAnimationViewHook(AnimationView::new);
        UiPort.register(EyelibManagerScreen::create);

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
                            AttachableDataTypes.RENDER_DATA.get(), Minecraft.getInstance().player);
                    return Optional.ofNullable(data.getScope());
                }
        );
    }
}



