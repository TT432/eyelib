package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bbmodel.Texture;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrModelEntry;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

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

    public record RepackedImage<B extends Model.Bone<B>, M extends Model<B>>(
            M model,
            Texture atlasTexture,
            NativeImage atlasImage,
            Int2ObjectMap<AtlasRegion> regions
    ) {
    }

    public record AtlasRegion(int x, int y, int w, int h) {
    }

    private record AtlasPacking(int width, int height, Int2ObjectMap<AtlasRegion> regions) {
    }

    public static <B extends Model.Bone<B>, M extends Model<B>> RepackedImage<B, M> repackedImage(
            List<M> models,
            List<NativeImage> images,
            BiFunction<B, List<Model.Cube>, B> boneFunction,
            BiFunction<B, Integer, B> idFunction,
            BiFunction<B, Integer, B> parentFunction,
            BiFunction<M, List<B>, M> modelFunction
    ) {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("models is empty");
        }
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("images is empty");
        }
        if (models.size() != images.size()) {
            throw new IllegalArgumentException("models.size != images.size");
        }

        int padding = 1;
        int n = images.size();
        int[] w = new int[n];
        int[] h = new int[n];

        for (int i = 0; i < n; i++) {
            NativeImage img = images.get(i);
            w[i] = img != null ? Math.max(1, img.getWidth()) : 1;
            h[i] = img != null ? Math.max(1, img.getHeight()) : 1;
        }

        AtlasPacking packing;
        try {
            packing = packAtlas(w, h, padding);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        NativeImage atlas = new NativeImage(packing.width(), packing.height(), true);
        for (int i = 0; i < n; i++) {
            AtlasRegion region = packing.regions().get(i);
            if (region == null) continue;
            NativeImage src = images.get(i);
            if (src == null) continue;

            int copyW = Math.min(region.w(), src.getWidth());
            int copyH = Math.min(region.h(), src.getHeight());
            for (int yy = 0; yy < copyH; yy++) {
                for (int xx = 0; xx < copyW; xx++) {
                    atlas.setPixelRGBA(region.x() + xx, region.y() + yy, src.getPixelRGBA(xx, yy));
                }
            }
        }

        Texture atlasTexture = new Texture(
                "atlas",
                null,
                null,
                null,
                "atlas",
                null,
                packing.width(),
                packing.height(),
                packing.width(),
                packing.height(),
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
                UUID.randomUUID().toString(),
                null,
                null
        );

        List<M> remappedModels = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            M model = models.get(i);
            AtlasRegion region = packing.regions().get(i);
            NativeImage img = images.get(i);

            int srcW = img != null ? Math.max(1, img.getWidth()) : 1;
            int srcH = img != null ? Math.max(1, img.getHeight()) : 1;
            int ox = region != null ? region.x() : 0;
            int oy = region != null ? region.y() : 0;

            List<B> newBones = new ArrayList<>(model.allBones().size());
            for (B bone : model.allBones().values()) {
                List<Model.Cube> newCubes = new ArrayList<>(bone.cubes().size());
                for (Model.Cube cube : bone.cubes()) {
                    newCubes.add(remapCubeUv(cube, srcW, srcH, packing.width(), packing.height(), ox, oy));
                }
                newBones.add(boneFunction.apply(bone, newCubes));
            }

            remappedModels.add(modelFunction.apply(model, newBones));
        }

        M merged = remappedModels.get(0);
        for (int i = 1; i < remappedModels.size(); i++) {
            merged = Models.add(merged, remappedModels.get(i), boneFunction, idFunction, parentFunction, modelFunction);
        }

        return new RepackedImage<>(merged, atlasTexture, atlas, packing.regions());
    }

    public static RepackedImage<BrBone, BrModelEntry> repackedImage(List<BrModelEntry> models, List<NativeImage> images) {
        return repackedImage(
                models,
                images,
                (bone, cubes) -> {
                    List<io.github.tt432.eyelib.client.model.bedrock.BrCube> brCubes = new ArrayList<>();
                    for (Model.Cube cube : cubes) {
                        if (cube instanceof io.github.tt432.eyelib.client.model.bedrock.BrCube brCube) {
                            brCubes.add(brCube);
                        }
                    }
                    return bone.withChildren(new Int2ObjectOpenHashMap<>()).withCubes(brCubes);
                },
                BrBone::withId,
                BrBone::withParent,
                (oldModel, bones) -> {
                    BrModelEntry oldEntry = (BrModelEntry) oldModel;
                    Int2ObjectMap<BrBone> allBones = new Int2ObjectOpenHashMap<>();
                    for (BrBone bone : bones) {
                        allBones.put(bone.id(), bone);
                    }

                    Int2ObjectMap<BrBone> toplevelBones = new Int2ObjectOpenHashMap<>();

                    allBones.int2ObjectEntrySet().forEach((entry) -> {
                        var name = entry.getIntKey();
                        var bone = entry.getValue();
                        if (bone.parent() == -1 || allBones.get(bone.parent()) == null)
                            toplevelBones.put(name, bone);
                        else
                            allBones.get(bone.parent()).children().put(name, bone);
                    });

                    Int2ObjectMap<GroupLocator> locators = new Int2ObjectOpenHashMap<>();
                    toplevelBones.int2ObjectEntrySet().forEach(entry -> locators.put(entry.getIntKey(), getLocator(entry.getValue())));

                    return oldEntry.withToplevelBones(toplevelBones)
                            .withAllBones(allBones)
                            .withLocator(new ModelLocator(locators));
                }
        );
    }

    private static GroupLocator getLocator(BrBone bone) {
        Int2ObjectMap<GroupLocator> children = new Int2ObjectOpenHashMap<>();
        bone.children().int2ObjectEntrySet().forEach((entry) -> {
            var name = entry.getIntKey();
            var group = entry.getValue();
            children.put(name, getLocator(group));
        });
        List<LocatorEntry> list = new ArrayList<>();
        for (var brLocator : bone.locators().values()) {
            list.add(brLocator.locatorEntry());
        }
        return new GroupLocator(children, list);
    }

    private static Model.Cube remapCubeUv(
            Model.Cube cube,
            int srcW,
            int srcH,
            int atlasW,
            int atlasH,
            int ox,
            int oy
    ) {
        List<List<org.joml.Vector2f>> oldUvs = cube.uvs();
        List<List<org.joml.Vector2f>> newUvs = new ArrayList<>(oldUvs.size());
        for (List<org.joml.Vector2f> face : oldUvs) {
            List<org.joml.Vector2f> faceNew = new ArrayList<>(face.size());
            for (org.joml.Vector2f uv : face) {
                float u = (uv.x * srcW + ox) / (float) atlasW;
                float v = (uv.y * srcH + oy) / (float) atlasH;
                faceNew.add(new org.joml.Vector2f(u, v));
            }
            newUvs.add(faceNew);
        }

        if (cube instanceof Model.Cube.ConstCube constCube) {
            return recreateConstCube(constCube, newUvs);
        }

        return new Model.Cube() {
            @Override
            public int faceCount() {
                return cube.faceCount();
            }

            @Override
            public int pointsPerFace() {
                return cube.pointsPerFace();
            }

            @Override
            public float positionX(int faceIndex, int pointIndex) {
                return cube.positionX(faceIndex, pointIndex);
            }

            @Override
            public float positionY(int faceIndex, int pointIndex) {
                return cube.positionY(faceIndex, pointIndex);
            }

            @Override
            public float positionZ(int faceIndex, int pointIndex) {
                return cube.positionZ(faceIndex, pointIndex);
            }

            @Override
            public float uvU(int faceIndex, int pointIndex) {
                return newUvs.get(faceIndex).get(pointIndex).x;
            }

            @Override
            public float uvV(int faceIndex, int pointIndex) {
                return newUvs.get(faceIndex).get(pointIndex).y;
            }

            @Override
            public float normalX(int faceIndex) {
                return cube.normalX(faceIndex);
            }

            @Override
            public float normalY(int faceIndex) {
                return cube.normalY(faceIndex);
            }

            @Override
            public float normalZ(int faceIndex) {
                return cube.normalZ(faceIndex);
            }
        };
    }

    private static Model.Cube recreateConstCube(Model.Cube.ConstCube cube, List<List<org.joml.Vector2f>> newUvs) {
        Class<?> clz = cube.getClass();
        if (!clz.isRecord()) {
            return new io.github.tt432.eyelib.client.model.Model.Cube.ConstCube() {
                @Override
                public int faceCount() {
                    return cube.faceCount();
                }

                @Override
                public int pointsPerFace() {
                    return cube.pointsPerFace();
                }

                @Override
                public List<List<org.joml.Vector3f>> vertexes() {
                    return cube.vertexes();
                }

                @Override
                public List<List<org.joml.Vector2f>> uvs() {
                    return newUvs;
                }

                @Override
                public List<org.joml.Vector3f> normals() {
                    return cube.normals();
                }
            };
        }

        try {
            var ctor = clz.getDeclaredConstructor(int.class, int.class, List.class, List.class, List.class);
            ctor.setAccessible(true);
            return (Model.Cube) ctor.newInstance(cube.faceCount(), cube.pointsPerFace(), cube.vertexes(), newUvs, cube.normals());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to recreate cube: " + clz.getName(), e);
        }
    }

    private static AtlasPacking packAtlas(int[] w, int[] h, int padding) throws java.io.IOException {
        long area = 0;
        int maxW = 1;
        int maxH = 1;
        int n = w.length;

        int[] rw = new int[n];
        int[] rh = new int[n];

        for (int i = 0; i < n; i++) {
            int ww = Math.max(1, w[i]) + padding * 2;
            int hh = Math.max(1, h[i]) + padding * 2;
            rw[i] = ww;
            rh[i] = hh;
            area += (long) ww * (long) hh;
            maxW = Math.max(maxW, ww);
            maxH = Math.max(maxH, hh);
        }

        int size = (int) Math.ceil(Math.sqrt(area));
        int start = nextPow2(Math.max(size, Math.max(maxW, maxH)));
        int atlasW = start;
        int atlasH = start;

        for (int i = 0; i < 12; i++) {
            AtlasPacking packed = tryPack(atlasW, atlasH, rw, rh, padding);
            if (packed != null) {
                return packed;
            }
            if (atlasW <= atlasH) atlasW *= 2;
            else atlasH *= 2;
        }

        throw new java.io.IOException("Failed to pack textures into atlas");
    }

    private static AtlasPacking tryPack(int atlasW, int atlasH, int[] rw, int[] rh, int padding) {
        int n = rw.length;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            STBRPContext ctx = STBRPContext.malloc(stack);
            STBRPNode.Buffer nodes = STBRPNode.malloc(Math.max(1, atlasW), stack);
            STBRectPack.stbrp_init_target(ctx, atlasW, atlasH, nodes);

            Int2ObjectMap<AtlasRegion> regions = new Int2ObjectOpenHashMap<>();
            try (STBRPRect.Buffer rects = STBRPRect.malloc(n, stack)) {
                for (int i = 0; i < n; i++) {
                    rects.get(i).id(i).w(rw[i]).h(rh[i]);
                }

                STBRectPack.stbrp_pack_rects(ctx, rects);

                for (int i = 0; i < n; i++) {
                    STBRPRect r = rects.get(i);
                    if (!r.was_packed()) {
                        return null;
                    }
                    regions.put(r.id(), new AtlasRegion(r.x() + padding, r.y() + padding, rw[r.id()] - padding * 2, rh[r.id()] - padding * 2));
                }
            }

            return new AtlasPacking(atlasW, atlasH, regions);
        }
    }

    private static int nextPow2(int v) {
        int x = Math.max(1, v - 1);
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
    }
}
