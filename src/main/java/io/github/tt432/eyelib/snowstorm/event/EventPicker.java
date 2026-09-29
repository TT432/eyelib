package io.github.tt432.eyelib.snowstorm.event;

import io.github.tt432.eyelib.wintersky.Config;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * EventPicker.vue 的模型逻辑 as-is 移植：从 Config.events 选事件 id。
 * 菜单开合/点击外部关闭/select $emit 属于 UI，不移植。
 */
public class EventPicker {

    private final Config config;

    /** JS props.blacklist。 */
    public @Nullable List<String> blacklist;

    public EventPicker(Config config) {
        this.config = config;
    }

    /**
     * getEventIDs as-is：{@code Object.keys(Config.events)}（插入序），
     * blacklist 为数组时过滤 {@code !blacklist.includes(id)}。
     */
    public List<String> getEventIDs() {
        List<String> ids = new ArrayList<>(config.events.keySet());
        List<String> bl = blacklist;
        if (bl != null) {
            ids.removeIf(bl::contains);
        }
        return ids;
    }
}
