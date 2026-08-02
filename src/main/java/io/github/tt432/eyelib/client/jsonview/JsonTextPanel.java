package io.github.tt432.eyelib.client.jsonview;

import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScrollPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 查看器右侧的 JSON 文本面板：逐行渲染，带轻量语法着色。
 *
 * <p>着色为逐行的轻量规则（非完整 tokenizer）：行首引号串后跟 {@code ':'} 视为 key（白），
 * 其余字符串绿、数字青、{@code true/false/null} 橙、标点灰。pretty JSON 不存在跨行字符串，
 * 因此逐行处理是安全的。
 *
 * @author TT432
 */
final class JsonTextPanel extends UIScrollPanel {
    private static final int COLOR_KEY = 0xFFFFFFFF;
    private static final int COLOR_STRING = 0xFF7FC97F;
    private static final int COLOR_NUMBER = 0xFF55C4C4;
    private static final int COLOR_LITERAL = 0xFFE0A050;
    private static final int COLOR_PUNCT = 0xFFAAAAAA;
    private static final int COLOR_PLAIN = 0xFFDDDDDD;

    private final int lineHeight;
    private List<String> lines = List.of();

    JsonTextPanel(int x, int y, int width, int height, int lineHeight) {
        super(x, y, width, height);
        this.border = 4;
        this.lineHeight = lineHeight;
    }

    /**
     * 替换展示的 JSON 行并重置滚动位置。
     */
    void setLines(List<String> lines) {
        this.lines = List.copyOf(lines);
        setScrollDistance(0);
    }

    @Override
    protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int lineY = y + border;
        for (String line : lines) {
            int drawX = x + border;
            for (Segment segment : tokenize(line)) {
                gfx.drawText(segment.text(), drawX, lineY, segment.color());
                drawX += gfx.textWidth(segment.text());
            }
            lineY += lineHeight;
        }
    }

    @Override
    public int getContentHeight() {
        return Math.max(height, lines.size() * lineHeight + border * 2);
    }

    /**
     * 一段文本及其颜色。
     */
    record Segment(String text, int color) {
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
            segments.add(new Segment(line.substring(0, i), COLOR_PLAIN));
        }
        // key：引号串后跟 ':'
        if (i < length && line.charAt(i) == '"') {
            int stringEnd = stringEnd(line, i);
            int colon = stringEnd;
            while (colon < length && line.charAt(colon) == ' ') {
                colon++;
            }
            if (colon < length && line.charAt(colon) == ':') {
                segments.add(new Segment(line.substring(i, stringEnd), COLOR_KEY));
                segments.add(new Segment(line.substring(stringEnd, colon + 1), COLOR_PUNCT));
                i = colon + 1;
            }
        }
        // 行内其余 token
        while (i < length) {
            char c = line.charAt(i);
            if (c == '"') {
                int end = stringEnd(line, i);
                segments.add(new Segment(line.substring(i, end), COLOR_STRING));
                i = end;
            } else if (c == ' ') {
                int end = i;
                while (end < length && line.charAt(end) == ' ') {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), COLOR_PLAIN));
                i = end;
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                int end = i + 1;
                while (end < length && isNumberChar(line.charAt(end))) {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), COLOR_NUMBER));
                i = end;
            } else if (Character.isLetter(c)) {
                int end = i + 1;
                while (end < length && Character.isLetter(line.charAt(end))) {
                    end++;
                }
                String word = line.substring(i, end);
                boolean literal = word.equals("true") || word.equals("false") || word.equals("null");
                segments.add(new Segment(word, literal ? COLOR_LITERAL : COLOR_PLAIN));
                i = end;
            } else {
                segments.add(new Segment(line.substring(i, i + 1), COLOR_PUNCT));
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
