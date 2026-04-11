package io.github.tt432.eyelib.util.client.texture;

import io.github.tt432.eyelib.core.util.texture.TexturePaths;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TexturePathHelper {
    public static String getEmissiveTexturePath(String baseTexture) {
        return TexturePaths.emissivePath(baseTexture);
    }
}
