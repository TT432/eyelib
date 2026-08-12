package io.github.tt432.eyelib.client.scratch;

import java.util.ArrayList;
import java.util.List;

/**
 * Scratch 3.0 官方积木轮廓几何：路径生成（忠实移植 scratchblocks {@code scratch3/draw.js}）、
 * miter 外扩（描边）与 ear-clipping 三角剖分。
 *
 * <p>所有轮廓为局部坐标（y 向下）、顶点序为屏幕系顺时针（即鞋带面积为正）。
 * 输出为扁平 float 数组 {@code [x0,y0,x1,y1,...]}。
 *
 * @author TT432
 */
public final class ScratchShapes {
    private ScratchShapes() {}

    /** 贝塞尔细分段数。 */
    private static final int BEZIER_SEG = 4;
    /** 90° 圆角细分段数。 */
    private static final int ARC_SEG = 4;
    /** 半圆（pill 端部）细分段数。 */
    private static final int PILL_SEG = 8;

    // ===== 公开 API =====

    /**
     * 命令积木轮廓：顶凹口 + 右缘 + 底凸榫 + 左缘（draw.js stackRect）。
     *
     * @param w 总宽（≥ {@link ScratchTheme#NOTCH_X} + {@link ScratchTheme#NOTCH_W} + 圆角）
     * @param h 总高（不含底凸榫深）
     */
    public static float[] outlineStack(float w, float h) {
        Path p = new Path();
        top(p, w);
        rightAndBottom(p, w, h, true, 0);
        return p.close();
    }

    /**
     * 平顶积木（hat/cap 无凹口/凸榫变体暂不用于 molang，预留）。
     * 顶凹口 + 平底（draw.js capRect）。
     */
    public static float[] outlineCap(float w, float h) {
        Path p = new Path();
        top(p, w);
        rightAndBottom(p, w, h, false, 0);
        return p.close();
    }

    /**
     * C 形积木轮廓（draw.js mouthRect，单 mouth、非 final）：
     * 顶凹口 → 头行底边凸榫（inset={@link ScratchTheme#INSET}）→ 内腔左缘 →
     * 下唇顶边凹口 → 右缘 → 最底边凸榫（inset=0）。
     *
     * @param w      总宽
     * @param headH  头行高（首行，含 padding）
     * @param mouthH 内腔口高（内容高，微调 {@link ScratchTheme#MOUTH_TRIM} 前）
     * @param lipH   下唇行高
     * @return 轮廓；总高 = headH + mouthH + lipH
     */
    public static float[] outlineC(float w, float headH, float mouthH, float lipH) {
        float inset = ScratchTheme.INSET;
        float trim = ScratchTheme.MOUTH_TRIM;
        Path p = new Path();
        top(p, w);
        // 头行底边：凸榫 inset，左端圆角向内腔（sweep=0 向下）
        rightAndBottom(p, w, headH, true, inset);
        // 内腔左缘 + 下唇（getArm：带凹口）
        float armTop = headH + mouthH - trim;
        p.lineTo(inset, armTop - ScratchTheme.CORNER);
        arc(p, inset + ScratchTheme.CORNER, armTop - ScratchTheme.CORNER, ScratchTheme.CORNER, Math.PI, Math.PI / 2);
        p.lineTo(inset + ScratchTheme.NOTCH_X, armTop);
        notch(p, inset + ScratchTheme.NOTCH_X, armTop, +1);
        p.lineTo(w - ScratchTheme.CORNER, armTop);
        arc(p, w - ScratchTheme.CORNER, armTop + ScratchTheme.CORNER, ScratchTheme.CORNER, Math.PI * 1.5, Math.PI * 2);
        // 最底边（inset=0）；draw.js: y += lipH + trim
        float y = armTop + lipH + trim;
        rightAndBottom(p, w, y, true, 0);
        return p.close();
    }

    /** C 形总高（与 {@link #outlineC} 一致）。 */
    public static float heightC(float headH, float mouthH, float lipH) {
        return headH + mouthH + lipH;
    }

