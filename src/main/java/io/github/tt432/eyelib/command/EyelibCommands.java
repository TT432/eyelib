package io.github.tt432.eyelib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
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

                                                            if (data == null) return -1;

                                                            ModelComponent modelComponent = data.getModelComponent();

                                                            modelComponent.setInfo(new ModelComponent.SerializableInfo(
                                                                    new ResourceLocation(StringArgumentType.getString(context, "model")),
                                                                    new ResourceLocation(StringArgumentType.getString(context, "texture")),
                                                                    new ResourceLocation(StringArgumentType.getString(context, "renderType")),
                                                                    List.of(new ResourceLocation(StringArgumentType.getString(context, "visitors")))
                                                            ));

                                                            data.sync();
                                                            return 0;
                                                        })))))));
    }
}
