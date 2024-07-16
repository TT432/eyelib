package io.github.tt432.eyelib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.util.ResourceLocations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * @author TT432
 */
@EventBusSubscriber
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibCommands {
    @SubscribeEvent
    public static void onEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("eyelib")
                .requires(s -> s.hasPermission(2))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("setModel")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("model", StringArgumentType.string())
                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("texture", StringArgumentType.string())
                                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("renderType", StringArgumentType.string())
                                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("visitors", StringArgumentType.string())
                                                        .executes(context -> {
                                                            Entity entity = context.getSource().getEntity();

                                                            if (entity == null) return -1;

                                                            RenderData<?> data = RenderData.getComponent(entity);
                                                            ModelComponent modelComponent = data.getModelComponent();

                                                            modelComponent.setInfo(new ModelComponent.SerializableInfo(
                                                                    ResourceLocations.of(StringArgumentType.getString(context, "model")),
                                                                    ResourceLocations.of(StringArgumentType.getString(context, "texture")),
                                                                    ResourceLocations.of(StringArgumentType.getString(context, "renderType")),
                                                                    List.of(ResourceLocations.of(StringArgumentType.getString(context, "visitors")))
                                                            ));

                                                            data.sync();
                                                            return 0;
                                                        })))))));
    }
}
