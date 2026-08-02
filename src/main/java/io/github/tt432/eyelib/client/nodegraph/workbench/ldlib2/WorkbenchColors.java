package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if !legacy {
import io.github.tt432.eyelib.molang.type.MolangType;

/**
 * 工作台配色常量（ARGB）。与 ldlib1 工作台逐值对齐：
 * 徽标 number 青 / string 绿 / array 黄 / dynamic 灰 / error 红；
 * JSON 着色沿用 JsonTextPanel 五色。
 */
final class WorkbenchColors {
    private WorkbenchColors() {
    }

    // ---- 徽标（MolangType → 颜色）----
    static final int BADGE_NUMBER = 0xFF55C4C4;
    static final int BADGE_STRING = 0xFF7FC97F;
    static final int BADGE_ARRAY = 0xFFFFFF55;
    static final int BADGE_DYNAMIC = 0xFFAAAAAA;
    static final int BADGE_ERROR = 0xFFFF5555;
    /** 徽标半透明黑底 pill。 */
    static final int BADGE_PILL_BG = 0x88000000;

    // ---- JSON 语法着色（JsonTextPanel 同值）----
    static final int JSON_KEY = 0xFFFFFFFF;
    static final int JSON_STRING = 0xFF7FC97F;
    static final int JSON_NUMBER = 0xFF55C4C4;
    static final int JSON_LITERAL = 0xFFE0A050;
    static final int JSON_PUNCT = 0xFFAAAAAA;
    static final int JSON_PLAIN = 0xFFDDDDDD;

    // ---- 调试面板 ----
    static final int TEXT = 0xFFFFFFFF;
    static final int DIM = 0xFFAAAAAA;
    static final int ERROR = 0xFFFF5555;

    /** 徽标颜色：error 优先，其余按 MolangType（number 三子类型同色）。 */
    static int badgeColor(boolean error, MolangType type) {
        if (error) {
            return BADGE_ERROR;
        }
        if (type.isNumber()) {
            return BADGE_NUMBER;
        }
        return switch (type) {
            case STRING -> BADGE_STRING;
            case ARRAY -> BADGE_ARRAY;
            default -> BADGE_DYNAMIC;
        };
    }
}
//?}
