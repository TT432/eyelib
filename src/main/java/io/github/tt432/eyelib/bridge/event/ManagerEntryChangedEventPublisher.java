package io.github.tt432.eyelib.bridge.event;

import io.github.tt432.eyelib.bridge.event.adapter.ManagerEntryChangedEvent;

import java.util.function.Consumer;
import io.github.tt432.eyelib.bridge.event.ManagerEventPort;

/**
 * 管理器条目变更事件 Port，封装 Forge/NeoForge 事件总线差异。
 *
 * @author TT432
 */
public interface ManagerEntryChangedEventPublisher {
    public static void addListener(Consumer<ManagerEventPort> listener) {
        //? if <1.20.6 {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(event -> listener.accept(event));
        //?} else {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(event -> listener.accept(event));
        //?}
    }
}

