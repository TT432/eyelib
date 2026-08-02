//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import io.github.tt432.eyelib.molang.type.MolangType;

/**
 * Molang 值类型 → 显示颜色（画布徽标与 scope 表共用，规格 §W3 类型着色）。
 */
public final class MolangTypeColors {
    /** 求值错误（红）。 */
    public static final int ERROR = 0xFFFF5555;
    /** number（float/int/bool，青）。 */
    public static final int NUMBER = 0xFF55C4C4;
    /** string（绿）。 */
    public static final int STRING = 0xFF7FC97F;
    /** array（黄）。 */
    public static final int ARRAY = 0xFFFFFF55;
    /** dynamic/未知（灰）。 */
    public static final int DYNAMIC = 0xFFAAAAAA;

    private MolangTypeColors() {
    }

    /** 按类型取色（不区分 error；error 由调用方叠加 {@link #ERROR}）。 */
    public static int of(MolangType type) {
        if (type.isNumber()) {
            return NUMBER;
        }
        return switch (type) {
            case STRING -> STRING;
            case ARRAY -> ARRAY;
            default -> DYNAMIC;
        };
    }
}
//?}
