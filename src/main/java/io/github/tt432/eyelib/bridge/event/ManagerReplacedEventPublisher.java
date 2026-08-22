package io.github.tt432.eyelib.bridge.event;

import io.github.tt432.eyelib.bridge.event.adapter.ManagerReplacedEvent;

import java.util.function.Consumer;

/**
 * 管理器批量替换事件 Port，封装 Forge/NeoForge 事件总线差异。
 * 整表替换（replaceAll/clear）与批量叠加（putAll/removeAll）均触发；
 * 订阅者按 managerName 过滤后做整体失效，无需逐条事件。
 *
 * @author TT432
 */
public interface ManagerReplacedEventPublisher {
    static void addListener(Consumer<String> listener) {
        //? if <1.20.6 {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.<ManagerReplacedEvent>addListener(event -> listener.accept(event.getManagerName()));
        //?} else {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.<ManagerReplacedEvent>addListener(event -> listener.accept(event.getManagerName()));
        //?}
    }
}
