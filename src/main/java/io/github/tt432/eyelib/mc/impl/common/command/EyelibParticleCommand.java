package io.github.tt432.eyelib.mc.impl.common.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.tt432.eyelib.client.particle.ParticleLookup;
import io.github.tt432.eyelib.common.runtime.ParticleCommandRuntime;
import io.github.tt432.eyelib.mc.impl.network.EyelibNetworkTransport;
import io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber
public final class EyelibParticleCommand {
    @SubscribeEvent
    public static void onRegister(final RegisterCommandsEvent event) {
        event.getDispatcher().register(root());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("eyelib")
                .then(particle());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> particle() {
        return Commands.literal("particle")
                .then(Commands.argument("effect", ResourceLocationArgument.id())
                        .suggests(EyelibParticleCommand::suggestEffects)
                        .executes(ctx -> execute(ctx, null))
                        .then(Commands.argument("position", Vec3Argument.vec3())
                                .executes(ctx -> execute(ctx, Vec3Argument.getVec3(ctx, "position")))
                        )
                );
    }

    private static CompletableFuture<Suggestions> suggestEffects(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ParticleCommandRuntime.suggestEffectIds(builder.getRemaining(), ParticleLookup.names(), EyelibParticleCommand::isValidResourceLocation)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static boolean isValidResourceLocation(String id) {
        try {
            new ResourceLocation(id);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, @Nullable Vec3 posArg) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            return 0;
        }
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "effect");

        Vec3 pos = posArg != null ? posArg : ctx.getSource().getPosition();
        ParticleCommandRuntime.SpawnParticleRequest request = ParticleCommandRuntime.buildSpawnParticleRequest(
                id.toString(),
                pos.x,
                pos.y,
                pos.z,
                () -> UUID.randomUUID().toString()
        );

        EyelibNetworkTransport.sendToPlayer(player, new SpawnParticlePacket(
                request.spawnId(),
                request.particleId(),
                new Vector3f(request.x(), request.y(), request.z())
        ));
        ctx.getSource().sendSuccess(() -> Component.literal(ParticleCommandRuntime.spawnSuccessMessage(request)), false);
        return 1;
    }
}

