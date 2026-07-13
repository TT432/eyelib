package io.github.tt432.eyelib.bridge.client.adapter;

import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
//? if <26.1 {
import net.minecraft.client.renderer.LightTexture;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
//? if <26.1 {
import net.minecraft.world.entity.animal.horse.Llama;
//?} else {
import net.minecraft.world.entity.animal.equine.Llama;
//?}
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 渲染编排 Port 接口容器，由 application 层 EntityRenderOrchestrator 注册实现。
 * bridge adapter 通过 {@code RenderPorts} 实例持有这些 Port 的运行时实现。
 *
 * @author TT432
 */
public final class EntityRenderPorts {
    private EntityRenderPorts() {}

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

    /**
     * MC 渲染 helper 的 Port 接口，屏蔽版本差异与 mixin accessor 细节。
     * bridge adapter 层提供实现，application 层通过 {@code RenderPorts} 实例调用。
     *
     * @author TT432
     */
    public interface RenderSystemPort {
        String getEntityTypeId(Entity entity);

        int getLlamaDecorColorIndex(Llama llama);

        void pushPoseRaw(PoseStack poseStack, PoseStack.Pose pose);

        void renderItemDirect(LivingEntity le, ItemStack item, ItemDisplayContext context,
                              boolean left, PoseStack poseStack, RenderSink sink, int light);

        /**
         * 将 MC {@link EquipmentSlot} 翻译为 Bedrock {@code context.item_slot} 字符串。
         * 1.21+ 新增的 {@code BODY} 槽（狼马身体盔甲）与 26.1+ 新增的 {@code SADDLE} 槽
         * 由 adapter 按 Stonecutter 版本切分处理，application 层版本无关。
         */
        String slotName(EquipmentSlot slot);

        //? if <26.1
        int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
        //? if >=26.1
        int FULL_BRIGHT = 0xF000F0;

        float @Nullable [] getEntityTintColor(@Nullable Entity entity);

        PoseStack createPoseStackFromMatrix(org.joml.Matrix4f matrix);

        void setupLlamaDecor(Entity entity, io.github.tt432.eyelib.molang.MolangScope scope);
    }
}


