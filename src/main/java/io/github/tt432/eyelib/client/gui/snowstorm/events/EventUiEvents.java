//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.events;

import io.github.tt432.eyelib.snowstorm.input.Input;
import org.jspecify.annotations.Nullable;

/**
 * input.js change(e, ...) 的 DOM Event 接缝（本包私有；inputs 包的同类助手
 * InputUiEvents 为 package-private 不可复用，集成时可由 Main 统一）。
 */
final class EventUiEvents {

    /** JS InputEvent（文本键入，registerEdit 600ms typing 合并）。 */
    static Input.UiEvent typing() {
        return Typing.INSTANCE;
    }

    /** JS 普通 Event（click/change/blur 等非键入交互，registerEdit 立即派发）。 */
    static Input.UiEvent simple() {
        return Simple.INSTANCE;
    }

    private enum Typing implements Input.UiEvent {
        INSTANCE;

        @Override
        public String nodeName() {
            return "INPUT";
        }

        @Override
        public @Nullable String selectedOptionId() {
            return null;
        }

        @Override
        public Input.@Nullable UiFile firstFile() {
            return null;
        }

        @Override
        public boolean isTypingEvent() {
            return true;
        }
    }

    private enum Simple implements Input.UiEvent {
        INSTANCE;

        @Override
        public String nodeName() {
            return "DIV";
        }

        @Override
        public @Nullable String selectedOptionId() {
            return null;
        }

        @Override
        public Input.@Nullable UiFile firstFile() {
            return null;
        }
    }

    private EventUiEvents() {
    }
}
//?}
