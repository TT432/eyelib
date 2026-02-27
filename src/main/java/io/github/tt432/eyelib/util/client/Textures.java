package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bbmodel.Texture;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;

import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

/**
 * @author TT432
 */
@UtilityClass
public class Textures {
    private static final String BLENDING_SHADER_SOURCE = """
            #version 430 core
            layout (local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
            layout (rgba8, binding = 0) uniform writeonly image2D u_OutputImage;
            layout (binding = 0) uniform sampler2D u_Textures[16];
            uniform int u_TextureCount;
            
            void main() {
                ivec2 pixelCoords = ivec2(gl_GlobalInvocationID.xy);
                ivec2 size = imageSize(u_OutputImage);
                if (pixelCoords.x >= size.x || pixelCoords.y >= size.y) {
                    return;
                }
                vec2 uv = vec2(pixelCoords) / vec2(size);
                vec4 finalColor = vec4(0.0, 0.0, 0.0, 0.0);
                for (int i = 0; i < u_TextureCount; ++i) {
                    vec4 layerColor = texture(u_Textures[i], uv);
                    finalColor = mix(finalColor, layerColor, layerColor.a);
                    finalColor.a = layerColor.a + finalColor.a * (1.0 - layerColor.a);
                }
                imageStore(u_OutputImage, pixelCoords, finalColor);
            }
            """;

    private static int computeProgram = -1;

