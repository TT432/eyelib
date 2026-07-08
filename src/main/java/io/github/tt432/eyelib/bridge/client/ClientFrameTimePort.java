package io.github.tt432.eyelib.bridge.client;

import net.minecraft.client.Minecraft;

/**
 * 客户端帧时间查询 Port，屏蔽不同版本间获取帧时间/部分刻度的 API 差异。
 *
 * @author TT432
 */
public interface ClientFrameTimePort {

    static float getFrameTime() {
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
