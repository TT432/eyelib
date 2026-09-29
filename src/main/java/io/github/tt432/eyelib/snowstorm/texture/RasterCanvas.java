package io.github.tt432.eyelib.snowstorm.texture;

import java.util.Arrays;

/**
 * canvas 2D 位图的纯栅格等价物（texture_edit.js 的 {@code this.canvas}+{@code ctx}）。
 *
 * <p>像素格式：{@code int[]} ABGR（与 NativeImage 通道序一致：{@code A<<24 | B<<16 | G<<8 | R}，
 * 非预乘），行优先，长度 {@code width * height}。
 *
 * <p>偏离记录（规格 R4）：canvas 2D 的 brush 抗锯齿在纯栅格下无对应，移植为硬边像素语义；
 * canvas API 的边界行为逐字保留——{@code getImageData} 越界读返回透明黑、
 * {@code putImageData}/{@code clearRect} 越界裁剪。
 */
public final class RasterCanvas {

    public int width;
    public int height;
    /** ABGR 像素，长度 {@code width * height}。直接暴露以对应 JS 对 canvas 位图的整段读写。 */
    public int[] pixels;

    public RasterCanvas(int width, int height) {
        resize(width, height);
    }

    /** JS 给 {@code canvas.width}/{@code canvas.height} 赋值：位图重置为全透明黑。 */
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    // ---------------------------------------------------------------- ABGR 通道

    public static int pack(int r, int g, int b, int a) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static int r(int abgr) {
        return abgr & 0xFF;
    }

    public static int g(int abgr) {
        return (abgr >>> 8) & 0xFF;
    }

    public static int b(int abgr) {
        return (abgr >>> 16) & 0xFF;
    }

    public static int a(int abgr) {
        return (abgr >>> 24) & 0xFF;
    }

    // ---------------------------------------------------------------- canvas 2D 语义

    /** {@code getImageData(x, y, 1, 1)} 单像素语义：越界返回透明黑（0）。 */
    public int getPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0;
        return pixels[y * width + x];
    }

    /** {@code putImageData} 单像素语义：越界裁剪（no-op）。 */
    public void setPixel(int x, int y, int abgr) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        pixels[y * width + x] = abgr;
    }

    /** {@code clearRect(x, y, w, h)}：区域内置透明黑，越界裁剪。 */
    public void clearRect(int x, int y, int w, int h) {
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(width, x + w);
        int y1 = Math.min(height, y + h);
        for (int yy = y0; yy < y1; yy++) {
            Arrays.fill(pixels, yy * width + x0, yy * width + x1, 0);
        }
    }

    /** {@code getImageData(0, 0, width, height)} 等价：拷贝全部像素。 */
    public int[] copyPixels() {
        return pixels.clone();
    }

    /** {@code putImageData(data, 0, 0)} 等价：整体替换（调用方保证尺寸一致）。 */
    public void replacePixels(int[] data) {
        System.arraycopy(data, 0, pixels, 0, pixels.length);
    }
}
