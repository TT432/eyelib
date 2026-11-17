package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.molang.MolangScope;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 一次模型渲染的宿主（G1）。
 * <p>
 * 为什么需要：渲染层过去只认 {@code Entity}，于是「方块锚定的作物/核心」「服务端 NPC 预测渲染」
 * 只能伪造本地玩家当宿主（{@code query.*} 语义被污染、必须手工 {@code applyEntityPose(false)}），
 * 或者干脆漏传宿主而**静默不渲染**。宿主抽象把「谁在渲染」与「以什么姿态/变量渲染」分开：
 * <ul>
 *   <li>{@link #owner()} 是 {@link RenderData} 的 owner 身份（{@code query.*} 的宿主）；</li>
 *   <li>{@link #entity()} 只表示「有没有实体可供实体专有渲染使用」（挂载物/栓绳/搬运方块/tint）；</li>
 *   <li>{@link #applyRootPose} 是模型根位姿的**唯一**实现（脚本缩放/幼体缩放/睡眠位移/身体朝向）。</li>
 * </ul>
 * 非实体宿主的 {@code query.*} 一律为缺席值（0/false/""），**不兜底任何实体**——兜底会让
 * 作物模型随玩家状态抖动，是比「缺席」更难排查的缺陷。
 * <p>
 * 三种宿主是封闭集：实体宿主、锚定宿主（无实体）、自定义作用域宿主（owner 与 entity 分离）。
 *
 * @author TT432
 */
public sealed interface RenderHost permits RenderHost.EntityHost, RenderHost.AnchorHost, RenderHost.ScopedHost {

    /** 宿主身份；{@code RenderData.owner} 必须是它（owner 是宿主身份，不是另一种宿主）。 */
    Object owner();

    /** 可供实体专有渲染使用的目标实体；非实体宿主为 {@code null}。 */
    @Nullable
    Entity entity();

    /**
     * 绑定 owner：**仅在 {@code cap.getOwner() == null} 时**初始化。
     * <p>
     * 绝不覆盖已有 owner：{@code RenderData.init} 会重建 scope（丢弃已注入的变量与宿主上下文），
     * 借用实体画挂载物的调用方若触发覆盖，会把数据自身的作用域（作物生长阶段、饰品状态）冲掉。
     * 需要迁移 owner 的调用方必须显式 {@code cap.init(newOwner)}。
     */
    default void bindOwner(RenderData<?> cap) {
        if (cap.getOwner() == null) {
            cap.ensureOwner(owner());
        }
    }

    /** 逐帧 tick 期变量（与渲染管线的 TickStage 同语义）；无实体宿主不写实体相关变量。 */
    void applyTickVariables(MolangScope scope, float partialTick);

    /** 提交期变量 = {@link #applyTickVariables} + 宿主引擎变量绑定。 */
    void applySubmitVariables(MolangScope scope, float partialTick);

    /** 同步行为状态（variant / mark_variant / scale）；无实体宿主为 no-op（并清掉残留）。 */
    void applyBehaviorContext(MolangScope scope);

    /**
     * 在 {@code poseStack} 当前顶部施加模型根位姿。
     *
     * @param applyEntityPose false 时只施加客户端实体脚本缩放（{@code variable.scale}），
     *                        不施加幼体缩放与身体朝向/睡眠位移
     */
    void applyRootPose(PoseStack poseStack, RenderData<?> cap, float partialTick, boolean applyEntityPose);

    /** 调用方未显式指定光照时使用的默认值。 */
    default int defaultPackedLight() {
        return EntityRenderPorts.RenderSystemPort.FULL_BRIGHT;
    }

    /** 调用方未显式指定 overlay 时使用的默认值。 */
    default int defaultOverlay() {
        return OverlayTexture.NO_OVERLAY;
    }

    static EntityHost of(Entity entity) {
        return new EntityHost(entity);
    }

    /**
     * 无实体的锚定宿主（方块/坐标）。
     *
     * @param packedLight 该锚点的打包光照；调用方负责按位置求值（不同版本的光照查询 API 不同，
     *                    本层刻意不收 {@code LevelReader} 以免引入版本分叉）
     */
    static AnchorHost anchor(Object owner, int packedLight) {
        return new AnchorHost(owner, packedLight);
    }

    static AnchorHost anchor(Object owner) {
        return new AnchorHost(owner, EntityRenderPorts.RenderSystemPort.FULL_BRIGHT);
    }

    /** 自定义作用域宿主：{@code RenderData} 归数据所有，但需要借用实体画挂载物。 */
    static ScopedHost scoped(Object owner, @Nullable Entity entity, RenderScopeVariables variables) {
        return new ScopedHost(owner, entity, variables);
    }

    /** 实体宿主：owner == entity。 */
    record EntityHost(Entity entity) implements RenderHost {
        public EntityHost {
            Objects.requireNonNull(entity, "entity");
        }

        @Override
        public Object owner() {
            return entity;
        }

        @Override
        public void applyTickVariables(MolangScope scope, float partialTick) {
            ClientEntitySetup.applyTickVariables(entity, scope, partialTick);
        }

        @Override
        public void applySubmitVariables(MolangScope scope, float partialTick) {
            ClientEntitySetup.applySubmitVariables(entity, scope, partialTick);
        }

        @Override
        public void applyBehaviorContext(MolangScope scope) {
            ClientEntitySetup.applyBehaviorContext(entity, scope);
        }

        @Override
        public void applyRootPose(PoseStack poseStack, RenderData<?> cap, float partialTick, boolean applyEntityPose) {
            ClientEntitySetup.applyRootPose(poseStack, cap,
                    applyEntityPose && entity instanceof LivingEntity living ? living : null, partialTick);
        }
    }

    /** 方块/坐标锚定宿主：无实体，{@code query.*} 全部缺席。 */
    record AnchorHost(Object owner, int packedLight) implements RenderHost {
        public AnchorHost {
            Objects.requireNonNull(owner, "owner");
        }

        @Override
        public @Nullable Entity entity() {
            return null;
        }

        @Override
        public void applyTickVariables(MolangScope scope, float partialTick) {
            ClientEntitySetup.applyTickVariables(null, scope, partialTick);
        }

        @Override
        public void applySubmitVariables(MolangScope scope, float partialTick) {
            ClientEntitySetup.applySubmitVariables(null, scope, partialTick);
        }

        @Override
        public void applyBehaviorContext(MolangScope scope) {
            ClientEntitySetup.applyBehaviorContext(null, scope);
        }

        @Override
        public void applyRootPose(PoseStack poseStack, RenderData<?> cap, float partialTick, boolean applyEntityPose) {
            ClientEntitySetup.applyRootPose(poseStack, cap, null, partialTick);
        }

        @Override
        public int defaultPackedLight() {
            return packedLight;
        }
    }

    /** 自定义作用域宿主：owner 与 entity 分离，可追加 {@code variable.*} 注入。 */
    record ScopedHost(Object owner, @Nullable Entity entity, RenderScopeVariables variables) implements RenderHost {
        public ScopedHost {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(variables, "variables");
        }

        @Override
        public void applyTickVariables(MolangScope scope, float partialTick) {
            ClientEntitySetup.applyTickVariables(entity, scope, partialTick);
            variables.apply(scope, partialTick);
        }

        @Override
        public void applySubmitVariables(MolangScope scope, float partialTick) {
            ClientEntitySetup.applySubmitVariables(entity, scope, partialTick);
            variables.apply(scope, partialTick);
        }

        @Override
        public void applyBehaviorContext(MolangScope scope) {
            ClientEntitySetup.applyBehaviorContext(entity, scope);
        }

        @Override
        public void applyRootPose(PoseStack poseStack, RenderData<?> cap, float partialTick, boolean applyEntityPose) {
            ClientEntitySetup.applyRootPose(poseStack, cap,
                    applyEntityPose && entity instanceof LivingEntity living ? living : null, partialTick);
        }
    }
}
