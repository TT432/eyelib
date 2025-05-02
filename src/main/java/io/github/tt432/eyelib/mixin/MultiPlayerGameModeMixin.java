package io.github.tt432.eyelib.mixin;

import io.github.tt432.eyelib.network.UpdateDestroyInfoPacket;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author TT432
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow
    private boolean isDestroying;

    @Inject(method = "lambda$startDestroyBlock$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private void startDestroyBlock1(BlockState blockstate1, PlayerInteractEvent.LeftClickBlock event, BlockPos loc, Direction face, int p_233728_, CallbackInfoReturnable<Packet> cir) {
        PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(false));
    }

    @Inject(method = "lambda$startDestroyBlock$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;destroyBlockProgress(ILnet/minecraft/core/BlockPos;I)V"))
    private void startDestroyBlock2(BlockState blockstate1, PlayerInteractEvent.LeftClickBlock event, BlockPos loc, Direction face, int p_233728_, CallbackInfoReturnable<Packet> cir) {
        PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(true));
    }

    @Inject(method = "startDestroyBlock", at = {@At(value = "RETURN", ordinal = 0), @At(value = "RETURN", ordinal = 1)})
    private void startDestroyBlock1(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(false));
    }

    @Inject(method = "startDestroyBlock", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I"))
    private void startDestroyBlock2(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(true));

        NeoForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onEvent(ClientTickEvent.Pre event) {
                PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(false));
                NeoForge.EVENT_BUS.unregister(this);
            }
        });
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void stopDestroyBlock(CallbackInfo ci) {
        if (isDestroying) {
            PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(false));
        }
    }

    @Unique
    boolean eyelib$destroyedBlock;

    @Inject(method = "continueDestroyBlock", at = @At(value = "FIELD", ordinal = 1, opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying:Z"))
    private void continueDestroyBlock1(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        eyelib$destroyedBlock = true;
        PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(false));
    }

    @Inject(method = "continueDestroyBlock", at = {
            @At(value = "RETURN", ordinal = 1),
    })
    private void continueDestroyBlock2(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(true));
        NeoForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onEvent(ClientTickEvent.Pre event) {
                PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(false));
                NeoForge.EVENT_BUS.unregister(this);
            }
        });
    }

    @Inject(method = "continueDestroyBlock", at = {
            @At(value = "RETURN", ordinal = 2),
            @At(value = "RETURN", ordinal = 3),
            @At(value = "RETURN", ordinal = 4),
    })
    private void continueDestroyBlock(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        if (eyelib$destroyedBlock) eyelib$destroyedBlock = false;
        else PacketDistributor.sendToServer(new UpdateDestroyInfoPacket(cir.getReturnValue()));
    }
}
