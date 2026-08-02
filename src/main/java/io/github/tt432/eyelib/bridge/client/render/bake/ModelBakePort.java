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

    /** GUI 预览绘制（blaze3d 访问集中在 bridge；Tesselator + position_tex 即时绘制）。 */
    static void twoSideDrawGuiPreview(BakedModel baked, org.joml.Matrix4f pose, ResourceLocation texture) {
        TwoSideModelBakeInfo.INSTANCE.drawGuiPreview(baked, pose, texture);
    }
    //?} else {
    static BakedModel twoSideGetBakedModel(Model model, boolean isSolid, Identifier texture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, isSolid, texture);
    }

    static BakedModel twoSideGetBakedModel(Model model, boolean isSolid, Identifier texture, Identifier meshTexture) {
        return TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, isSolid, texture, meshTexture);
    }
    //?}
}
