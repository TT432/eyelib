package io.github.tt432.eyelib.bridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * 渲染编排 Port 接口容器，由 application 层 EntityRenderOrchestrator 注册实现。
 * bridge adapter 通过 {@code RenderPorts} 实例持有这些 Port 的运行时实现。
 *
 * @author TT432
 */
public final class EntityRenderPorts {
    private EntityRenderPorts() {
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
}
