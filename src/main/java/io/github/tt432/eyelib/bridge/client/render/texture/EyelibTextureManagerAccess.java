package io.github.tt432.eyelib.bridge.client.render.texture;

import java.util.function.Predicate;

/**
 * TextureManager 访问器接口（由 TextureManagerMixin 实现）：按路径谓词驱逐
 * eyelib 生成的 DynamicTexture（addon 基图与 clamped/_color_mask 派生纹理），
 * 使下一次 getTexture 按最新数据源重建。
 * vanilla 资源重载对 DynamicTexture 是 no-op（DynamicTexture.load 空实现，
 * 1.20.1 反编译实证），不主动驱逐则 GPU 永久持有旧图。
 *
 * @author TT432
 */
public interface EyelibTextureManagerAccess {
    /**
     * 驱逐 byPath 中路径匹配且为 DynamicTexture 的条目（close 释放 GL 后移除）。
     * 须在主线程调用（由 {@code NativeImageIO#evictTexturesMatching} 保证线程跳转）。
     *
     * @param pathFilter 作用于 ResourceLocation/Identifier 的 path 段（无命名空间）
     */
    void eyelib$evictTextures(Predicate<String> pathFilter);
}
