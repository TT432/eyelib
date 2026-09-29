package io.github.tt432.eyelib.wintersky;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * wintersky event_class.js 逐字移植：简单事件分发。
 */
public class EventClass {

    private final Map<String, List<Consumer<Object>>> events = new LinkedHashMap<>();

    public void dispatchEvent(String eventName, @Nullable Object data) {
        List<Consumer<Object>> list = events.get(eventName);
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            Consumer<Object> cb = list.get(i);
            if (cb != null) {
                cb.accept(data);
            }
        }
    }

    public void on(String eventName, Consumer<Object> cb) {
        events.computeIfAbsent(eventName, k -> new ArrayList<>()).add(cb);
    }

    public void removeEventListener(String eventName, Consumer<Object> cb) {
        List<Consumer<Object>> list = events.get(eventName);
        if (list != null) {
            MathUtil.removeFromArray(list, cb);
        }
    }
}
