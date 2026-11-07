package io.github.tt432.eyelib.bridge.client.adapter;

import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.molang.EngineEntityVariable;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
//? if <26.1 {
import net.minecraft.client.renderer.LightTexture;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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

        /**
         * 解析宿主提供的 Bedrock 引擎变量（见 {@link EngineEntityVariable}）。
         *
         * @return 当前实体的变量值；该变量不适用于此实体时返回 {@link Float#NaN}
         *         （application 层据此决定是否绑定，未绑定 = 资源包读写自己的值）。
         */
        float engineVariable(Entity entity, EngineEntityVariable variable);

        void pushPoseRaw(PoseStack poseStack, PoseStack.Pose pose);

        void renderItemDirect(LivingEntity le, ItemStack item, ItemDisplayContext context,
                              boolean left, PoseStack poseStack, RenderSink sink, int light);

        /**
         * 实体当前搬运的方块状态（Bedrock {@code query.is_carrying_block} 的宿主来源，如末影人）。
         * 无搬运方块时返回 {@code null}。
         */
        @Nullable
        BlockState getCarriedBlockState(Entity entity);

        /**
         * 用宿主游戏的方块渲染器绘制一个方块状态。
         * 与实体模型使用同一 {@link PoseStack}，方块模型/贴图/渲染类型全部由宿主决定，
         * application 层不硬编码任何方块 id。
         */
        void renderBlockState(BlockState state, PoseStack poseStack, RenderSink sink, int light);

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
    }
}


