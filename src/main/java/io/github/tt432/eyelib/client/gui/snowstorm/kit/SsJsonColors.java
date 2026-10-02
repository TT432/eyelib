package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 语法着色（CodeViewer.vue：prism-okaidia + 覆盖 as-is）——
 * property/key #7bcbf0、boolean/number #ff6868、string okaidia #e6db74、punctuation #f8f8f2。
 *
 * <p>用法：{@code SsJsonColors.highlightLine(line)} → 着色 Component（CodeViewer 换用）。
 * 轻量逐行 tokenizer（引号字符串/数字/true/false/null/标点；字符串后接 ':' 视为 key），
 * 非完整 Prism。
 */
public final class SsJsonColors {

    private static final int PUNCTUATION = 0xFFF8F8F2;
    private static final int STRING = 0xFFE6DB74;
    private static final int NUMBER = 0xFFFF6868;
    private static final int PROPERTY = 0xFF7BCBF0;
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
                    // .token.property：字符串后（跳过空白）接 ':' 即 JSON key
                    int j = i + 1;
                    while (j < line.length() && (line.charAt(j) == ' ' || line.charAt(j) == '\t')) {
                        j++;
                    }
                    int stringColor = j < line.length() && line.charAt(j) == ':' ? PROPERTY : STRING;
                    String s = cur.toString();
                    cur.setLength(0);
                    out.append(Component.literal(s).withStyle(Style.EMPTY.withColor(stringColor)));
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
