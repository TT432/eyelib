package io.github.tt432.eyelib.molang;

/**
 * usage:
 *
 * <pre>{@code
 * @MolangMapping("math")
 * class MolangMath {
 *     public static float max(float a, float b) {
 *         return a > b ? a : b;
 *     }
 * }
 * }</pre>
 *
 * 此处的 max 可以对应 math.max(a, b)
 *
 * @author TT432
 */
public @interface MolangMapping {
    /**
     * @return molang function or field name
     */
    String value();
}
