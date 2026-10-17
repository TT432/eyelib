package io.github.tt432.eyelib.client;

import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.client.gui.GuiHookPort;
import io.github.tt432.eyelib.bridge.ui.UiPort;
import io.github.tt432.eyelib.capability.AttachableDataTypes;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ClientAssetPort;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.debug.client.gui.ModelPreviewScreen;
import io.github.tt432.eyelib.client.gui.manager.AnimationView;
import io.github.tt432.eyelib.client.gui.manager.EyelibManagerScreen;
import io.github.tt432.eyelib.client.particle.MinecraftParticleRuntimeEnvironment;
import io.github.tt432.eyelib.client.render.EntityRenderOrchestrator;
import io.github.tt432.eyelib.particle.ParticleSpawnRuntimeAdapter;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.Map;
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
        // capability 侧的资产查询（模型 / 材质表）由 client 注册表提供，避免 capability → client 的包级循环
        ClientAssetPort.install(new ClientAssetPort() {
            @Override
            public @Nullable Model model(String id) {
                return ModelManager.INSTANCE.get(id);
            }

            @Override
            public Map<String, BrMaterialEntry> materials() {
                return MaterialManager.INSTANCE.all();
            }
        });
        GuiHookPort.setModelPreviewScreenHook(ModelPreviewScreen::new);
        GuiHookPort.setAnimationViewHook(AnimationView::new);
        GuiHookPort.setNodegraphEditorHook(() -> io.github.tt432.eyelib.client.nodegraph.NodegraphGate.openEditor(null));
        // eproject 项目库启动加载（规格 nodegraph-eproject-variables §2.3）
        io.github.tt432.eyelib.client.nodegraph.EprojectService.loadAll();
        // config/emolang 自定义函数启动加载（编辑器打开时还会重扫）
        io.github.tt432.eyelib.client.nodegraph.EmolangLoader.reloadAll();
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



