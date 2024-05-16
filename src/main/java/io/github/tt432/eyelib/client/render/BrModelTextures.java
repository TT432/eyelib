package io.github.tt432.eyelib.client.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author TT432
 */
public class BrModelTextures {
    public record TwoSideInfo(
            String boneName,
            boolean[] cubeNeedTwoSide
    ) {
    }

    public record TwoSideInfoMap(
            Map<String, TwoSideInfo> map
    ) {
        public boolean isTwoSide(String boneName, int idx) {
            return !map.containsKey(boneName) || map.get(boneName).cubeNeedTwoSide[idx];
        }
    }

    private static final Map<String, Map<ResourceLocation, TwoSideInfoMap>> map = new HashMap<>();

    public static TwoSideInfoMap getTwoSideInfo(BrModel model, boolean isSolid, ResourceLocation texture) {
        return map.computeIfAbsent(model.identifier(), ___ -> new HashMap<>())
                .computeIfAbsent(texture, __ -> {
                    Minecraft.getInstance().textureManager.getTexture(texture).bind();

                    int[] width = new int[1];
                    int[] height = new int[1];
                    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
                    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);

                    ImmutableMap.Builder<String, TwoSideInfo> builder = ImmutableMap.builder();

                    try (NativeImage nativeimage = new NativeImage(width[0], height[0], false)) {
                        nativeimage.downloadTexture(0, false);
                        nativeimage.flipY();

                        model.allBones().forEach((boneName, bone) ->
                                builder.put(boneName, new TwoSideInfo(boneName, processBone(bone, isSolid, nativeimage))));
                    }

                    return new TwoSideInfoMap(builder.build());
                });
    }

    private static boolean[] processBone(BrBone bone, boolean isSolid, NativeImage intBuffer) {
        bone.children().forEach(boneIn -> processBone(boneIn, isSolid, intBuffer));

        boolean[] result = new boolean[bone.cubes().size()];

        for (int i = 0; i < bone.cubes().size(); i++) {
            BrCube brCube = bone.cubes().get(i);

            for (BrFace face : brCube.faces()) {
                if (cubeAnyTransparent(isSolid, intBuffer, face)) {
                    result[i] = true;
                    break;
                }
            }
        }

        return result;
    }

    private static boolean cubeAnyTransparent(boolean isSolid, NativeImage buffer, BrFace face) {
        Vector2f[] uv = face.getUv();
        int x = (int) (uv[0].x * buffer.getWidth());
        int y = (int) (uv[0].y * buffer.getHeight());
        int width = (int) ((uv[2].x - uv[0].x) * buffer.getWidth());
        int height = (int) ((uv[2].y - uv[0].y) * buffer.getHeight());

        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                var a = buffer.getPixelRGBA(x + k, y + j);

                if ((((a & 0xFF) != 0xFF) && !isSolid) || ((a & 0xFF) == 0 && isSolid)) {
                    return true;
                }
            }
        }

        return false;
    }
}
