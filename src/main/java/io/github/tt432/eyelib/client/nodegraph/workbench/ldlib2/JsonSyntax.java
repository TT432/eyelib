package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 轻量语法着色（逐行规则，非完整 tokenizer）：行首引号串后跟 {@code ':'} 视为 key（白），
 * 其余字符串绿、数字青、{@code true/false/null} 橙、标点灰。
 * 规则与颜色值复制自原 JsonTextPanel（独立屏幕 2026-08-03 删除，两版工作台各持一份）；
 * pretty JSON 不存在跨行字符串，逐行处理是安全的。
 */
final class JsonSyntax {
    private JsonSyntax() {
    }

    /**
     * 一段文本及其颜色。
     */
    record Segment(String text, int color) {
    }

    /** 单行 JSON → 着色片段序列。 */
    static List<Segment> tokenize(String line) {
        List<Segment> segments = new ArrayList<>();
        int length = line.length();
        int i = 0;
        // 行首缩进
        while (i < length && line.charAt(i) == ' ') {
            i++;
        }
        if (i > 0) {
            segments.add(new Segment(line.substring(0, i), WorkbenchColors.JSON_PLAIN));
        }
        // key：引号串后跟 ':'
        if (i < length && line.charAt(i) == '"') {
            int stringEnd = stringEnd(line, i);
            int colon = stringEnd;
            while (colon < length && line.charAt(colon) == ' ') {
                colon++;
            }
            if (colon < length && line.charAt(colon) == ':') {
                segments.add(new Segment(line.substring(i, stringEnd), WorkbenchColors.JSON_KEY));
                segments.add(new Segment(line.substring(stringEnd, colon + 1), WorkbenchColors.JSON_PUNCT));
                i = colon + 1;
            }
        }
        // 行内其余 token
        while (i < length) {
            char c = line.charAt(i);
            if (c == '"') {
                int end = stringEnd(line, i);
                segments.add(new Segment(line.substring(i, end), WorkbenchColors.JSON_STRING));
                i = end;
            } else if (c == ' ') {
                int end = i;
                while (end < length && line.charAt(end) == ' ') {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), WorkbenchColors.JSON_PLAIN));
                i = end;
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                int end = i + 1;
                while (end < length && isNumberChar(line.charAt(end))) {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), WorkbenchColors.JSON_NUMBER));
                i = end;
            } else if (Character.isLetter(c)) {
                int end = i + 1;
                while (end < length && Character.isLetter(line.charAt(end))) {
                    end++;
                }
                String word = line.substring(i, end);
                boolean literal = word.equals("true") || word.equals("false") || word.equals("null");
                segments.add(new Segment(word, literal ? WorkbenchColors.JSON_LITERAL : WorkbenchColors.JSON_PLAIN));
                i = end;
            } else {
                segments.add(new Segment(line.substring(i, i + 1), WorkbenchColors.JSON_PUNCT));
                i++;
            }
        }
        return segments;
    }

    /** 单行 JSON → 着色 Component（片段颜色写入 {@link Style#withColor}）。 */
    static Component highlight(String line) {
        MutableComponent component = Component.empty();
        for (Segment segment : tokenize(line)) {
            component.append(Component.literal(segment.text())
                    .withStyle(Style.EMPTY.withColor(segment.color())));
        }
        return component;
    }

    private static boolean isNumberChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E';
    }

    /**
     * 返回从 start（{@code '"'} 处）开始的字符串结束索引（闭引号之后），未闭合时返回行尾。
     */
    private static int stringEnd(String line, int start) {
        int i = start + 1;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return line.length();
    }
}
//?}
