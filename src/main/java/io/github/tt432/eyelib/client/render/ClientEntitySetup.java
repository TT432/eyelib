package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.capability.DataAttachmentPort;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.molang.ComponentStoreView;
import io.github.tt432.eyelib.bridge.molang.MolangContextPort;
import io.github.tt432.eyelib.bridge.molang.MolangEntityContextView;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体渲染 setup 装配簇（clientEntity 解析/组件重建/scope 上下文注入），
 * 从 {@link EntityRenderOrchestrator} 提取。
 *
 * @author TT432
 */
final class ClientEntitySetup {
    private static final HostRole<MolangEntityContextView> MOLANG_ENTITY_CONTEXT =
            HostRole.of("molang_entity_context", MolangEntityContextView.class);

    /** 渲染帧序号，由 {@link EntityRenderOrchestrator#onRenderStage} 每帧递增；用于同帧内 setup/tick 去重。 */
    private static long frameCounter = 0;

    private ClientEntitySetup() {
    }

    /** 进入新的一帧（渲染帧序号递增）。 */
    static void beginFrame() {
        frameCounter++;
    }

    /** 当前渲染帧序号。 */
    static long frame() {
        return frameCounter;
    }

    static List<Runnable> setup(Entity entity) {
        var cap = RenderData.getComponent(entity);
        cap.ensureOwner(entity);
        return setupClientEntity(entity, cap);
    }

