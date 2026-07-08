package io.github.tt432.eyelib.bridge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
//? if <26.1 {
import net.minecraft.world.level.GameRules;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * AI 调试 HTTP 服务器（{@code common/debug/AIDebugServer}）所用的版本特定 MC 客户端操作的封装。
 *
 * <p>{@code common/debug} 不在 StonecutterCommentPlacementTest 白名单（白名单为 {@code debug/}，
 * 即 {@code io.github.tt432.eyelib.debug}），故版本差异必须收敛到本 ACL(bridge) 中：
 * <ul>
 *   <li>{@link #executeServerCommand} —— {@code performPrefixedCommand} 返回 int(legacy) / void 的差异</li>
 *   <li>{@link #createFlatLevel} —— {@code LevelSettings} 构造器与 {@code createFreshLevel} 签名跨 3 版本差异</li>
 *   <li>{@link #dimensionString} —— {@code ResourceKey.location()} vs 直接 toString 的差异</li>
 *   <li>{@link #minecraftVersion} —— 运行版本字符串</li>
 * </ul>
 * 主线程调度（{@code mc.tell}/{@code mc.submit}）由 {@link ClientTaskPort} 统一处理。
 *
 * @author TT432
 */
public interface DebugServerPort {

    /**
     * 在集成服务器上以本地玩家身份执行命令，返回人类可读的结果描述。
     */
    static String executeServerCommand(Minecraft mc, String cmd) {
        var integrated = mc.getSingleplayerServer();
        if (integrated == null) return "No integrated server available (singleplayer only)";
        if (mc.player == null) return "No local player available";

        var serverPlayer = integrated.getPlayerList().getPlayer(mc.player.getUUID());
        var source = serverPlayer != null
                ? serverPlayer.createCommandSourceStack()
                : integrated.createCommandSourceStack();

        //? if legacy {
        int n = integrated.getCommands().performPrefixedCommand(source, cmd);
        return "executed on server (result=" + n + ")";
        //?} else {
        integrated.getCommands().performPrefixedCommand(source, cmd);
        return "executed on server";
        //?}
    }

    /**
     * 以超平坦预设创建并进入指定名称的单人世界。
     */
    static void createFlatLevel(Minecraft mc, String name) {
        //? if <1.20.6 {
        LevelSettings levelSettings = new LevelSettings(
                name, GameType.CREATIVE, false, Difficulty.NORMAL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        WorldOptions worldOptions = new WorldOptions(0L, true, false);
        mc.createWorldOpenFlows().createFreshLevel(
                name, levelSettings, worldOptions,
                registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                    .getHolderOrThrow(WorldPresets.FLAT)
                                    .value()
                                    .createWorldDimensions());
        //?} elif <26.1 {
        LevelSettings levelSettings = new LevelSettings(
                name, GameType.CREATIVE, false, Difficulty.NORMAL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        WorldOptions worldOptions = new WorldOptions(0L, true, false);
        mc.createWorldOpenFlows().createFreshLevel(
                name, levelSettings, worldOptions,
                registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                    .getHolderOrThrow(WorldPresets.FLAT)
                                    .value()
                                    .createWorldDimensions(),
                null);
        //?} else {
        LevelSettings levelSettings = new LevelSettings(
                name, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false),
                true, WorldDataConfiguration.DEFAULT);
        WorldOptions worldOptions = new WorldOptions(0L, true, false);
        mc.createWorldOpenFlows().createFreshLevel(
                name, levelSettings, worldOptions,
                registry -> registry.lookupOrThrow(Registries.WORLD_PRESET)
                                    .getOrThrow(WorldPresets.FLAT)
                                    .value()
                                    .createWorldDimensions(),
                new net.minecraft.client.gui.screens.GenericMessageScreen(
                        net.minecraft.network.chat.Component.translatable("selectWorld.data_read")));
        //?}
    }

    /**
     * @return 给定世界的维度标识字符串（{@code "namespace:path"}）
     */
    static String dimensionString(Level level) {
        //? if <26.1 {
        return level.dimension().location().toString();
        //?} else {
        return level.dimension().toString();
        //?}
    }

    /**
     * @return 当前 Stonecutter 版本节点的可读版本字符串
     */
    static String minecraftVersion() {
        //? if <1.20.6 {
        return "1.20.1";
        //?} elif <26.1 {
        return "1.21.1";
        //?} else {
        return "26.1.2";
        //?}
    }
}
