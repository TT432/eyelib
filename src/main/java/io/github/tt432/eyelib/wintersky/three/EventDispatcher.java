package io.github.tt432.eyelib.wintersky.three;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * three.js r134 EventDispatcher 移植（Object3D added/removed 事件依赖）。
 */
public class EventDispatcher {

    /** three.js 事件对象最小复刻：仅 type 字段被使用。 */
    public static final class Event {
        public final String type;
        public @Nullable Object target;

        public Event(String type) {
            this.type = type;
        }
    }

    private final Map<String, List<Consumer<Event>>> listenerMap = new HashMap<>();

    public void addEventListener(String type, Consumer<Event> listener) {
        List<Consumer<Event>> list = listenerMap.computeIfAbsent(type, k -> new ArrayList<>());
        if (!list.contains(listener)) {
            list.add(listener);
        }
    }

    public boolean hasEventListener(String type, Consumer<Event> listener) {
        List<Consumer<Event>> list = listenerMap.get(type);
        return list != null && list.contains(listener);
    }

    public void removeEventListener(String type, Consumer<Event> listener) {
        List<Consumer<Event>> list = listenerMap.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void dispatchEvent(Event event) {
        List<Consumer<Event>> list = listenerMap.get(event.type);

        if (list != null) {
            event.target = this;
            // Make a copy, in case listeners are removed while iterating.
            List<Consumer<Event>> copy = new ArrayList<>(list);
            for (Consumer<Event> listener : copy) {
                listener.accept(event);
            }
            event.target = null;
        }
    }
}