    /**
     * reporter pill 轮廓（draw.js pillRect：rect rx=ry=h/2）。
     */
    public static float[] outlinePill(float w, float h) {
        float r = h / 2;
        Path p = new Path();
        p.moveTo(r, 0);
        p.lineTo(w - r, 0);
        arc(p, w - r, r, r, Math.PI * 1.5, Math.PI * 2.5, PILL_SEG);
        p.lineTo(r, h);
        arc(p, r, r, r, Math.PI * 0.5, Math.PI * 1.5, PILL_SEG);
        return p.close();
    }

    /**
     * 布尔六边形轮廓（draw.js pointedPath）。
     */
    public static float[] outlineBoolean(float w, float h) {
        float r = h / 2;
        Path p = new Path();
        p.moveTo(r, 0);
        p.lineTo(w - r, 0);
        p.lineTo(w, r);
        p.lineTo(w - r, h);
        p.lineTo(r, h);
        p.lineTo(0, r);
        return p.close();
    }

    /**
     * 圆角矩形轮廓（输入框、空插槽占位等 UI 元素用；屏幕系顺时针）。
     */
    public static float[] outlineRoundRect(float w, float h, float r) {
        r = Math.min(r, Math.min(w, h) / 2);
        Path p = new Path();
        p.moveTo(r, 0);
        p.lineTo(w - r, 0);
        arc(p, w - r, r, r, -Math.PI / 2, 0);
        p.lineTo(w, h - r);
        arc(p, w - r, h - r, r, 0, Math.PI / 2);
        p.lineTo(r, h);
        arc(p, r, h - r, r, Math.PI / 2, Math.PI);
        p.lineTo(0, r);
        arc(p, r, r, r, Math.PI, Math.PI * 1.5);
        return p.close();
    }

    /**
     * miter 外扩轮廓（描边底层用）：每顶点沿两邻边外法线的角平分线偏移 d。
     * 输入须为屏幕系顺时针简单多边形。
     */
    public static float[] offsetOutline(float[] outline, float d) {
        int n = outline.length / 2;
        float[] out = new float[outline.length];
        for (int i = 0; i < n; i++) {
            float ax = outline[((i - 1 + n) % n) * 2], ay = outline[((i - 1 + n) % n) * 2 + 1];
            float bx = outline[i * 2], by = outline[i * 2 + 1];
            float cx = outline[((i + 1) % n) * 2], cy = outline[((i + 1) % n) * 2 + 1];
            // 屏幕系顺时针：外法线 = 边的右手侧 (dy, -dx)
            float[] n1 = outwardNormal(ax, ay, bx, by);
            float[] n2 = outwardNormal(bx, by, cx, cy);
            float mx = n1[0] + n2[0], my = n1[1] + n2[1];
            float len = (float) Math.sqrt(mx * mx + my * my);
            if (len < 1e-6f) {
                mx = n2[0];
                my = n2[1];
            } else {
                mx /= len;
                my /= len;
            }
            float dot = mx * n2[0] + my * n2[1];
            float miterLen = d / Math.max(dot, 1e-3f);
            // 限制 miter 长度防尖刺（180° 近回头边）
            miterLen = Math.min(miterLen, d * 4);
            out[i * 2] = bx + mx * miterLen;
            out[i * 2 + 1] = by + my * miterLen;
        }
        return out;
    }

