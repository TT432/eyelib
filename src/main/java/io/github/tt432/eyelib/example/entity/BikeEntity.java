package io.github.tt432.eyelib.example.entity;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.PlayState;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationController;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.animation.builder.AnimationBuilder;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationFactory;
import io.github.tt432.eyelib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import static io.github.tt432.eyelib.api.bedrock.animation.LoopType.LOOP;

public class BikeEntity extends Animal implements Animatable {
    public AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private <E extends Animatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bike.idle", LOOP));
        return PlayState.CONTINUE;
    }

    public BikeEntity(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
        this.noCulling = true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isVehicle()) {
            player.startRiding(this);
            return super.mobInteract(player, hand);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
    }

    @Override
    public void travel(Vec3 pos) {
        if (this.isAlive()) {
            if (this.isVehicle()) {
                LivingEntity livingentity = (LivingEntity) this.getControllingPassenger();
                this.setYRot(livingentity.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(livingentity.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;
                float f = livingentity.xxa * 0.5F;
                float f1 = livingentity.zza;
                if (f1 <= 0.0F) {
                    f1 *= 0.25F;
                }

                this.setSpeed(0.3F);
                super.travel(new Vec3(f, pos.y, f1));
            }
        }
    }

    @Nullable
    public Entity getControllingPassenger() {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }

    @Override
    public boolean canBeControlledByRider() {
        return true;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<BikeEntity>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_241840_1_, AgeableMob p_241840_2_) {
        return null;
    }

}
