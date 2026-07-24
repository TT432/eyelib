package io.github.tt432.eyelib.bridge.client;

import net.minecraft.client.Minecraft;

/**
 * 客户端帧时序查询 Port，屏蔽不同版本间获取 partial tick / delta ticks 的 API 差异。
 *
 * <p><b>注意</b>：本 Port 提供的是<b>渲染插值系数</b>（partial tick / delta ticks，单位：刻），
 * 表示自上一渲染帧以来经过的游戏刻数，用于动画/渲染插值。它<b>不是</b>帧耗时（frame duration，
 * 如 {@code getFrameTimeNs()} 或帧间隔），性能采样场景禁止使用本 Port。
 *
 * @author TT432
 */
public interface ClientFrameTimePort {

    /**
     * 实时 delta ticks：自上一渲染帧以来经过的实时刻数（渲染插值系数，单位：刻）。
     * 与帧耗时无关，不随游戏暂停归零。
     */
    static float getRealtimeDeltaTicks() {
        //? if <1.20.6 {
        return Minecraft.getInstance().getFrameTime();
        //?} elif <26.1 {
        return Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        //?} else {
        return Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
        //?}
    }

    static float getGameTimeDeltaPartialTick() {
        //? if <1.20.6 {
        return Minecraft.getInstance().timer.partialTick;
        //?} elif <26.1 {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        //?} else {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        //?}
    }
}
