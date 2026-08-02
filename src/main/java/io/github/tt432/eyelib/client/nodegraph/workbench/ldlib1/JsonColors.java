//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 语法着色（逐行轻量规则，非完整 tokenizer）。
 *
 * <p>规则与颜色取值沿用原 JsonTextPanel 规则（独立屏幕 2026-08-03 删除，
 * 不可修改，故在此复制算法）：行首引号串后跟 {@code ':'} 视为 key（白），其余字符串绿、
 * 数字青、{@code true/false/null} 橙、标点灰。pretty JSON 不存在跨行字符串，逐行处理安全。
 */
public final class JsonColors {
    public static final int KEY = 0xFFFFFFFF;
    public static final int STRING = 0xFF7FC97F;
    public static final int NUMBER = 0xFF55C4C4;
    public static final int LITERAL = 0xFFE0A050;
    public static final int PUNCT = 0xFFAAAAAA;
    public static final int PLAIN = 0xFFDDDDDD;

    private JsonColors() {
    }

    /**
     * 一段文本及其颜色（ARGB）。
     */
    public record Segment(String text, int color) {
    }

    /** 把多行 JSON 文本着色为「行 → 段列表」。 */
    public static List<List<Segment>> toLines(String text) {
        String[] rawLines = text.split("\n", -1);
        List<List<Segment>> lines = new ArrayList<>(rawLines.length);
        for (String raw : rawLines) {
            lines.add(tokenize(raw));
        }
        return lines;
    }

    static List<Segment> tokenize(String line) {
        List<Segment> segments = new ArrayList<>();
        int length = line.length();
        int i = 0;
        // 行首缩进
        while (i < length && line.charAt(i) == ' ') {
            i++;
        }
        if (i > 0) {
            segments.add(new Segment(line.substring(0, i), PLAIN));
        }
        // key：引号串后跟 ':'
        if (i < length && line.charAt(i) == '"') {
            int stringEnd = stringEnd(line, i);
            int colon = stringEnd;
            while (colon < length && line.charAt(colon) == ' ') {
                colon++;
            }
            if (colon < length && line.charAt(colon) == ':') {
                segments.add(new Segment(line.substring(i, stringEnd), KEY));
                segments.add(new Segment(line.substring(stringEnd, colon + 1), PUNCT));
                i = colon + 1;
            }
        }
        // 行内其余 token
        while (i < length) {
            char c = line.charAt(i);
            if (c == '"') {
                int end = stringEnd(line, i);
                segments.add(new Segment(line.substring(i, end), STRING));
                i = end;
            } else if (c == ' ') {
                int end = i;
                while (end < length && line.charAt(end) == ' ') {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), PLAIN));
                i = end;
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                int end = i + 1;
                while (end < length && isNumberChar(line.charAt(end))) {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), NUMBER));
                i = end;
            } else if (Character.isLetter(c)) {
                int end = i + 1;
                while (end < length && Character.isLetter(line.charAt(end))) {
                    end++;
                }
                String word = line.substring(i, end);
                boolean literal = word.equals("true") || word.equals("false") || word.equals("null");
                segments.add(new Segment(word, literal ? LITERAL : PLAIN));
                i = end;
            } else {
                segments.add(new Segment(line.substring(i, i + 1), PUNCT));
                i++;
            }
        }
        return segments;
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
            } else if (c == '"') {
                return i + 1;
            } else {
                i++;
            }
        }
        return line.length();
    }
}
//?}
