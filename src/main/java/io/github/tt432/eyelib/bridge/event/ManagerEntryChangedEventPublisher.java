package io.github.tt432.eyelib.bridge.event;

import java.util.function.Consumer;

/**
 * 管理器条目变更事件 Port，封装 Forge/NeoForge 事件总线差异。
 *
 * @author TT432
 */
public final class ManagerEntryChangedEventPublisher {
    private ManagerEntryChangedEventPublisher() {
    }

    public static void addListener(Consumer<ManagerEntryChangedEvent> listener) {
        //? if <1.20.6 {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(listener);
        //?} else {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(listener);
        //?}
    }
}
