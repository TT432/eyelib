package io.github.tt432.eyelib.mixin.attachment;

import io.github.tt432.eyelib.attachment.network.UpdateDestroyInfoPacket;
import io.github.tt432.eyelib.network.EyelibNetworkTransport;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if <1.20.6 {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
//?} else {
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}
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

    @Inject(method = "startDestroyBlock", at = {@At(value = "RETURN", ordinal = 0), @At(value = "RETURN", ordinal = 1)})
    private void startDestroyBlock1(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(false));
    }

    @Inject(method = "startDestroyBlock", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I"))
    private void startDestroyBlock2(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(true));

        eyelib$scheduleNextTick(() -> EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(false)));
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void stopDestroyBlock(CallbackInfo ci) {
        if (isDestroying) {
            EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(false));
        }
    }

    @Unique
    boolean eyelib$destroyedBlock;

    @Inject(method = "continueDestroyBlock", at = @At(value = "FIELD", ordinal = 1, opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying:Z"))
    private void continueDestroyBlock1(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        eyelib$destroyedBlock = true;
        EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(false));
    }

    @Inject(method = "continueDestroyBlock", at = {
            @At(value = "RETURN", ordinal = 1),
    })
    private void continueDestroyBlock2(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(true));

        eyelib$scheduleNextTick(() -> EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(false)));
    }

    @Inject(method = "continueDestroyBlock", at = {
            @At(value = "RETURN", ordinal = 2),
            @At(value = "RETURN", ordinal = 3),
            @At(value = "RETURN", ordinal = 4),
    })
    private void continueDestroyBlock(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        if (eyelib$destroyedBlock) {
            eyelib$destroyedBlock = false;
        } else {
            EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(cir.getReturnValue()));
        }
    }

    @Unique
    private static void eyelib$scheduleNextTick(Runnable task) {
        var executed = new boolean[]{false};
        //? if <1.20.6 {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
        //?} else {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
        //?}
            //? if <1.20.6 {
            if (!executed[0] && event.phase == TickEvent.Phase.END) {
            //?} else {
            if (!executed[0]) {
            //?}
                executed[0] = true;
                task.run();
            }
        });
    }
}
