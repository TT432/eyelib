package io.github.tt432.eyelib.bridge.client.render.texture;

import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;

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

}
