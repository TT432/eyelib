package io.github.tt432.eyelib.wintersky.three;

import org.jspecify.annotations.Nullable;

/**
 * 浏览器 {@code Image} 对象的移植接缝：wintersky 的 Config 直接读写
 * {@code texture.image.src} 并挂 {@code onload} 回调。
 *
 * <p>JS 中 src 赋值后浏览器异步解码并触发 onload；本移植在 src 赋值时
 * <b>同步</b>触发 onload（集成 eyelib 时由纹理后端替换，见 ADR-0034）。
 */
public class TextureImage {

    public @Nullable Runnable onload;

    private @Nullable String src;

    public @Nullable String getSrc() {
        return src;
    }

    public void setSrc(@Nullable String src) {
        this.src = src;
        Runnable cb = this.onload;
        if (cb != null) {
            cb.run();
        }
    }
}
