package io.github.tt432.eyelib.bridge.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
//? if <26.1 {
import com.mojang.blaze3d.platform.GlStateManager;
//?} else {
import com.mojang.blaze3d.opengl.GlTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
//?}
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

/** @author TT432 */
@UtilityClass
public class TextureLayerMerger {
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
                vec2 uv = (vec2(pixelCoords) + vec2(0.5)) / vec2(size);
                vec4 finalColor = vec4(0.0, 0.0, 0.0, 0.0);
                for (int i = 0; i < u_TextureCount; ++i) {
                    vec4 dst = finalColor;
                    vec4 src = texture(u_Textures[i], uv);
                    finalColor.rgb = src.rgb * src.a + dst.rgb * (1.0 - src.a);
                    finalColor.a = src.a + dst.a * (1.0 - src.a);
                }
                imageStore(u_OutputImage, pixelCoords, finalColor);
            }
            """;

    private static int computeProgram = -1;

    public NativeImage merge(List<ResourceLocation> textures) {
        if (textures.isEmpty() || textures.size() > 16) {
            return new NativeImage(16, 16, false);
        }
        // 过滤掉纹理管理器中不存在的纹理，避免 FileNotFoundException
        //? if <26.1 {
        List<ResourceLocation> validTextures = filterExisting(textures);
        if (validTextures.isEmpty()) {
            return new NativeImage(16, 16, false);
        }

        int maxX = 16;
        int maxY = 16;
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);

        for (ResourceLocation resourceLocation : validTextures) {
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
            GL20.glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA8, maxX, maxY, 0, GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer) null);
            glBindImageTexture(0, outputTexture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RGBA8);

            int validCount = validTextures.size();
            int[] samplers = new int[validCount];
            for (int i = 0; i < validCount; i++) {
                AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(validTextures.get(i));
                GlStateManager._activeTexture(GL_TEXTURE0 + i + 1);
                texture.bind();
                samplers[i] = i + 1;
            }

            GL20.glUniform1iv(GL20.glGetUniformLocation(program, "u_Textures"), samplers);
            GL20.glUniform1i(GL20.glGetUniformLocation(program, "u_TextureCount"), validCount);

            glDispatchCompute((maxX + 7) / 8, (maxY + 7) / 8, 1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            GlStateManager._bindTexture(outputTexture);
            finalImage.downloadTexture(0, false);
        } finally {
            if (program != 0) GL20.glUseProgram(0);
            if (outputTexture != -1) GlStateManager._deleteTexture(outputTexture);
            GlStateManager._activeTexture(GL_TEXTURE0);
        }

        return finalImage;
        //?} else {
        List<ResourceLocation> validTextures = filterExisting(textures);
        if (validTextures.isEmpty()) {
            return new NativeImage(16, 16, false);
        }

        int maxX = 16;
        int maxY = 16;
        List<GlTexture> glTextures = new ArrayList<>(validTextures.size());

        for (ResourceLocation resourceLocation : validTextures) {
            try {
                AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
                var gpuTexture = texture.getTexture();
                if (!(gpuTexture instanceof GlTexture glTexture)) continue;
                glTextures.add(glTexture);
                if (glTexture.getWidth(0) > maxX) maxX = glTexture.getWidth(0);
                if (glTexture.getHeight(0) > maxY) maxY = glTexture.getHeight(0);
            } catch (Exception ignored) {
            }
        }
        if (glTextures.isEmpty()) {
            return new NativeImage(16, 16, false);
        }

        int program = 0;
        int outputTexture = -1;

        try {
            program = getComputeProgram();
            GL20.glUseProgram(program);

            outputTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, outputTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL20.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, maxX, maxY, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer) null);
            glBindImageTexture(0, outputTexture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RGBA8);

            int validCount = glTextures.size();
            int[] samplers = new int[validCount];
            for (int i = 0; i < validCount; i++) {
                GL13.glActiveTexture(GL_TEXTURE0 + i + 1);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextures.get(i).glId());
                samplers[i] = i + 1;
            }

            GL20.glUniform1iv(GL20.glGetUniformLocation(program, "u_Textures"), samplers);
            GL20.glUniform1i(GL20.glGetUniformLocation(program, "u_TextureCount"), validCount);

            glDispatchCompute((maxX + 7) / 8, (maxY + 7) / 8, 1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            return NativeImageIO.readBackTexture(outputTexture, maxX, maxY);
        } finally {
            if (program != 0) GL20.glUseProgram(0);
            if (outputTexture != -1) GL11.glDeleteTextures(outputTexture);
            GL13.glActiveTexture(GL_TEXTURE0);
        }
        //?}
    }

    private static List<ResourceLocation> filterExisting(List<ResourceLocation> textures) {
        //? if <26.1 {
        List<ResourceLocation> valid = new ArrayList<>(textures.size());
        for (ResourceLocation resourceLocation : textures) {
            try {
                AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
                if (tex != MissingTextureAtlasSprite.getTexture()) {
                    valid.add(resourceLocation);
                }
            } catch (Exception ignored) {
            }
        }
        return valid;
        //?} else {
        return new ArrayList<>(textures);
        //?}
    }

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
            throw new RuntimeException("Failed to compile compute shader: " + GL20.glGetShaderInfoLog(shader, 512));
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Failed to link compute program: " + GL20.glGetProgramInfoLog(program, 512));
        }

        GL20.glDeleteShader(shader);
        computeProgram = program;
        return computeProgram;
    }
}
