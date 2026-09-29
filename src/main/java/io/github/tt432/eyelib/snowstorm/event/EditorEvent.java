package io.github.tt432.eyelib.snowstorm.event;

/**
 * EventList.vue 中 {@code group.events} 的条目（event_entry）：{uuid, id, event}。
 *
 * <p>JS 中 {@code Config.events[id]} 与 {@code event_entry.event} 是同一对象引用；
 * Java 侧由 {@link EventList} 在每次 modify 时把 {@link EventSubpart#toJson()} 回写 Config.events。</p>
 */
public class EditorEvent {

    /** 编辑器侧 id（guid()，addEvent/import 回填时生成）。 */
    public String uuid = "";
    /** 事件 id（Config.events 的键）。 */
    public String id = "";
    /** 事件定义（递归 subpart 根）。 */
    public EventSubpart event = new EventSubpart();
}