    private static int getComputeProgram() {
        if (computeProgram != -1) {
            return computeProgram;
        }

        RenderSystem.assertOnRenderThread();

        int shader = GL20.glCreateShader(GL_COMPUTE_SHADER);
        if (shader == 0) {
            throw new RuntimeException("Failed to create compute shader.");
        }

        GL20.glShaderSource(shader, BLENDING_SHADER_SOURCE);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 512);
            throw new RuntimeException("Failed to compile compute shader: " + log);
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(program, 512);
            throw new RuntimeException("Failed to link compute program: " + log);
        }

        GL20.glDeleteShader(shader);

        computeProgram = program;
        return computeProgram;
    }

    /**
     *
     * @param baseTexture 基础纹理的路径
     * @return 发光纹理的路径
     */
    public static String getEmissiveTexturePath(String baseTexture) {
        int lastIndexOfDot = baseTexture.lastIndexOf(".png");

        if (lastIndexOfDot != -1) {
            String beforeDot = baseTexture.substring(0, lastIndexOfDot);
            return beforeDot + ".emissive.png";
        } else {
            return baseTexture;
        }
    }

    /**
     * @param textures textures
     * @return 合并后的图片，需要手动释放
     */
    public NativeImage layerMerging(List<ResourceLocation> textures) {
        if (textures.isEmpty() || textures.size() > 16) {
            return new NativeImage(16, 16, false);
        }

        int maxX = 16;
        int maxY = 16;

        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);

        for (ResourceLocation resourceLocation : textures) {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
            if (texture == MissingTextureAtlasSprite.getTexture()) continue;
            texture.bind();
            glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, widthBuffer);
            glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, heightBuffer);
            if (widthBuffer.get(0) > maxX) maxX = widthBuffer.get(0);
            if (heightBuffer.get(0) > maxY) maxY = heightBuffer.get(0);
            widthBuffer.rewind();
            heightBuffer.rewind();
        }

        NativeImage finalImage = new NativeImage(maxX, maxY, false);
        int program = 0;
        int outputTexture = -1;

        try {
            program = getComputeProgram();
            GL20.glUseProgram(program);

            outputTexture = GlStateManager._genTexture();
            GlStateManager._bindTexture(outputTexture);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            GL20.glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA8, maxX, maxY, 0, GL11.GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer) null);
            glBindImageTexture(0, outputTexture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RGBA8);

            int[] samplers = new int[textures.size()];
            for (int i = 0; i < textures.size(); i++) {
                AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(textures.get(i));
                GlStateManager._activeTexture(GL_TEXTURE0 + i + 1);
                texture.bind();
                samplers[i] = i + 1;
            }

            GL20.glUniform1iv(GL20.glGetUniformLocation(program, "u_Textures"), samplers);
            GL20.glUniform1i(GL20.glGetUniformLocation(program, "u_TextureCount"), textures.size());

            glDispatchCompute((maxX + 7) / 8, (maxY + 7) / 8, 1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            GlStateManager._bindTexture(outputTexture);
            finalImage.downloadTexture(0, false);
        } finally {
            if (program != 0) {
                GL20.glUseProgram(0);
            }
            if (outputTexture != -1) {
                GlStateManager._deleteTexture(outputTexture);
            }
            GlStateManager._activeTexture(GL_TEXTURE0);
        }

        return finalImage;
    }

    public record ModelWithTexture(
            Model model,
            Texture atlasTexture
    ) {
    }

    public record RepackedUV(UV uv, int newX, int newY) {
    }

    private record AutoRepackResult(
            List<RepackedUV> repackedUVs,
            int finalTexH,
            int finalTexW
    ) {
    }

    /**
     * unit: px
     */
    public record UV(Model model, int boneId, Model.Face face, Texture texture, int x, int y, int w, int h) {
        public static List<UV> from(Model model, int boneId, int texW, int texH, Model.Cube cube, Texture texture) {
            List<UV> uvs = new ArrayList<>();
            for (Model.Face face : cube.faces()) {
                Model.Face.Rect uvbox = face.uvbox();
                var u0 = uvbox.u0();
                var u1 = uvbox.u1();
                var v0 = uvbox.v0();
                var v1 = uvbox.v1();
                uvs.add(new UV(
                        model,
                        boneId,
                        face,
                        texture,
                        (int) (u0 * texW),
                        (int) (v0 * texH),
                        (int) ((u1 - u0) * texW),
                        (int) ((v1 - v0) * texH)
                ));
            }
            return uvs;
        }

        @Override
        public @NotNull String toString() {
            return "UV[cube=" + face.toString() +
                    "x=" + x +
                    "y=" + y +
                    "w=" + w +
                    "h=" + h + "]";
        }
    }

    private static List<UV> collectUVs(int texW, int texH, Model model, Texture texture) {
        List<UV> uvs = new ArrayList<>();

        for (Model.Bone bone : model.allBones().values()) {
            for (Model.Cube cube : bone.cubes()) {
                uvs.addAll(UV.from(model, bone.id(), texW, texH, cube, texture));
            }
        }

        return uvs;
    }

    @Nullable
    private static List<RepackedUV> repack(int texW, int texH, Int2ObjectMap<UV> indexedUVs) {
        List<RepackedUV> results = new ArrayList<>();

        try (STBRPContext ctx = STBRPContext.malloc()) {
            try (STBRPNode.Buffer nodes = STBRPNode.malloc(Math.max(1, texW))) {
                STBRectPack.stbrp_init_target(ctx, texW, texH, nodes);

                try (STBRPRect.Buffer rects = STBRPRect.malloc(indexedUVs.size())) {
                    for (Int2ObjectMap.Entry<UV> uvEntry : indexedUVs.int2ObjectEntrySet()) {
                        rects.get(uvEntry.getIntKey())
                                .id(uvEntry.getIntKey())
                                .w(uvEntry.getValue().w())
                                .h(uvEntry.getValue().h());
                    }

                    STBRectPack.stbrp_pack_rects(ctx, rects);

                    for (Int2ObjectMap.Entry<UV> uvEntry : indexedUVs.int2ObjectEntrySet()) {
                        STBRPRect r = rects.get(uvEntry.getIntKey());
                        if (!r.was_packed()) {
                            return null;
                        }
                        results.add(new RepackedUV(uvEntry.getValue(), r.x(), r.y()));
                    }
                }

                return results;
            }
        }
    }

    private static AutoRepackResult autoSizeRepack(int startTexW, int startTexH, Int2ObjectMap<UV> indexedUVs) {
        List<RepackedUV> result;
        while ((result = repack(startTexW, startTexH, indexedUVs)) == null) {
            startTexH *= 2;
            startTexW *= 2;
        }
        return new AutoRepackResult(result, startTexH, startTexW);
    }

    private static Model.Face remapUV(RepackedUV uv, int texW, int texH) {
        int imgW = uv.uv.texture.imageWidth();
        int imgH = uv.uv.texture.imageHeight();

        var face = uv.uv.face;

        List<Model.Vertex> mappedVertices = new ArrayList<>();

        for (Model.Vertex vertex : face.vertexes()) {
            mappedVertices.add(vertex.withUv(
                    vertex.uv()
                            .mul(imgW, imgH, new Vector2f())
                            .sub(uv.uv.x, uv.uv.y)
                            .add(uv.newX, uv.newY)
                            .div(texW, texH)
            ));
        }

        return face.withVertexes(mappedVertices);
    }

    private static List<Model> replaceModelUV(List<RepackedUV> repackedUVs, int texW, int texH) {
        List<Model> result = new ArrayList<>();
        Map<Model, List<RepackedUV>> byModel = new HashMap<>();

        repackedUVs.forEach(repackedUV -> byModel.computeIfAbsent(repackedUV.uv().model, s -> new ArrayList<>()).add(repackedUV));

        for (var entry : byModel.entrySet()) {
            var model = entry.getKey();
            var bones = new Int2ObjectOpenHashMap<>(model.allBones());
            Int2ObjectOpenHashMap<Model.Bone> newBones = new Int2ObjectOpenHashMap<>();

            Int2ObjectMap<List<Model.Face>> byBoneCubes = new Int2ObjectOpenHashMap<>();

            for (RepackedUV repackedUV : entry.getValue()) {
                byBoneCubes.computeIfAbsent(repackedUV.uv.boneId, s -> new ArrayList<>()).add(remapUV(repackedUV, texW, texH));
            }

            bones.keySet().forEach(i -> {
                if (byBoneCubes.containsKey(i)) {
                    newBones.put(i, bones.get(i).withChildren(new Int2ObjectOpenHashMap<>()).withCubes(List.of(new Model.Cube(byBoneCubes.get(i)))));
                } else {
                    newBones.put(i, bones.get(i).withChildren(new Int2ObjectOpenHashMap<>()));
                }
            });
            result.add(new Model(model.name(), newBones, model.locator()));
        }

        return result;
    }

    public static ModelWithTexture repackModels(List<ModelWithTexture> modelWithTextures) {
        List<Model> models = new ArrayList<>();
        List<Texture> textures = new ArrayList<>();

        modelWithTextures.forEach(model -> {
            models.add(model.model);
            textures.add(model.atlasTexture);
        });

        return repackModels(models, textures);
    }

    public static ModelWithTexture repackModels(List<Model> models, List<Texture> textures) {
        int texW = 0;
        int texH = 0;

        for (Texture texture : textures) {
            if (texture.imageHeight() > texH) {
                texH = texture.imageHeight();
            }

            if (texture.imageWidth() > texW) {
                texW = texture.imageWidth();
            }
        }

        Int2ObjectMap<UV> indexedUVs = new Int2ObjectOpenHashMap<>();
        List<UV> uvs = new ArrayList<>();

        for (int i = 0; i < models.size(); i++) {
            var model = models.get(i);
            Texture texture = textures.get(i);

            uvs.addAll(collectUVs(texture.imageWidth(), texture.imageHeight(), model, texture));
        }

        for (int i = 0; i < uvs.size(); i++) {
            indexedUVs.put(i, uvs.get(i));
        }

        var repackResult = autoSizeRepack(texW, texH, indexedUVs);
        NativeImage repackedImage = new NativeImage(repackResult.finalTexW, repackResult.finalTexH, true);

        for (RepackedUV repackedUV : repackResult.repackedUVs) {
            UV uv = repackedUV.uv;
            uv.texture.nativeImage().copyRect(repackedImage, uv.x, uv.y, repackedUV.newX, repackedUV.newY, uv.w, uv.h, false, false);
        }

        String uuid = UUID.randomUUID().toString();
        Texture atlasTexture = new Texture(
                uuid,
                null,
                null,
                null,
                uuid,
                null,
                repackResult.finalTexW,
                repackResult.finalTexH,
                repackResult.finalTexW,
                repackResult.finalTexH,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                0,
                null,
                null,
                false,
                true,
                true,
                false,
                uuid,
                null,
                repackedImage
        );

        NativeImage r = new NativeImage(repackResult.finalTexW, repackResult.finalTexH, true);
        r.copyFrom(repackedImage);
        Minecraft.getInstance().getTextureManager().register(ResourceLocation.withDefaultNamespace(uuid), new DynamicTexture(r));

        return new ModelWithTexture(Models.merge(replaceModelUV(repackResult.repackedUVs, repackResult.finalTexW, repackResult.finalTexH)), atlasTexture);
    }
}
