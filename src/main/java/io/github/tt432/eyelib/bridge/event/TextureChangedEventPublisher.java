package io.github.tt432.eyelib.bridge.event;

import io.github.tt432.eyelib.bridge.event.adapter.TextureChangedEvent;

/**
 * 纹理变更事件发布 Port，封装 Forge/NeoForge 事件总线差异。
 *
 * @author TT432
 */
public interface TextureChangedEventPublisher {
    public static void post() {
        //? if <1.20.6 {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new TextureChangedEvent());
        //?} else {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new TextureChangedEvent());
        //?}
    }
}

