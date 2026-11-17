package io.github.tt432.eyelib.util.registry;

import org.jspecify.annotations.Nullable;

/**
 * 注册表变更事件（G5）。
 * <p>
 * 为什么需要类型化事件：既有通知通道只有 Forge/NeoForge 事件总线上的弱类型载荷
 * （{@code managerName} 字符串 + {@code Object entryData}），订阅者必须按管理器名做字符串比较，
 * 批量替换（资源重载/addon 叠加）更是没有明细。本类型让「按注册表订阅」在 domain 层就能完成，
 * 也让应用层能做类型化过滤。
 * <p>
 * 线程：回调在**触发写入的线程**上同步执行（无队列、无 executor）。资源重载路径在客户端主线程，
 * 因此监听器内不要做重活；需要跳线程的订阅者自行处理。
 *
 * @param kind       变更种类
 * @param id         条目 id；{@link Kind#BULK_REPLACED} 为 null
 * @param value      变更后的值；{@link Kind#ENTRY_REMOVED} 与 {@link Kind#BULK_REPLACED} 为 null
 * @param generation 变更后的注册表代际（单调递增）
 * @param snapshot   变更后的完整快照（批量事件据此整表重读）
 * @author TT432
 */
public record RegistryChange<T>(
        Kind kind,
        @Nullable String id,
        @Nullable T value,
        long generation,
        RegistrySnapshot<T> snapshot
) {

    public enum Kind {
        /** 单条写入/覆盖。 */
        ENTRY_PUT,
        /** 单条移除。 */
        ENTRY_REMOVED,
        /** 批量替换（{@code putAll}/{@code replaceAll}/{@code removeAll}/{@code clear}），无逐条明细。 */
        BULK_REPLACED
    }
}
