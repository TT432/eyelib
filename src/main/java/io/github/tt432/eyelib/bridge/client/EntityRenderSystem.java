package io.github.tt432.eyelib.bridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if <26.1 {
import net.minecraft.client.renderer.LightTexture;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
//? if <26.1 {
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
//?} else {
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
//?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
//?}

/**
 * 实体渲染的 MC 翻译层：订阅 Forge 事件、翻译版本差异参数，通过 {@link EntityRenderPorts} 回调 application 编排。
 * 编排逻辑（实体遍历、组件装配、渲染调度）在 {@link io.github.tt432.eyelib.client.render.EntityRenderOrchestrator}。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class EntityRenderSystem {
    public static volatile int renderCount = 0;
    public static volatile int errorCount = 0;
    public static volatile String lastError = null;

    static {
        try {
            Class.forName("io.github.tt432.eyelib.client.ClientBootstrap");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private EntityRenderSystem() {
    }

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        EntityRenderPorts.setupClientEntityPort.setup(event.getEntity());
    }

    //? if <26.1 {
    @SubscribeEvent
    public static void onEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        //? if <1.20.6 {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float partialTick = event.getPartialTick();
        //?} else {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        //?}

        EntityRenderPorts.renderStagePort.onRenderStage(partialTick, position.x, position.y, position.z);
    }
    //?} else {
    @SubscribeEvent
    public static void onEvent(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        //? if <1.20.6 {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        //?} else {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        //?}
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

        EntityRenderPorts.renderStagePort.onRenderStage(partialTick, position.x, position.y, position.z);

        var sharedBuffer = new com.mojang.blaze3d.vertex.ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(sharedBuffer);
        EntityRenderPorts.renderBufferPort.renderEntities(
                partialTick, position.x, position.y, position.z, event.getPoseStack(), bufferSource);
        try { bufferSource.endBatch(); } catch (Throwable ignored) {}
        sharedBuffer.close();
    }
    //?}

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onEvent(LivingEvent.LivingTickEvent event) {
    //?} elif <26.1 {
    public static void onEvent(EntityTickEvent event) {
    //?} else {
    public static void onEvent(EntityTickEvent.Pre event) {
    //?}
        var entity = event.getEntity();
        if (entity instanceof Bee bee) {
            //? if <1.20.6 {
            bee.updateSwingTime();
            //?} elif <26.1 {
            ((io.github.tt432.eyelib.mixin.LivingEntityAccessor) bee).eyelib$invokeUpdateSwingTime();
            //?}
        }
    }

    //? if <26.1 {
    @SubscribeEvent
    public static <E extends LivingEntity, M extends EntityModel<E>> void onEvent(RenderLivingEvent.Pre<E, M> event) {
        LivingEntity entity = event.getEntity();
        int overlay = LivingEntityRenderer.getOverlayCoords(entity,
                ((LivingEntityRendererAccessor) event.getRenderer()).callGetWhiteOverlayProgress(entity, event.getPartialTick()));

        var params = new RenderEntityParams(entity, event.getMultiBufferSource(), event.getPoseStack(),
                                            event.getPackedLight(), event.getPartialTick(), overlay);
        if (EntityRenderPorts.renderEntityPort.render(params)) {
            event.setCanceled(true);
        }
    }
    //?} else {
    @SubscribeEvent
    public static <T extends LivingEntity, S extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M extends EntityModel<? super S>> void onEvent(
            RenderLivingEvent.Pre<T, S, M> event) {
        var state = event.getRenderState();
        LivingEntity entity = findEntityByRenderState(state.entityType, state.x, state.y, state.z, event.getPartialTick());
        if (entity == null) return;

        com.mojang.blaze3d.vertex.ByteBufferBuilder byteBufferBuilder = new com.mojang.blaze3d.vertex.ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(byteBufferBuilder);

        var params = new RenderEntityParams(entity, bufferSource, event.getPoseStack(),
                                            state.lightCoords, event.getPartialTick(), OverlayTexture.NO_OVERLAY);
        boolean rendered = EntityRenderPorts.renderEntityPort.render(params);

        bufferSource.endBatch();
        byteBufferBuilder.close();
        if (rendered) event.setCanceled(true);
    }

    @Nullable
    private static LivingEntity findEntityByRenderState(
            net.minecraft.world.entity.EntityType<?> entityType,
            double targetX,
            double targetY,
            double targetZ,
            float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;

        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (entity.getType() != entityType) continue;

            double renderX = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double renderY = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double renderZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            if (Math.abs(renderX - targetX) < 0.5
                    && Math.abs(renderY - targetY) < 0.5
                    && Math.abs(renderZ - targetZ) < 0.5) {
                return livingEntity;
            }
        }

        return null;
    }
    //?}

    //? if <26.1 {
    public static ResourceLocation getEntityTypeId(Entity entity) {
    //?} else {
    public static Identifier getEntityTypeId(Entity entity) {
    //?}
        //? if <1.20.6 {
        return ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        //?} else {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        //?}
    }

    public static int getLlamaDecorColorIndex(Llama llama) {
        //? if <1.20.6 {
        if (llama.inventory.getItem(AbstractHorse.INV_SLOT_ARMOR)
        //?} else {
        if (llama.getInventory().getItem(AbstractHorse.EQUIPMENT_SLOT_OFFSET)
        //?}
                           .getItem() instanceof BlockItem bi && bi.getBlock() instanceof WoolCarpetBlock wc) {
            return wc.getColor().getId() + 1;
        } else {
            return 0;
        }
    }

    public static void pushPoseRaw(PoseStack poseStack, PoseStack.Pose pose) {
        //? if <1.20.6 {
        poseStack.poseStack.addLast(pose);
        //?} elif <26.1 {
        ((io.github.tt432.eyelib.mixin.PoseStackAccessor) poseStack).eyelib$getPoseStackDeque().addLast(pose);
        //?}
    }

    public static void renderItemDirect(LivingEntity le, ItemStack item, ItemDisplayContext context,
                                        boolean left, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        //? if <26.1 {
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                  .renderItem(le, item, context, left, poseStack, bufferSource, light);
        //?}
    }

    public static void flushBuffer(MultiBufferSource source) {
        //? if <26.1 {
        if (source instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
        //?}
    }

    //? if <26.1
    public static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    //? if >=26.1
    public static final int FULL_BRIGHT = 0xF000F0;

    public static float @Nullable [] getEntityTintColor(@Nullable Entity entity) {
        if (entity instanceof Sheep sheep) {
            DyeColor dyeColor = sheep.getColor();
            if (dyeColor != null) {
                //? if <1.20.6 {
                float[] diffuse = dyeColor.getTextureDiffuseColors();
                return new float[]{diffuse[0], diffuse[1], diffuse[2], 1.0F};
                //?} else {
                int diffuse = dyeColor.getTextureDiffuseColor();
                //? if <26.1 {
                return new float[]{
                        net.minecraft.util.FastColor.ARGB32.red(diffuse) / 255.0F,
                        net.minecraft.util.FastColor.ARGB32.green(diffuse) / 255.0F,
                        net.minecraft.util.FastColor.ARGB32.blue(diffuse) / 255.0F,
                        1.0F
                };
                //?} else {
                return new float[]{
                        net.minecraft.util.ARGB.red(diffuse) / 255.0F,
                        net.minecraft.util.ARGB.green(diffuse) / 255.0F,
                        net.minecraft.util.ARGB.blue(diffuse) / 255.0F,
                        1.0F
                };
                //?}
                //?}
            }
        }
        return null;
    }

    public static PoseStack createPoseStackFromMatrix(org.joml.Matrix4f matrix) {
        PoseStack poseStack = new PoseStack();
        //? if <1.20.6 {
        poseStack.poseStack.addLast(new PoseStack.Pose(new org.joml.Matrix4f(matrix), new org.joml.Matrix3f(matrix)));
        //?} elif <26.1 {
        ((io.github.tt432.eyelib.mixin.PoseStackAccessor) poseStack).eyelib$getPoseStackDeque()
                .addLast(io.github.tt432.eyelib.mixin.PoseStackPoseAccessor.eyelib$create(new org.joml.Matrix4f(matrix), new org.joml.Matrix3f(matrix)));
        //?} else {
        poseStack.last().pose().set(matrix);
        poseStack.last().normal().set(new org.joml.Matrix3f(matrix));
        //?}
        return poseStack;
    }
}
