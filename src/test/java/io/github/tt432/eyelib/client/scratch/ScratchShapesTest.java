package io.github.tt432.eyelib.client.scratch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ScratchShapes} 几何契约：轮廓特征（凹口/凸榫/内腔）、三角剖分面积守恒、描边外扩。
 *
 * @author TT432
 */
class ScratchShapesTest {

    private static final float EPS = 1e-3f;

    // ===== 轮廓特征 =====

    @Test
    void stackOutlineHasTopNotchAndBottomBump() {
        float[] o = ScratchShapes.outlineStack(80, ScratchTheme.FIRST_LINE_H);
        // 顶凹口：顶边区间内存在 0 < y <= NOTCH_DEPTH 的顶点
        assertTrue(hasPointInRegion(o, ScratchTheme.NOTCH_X, ScratchTheme.NOTCH_X + ScratchTheme.NOTCH_W,
                EPS, ScratchTheme.NOTCH_DEPTH + EPS), "顶部应有凹口");
        // 底凸榫：存在 y > h 的顶点
        float h = ScratchTheme.FIRST_LINE_H;
        assertTrue(hasYGreater(o, h + EPS), "底部应有凸榫超出 h");
        // 凸榫深度恰好 NOTCH_DEPTH
        assertEquals(h + ScratchTheme.NOTCH_DEPTH, maxY(o), 0.1f);
    }

    @Test
    void capOutlineHasFlatBottom() {
        float[] o = ScratchShapes.outlineCap(80, 20);
        assertEquals(20, maxY(o), EPS);
    }

    @Test
    void cOutlineHasCavityAndAlignedNotches() {
        float headH = ScratchTheme.FIRST_LINE_H;
        float mouthH = 30;
        float lipH = ScratchTheme.LIP_LINE_H;
        float[] o = ScratchShapes.outlineC(100, headH, mouthH, lipH);
        // 总高 = headH + mouthH + lipH（凸榫额外 +NOTCH_DEPTH）
        float total = headH + mouthH + lipH;
        assertEquals(total + ScratchTheme.NOTCH_DEPTH, maxY(o), 0.1f);
        // 内腔左缘：存在 x ≈ INSET 且 headH < y < headH+mouthH 的顶点
        assertTrue(hasPointNearX(o, ScratchTheme.INSET, headH + 1, headH + mouthH - 1),
                "C 形应有 x=INSET 的内腔左缘");
        // 下唇顶边凹口与头行底凸榫对齐：armTop 附近存在凹口顶点（y > armTop）
        float armTop = headH + mouthH - ScratchTheme.MOUTH_TRIM;
        assertTrue(hasPointInRegion(o, ScratchTheme.INSET + ScratchTheme.NOTCH_X,
                        ScratchTheme.INSET + ScratchTheme.NOTCH_X + ScratchTheme.NOTCH_W,
                        armTop + EPS, armTop + ScratchTheme.NOTCH_DEPTH + EPS),
                "下唇顶边应有凹口");
    }

    @Test
    void pillOutlineIsSymmetric() {
        float w = 40, h = ScratchTheme.REPORTER_H;
        float[] o = ScratchShapes.outlinePill(w, h);
        assertEquals(h, maxY(o), EPS);
        assertEquals(0, minY(o), EPS);
        assertEquals(w, maxX(o), EPS);
        // 半圆端部：存在 x < h/2 且 y ≈ h/2 的顶点
        assertTrue(hasPointInRegion(o, -EPS, h / 2, h / 2 - EPS, h / 2 + EPS), "pill 左端应有半圆");
    }

    @Test
    void booleanOutlineIsHexagon() {
        float w = 30, h = ScratchTheme.REPORTER_H;
        float[] o = ScratchShapes.outlineBoolean(w, h);
        assertEquals(6, o.length / 2, "布尔轮廓应为六边形");
        assertEquals(w, maxX(o), EPS);
        assertEquals(h, maxY(o), EPS);
    }

    // ===== 三角剖分 =====

