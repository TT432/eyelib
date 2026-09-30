package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import io.github.tt432.eyelib.snowstorm.input.Input;
import org.jspecify.annotations.Nullable;

/**
 * input.js change(e, ...) 的 DOM Event 接缝实现（LDLib2 侧）：
 * JS 靠 {@code e instanceof InputEvent || KeyboardEvent} 判定 typing（registerEdit 600ms 合并），
 * LDLib2 无 DOM 事件层级，由控件按交互性质显式选择事件种类。
 */
final class InputUiEvents {

    /** JS InputEvent/KeyboardEvent（文本键入，typing 600ms 合并）。 */
    static Input.UiEvent typingEvent() {
        return TypingEvent.INSTANCE;
    }

    /** JS 普通 Event（click/change 等非键入交互，registerEdit 立即派发）。 */
    static Input.UiEvent simpleEvent() {
        return SimpleEvent.INSTANCE;
    }

    /** JS &lt;select&gt; 的 change 事件（{@code e.target.nodeName == 'SELECT'}）。 */
    static Input.UiEvent selectChangeEvent(String selectedKey) {
        return new SelectChangeEvent(selectedKey);
    }

    private enum TypingEvent implements Input.UiEvent {
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

    private enum SimpleEvent implements Input.UiEvent {
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

    private record SelectChangeEvent(String selectedKey) implements Input.UiEvent {
        @Override
        public String nodeName() {
            return "SELECT";
        }

        @Override
        public @Nullable String selectedOptionId() {
            return selectedKey;
        }

        @Override
        public Input.@Nullable UiFile firstFile() {
            return null;
        }
    }

    private InputUiEvents() {
    }
}
//?}
