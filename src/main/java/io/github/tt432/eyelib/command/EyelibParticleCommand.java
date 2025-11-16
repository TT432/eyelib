package io.github.tt432.eyelib.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.network.SpawnParticlePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber
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
        String remaining = builder.getRemaining().toLowerCase();
        Eyelib.getParticleManager().getAllData().keySet().forEach(k -> {
            String s = k.toLowerCase();
            if (!s.startsWith(remaining)) return;
            try {
                ResourceLocation.parse(k);
                builder.suggest(k);
            } catch (Exception ignored) {
            }
        });
        return builder.buildFuture();
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, Vec3 posArg) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            return 0;
        }
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "effect");

        Vec3 pos = posArg != null ? posArg : ctx.getSource().getPosition();
        Vector3f position = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);

        String spawnId = UUID.randomUUID().toString();
        PacketDistributor.sendToPlayer(player, new SpawnParticlePacket(spawnId, id, position));
        ctx.getSource().sendSuccess(() -> Component.literal("已生成粒子: " + id + " @ " + pos.x + ", " + pos.y + ", " + pos.z), false);
        return 1;
    }
}
