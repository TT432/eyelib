package io.github.tt432.eyelib.client.molang;

import io.github.tt432.eyelib.bridge.molang.EntityStatePort;
import io.github.tt432.eyelib.bridge.capability.DataAttachmentPort;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.adapter.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.behavior.component.MarkVariant;
import io.github.tt432.eyelib.behavior.component.Variant;
import io.github.tt432.eyelib.behavior.component.property.SkinId;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.molang.mapping.api.MolangQueryRuntimeBridge;
import org.jspecify.annotations.Nullable;
import java.util.Map;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import java.util.function.Function;

import static io.github.tt432.eyelib.molang.MolangValue.FALSE;
import static io.github.tt432.eyelib.molang.MolangValue.TRUE;

/**
 * @author TT432
 */
@MolangMapping(value = "query", pureFunction = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public final class MolangQuery {

    private static final HostRole<Creeper> CREEPER = HostRole.of("Creeper", Creeper.class);
    private static final HostRole<WitherBoss> WITHER_BOSS = HostRole.of("WitherBoss", WitherBoss.class);
    private static final HostRole<Entity> ENTITY = HostRole.of("Entity", Entity.class);
    private static final HostRole<LivingEntity> LIVING_ENTITY = HostRole.of("LivingEntity", LivingEntity.class);
    @MolangFunction(value = "anim_time", alias = "life_time", description = "动画播放秒数")
    public static float animTime(MolangScope scope) {
        return scope.getHostContext().get(HostRoles.ANIMATION_DATA).map(BrAnimationEntry.Data::animTime)
                    .orElseGet(() -> scope.getHostContext().get(HostRoles.CONTROLLER_DATA)
                                          .map(BrAnimationController.Data::animTime).orElse(0F));
    }

    @MolangFunction(value = "delta_time", description = "距离上一帧的秒数")
    public static float deltaTime(MolangScope scope) {
        return scope.getHostContext().get(HostRoles.ANIMATION_DATA).map(BrAnimationEntry.Data::deltaTime).orElse(0F);
    }

    @MolangFunction(value = "any_animation_finished", description = "任意动画播放完毕（动画控制器）")
    public static float anyAnimationFinished(MolangScope scope) {
        return scope.getHostContext().get(HostRoles.ANIMATION_CONTROLLER)
                    .flatMap(c -> scope.getHostContext().get(HostRoles.CONTROLLER_DATA)
                                       .map(c::anyAnimationFinished)
                    ).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "all_animations_finished", description = "所有动画播放完毕（动画控制器）")
    public static float allAnimationsFinished(MolangScope scope) {
        return scope.getHostContext().get(HostRoles.ANIMATION_CONTROLLER)
                    .flatMap(c -> scope.getHostContext().get(HostRoles.CONTROLLER_DATA)
                                       .map(c::allAnimationFinished)
                    ).orElse(false) ? TRUE : FALSE;
    }

    @MolangFunction(value = "variant", description = "变体")
    public static float variant(MolangScope scope) {
        return livingFloat(scope, l -> {
            SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                    DataAttachmentPort.syncedBehaviorState(), l);
            if (synced != null) {
                return (float) synced.variant();
            }
            Variant component = DataAttachmentHelper.getOrCreate(DataAttachmentPort.entityBehaviorData(), l)
                                                    .component(Variant.class);
            // variant 的唯一真源是行为包 spawn 事件（见 BehaviorSpawnApplicator）；
            // 无行为包数据时保持 BE 中性值 0，禁止从 JE 实体状态反向推导。
            return component != null ? (float) component.value() : 0;
        });
    }

    @MolangFunction(value = "mark_variant", description = "变体")
    public static float markVariant(MolangScope scope) {
        return livingFloat(scope, l -> {
            SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                    DataAttachmentPort.syncedBehaviorState(), l);
            if (synced != null) {
                return (float) synced.markVariant();
            }
            MarkVariant component = DataAttachmentHelper.getOrCreate(DataAttachmentPort.entityBehaviorData(), l)
                                                        .component(MarkVariant.class);
            return component != null ? (float) component.value() : 0;
        });
    }

    @MolangFunction(value = "damage_x", description = "受伤来源方向 x")
    public static float damageX(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get(), living)
                                            .lastHurtX());
    }

    @MolangFunction(value = "damage_y", description = "受伤来源方向 x")
    public static float damageY(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get(), living)
                                            .lastHurtY());
    }

    @MolangFunction(value = "damage_z", description = "受伤来源方向 x")
    public static float damageZ(MolangScope scope) {
        return livingFloat(scope, living ->
                (float) DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get(), living)
                                            .lastHurtZ());
    }

    @MolangFunction(value = "facing_target_to_range_attack", description = "正在进行远程攻击")
    public static float facingTargetToRageAttack(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .facing_target_to_range_attack());
    }

    @MolangFunction(value = "is_avoiding_mobs", description = "正在从怪物逃离")
    public static float isAvoidingMobs(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_avoiding_mobs());
    }

    @MolangFunction(value = "is_grazing", description = "正在吃草")
    public static float isGrazing(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_grazing());
    }

    @MolangFunction(value = "modified_distance_moved", description = "移动过的距离")
    public static float modifiedDistanceMoved(MolangScope scope) {
        return scope.getHostContext().get(ENTITY)
                    .map(e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), e)
                                                  .distanceWalked())
                    .orElse(0F);
    }

    @MolangFunction(value = "is_digging", description = "正在挖掘（玩家）")
    public static float isDigging(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_dig());
    }

    @MolangFunction(value = "is_avoid", description = "正在逃离(比如苦力怕逃离猫)")
    public static float isAvoid(MolangScope scope) {
        return entityBool(scope, e -> DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), e)
                                                          .is_avoid());
    }

    @MolangFunction(value = "is_jumping", description = "正在跳跃")
    public static float isJumping(MolangScope scope) {
        return livingBool(scope, entity -> {
            return EntityStatePort.isJumping(entity);
        });
    }

    @MolangFunction(value = "creeper_swell", alias = "swell_amount", description = "苦力怕爆炸计时")
    public static float creeperSwell(MolangScope scope) {
        return scope.getHostContext().get(CREEPER).map(creeper -> {
                    return EntityStatePort.creeperSwell(creeper);
                })
                    .orElse(Float.valueOf(scope.getHostContext().get(WITHER_BOSS)
                                                .map(WitherBoss::getInvulnerableTicks)
                                               .orElse(0)));
    }

    // ==================== 2026-08-16 补齐：包数据实扫未实现的客户端侧 query ====================
    // 语义来源：Microsoft 官方 Molang Query Functions 文档（bedrock.dev 镜像逐条核对）。
    // biome/camera/cape/surface_particle 系列官方标注"仅客户端资源包侧"，故落在本类。

    @MolangFunction(value = "entity_biome_has_any_identifier", description = "实体所站生物群系 identifier 与任一参数匹配（官方：仅客户端）")
    public static float entityBiomeHasAnyIdentifier(MolangScope scope, String... identifiers) {
        return entityBool(scope, entity -> {
            var key = entity.level().getBiome(entity.blockPosition()).unwrapKey().orElse(null);
            if (key == null) {
                return false;
            }
            for (String id : identifiers) {
                if (ResourceLocationBridge.parseMc(id).equals(key.location())) {
                    return true;
                }
            }
            return false;
        });
    }

    @MolangFunction(value = "entity_biome_has_any_tags", description = "实体所站生物群系带任一给定标签（官方：仅客户端）")
    public static float entityBiomeHasAnyTags(MolangScope scope, String... tags) {
        return entityBool(scope, entity -> {
            var holder = entity.level().getBiome(entity.blockPosition());
            for (String tag : tags) {
                var tagKey = net.minecraft.tags.TagKey.create(Registries.BIOME, ResourceLocationBridge.parseMc(tag));
                if (holder.is(tagKey)) {
                    return true;
                }
            }
            return false;
        });
    }

    @MolangFunction(value = "frame_alpha", description = "当前帧在两 AI tick 之间的插值比例（= partial tick）")
    public static float frameAlpha(MolangScope scope) {
        return MolangQueryRuntimeBridge.resolvePartialTick(scope);
    }

    @MolangFunction(value = "camera_rotation", description = "相机旋转（0=x/pitch，1=y/yaw；JE 取相机实体视角旋转近似）")
    public static float cameraRotation(MolangScope scope, float axis) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            return 0F;
        }
        float partialTick = frameAlpha(scope);
        return axis == 0 ? cameraEntity.getViewXRot(partialTick) : cameraEntity.getViewYRot(partialTick);
    }

    @MolangFunction(value = "rotation_to_camera", description = "实体对准相机所需旋转（0=x/pitch，1=y/yaw）")
    public static float rotationToCamera(MolangScope scope, float axis) {
        var mc = Minecraft.getInstance();
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) {
            return 0F;
        }
        return scope.getHostContext().get(ENTITY).map(entity -> {
            float partialTick = frameAlpha(scope);
            Vec3 from = entity.getEyePosition(partialTick);
            Vec3 to = cameraEntity.getEyePosition(partialTick);
            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double dz = to.z - from.z;
            if (axis == 0) {
                return (float) (-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
            }
            return (float) (Math.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        }).orElse(0F);
    }

    @MolangFunction(value = "is_first_person", description = "实体正以第一人称渲染（JE：自身为相机实体且第一人称视角）")
    public static float isFirstPerson(MolangScope scope) {
        return entityBool(scope, e -> {
            var mc = Minecraft.getInstance();
            return e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        });
    }

    @MolangFunction(value = "is_local_player", description = "实体是本窗口的本地玩家（官方：行为包侧恒 0）")
    public static float isLocalPlayer(MolangScope scope) {
        return entityBool(scope, e -> e instanceof Player player && player.isLocalPlayer());
    }

    @MolangFunction(value = "is_on_screen", description = "实体在屏幕上（JE 无渲染视锥上下文，近似：与本客户端同维度且距相机实体不超出视距）")
    public static float isOnScreen(MolangScope scope) {
        return entityBool(scope, e -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null || e.level() != mc.level) {
                return false;
            }
            Entity cameraEntity = mc.getCameraEntity();
            if (cameraEntity == null) {
                return false;
            }
            if (e == cameraEntity) {
                return true;
            }
            double range = mc.options.getEffectiveRenderDistance() * 16.0;
            return e.position().distanceToSqr(cameraEntity.getEyePosition(frameAlpha(scope))) <= range * range;
        });
    }

    @MolangFunction(value = "has_cape", description = "玩家有披风")
    public static float hasCape(MolangScope scope) {
        return entityBool(scope, io.github.tt432.eyelib.bridge.client.ClientEntityVisualPort::hasCape);
    }

    @MolangFunction(value = "state_time", description = "动画控制器当前状态的进行时间（仅 AC 上下文有效；与 anim_time 的 AC 分支同源）")
    public static float stateTime(MolangScope scope) {
        return scope.getHostContext().get(HostRoles.CONTROLLER_DATA)
                    .map(BrAnimationController.Data::animTime)
                    .orElse(0F);
    }

    @MolangFunction(value = "surface_particle_color", description = "脚下方块（向下扫描 ≤10 格）粒子颜色 struct{r,g,b,a}（JE：粒子贴图平均色 × 群系着色近似，见 ClientEntityVisualPort；26.1 未适配恒 0）")
    public static Map<String, Float> surfaceParticleColor(MolangScope scope) {
        Map<String, Float> zero = Map.of("r", 0F, "g", 0F, "b", 0F, "a", 0F);
        var data = surfaceParticleData(scope);
        return data == null ? zero : Map.of("r", data.r(), "g", data.g(), "b", data.b(), "a", data.a());
    }

    @MolangFunction(value = "surface_particle_texture_coordinate", description = "脚下方块粒子贴图坐标 struct{u,v}（JE：粒子贴图图集原点近似；26.1 未适配恒 0）")
    public static Map<String, Float> surfaceParticleTextureCoordinate(MolangScope scope) {
        Map<String, Float> zero = Map.of("u", 0F, "v", 0F);
        var data = surfaceParticleData(scope);
        return data == null ? zero : Map.of("u", data.u(), "v", data.v());
    }

    @MolangFunction(value = "surface_particle_texture_size", description = "脚下方块粒子贴图尺寸 struct{x,y}（像素；26.1 未适配恒 0）")
    public static Map<String, Float> surfaceParticleTextureSize(MolangScope scope) {
        Map<String, Float> zero = Map.of("x", 0F, "y", 0F);
        var data = surfaceParticleData(scope);
        return data == null ? zero : Map.of("x", data.width(), "y", data.height());
    }

    /** 实体脚下方块的表面粒子贴图数据（无实体/无表面方块/26.1 未适配 → null）。 */
    private static io.github.tt432.eyelib.bridge.client.ClientEntityVisualPort.@Nullable SurfaceParticleData surfaceParticleData(MolangScope scope) {
        Entity entity = scope.getHostContext().get(ENTITY).orElse(null);
        if (entity == null) {
            return null;
        }
        SurfaceHit hit = surfaceBlock(entity);
        if (hit == null) {
            return null;
        }
        return io.github.tt432.eyelib.bridge.client.ClientEntityVisualPort
                .surfaceParticle(hit.state(), entity.level(), hit.pos());
    }

    /** 脚下方块命中（状态 + 坐标），供 surface_particle_* 使用。 */
    private record SurfaceHit(BlockState state, BlockPos pos) {
    }

    /** 实体脚下方块（向下扫描 ≤10 格的首个非空气方块，官方语义）；无 → null。 */
    private static @Nullable SurfaceHit surfaceBlock(Entity entity) {
        var level = entity.level();
        BlockPos feet = entity.blockPosition();
        for (int i = 1; i <= 10; i++) {
            BlockPos pos = feet.below(i);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                return new SurfaceHit(state, pos);
            }
        }
        return null;
    }

    private static float livingBool(MolangScope scope, ToBooleanFunction<LivingEntity> function) {
        return scope.getHostContext().get(LIVING_ENTITY)
                    .map(l -> function.apply(l) ? TRUE : FALSE)
                    .orElse(0F);
    }

    private static float entityBool(MolangScope scope, ToBooleanFunction<Entity> function) {
        return scope.getHostContext().get(ENTITY).map(l -> function.apply(l) ? TRUE : FALSE).orElse(0F);
    }

    private static float livingFloat(MolangScope scope, Function<LivingEntity, Float> function) {
        return scope.getHostContext().get(LIVING_ENTITY)
                    .map(function)
                    .orElse(0F);
    }

    @MolangFunction(value = "equipped_item_is_attachable", description = "手持物品为 attachable")
    public static float equippedItemIsAttachable(MolangScope scope) {
        return scope.getHostContext().get(LIVING_ENTITY)
                    .map(entity -> {
                        if (AttachableResolver.resolve(entity, entity.getMainHandItem()) != null
                                || AttachableResolver.resolve(entity, entity.getOffhandItem()) != null) {
                            return TRUE;
                        }
                        return FALSE;
                    }).orElse(FALSE);
    }

    @MolangFunction(value = "any", description = "第一个参数与后续任一参数相等则返回 1")
    public static float any(MolangScope scope, float first, float... others) {
        for (float other : others) {
            if (first == other) {
                return TRUE;
            }
        }
        return FALSE;
    }

    @MolangFunction(value = "skin_id", description = "皮肤 id（无组件时回退 0，保证变体公式可算）")
    public static float skinId(MolangScope scope) {
        return livingFloat(scope, l -> {
            SkinId component = DataAttachmentHelper.getOrCreate(DataAttachmentPort.entityBehaviorData(), l)
                                                   .component(SkinId.class);
            return component != null ? (float) component.value() : 0;
        });
    }

    @FunctionalInterface
    interface ToBooleanFunction<K> {
        boolean apply(K key);
    }
}

