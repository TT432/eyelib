package io.github.tt432.eyelib.core.util.texture;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TexturePaths {
    public static String emissivePath(String baseTexture) {
        int lastIndexOfDot = baseTexture.lastIndexOf(".png");

        if (lastIndexOfDot != -1) {
            String beforeDot = baseTexture.substring(0, lastIndexOfDot);
            return beforeDot + ".emissive.png";
        }

        return baseTexture;
    }
}
