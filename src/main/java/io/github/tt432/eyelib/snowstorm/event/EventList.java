package io.github.tt432.eyelib.snowstorm.event;

import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.Config;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EventList.vue 的模型/数据操作逻辑 as-is 移植（事件定义侧；UI 不移植）。
 *
 * <p>对应 JS {@code Data.events.events} 分组：{@code events} 列表 + 镜像到
 * {@code Config.events}（wintersky 扁平 map）。JS 靠对象引用保持同步，
 * Java 侧在每次 modify 时把 entry.event.toJson() 回写对应键；
 * rename 的删旧键+插新键顺序 quirk（新键移到 map 末尾）按 JS 保留。</p>
 *
 * <p>事件 timeline 结构（event_timeline Input）归 input.js 切片，此处不含。</p>
 */
public class EventList {

    /** JS {@code group.events}。 */
    public final List<EditorEvent> events = new ArrayList<>();

    private final Config config;

    public EventList(Config config) {
        this.config = config;
    }

    /**
     * renameEvent as-is：删旧键、插新键（移到 map 末尾）、更新 entry.id。
     * JS quirk：新旧 id 相同时照样删除再插入（键移到末尾）。
     */
    public void renameEvent(EditorEvent entry, String value) {
        config.events.remove(entry.id);
        entry.id = value;
        config.events.put(value, entry.event.toJson());
        modifyEvent(null, null);
    }

    /** addEvent as-is：id 从 'event' 起递增避让（event, event1, event2…），查重只看本列表。 */
    public EditorEvent addEvent() {
        String originalId = "event";
        String id = originalId;
        int i = 0;
        while (findById(id) != null) {
            i++;
            id = originalId + i;
        }
        EditorEvent entry = new EditorEvent();
        entry.uuid = SnowstormUtil.guid();
        entry.id = id;
        entry.event.modify_listener = (event, type) -> {
            config.events.put(entry.id, entry.event.toJson());
            modifyEvent(event, type);
        };
        config.events.put(id, entry.event.toJson());
        events.add(entry);
        modifyEvent(null, null);
        return entry;
    }

    /** removeEvent as-is。 */
    public void removeEvent(EditorEvent entry) {
        events.remove(entry);
        config.events.remove(entry.id);
        modifyEvent(null, null);
    }

    /** 拖拽排序（sort.js → 模型方法）：见 {@link Sort#move}。JS sort() 结束后不触发 registerEdit。 */
    public boolean move(int originalIndex, int hoverIndex) {
        return Sort.move(events, originalIndex, hoverIndex);
    }

    /**
     * modifyEvent as-is：{@code registerEdit('edit event', event, type == 'text')}。
     * 同时是所有嵌套 subpart modify_event 冒泡的终点。
     * （Java 侧 registerEdit 无 DOM event 参数，typing 合并由 cooldown 位表达。）
     */
    public void modifyEvent(@Nullable Object event, @Nullable String type) {
        EditHistory.registerEdit("edit event", "text".equals(type));
    }

    public @Nullable EditorEvent findById(String id) {
        for (EditorEvent entry : events) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    /**
     * import.js 事件定义侧回填 as-is（import.js L63-70）：
     * 清空列表，按 Config.events 的键序重建 {uuid: guid(), id, event}。
     */
    public void updateFromConfig() {
        events.clear();
        for (Map.Entry<String, Object> e : config.events.entrySet()) {
            EditorEvent entry = new EditorEvent();
            entry.uuid = SnowstormUtil.guid();
            entry.id = e.getKey();
            entry.event = EventSubpart.fromJson(e.getValue());
            entry.event.modify_listener = (event, type) -> {
                config.events.put(entry.id, entry.event.toJson());
                modifyEvent(event, type);
            };
            propagateListener(entry.event);
            events.add(entry);
        }
    }

    /** 给整棵已有 subpart 树补冒泡接缝（import 后嵌套 option 的编辑也要冒泡到本组）。 */
    private void propagateListener(EventSubpart subpart) {
        List<EventSubpart> sequence = subpart.sequence;
        if (sequence != null) {
            for (EventSubpart option : sequence) {
                option.modify_listener = subpart.modify_listener;
                propagateListener(option);
            }
        }
        List<EventSubpart> randomize = subpart.randomize;
        if (randomize != null) {
            for (EventSubpart option : randomize) {
                option.modify_listener = subpart.modify_listener;
                propagateListener(option);
            }
        }
    }
}
