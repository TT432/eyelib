package io.github.tt432.eyelib.bridge.behavior.adapter;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.tt432.eyelib.behavior.BehaviorEntityRegistry;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.capability.EyelibAttachableData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//? if <1.20.6 {
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
//?}

/**
 * 命令队列执行适配器：每 tick 排空实体的 {@code queue_command} 待执行命令，
 * 以实体为命令源在逻辑服务端执行。
 * <p>
 * Bedrock {@code queue_command} 语义：命令在事件触发时入队、随后由实体执行；
 * 本适配器在实体 tick 时消费队列，命令源为实体自身（支持 {@code @s} 等相对选择器）。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber
//?} else {
@EventBusSubscriber(modid = "eyelib")
//?}
public final class CommandQueueTickAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandQueueTickAdapter.class);

    private CommandQueueTickAdapter() {
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onEvent(LivingEvent.LivingTickEvent event) {
    //?} else {
    public static void onEvent(EntityTickEvent.Post event) {
    //?}
        Entity entity = event.getEntity();
        // 无行为包时不存在 ENTITY_BEHAVIOR_DATA 附件与入队命令，全局短路。
        if (BehaviorEntityRegistry.isEmpty()) return;
        // 仅逻辑服务端执行命令：客户端 tick（含单机集成服务器共享对象）不消费队列，
        // 避免命令重复执行。
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel)) return;

        EntityBehaviorData data = DataAttachmentHelper.getOrNull(EyelibAttachableData.ENTITY_BEHAVIOR_DATA.get(), entity);
        if (data == null) return;

        List<String> commands = data.drainCommands();
        if (commands.isEmpty()) return;

        //? if <26.1 {
        MinecraftServer server = entity.getServer();
        //?} else {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) entity.level()).getServer();
        //?}
        if (server == null) return;

        //? if <26.1 {
        var source = entity.createCommandSourceStack();
        //?} else {
        var source = entity.createCommandSourceStackForNameResolution(
                (net.minecraft.server.level.ServerLevel) entity.level());
        //?}
        for (String cmd : commands) {
            try {
                server.getCommands().getDispatcher().execute(cmd, source);
            } catch (CommandSyntaxException e) {
                LOGGER.warn("queue_command 命令执行失败: '{}' — {}", cmd, e.getMessage());
            }
        }
    }
}