    /**
     * 模型根位姿的唯一实现（脚本缩放 / 幼体缩放 / 睡眠位移 / 身体朝向）。
     * <p>
     * 没有 client entity 定义的宿主保持恒等（与历史行为一致：原版实体不施加任何模型根变换）；
     * {@code living} 为 null 表示非实体宿主或调用方关闭了实体姿态，此时仍施加脚本缩放
     * （{@code scripts.scale} 属于客户端实体数据，与宿主是否有实体无关）。
     *
     * @param living 施加幼体缩放与身体朝向/睡眠位移的实体；为 null 时只施加脚本缩放
     */
    static void applyRootPose(PoseStack poseStack, RenderData<?> cap,
                              @Nullable LivingEntity living, float partialTick) {
        var clientEntity = cap.getClientEntityComponent().getClientEntity();

        if (clientEntity == null) return;

        LivingEntity livingEntity = living;

        // vanilla 在 RenderLivingEvent.Pre 之前只施加了实体位置平移（无旋转/缩放），
        // 因此这里必须自行复刻 vanilla LivingEntityRenderer 的睡眠变换：
        //   translate(-step * (eyeHeight(STANDING) - 0.1))  → 把模型根（脚）移到床脚一侧
        //   mulPose(YP(-bedOrientation.toYRot() + 180))    → 床轴朝向（180 为模型面向 +Z 与 vanilla -Z 的差）
        // 平躺本身由 get_in_bed 剪辑负责，这里只摆正床轴与落点。
        // 顺序必须与 vanilla 一致：translate 在 scripts.scale / 幼体缩放之前施加，
        // 否则平移量会被缩放系数乘小（村民 scale=0.8 时少移 0.3 格，幼体 0.5 时少移 0.76 格）。
        // 判据取「非空的床朝向」而不是 boolean：translate 与 mulPose 两处都要窄化，而 boolean 蕴含
        // 非空这件事 NullAway 追踪不到。
        Direction sleepingBed = null;
        if (livingEntity != null && livingEntity.hasPose(Pose.SLEEPING)) {
            sleepingBed = livingEntity.getBedOrientation();
            if (sleepingBed != null) {
                float bedOffset = livingEntity.getEyeHeight(Pose.STANDING) - 0.1F;
                poseStack.translate(-sleepingBed.getStepX() * bedOffset,
                                    0F,
                                    -sleepingBed.getStepZ() * bedOffset);
            }
        }

        clientEntity.scripts().ifPresent(s -> {
            var scope = cap.getScope();
            if (scope == null) {
                return;
            }
            poseStack.scale(s.getScaleX(scope), s.getScaleY(scope), s.getScaleZ(scope));
        });

        if (livingEntity != null) {
            if (livingEntity.isBaby()) {
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            if (sleepingBed != null) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-sleepingBed.toYRot() + 180F));
            } else {
                float yBodyRot = Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
                poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
            }
        }
    }

    /** 逐帧 tick 期变量（与管线 TickStage 同语义）：子帧插值与挥击进度。 */
    static void applyTickVariables(@Nullable Entity entity, MolangScope scope, float partialTick) {
        scope.set("variable.partial_tick", partialTick);
        if (entity instanceof LivingEntity livingEntity)
            scope.set("variable.attack_time", ((float) livingEntity.swingTime) / livingEntity.getCurrentSwingDuration());
    }

    /** 提交期变量 = tick 期变量 + 宿主引擎变量（{@code query.*} 的引擎侧投影）。 */
    static void applySubmitVariables(@Nullable Entity entity, MolangScope scope, float partialTick) {
        if (entity != null) {
            EngineEntityVariables.bind(entity, scope);
        }
        applyTickVariables(entity, scope, partialTick);
    }

    /** 同步行为状态注入；无实体宿主清掉残留（避免上一帧/上一宿主的 variant 泄漏）。 */
    static void applyBehaviorContext(@Nullable Entity entity, MolangScope scope) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            scope.getHostContext().remove(MOLANG_ENTITY_CONTEXT);
            return;
        }
        SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                DataAttachmentPort.syncedBehaviorState(), livingEntity);
        if (synced == null) {
            scope.getHostContext().remove(MOLANG_ENTITY_CONTEXT);
            return;
        }

        ComponentStoreView store = MolangContextPort.newComponentStore();
        store.put("minecraft:variant", synced.variant());
        store.put("minecraft:mark_variant", synced.markVariant());
        store.put("minecraft:scale", synced.scale());
        scope.getHostContext().put(MOLANG_ENTITY_CONTEXT, MolangContextPort.newMolangEntityContext(store));
    }

    static List<Runnable> setupClientEntity(Entity entity, RenderData<?> cap) {
        cap.ensureOwner(entity);

        String entityId = RenderPorts.get().renderSystemPort().getEntityTypeId(entity);
        return setupClientEntity(entityId, cap);
    }

    /**
     * 按实体类型 id 装配（自动解析绑定）。
     * <p>
     * 只对 {@link ClientEntityComponent.BindingKind#AUTO_BY_ID} 生效：固定实例绑定不会被「宿主类型 id」
     * 重写（历史缺陷：String 重载在 stale 时覆盖外部显式设置的实例，表现为「首次特效不渲染、
     * 第二次同 id 才生效」）。
     */
    static List<Runnable> setupClientEntity(String entityId, RenderData<?> cap) {
        ClientEntityComponent component = cap.getClientEntityComponent();
        long generation = ClientEntityManager.generation();
        if (component.bindingKind() == ClientEntityComponent.BindingKind.AUTO_BY_ID) {
            if (component.boundId() == null) {
                component.bindById(entityId);
            }
            if (component.needsRebind(generation)) {
                String boundId = component.boundId();
                component.applyResolved(boundId == null ? null : ClientEntityManager.byId(boundId), generation);
            }
        }
        return apply(cap);
    }

    /** 按显式实例装配（引用变化时转为固定实例绑定；{@code null} 表示「保持现状」）。 */
    static List<Runnable> setupClientEntity(@Nullable BrClientEntity clientEntity, RenderData<?> cap) {
        ClientEntityComponent component = cap.getClientEntityComponent();
        if (clientEntity != null && component.getClientEntity() != clientEntity) {
            component.bindInstance(clientEntity);
        }
        return apply(cap);
    }

    /** 按当前绑定装配（不改绑定、不重新解析）：由绑定方在需要时驱动。 */
    static List<Runnable> setupClientEntity(RenderData<?> cap) {
        return apply(cap);
    }

    /** 组件装配主体：RC 重建 / 静态 scope 注入 / 动画 setup / 回落原版。 */
    private static List<Runnable> apply(RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        RenderControllerComponent renderControllerComponent = cap.getRenderControllerComponent();
        List<Runnable> syncedActions = new ArrayList<>();

        boolean clientEntityChanged = clientEntityComponent.consumeChanged();
        BrClientEntity appliedClientEntity = clientEntityComponent.getClientEntity();

        if (appliedClientEntity != null) {
            if (clientEntityChanged) {
                renderControllerComponent.clear();
                appliedClientEntity.scripts().ifPresent(s -> {
                    if (cap.getScope() != null) {
                        s.initialize().eval(cap.getScope());
                        s.pre_animation().eval(cap.getScope());
                    }
                });
            }
        }

        List<ModelComponent> components = cap.getModelComponents();

        if (appliedClientEntity != null) {
            BrClientEntity ce = appliedClientEntity;

            // texture./geometry./material. 短名仅随 clientEntity/scope 变化，守卫内一次性注入（原每帧重建）
            MolangScope scope = cap.getScope();
            if (scope != null && clientEntityComponent.consumeStaticScopeInit(scope)) {
                RenderControllerEntry.initStaticScope(scope, ce);
            }

            int conditionMask = 0;
            int modelVersion = clientEntityComponent.getModelVersion();
            List<ModelComponent> rebuilt = new ArrayList<>();
            for (int i = 0; i < ce.render_controllers().size(); i++) {
                String renderController = ce.render_controllers().get(i);
                io.github.tt432.eyelib.molang.MolangValue condition = ce.renderControllerConditions()
                                                                        .get(renderController);
                if (condition != null && cap.getScope() != null && !condition.evalAsBool(cap.getScope())) {
                    continue;
                }
                conditionMask |= 1 << i;
                RenderControllerEntry renderControllerEntry = RenderControllerManager.INSTANCE.get(renderController);
                RenderControllerComponent.Slot renderControllerSlot = renderControllerComponent.syncSlot(i, renderControllerEntry);
                if (renderControllerEntry != null && cap.getScope() != null)
                    rebuilt.addAll(renderControllerEntry.setupModel(cap.getScope(), appliedClientEntity, clientEntityComponent.getModels(), modelVersion, renderControllerSlot, syncedActions));
            }
            renderControllerComponent.setConditionMask(conditionMask);
            renderControllerComponent.trim(ce.render_controllers().size());
            renderControllerComponent.markSetupFrame(frameCounter);

            // Opt18-A2：全部 RC 静态缓存命中时 rebuilt 与现有组件实例序完全一致——
            // 跳过 clear/addAll 与 bind 骨骼失效（内容未变，bind 集合不变），
            // 消除逐帧 ArrayList 重建与 Opt16 缓存的无效化。
            if (!identicalComponents(components, rebuilt)) {
                // components 整体替换：bind 骨骼缓存失效（Opt16 失效契约）
                cap.invalidateBindBones();
                components.clear();
                components.addAll(rebuilt);
            }

            if (components.isEmpty() && !ce.geometry().isEmpty()) {
                var entry = ce.geometry().entrySet().stream().findFirst().orElse(null);
                if (entry != null) {
                    var defaultTexture = ce.textures().get(entry.getKey());
                    if (defaultTexture == null && !ce.textures().isEmpty()) {
                        defaultTexture = ce.textures().values().stream().findFirst().orElse(null);
                    }
                    if (defaultTexture != null) {
                        var modelComponent = new ModelComponent();
                        modelComponent.setInfo(new ModelComponentInfo(
                                entry.getValue(), PortResourceLocation.parse(defaultTexture),
                                PortResourceLocation.parse("entity_translucent")));
                        // 组件集变化：bind 骨骼缓存失效（Opt16 失效契约）
                        cap.invalidateBindBones();
                        components.add(modelComponent);
                    }
                }
            }

            ce.scripts().ifPresent(s -> cap.getAnimationComponent().setup(ce.animations(), s.animate()));

            if (cap.getScope() != null) {
                cap.getScope().getHostContext().put(HostRoles.CLIENT_ENTITY, ce);
            }
        } else {
            if (!components.isEmpty()) {
                cap.invalidateBindBones();
                components.clear();
            }
            renderControllerComponent.clear();
            renderControllerComponent.setConditionMask(0);
            if (cap.getScope() != null) {
                cap.getScope().getHostContext().remove(HostRoles.CLIENT_ENTITY);
            }
        }

        return syncedActions;
    }

    /** 实例序恒等比较：静态缓存命中路径逐帧返回同一实例序，据此跳过组件列表重建。 */
    private static boolean identicalComponents(List<ModelComponent> current, List<ModelComponent> rebuilt) {
        if (current.size() != rebuilt.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i) != rebuilt.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 求值实体全部 render_controller 条件的启用位掩码（bit i = render_controllers[i] 启用）。
     * 与 setupClientEntity 的跳过逻辑一致：无条件恒启用，scope 缺失时启用。
     */
    static int evalConditionMask(BrClientEntity ce, @Nullable MolangScope scope) {
        int mask = 0;
        for (int i = 0; i < ce.render_controllers().size(); i++) {
            var condition = ce.renderControllerConditions().get(ce.render_controllers().get(i));
            if (condition == null || scope == null || condition.evalAsBool(scope)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }
}
