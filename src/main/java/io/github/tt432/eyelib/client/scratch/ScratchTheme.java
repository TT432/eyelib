package io.github.tt432.eyelib.client.scratch;

import io.github.tt432.eyelib.molang.scratch.ScratchCategory;

/**
 * Scratch 3.0 官方视觉规格的 MC 适配常量。
 *
 * <p>来源：scratchblocks/scratchblocks 的 {@code scratch3/draw.js}、{@code scratch3/blocks.js}、
 * {@code scratch3/style.css.js}。所有尺寸为官方像素 × 0.5（MC 字体行高 9px vs Scratch 12pt≈16px），
 * 比例与相对关系保持忠实。
 *
 * @author TT432
 */
public final class ScratchTheme {
    private ScratchTheme() {}

    // ===== 尺寸（官方值 × 0.5） =====

    /** 命令积木行高（官方 40）。 */
    public static final float LINE_H = 20;
    /** 首行高：行高 + 上下 padding（官方 40 + 4 + 4 = 48）。 */
    public static final float FIRST_LINE_H = 24;
    /** 后续行高（官方 40 - 11 = 29）。 */
    public static final float LIP_LINE_H = 14;
    /** reporter / 输入框高（官方 32）。 */
    public static final float REPORTER_H = 16;
    /** 元素间距（官方 8）。 */
    public static final float GAP = 4;
    /** 圆角半径（官方 4）。 */
    public static final float CORNER = 2;
    /** 凹口左肩 x（官方 12）。 */
    public static final float NOTCH_X = 6;
    /** 凹口总宽（官方 36）。 */
    public static final float NOTCH_W = 18;
    /** 凹口深（官方 8）。 */
    public static final float NOTCH_DEPTH = 4;
    /** C 形内缩（官方 16）。 */
    public static final float INSET = 8;
    /** C 形最小宽（官方 160）。 */
    public static final float MIN_W_C = 80;
    /** 命令积木最小宽（官方 64）。 */
    public static final float MIN_W_CMD = 32;
    /** reporter 最小宽（官方 48）。 */
    public static final float MIN_W_REPORTER = 24;
    /** 空 C 腔内腔高（官方 max(29, 0+3)-2 = 27 → 微调后视觉 ~24）。 */
    public static final float EMPTY_MOUTH_H = 14;
    /** 描边外扩（官方 stroke-width 1）。 */
    public static final float STROKE = 1;
    /** C 形 mouth/lip 高度微调（官方 ∓3）。 */
    public static final float MOUTH_TRIM = 1.5f;

    // ===== 水平 padding（官方值 × 0.5；horizontalPadding 表） =====

    /** 默认水平 padding（官方 8）。 */
    public static final float PAD_DEFAULT = 4;
    /** 圆容器内 label/dropdown/boolean（官方 12）。 */
    public static final float PAD_IN_ROUND = 6;
    /** 布尔容器内 label/dropdown/round（官方 20）。 */
    public static final float PAD_IN_BOOL = 10;
    /** 布尔容器内 round 积木（官方 24）。 */
    public static final float PAD_ROUND_IN_BOOL = 12;
    /** 布尔容器内 boolean（官方 8）。 */
    public static final float PAD_BOOL_IN_BOOL = 4;
    /** 输入框内边距（官方 11）。 */
    public static final float PAD_INPUT = 6;
    /** 输入框最小宽（官方 40）。 */
    public static final float MIN_W_INPUT = 20;

    // ===== 颜色（官方 style.css.js 普通模式，ARGB） =====

    public static final int LABEL = 0xFFFFFFFF;      // #fff
    public static final int LITERAL_TEXT = 0xFF575E75;
    public static final int INPUT_FILL = 0xFFFFFFFF; // #fff

    /** 分类主色 / 描边色（Primary / Tertiary）。 */
    public static int fill(ScratchCategory c) {
        return switch (c) {
            case LITERAL -> 0xFFBFBFBF;   // grey
            case VARIABLE -> 0xFFFF8C1A;  // variables
            case QUERY -> 0xFF5CB1D6;     // sensing
            case MATH, OPERATOR -> 0xFF59C059; // operators
            case CUSTOM -> 0xFFFF6680;    // custom
            case ACCESS -> 0xFF0FBD8C;    // extension
            case CONTROL -> 0xFFFFAB19;   // control
            case ACTION -> 0xFF4C97FF;    // motion
            case RAW -> 0xFFED4242;       // obsolete
        };
    }

    public static int stroke(ScratchCategory c) {
        return switch (c) {
            case LITERAL -> 0xFF909090;
            case VARIABLE -> 0xFFDB6E00;
            case QUERY -> 0xFF2E8EB8;
            case MATH, OPERATOR -> 0xFF389438;
            case CUSTOM -> 0xFFFF3355;
            case ACCESS -> 0xFF0B8E69;
            case CONTROL -> 0xFFCF8B17;
            case ACTION -> 0xFF3373CC;
            case RAW -> 0xFFCA2B2B;
        };
    }

    /** 分类深色（-dark：boolean 输入槽底色用 Tertiary）。 */
    public static int dark(ScratchCategory c) {
        return stroke(c);
    }
}
