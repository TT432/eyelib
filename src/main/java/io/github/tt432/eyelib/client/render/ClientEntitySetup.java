package io.github.tt432.eyelib.client.render;

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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实体渲染 setup 装配簇（clientEntity 解析/组件重建/scope 上下文注入），
 * 从 {@link EntityRenderOrchestrator} 提取。
 *
 * @author TT432
 */
final class ClientEntitySetup {
    private static final HostRole<MolangEntityContextView> MOLANG_ENTITY_CONTEXT =
            HostRole.of("molang_entity_context", MolangEntityContextView.class);

    /**
     * BE 重命名实体的 JE 类型别名：BE 村与掠夺更新把 villager/zombie_villager 重命名为 *_v2，
     * 资源包（如 A&S）只注册新名；JE 1.20.1 实体类型仍是旧名，查表时回退别名。
     */
    private static final Map<String, String> JE_TO_BE_ENTITY_ALIAS = Map.of(
            "minecraft:villager", "minecraft:villager_v2",
            "minecraft:zombie_villager", "minecraft:zombie_villager_v2"
    );

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

    static <T> void setupEntityClientEntityData(SimpleRenderAction<T> data) {
        var cap = data.renderData();
        var clientEntity = cap.getClientEntityComponent().getClientEntity();
        var poseStack = data.poseStack();

        if (clientEntity == null) return;

        clientEntity.scripts().ifPresent(s -> {
            var scope = cap.getScope();
            if (scope == null) {
                return;
            }
            poseStack.scale(s.getScaleX(scope), s.getScaleY(scope), s.getScaleZ(scope));
        });

        if (data.applyEntityPose() && data.entity() instanceof LivingEntity livingEntity) {
            if (livingEntity.isBaby()) {
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            float yBodyRot = Mth.rotLerp(data.partialTick(), livingEntity.yBodyRotO, livingEntity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
        }
    }

    static void setupExtraMolang(Entity entity, MolangScope scope, float partialTick) {
        RenderPorts.get().renderSystemPort().setupLlamaDecor(entity, scope);

        scope.set("variable.partial_tick", partialTick);
        if (entity instanceof LivingEntity livingEntity)
            scope.set("variable.attack_time", ((float) livingEntity.swingTime) / livingEntity.getCurrentSwingDuration());
    }

    static void setupSyncedBehaviorContext(LivingEntity entity, MolangScope scope) {
        SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                DataAttachmentPort.syncedBehaviorState(), entity);
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

    static List<Runnable> setupClientEntity(String entityId, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        long generation = ClientEntityManager.INSTANCE.generation();

        // 注册表代际变化（addon 卸载/换包/mod 重注册）时重新解析：
        // 解析结果为 null 也会记录代际——vanilla 实体不再每次 setup 都查表，
        // 已卸载 addon 的实体回落 vanilla 渲染。
        if (clientEntityComponent.isStale(generation)) {
            BrClientEntity clientEntity = ClientEntityManager.INSTANCE.get(entityId.toString());
            if (clientEntity == null) {
                String alias = JE_TO_BE_ENTITY_ALIAS.get(entityId.toString());
                if (alias != null) {
                    clientEntity = ClientEntityManager.INSTANCE.get(alias);
                }
            }
            clientEntityComponent.setClientEntity(clientEntity);
            clientEntityComponent.markResolvedFrom(generation);
        }

        return setupClientEntity(clientEntityComponent.getClientEntity(), cap);
    }

    static List<Runnable> setupClientEntity(@Nullable BrClientEntity clientEntity, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        RenderControllerComponent renderControllerComponent = cap.getRenderControllerComponent();
        List<Runnable> syncedActions = new ArrayList<>();
        // 引用不同才视为「外部显式指定」：setClientEntity 标记 externallySet，
        // 不参与注册表代际失效；资源重载后由外部持有者负责重设新实例。
        // 每帧把当前 ce 传回的重 setup 调用方（RC 条件重估等）引用相等，不触碰该标记。
        if (clientEntity != null && clientEntityComponent.getClientEntity() != clientEntity) {
            clientEntityComponent.setClientEntity(clientEntity);
        }

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
