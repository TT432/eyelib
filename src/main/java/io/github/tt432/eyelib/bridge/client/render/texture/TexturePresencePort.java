package io.github.tt432.eyelib.bridge.client.render.texture;

import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}

/**
 * 纹理存在性 / 缺失纹理的 Port，屏蔽 MC TextureManager 与 MissingTextureAtlasSprite 的版本差异。
 * application 层用 {@link PortResourceLocation} 表达纹理位置，由本 Port 完成 MC 接触点转换。
 *
 * @author TT432
 */
public interface TexturePresencePort {

    /**
     * 返回 MC 缺失纹理对应的 PortResourceLocation（等价于 MissingTextureAtlasSprite.getLocation()）。
     */
    public static PortResourceLocation missingLocation() {
        return ResourceLocationBridge.fromMc(MissingTextureAtlasSprite.getLocation());
    }

    /**
     * 判断给定纹理是否已在 TextureManager 中注册且非缺失纹理。
     * <p>&lt;26.1 实际查询 TextureManager；&gt;=26.1 路径不可达
     * （调用方 RenderParams.asEmissive 在 26.1 直接 no-op），返回 false 仅用于编译。
     */
    //? if <26.1 {
    public static boolean isLoaded(PortResourceLocation location) {
        return Minecraft.getInstance().getTextureManager()
                .getTexture(ResourceLocationBridge.toMc(location), MissingTextureAtlasSprite.getTexture())
                != MissingTextureAtlasSprite.getTexture();
    }
    //?} else {
    public static boolean isLoaded(PortResourceLocation location) {
        return false;
    }
    //?}
}
