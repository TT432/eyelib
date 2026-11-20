package io.github.tt432.eyelib.bridge.client.render.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jspecify.annotations.Nullable;

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

    /**
     * byPath 是否已持有指定纹理（含 eyelib 上传的派生纹理）。
     * 用于派生纹理（clamped/ 等）的存在性检查：实体首帧 variant 未同步时
     * 贴图解析结果随后变化，但 needReload 已为 false，若无此检查，
     * 新键的派生纹理永不入队 → MissingTexture 永久紫黑。
     * 须在主线程调用（byPath 非线程安全）。
     *
     * <p>「持有」不包含 MissingTexture 占位：vanilla {@code getTexture} 未命中时会
     * {@code new SimpleTexture(path)} 并 register，资源不存在则 byPath 被写成
     * MissingTexture（仅资源重载时清除）。把占位当成存在，派生纹理会被判为已生成
     * 而永不创建（2026-11-20 赛博马赛克根因之一）。
     *
     * @param namespacedPath 形如 "namespace:path" 的完整纹理路径
     */
    boolean eyelib$hasTexture(String namespacedPath);

    /**
     * 只读查询 byPath（不注册、不加载、无副作用）。
     * vanilla {@code getTexture} 会在未命中时注册 SimpleTexture，对运行期派生纹理
     * （clamped/、_color_mask/，资源中不存在）等于把 byPath 永久写成 MissingTexture。
     * 读取像素（烘焙/导出）必须先经本方法判断，未注册时按「无像素」处理。
     * 须在主线程调用（byPath 非线程安全）。
     *
     * @param namespacedPath 形如 "namespace:path" 的完整纹理路径
     * @return 已注册的纹理对象；未注册返回 {@code null}
     */
    @Nullable
    AbstractTexture eyelib$peekTexture(String namespacedPath);
}
