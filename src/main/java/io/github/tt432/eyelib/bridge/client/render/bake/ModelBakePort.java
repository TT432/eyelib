package io.github.tt432.eyelib.bridge.client.render.bake;

import io.github.tt432.eyelib.bridge.client.render.bake.adapter.TwoSideModelBakeInfo;
import io.github.tt432.eyelib.model.Model;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}

/**
 * 模型烘焙信息端口，为 application 层提供跨版本烘焙操作访问。
 */
public interface ModelBakePort {

    static void twoSideInvalidateModel(String entryName) {
        TwoSideModelBakeInfo.INSTANCE.invalidateModel(entryName);
    }

    /** 批量替换（资源重载整表写入）时失效全部烘焙缓存。 */
    static void twoSideInvalidateAll() {
        TwoSideModelBakeInfo.INSTANCE.invalidateAll();
    }

    //? if <26.1 {
    static TwoSideModelBakeInfo.TwoSideInfoMap twoSideGetBakeInfo(Model model, boolean isSolid, ResourceLocation texture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakeInfo(model, isSolid, texture);
    }
    //?} else {
    static TwoSideModelBakeInfo.TwoSideInfoMap twoSideGetBakeInfo(Model model, boolean isSolid, Identifier texture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakeInfo(model, isSolid, texture);
    }
    //?}

    static BakedModel twoSideBake(Model model, TwoSideModelBakeInfo.TwoSideInfoMap info) {
        return TwoSideModelBakeInfo.INSTANCE.bake(model, info);
    }

    //? if <26.1 {
    static BakedModel twoSideGetBakedModel(Model model, boolean isSolid, ResourceLocation texture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, isSolid, texture);
    }

    static BakedModel twoSideGetBakedModel(Model model, boolean isSolid, ResourceLocation texture, ResourceLocation meshTexture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, isSolid, texture, meshTexture);
    }

    /** 只读探测烘焙缓存（不触发计算）；未命中返回 null。供「未烘焙即跳过本帧」的渲染路径使用。 */
    static BakedModel twoSidePeekBakedModel(Model model, boolean isSolid, ResourceLocation texture, ResourceLocation meshTexture) {
        return TwoSideModelBakeInfo.INSTANCE.peekBakedModel(model, isSolid, texture, meshTexture);
    }

    /** 后台线程异步预热烘焙缓存（不触碰 GL）；已在飞行中的同组合请求自动去重。 */
    static void twoSideWarmAsync(Model model, ResourceLocation texture, ResourceLocation meshTexture) {
        TwoSideModelBakeInfo.INSTANCE.warmBakedModelAsync(model, texture, meshTexture);
    }

    /** 指定组合的异步预热是否已失败（CPU 纹理源缺失），失败时调用方应回退同步烘焙。 */
    static boolean twoSideWarmFailed(Model model, ResourceLocation texture, ResourceLocation meshTexture) {
        return TwoSideModelBakeInfo.INSTANCE.isWarmFailed(model, texture, meshTexture);
    }

    /** GUI 预览绘制（blaze3d 访问集中在 bridge；Tesselator + position_tex 即时绘制）。 */
    static void twoSideDrawGuiPreview(BakedModel baked, org.joml.Matrix4f pose, ResourceLocation texture) {
        TwoSideModelBakeInfo.INSTANCE.drawGuiPreview(baked, pose, texture);
    }

    /** GUI 预览纯色绘制（无纹理路径：position_color + 逐面漫反射明暗烘顶点色）。 */
    static void twoSideDrawGuiPreviewFlat(BakedModel baked, org.joml.Matrix4f pose) {
        TwoSideModelBakeInfo.INSTANCE.drawGuiPreviewFlat(baked, pose);
    }
    //?} else {
    static BakedModel twoSideGetBakedModel(Model model, boolean isSolid, Identifier texture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, isSolid, texture);
    }

    static BakedModel twoSideGetBakedModel(Model model, boolean isSolid, Identifier texture, Identifier meshTexture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, isSolid, texture, meshTexture);
    }

    /** 只读探测烘焙缓存（不触发计算）；未命中返回 null。供「未烘焙即跳过本帧」的渲染路径使用。 */
    static BakedModel twoSidePeekBakedModel(Model model, boolean isSolid, Identifier texture, Identifier meshTexture) {
        return TwoSideModelBakeInfo.INSTANCE.peekBakedModel(model, isSolid, texture, meshTexture);
    }

    /** 后台线程异步预热烘焙缓存（不触碰 GL）；已在飞行中的同组合请求自动去重。 */
    static void twoSideWarmAsync(Model model, Identifier texture, Identifier meshTexture) {
        TwoSideModelBakeInfo.INSTANCE.warmBakedModelAsync(model, texture, meshTexture);
    }

    /** 指定组合的异步预热是否已失败（CPU 纹理源缺失），失败时调用方应回退同步烘焙。 */
    static boolean twoSideWarmFailed(Model model, Identifier texture, Identifier meshTexture) {
        return TwoSideModelBakeInfo.INSTANCE.isWarmFailed(model, texture, meshTexture);
    }
    //?}
}
