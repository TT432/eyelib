package io.github.tt432.eyelib.client.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.client.model.Model;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2fc;

import java.util.HashMap;
import java.util.List;
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
            return !map.containsKey(boneName)
                    || map.get(boneName).cubeNeedTwoSide.length <= idx
                    || map.get(boneName).cubeNeedTwoSide[idx];
        }
    }

    private static final Map<String, Map<ResourceLocation, TwoSideInfoMap>> map = new HashMap<>();

    public static TwoSideInfoMap getTwoSideInfo(Model model, boolean isSolid, ResourceLocation texture) {
        return map.computeIfAbsent(model.name(), ___ -> new HashMap<>())
                .computeIfAbsent(texture, __ -> {
                    Minecraft.getInstance().getTextureManager().getTexture(texture).bind();

                    int[] width = new int[1];
                    int[] height = new int[1];
                    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
                    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);

                    ImmutableMap.Builder<String, TwoSideInfo> builder = ImmutableMap.builder();

                    if (width[0] != 0 && height[0] != 0) {
                        try (NativeImage nativeimage = new NativeImage(width[0], height[0], false)) {
                            nativeimage.downloadTexture(0, false);

                            model.toplevelBones().forEach((boneName, bone) ->
                                    builder.put(boneName, new TwoSideInfo(boneName, processBone(bone, isSolid, nativeimage))));
                        }
                    }

                    return new TwoSideInfoMap(builder.build());
                });
    }

    private static boolean[] processBone(Model.Bone bone, boolean isSolid, NativeImage intBuffer) {
        bone.children().values().forEach(boneIn -> processBone(boneIn, isSolid, intBuffer));

        boolean[] result = new boolean[bone.cubes().size()];

        for (int i = 0; i < bone.cubes().size(); i++) {
            var cube = bone.cubes().get(i);

            for (int i1 = 0; i1 < cube.vertexes().size(); i1++) {
                if (cubeAnyTransparent(isSolid, intBuffer, cube.uvs().get(i1))) {
                    result[i] = true;
                    break;
                }
            }
        }

        return result;
    }

    private static boolean cubeAnyTransparent(boolean isSolid, NativeImage buffer, List<Vector2fc> uv) {
        int x = (int) (uv.getFirst().x() * buffer.getWidth());
        int y = (int) (uv.getFirst().y() * buffer.getHeight());
        int width = (int) ((uv.get(2).x() - uv.getFirst().x()) * buffer.getWidth());
        int height = (int) ((uv.get(2).y() - uv.getFirst().y()) * buffer.getHeight());

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
