package io.github.tt432.eyelib.example.item;

import io.github.tt432.eyelib.api.Syncable;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.LoopType;
import io.github.tt432.eyelib.api.bedrock.animation.PlayState;
import io.github.tt432.eyelib.api.sound.SoundPlayer;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationController;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.animation.builder.AnimationBuilder;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationFactory;
import io.github.tt432.eyelib.common.bedrock.animation.util.AnimationState;
import io.github.tt432.eyelib.example.GeckoLibMod;
import io.github.tt432.eyelib.example.client.renderer.item.FistRenderer;
import io.github.tt432.eyelib.network.EyelibNetworkHandler;
import io.github.tt432.eyelib.util.GeckoLibUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Consumer;

/**
 * @author DustW
 */
public class FistItem extends Item implements Animatable, Syncable, SoundPlayer {
    private static final String CONTROLLER_NAME = "fist";
    private static final int ANIM_OPEN = 0;
    public AnimationFactory factory = GeckoLibUtil.createFactory(this);

    public FistItem(Properties pProperties) {
        super(pProperties.tab(GeckoLibMod.geckolibItemGroup));
        EyelibNetworkHandler.registerSyncable(this);
    }

    private <P extends Item & Animatable> PlayState predicate(AnimationEvent<P> event) {
        // Not setting an animation here as that's handled below
        return PlayState.CONTINUE;
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IItemRenderProperties() {
            private final BlockEntityWithoutLevelRenderer renderer = new FistRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimationData data) {
        var controller = new AnimationController<>(this, CONTROLLER_NAME, 20, this::predicate);

        // Registering a sound listener just makes it so when any sound keyframe is hit
        // the method will be called.
        // To register a particle listener or custom event listener you do the exact
        // same thing, just with registerParticleListener and
        // registerCustomInstructionListener, respectively.
        data.addAnimationController(controller);
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity player) {
        Level level = player.level;

        if (!level.isClientSide) {
            // Gets the item that the player is holding, should be a JackInTheBoxItem
            final ItemStack stack = player.getMainHandItem();
            final int id = GeckoLibUtil.guaranteeIDForStack(stack, (ServerLevel) level);
            // Tell all nearby clients to trigger this JackInTheBoxItem
            final PacketDistributor.PacketTarget target = PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player);
            EyelibNetworkHandler.syncAnimation(target, this, id, ANIM_OPEN);
        }

        return super.hurtEnemy(pStack, pTarget, player);
    }

    @Override
    public SoundPlayingState stopInAnimationFinished() {
        return SoundPlayingState.STOP_ON_NEXT;
    }

    @Override
    public void onAnimationSync(int id, int state) {
        if (state == ANIM_OPEN) {
            // Always use GeckoLibUtil to get AnimationControllers when you don't have
            // access to an AnimationEvent
            final AnimationController controller = GeckoLibUtil.getControllerForID(this.factory, id, CONTROLLER_NAME);

            if (controller.getAnimationState() == AnimationState.STOPPED) {
                // If you don't do this, the popup animation will only play once because the
                // animation will be cached.
                controller.markNeedsReload();
                // Set the animation to open the JackInTheBoxItem which will start playing music
                // and
                // eventually do the actual animation. Also sets it to not loop
                controller.setAnimation(new AnimationBuilder().addAnimation("po", LoopType.Impl.PLAY_ONCE));
            }
        }
    }

    @Override
    public SoundInstance getSound(ResourceLocation location) {
        return SoundPlayer.forEntity(Minecraft.getInstance().player, location);
    }
}