    /**
     * ear-clipping 三角剖分。输入屏幕系顺时针简单多边形，输出三角形顶点流
     * {@code [ax,ay,bx,by,cx,cy, ...]}，长度为 6 的倍数。
     */
    public static float[] triangulate(float[] outline) {
        int n = outline.length / 2;
        if (n < 3) {
            return new float[0];
        }
        float area = signedArea(outline);
        List<Integer> idx = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            idx.add(i);
        }
        List<Float> out = new ArrayList<>(n * 3 * 2);
        int guard = 0;
        while (idx.size() > 3 && guard++ < n * n) {
            boolean clipped = false;
            for (int i = 0; i < idx.size(); i++) {
                int ia = idx.get((i - 1 + idx.size()) % idx.size());
                int ib = idx.get(i);
                int ic = idx.get((i + 1) % idx.size());
                if (!isConvex(outline, ia, ib, ic, area)) {
                    continue;
                }
                if (anyPointInside(outline, idx, ia, ib, ic)) {
                    continue;
                }
                out.add(outline[ia * 2]);
                out.add(outline[ia * 2 + 1]);
                out.add(outline[ib * 2]);
                out.add(outline[ib * 2 + 1]);
                out.add(outline[ic * 2]);
                out.add(outline[ic * 2 + 1]);
                idx.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) {
                // 退化（自交/共线）：放弃剩余部分，避免死循环
                break;
            }
        }
        if (idx.size() == 3) {
            for (int i : idx) {
                out.add(outline[i * 2]);
                out.add(outline[i * 2 + 1]);
            }
        }
        float[] arr = new float[out.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = out.get(i);
        }
        return arr;
    }

    /** 鞋带面积（屏幕系顺时针为正）。 */
    public static float signedArea(float[] outline) {
        float sum = 0;
        int n = outline.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sum += outline[i * 2] * outline[j * 2 + 1] - outline[j * 2] * outline[i * 2 + 1];
        }
        return sum / 2;
    }

    // ===== 路径段（draw.js 移植） =====

    /** getTop：左上圆角 → 顶边 → 凹口 → 右上圆角。入口点 (0, CORNER)。 */
    private static void top(Path p, float w) {
        float c = ScratchTheme.CORNER;
        p.moveTo(0, c);
        arc(p, c, c, c, Math.PI, Math.PI * 1.5);
        p.lineTo(ScratchTheme.NOTCH_X, 0);
        notch(p, ScratchTheme.NOTCH_X, 0, +1);
        p.lineTo(w - c, 0);
        arc(p, w - c, c, c, Math.PI * 1.5, Math.PI * 2);
    }

    /**
     * getRightAndBottom：右缘向下 → 右下圆角 → （可选凸榫）→ 底边向左 → 左端圆角。
     * 入口点 (w, CORNER)（上接 {@link #top} 或下唇右圆角）。
     *
     * @param inset   凸榫左移量（C 形内腔为 {@link ScratchTheme#INSET}，普通为 0）
     * @param hasBump 是否画底凸榫
     */
    private static void rightAndBottom(Path p, float w, float y, boolean hasBump, float inset) {
        float c = ScratchTheme.CORNER;
        p.lineTo(w, y - c);
        arc(p, w - c, y - c, c, 0, Math.PI / 2);
        if (hasBump) {
            p.lineTo(inset + ScratchTheme.NOTCH_X + ScratchTheme.NOTCH_W, y);
            notch(p, inset + ScratchTheme.NOTCH_X + ScratchTheme.NOTCH_W, y, -1);
        }
        p.lineTo(inset + c, y);
        if (inset == 0) {
            // 左下圆角（向上折返）
            arc(p, c, y - c, c, Math.PI / 2, Math.PI);
        } else {
            // C 形内腔左上圆角（向下进入内腔）
            arc(p, inset + c, y + c, c, Math.PI * 1.5, Math.PI);
        }
    }

    /**
     * topNotch：凹口/凸榫统一采样。
     *
     * @param dir +1 = 顶边凹口（从左肩向右，向下凹）；-1 = 底边凸榫（从右肩向左，向下凸）
     */
    private static void notch(Path p, float x0, float y, int dir) {
        // draw.js topNotch 相对位移 × 0.5：
        // c 2 0 3 1 4 2 | l 4 4 | c 1 1 2 2 4 2 | h 12 | c 2 0 3 -1 4 -2 | l 4 -4 | c 1 -1 2 -2 4 -2
        float d = dir;
        cubic(p, x0, y, x0 + d * 1, y, x0 + d * 1.5f, y + 0.5f, x0 + d * 2, y + 1);
        p.lineTo(x0 + d * 4, y + 3);
        cubic(p, x0 + d * 4, y + 3, x0 + d * 4.5f, y + 3.5f, x0 + d * 5, y + 4, x0 + d * 6, y + 4);
        p.lineTo(x0 + d * 12, y + 4);
        cubic(p, x0 + d * 12, y + 4, x0 + d * 13, y + 4, x0 + d * 13.5f, y + 3.5f, x0 + d * 14, y + 3);
        p.lineTo(x0 + d * 16, y + 1);
        cubic(p, x0 + d * 16, y + 1, x0 + d * 16.5f, y + 0.5f, x0 + d * 17, y, x0 + d * 18, y);
    }

    // ===== 基础图元 =====

    /** 三次贝塞尔采样（BEZIER_SEG 段，不含起点）。 */
    private static void cubic(Path p, float x0, float y0, float cx1, float cy1,
                              float cx2, float cy2, float x1, float y1) {
        for (int i = 1; i <= BEZIER_SEG; i++) {
            float t = (float) i / BEZIER_SEG;
            float u = 1 - t;
            float x = u * u * u * x0 + 3 * u * u * t * cx1 + 3 * u * t * t * cx2 + t * t * t * x1;
            float y = u * u * u * y0 + 3 * u * u * t * cy1 + 3 * u * t * t * cy2 + t * t * t * y1;
            p.lineTo(x, y);
        }
    }

    /**
     * 圆弧采样（圆心、半径、起止角；y 向下坐标系，角增方向 = 屏幕顺时针）。
     * 若 end &lt; start 则递减采样（SVG sweep=0）。
     */
    private static void arc(Path p, float cx, float cy, float r, double start, double end) {
        arc(p, cx, cy, r, start, end, ARC_SEG);
    }

    private static void arc(Path p, float cx, float cy, float r, double start, double end, int seg) {
        for (int i = 1; i <= seg; i++) {
            double t = start + (end - start) * i / seg;
            p.lineTo(cx + r * (float) Math.cos(t), cy + r * (float) Math.sin(t));
        }
    }

    // ===== 三角剖分内部 =====

    private static boolean isConvex(float[] pts, int ia, int ib, int ic, float area) {
        float cross = cross(pts, ia, ib, ic);
        // 屏幕系顺时针（area>0）的凸顶点 cross>0
        return cross * area > 1e-9;
    }

    private static float cross(float[] pts, int ia, int ib, int ic) {
        float bax = pts[ib * 2] - pts[ia * 2];
        float bay = pts[ib * 2 + 1] - pts[ia * 2 + 1];
        float bcx = pts[ic * 2] - pts[ib * 2];
        float bcy = pts[ic * 2 + 1] - pts[ib * 2 + 1];
        return bax * bcy - bay * bcx;
    }

    private static boolean anyPointInside(float[] pts, List<Integer> idx, int ia, int ib, int ic) {
        for (int k : idx) {
            if (k == ia || k == ib || k == ic) {
                continue;
            }
            if (pointInTriangle(pts, k, ia, ib, ic)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pointInTriangle(float[] pts, int p, int a, int b, int c) {
        float px = pts[p * 2], py = pts[p * 2 + 1];
        float d1 = sign(px, py, pts, a, b);
        float d2 = sign(px, py, pts, b, c);
        float d3 = sign(px, py, pts, c, a);
        boolean hasNeg = d1 < -1e-6 || d2 < -1e-6 || d3 < -1e-6;
        boolean hasPos = d1 > 1e-6 || d2 > 1e-6 || d3 > 1e-6;
        return !(hasNeg && hasPos);
    }

    private static float sign(float px, float py, float[] pts, int a, int b) {
        return (px - pts[b * 2]) * (pts[a * 2 + 1] - pts[b * 2 + 1])
                - (pts[a * 2] - pts[b * 2]) * (py - pts[b * 2 + 1]);
    }

    private static float[] outwardNormal(float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-9f) {
            return new float[]{0, 0};
        }
        // 屏幕系顺时针多边形的外法线 = (dy, -dx)
        return new float[]{dy / len, -dx / len};
    }

    // ===== 路径容器 =====

    private static final class Path {
        private final List<Float> pts = new ArrayList<>();
        private float x, y;
        private float startX, startY;

        void moveTo(float x, float y) {
            this.x = this.startX = x;
            this.y = this.startY = y;
            pts.add(x);
            pts.add(y);
        }

        void lineTo(float x, float y) {
            // 合并共线重复点，降低剖分退化概率
            if (Math.abs(x - this.x) < 1e-6 && Math.abs(y - this.y) < 1e-6) {
                return;
            }
            this.x = x;
            this.y = y;
            pts.add(x);
            pts.add(y);
        }

        float[] close() {
            if (Math.abs(x - startX) < 1e-6 && Math.abs(y - startY) < 1e-6 && pts.size() > 2) {
                pts.remove(pts.size() - 1);
                pts.remove(pts.size() - 1);
            }
            float[] arr = new float[pts.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = pts.get(i);
            }
            return arr;
        }
    }
}
