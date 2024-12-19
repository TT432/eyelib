package io.github.tt432.eyelib.compute;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.extern.slf4j.Slf4j;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

/**
 * @author TT432
 */
@Slf4j
public record ComputeShader(
        String content,
        int program
) {
    public static ComputeShader of(String content) {
        RenderSystem.assertOnRenderThread();

        int shader = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(shader, content);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            log.error("Shader compile error: {}", glGetShaderInfoLog(shader));
        }

        var program = glCreateProgram();
        glAttachShader(program, shader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            log.error("Program link error: {}", glGetProgramInfoLog(program));
        }

        return new ComputeShader(content, program);
    }
}
