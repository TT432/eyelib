package io.github.tt432.eyelib.ui;

import java.util.function.Consumer;

/**
 * MC 无关的文本输入框接口。
 *
 * @author TT432
 */
public interface UITextField extends UIWidget {
    String getValue();

    void setValue(String value);

    void setResponder(Consumer<String> responder);

    void setHint(String text);

    void setMaxLength(int max);

    void setBordered(boolean bordered);

    void setCanLoseFocus(boolean canLoseFocus);
}
