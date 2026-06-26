package io.github.tt432.eyelib.bridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * 渲染编排 Port，由 application 层 EntityRenderOrchestrator 注册实现。
 * bridge EntityRenderSystem 订阅 Forge 事件、翻译版本差异参数后通过这些 Port 回调编排逻辑，
 * 使 bridge 只保留 MC 翻译职责，编排决策归 application。
 *
 * @author TT432
 */
public final class EntityRenderPorts {
    private EntityRenderPorts() {
    }

    @FunctionalInterface
    public interface RenderStagePort {
        void onRenderStage(float partialTick, double camX, double camY, double camZ);
    }

    @FunctionalInterface
    public interface RenderBufferPort {
        void renderEntities(float partialTick, double camX, double camY, double camZ,
                            PoseStack poseStack, MultiBufferSource.BufferSource bufferSource);
    }

    @FunctionalInterface
    public interface RenderEntityPort {
        boolean render(RenderEntityParams params);
    }

    @FunctionalInterface
    public interface SetupClientEntityPort {
        List<Runnable> setup(Entity entity);
    }

    public static RenderStagePort renderStagePort = (p, x, y, z) -> {};
    public static RenderBufferPort renderBufferPort = (p, x, y, z, ps, bs) -> {};
    public static RenderEntityPort renderEntityPort = params -> false;
    public static SetupClientEntityPort setupClientEntityPort = e -> List.of();
}