    @Test
    void triangulatePreservesArea() {
        float[][] shapes = {
                ScratchShapes.outlineStack(80, ScratchTheme.FIRST_LINE_H),
                ScratchShapes.outlineC(100, ScratchTheme.FIRST_LINE_H, 30, ScratchTheme.LIP_LINE_H),
                ScratchShapes.outlinePill(40, ScratchTheme.REPORTER_H),
                ScratchShapes.outlineBoolean(30, ScratchTheme.REPORTER_H),
        };
        for (float[] o : shapes) {
            float[] tris = ScratchShapes.triangulate(o);
            assertEquals(0, tris.length % 6, "三角形顶点流长度应为 6 的倍数");
            float triArea = triangleAreaSum(tris);
            float polyArea = ScratchShapes.signedArea(o);
            assertEquals(polyArea, triArea, Math.max(1.0f, polyArea * 0.02f),
                    "剖分面积应守恒");
        }
    }

    @Test
    void triangulatedVerticesStayInBounds() {
        float[] o = ScratchShapes.outlineC(100, ScratchTheme.FIRST_LINE_H, 30, ScratchTheme.LIP_LINE_H);
        float[] tris = ScratchShapes.triangulate(o);
        float maxY = maxY(o), maxX = maxX(o);
        for (int i = 0; i < tris.length; i += 2) {
            assertTrue(tris[i] >= -EPS && tris[i] <= maxX + EPS);
            assertTrue(tris[i + 1] >= -EPS && tris[i + 1] <= maxY + EPS);
        }
    }

    // ===== 描边外扩 =====

    @Test
    void offsetOutlineGrowsArea() {
        float[] o = ScratchShapes.outlineStack(80, ScratchTheme.FIRST_LINE_H);
        float[] outer = ScratchShapes.offsetOutline(o, ScratchTheme.STROKE);
        assertTrue(ScratchShapes.signedArea(outer) > ScratchShapes.signedArea(o),
                "外扩轮廓面积应增大");
        // 外扩后的剖分也应守恒
        float[] tris = ScratchShapes.triangulate(outer);
        assertEquals(ScratchShapes.signedArea(outer), triangleAreaSum(tris),
                Math.max(1.0f, ScratchShapes.signedArea(outer) * 0.02f));
    }

    @Test
    void offsetOutlineExpandsBounds() {
        float[] o = ScratchShapes.outlinePill(40, ScratchTheme.REPORTER_H);
        float[] outer = ScratchShapes.offsetOutline(o, 1);
        assertTrue(maxX(outer) > maxX(o) + 0.5f, "右缘应外扩");
        assertTrue(maxY(outer) > maxY(o) + 0.5f, "底缘应外扩");
        assertTrue(minY(outer) < minY(o) - 0.5f, "顶缘应外扩");
    }

    // ===== 工具 =====

    private static boolean hasPointInRegion(float[] o, float x0, float x1, float y0, float y1) {
        for (int i = 0; i < o.length; i += 2) {
            if (o[i] >= x0 && o[i] <= x1 && o[i + 1] >= y0 && o[i + 1] <= y1) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPointNearX(float[] o, float x, float y0, float y1) {
        for (int i = 0; i < o.length; i += 2) {
            if (Math.abs(o[i] - x) < 0.1f && o[i + 1] >= y0 && o[i + 1] <= y1) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasYGreater(float[] o, float y) {
        for (int i = 1; i < o.length; i += 2) {
            if (o[i] > y) {
                return true;
            }
        }
        return false;
    }

    private static float maxX(float[] o) {
        float m = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < o.length; i += 2) {
            m = Math.max(m, o[i]);
        }
        return m;
    }

    private static float maxY(float[] o) {
        float m = Float.NEGATIVE_INFINITY;
        for (int i = 1; i < o.length; i += 2) {
            m = Math.max(m, o[i]);
        }
        return m;
    }

    private static float minY(float[] o) {
        float m = Float.POSITIVE_INFINITY;
        for (int i = 1; i < o.length; i += 2) {
            m = Math.min(m, o[i]);
        }
        return m;
    }

    private static float triangleAreaSum(float[] tris) {
        float sum = 0;
        for (int i = 0; i < tris.length; i += 6) {
            sum += Math.abs(
                    (tris[i + 2] - tris[i]) * (tris[i + 5] - tris[i + 1])
                            - (tris[i + 4] - tris[i]) * (tris[i + 3] - tris[i + 1])) / 2;
        }
        return sum;
    }
}
