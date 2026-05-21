package io.github.tt432.eyelibutil.texture;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
/** @author TT432 */
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