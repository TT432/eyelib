package io.github.tt432.eyelib.util.client.texture;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TexturePathHelper {
    public static String getEmissiveTexturePath(String baseTexture) {
        int lastIndexOfDot = baseTexture.lastIndexOf(".png");

        if (lastIndexOfDot != -1) {
            String beforeDot = baseTexture.substring(0, lastIndexOfDot);
            return beforeDot + ".emissive.png";
        }

        return baseTexture;
    }
}
