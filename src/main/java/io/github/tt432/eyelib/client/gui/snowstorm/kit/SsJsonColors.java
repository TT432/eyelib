package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 语法着色（common.css Prism 段 as-is）：punctuation #5ba8c5、string #94e400（绿）、
 * number/boolean #b99cff（紫）、key 白 text 色。
 *
 * <p>用法：{@code SsJsonColors.highlightLine(line)} → 着色 Component（CodeViewer 换用）。
 * 轻量逐行 tokenizer（引号字符串/数字/true/false/null/标点），非完整 Prism。
 */
public final class SsJsonColors {

    private static final int PUNCTUATION = 0xFF5BA8C5;
    private static final int STRING = 0xFF94E400;
    private static final int NUMBER = 0xFFB99CFF;
    private static final int TEXT = 0xFFbcc3ca;

    private SsJsonColors() {
    }

    /** 逐行 JSON 着色：返回多段着色 Component。 */
    public static Component highlightLine(String line) {
        MutableComponent out = Component.empty();
        StringBuilder cur = new StringBuilder();
        int state = 0; // 0=普通 1=字符串内
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (state == 1) {
                cur.append(c);
                if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    out.append(flush(cur, STRING));
                    state = 0;
                }
            } else if (c == '"') {
                out.append(flush(cur, TEXT));
                cur.append(c);
                state = 1;
            } else if ("{}[],:".indexOf(c) >= 0) {
                out.append(flush(cur, TEXT));
                out.append(Component.literal(String.valueOf(c)).withStyle(Style.EMPTY.withColor(PUNCTUATION)));
            } else {
                cur.append(c);
            }
        }
        out.append(flush(cur, state == 1 ? STRING : TEXT));
        return out;
    }

    private static MutableComponent flush(StringBuilder cur, int fallbackColor) {
        if (cur.length() == 0) return Component.empty();
        String s = cur.toString();
        cur.setLength(0);
        int color = fallbackColor;
        if (fallbackColor == TEXT) {
            String t = s.trim();
            if (t.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?") || t.equals("true") || t.equals("false")
                    || t.equals("null")) {
                color = NUMBER;
            }
        }
        return Component.literal(s).withStyle(Style.EMPTY.withColor(color));
    }
}
//?}
