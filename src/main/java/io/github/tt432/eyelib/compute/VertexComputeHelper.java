package io.github.tt432.eyelib.compute;

import lombok.Getter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author TT432
 */
public class VertexComputeHelper {
    private final UnsafeWithGlBuffer transformBuffer;
    private final UnsafeWithGlBuffer normalBuffer;
    private final UnsafeWithGlBuffer transformIndexBuffer;
    private final UnsafeWithGlBuffer extraDataBuffer;

    private final UnsafeWithGlBuffer[] buffers;

    private int transformIndex = -1;
    @Getter
    private int vertices;

    @Getter
    private ByteBuffer vertexBufferResult;

    public VertexComputeHelper() {
        transformBuffer = new UnsafeWithGlBuffer(16);
        normalBuffer = new UnsafeWithGlBuffer(12);
        transformIndexBuffer = new UnsafeWithGlBuffer(4);
        extraDataBuffer = new UnsafeWithGlBuffer(4 * 3);
        buffers = new UnsafeWithGlBuffer[]{
                transformBuffer,
                normalBuffer,
                transformIndexBuffer,
                extraDataBuffer
        };
    }

    private static final Matrix4f MATRIX = new Matrix4f();

    public void pushTransform(Matrix4f transform, Matrix3f normal, int color, int overlay, int light) {
        transformIndex++;

        long transformPointer = transformBuffer.reserve(4 * 4 * 4);
        long normalPointer = normalBuffer.reserve(4 * 4 * 3);

        transform.get(MemoryUtil.memByteBuffer(transformPointer, 4 * 4 * 4));
        MATRIX.set(normal).get3x4(MemoryUtil.memByteBuffer(normalPointer, 4 * 4 * 3));

        MemoryUtil.memPutInt(extraDataBuffer.reserve(4), color);
        MemoryUtil.memPutInt(extraDataBuffer.reserve(4), overlay);
        MemoryUtil.memPutInt(extraDataBuffer.reserve(4), light);
    }

    public void addIndex(int vertexCount) {
        var indexPointer = transformIndexBuffer.reserve(vertexCount * 4);

        for (int i = 0; i < vertexCount; i++) {
            MemoryUtil.memPutInt(indexPointer + i * 4L, transformIndex);
        }

        vertices += vertexCount;
    }

    public void clear() {
        for (UnsafeWithGlBuffer buffer : buffers) {
            buffer.reset();
        }
        transformIndex = -1;
        vertices = 0;
    }

    public void compute(int vertexBuffer) {
        if (transformIndexBuffer.writeOffset <= 0) {
            vertexBufferResult = null;
            return;
        }

        for (UnsafeWithGlBuffer buffer : buffers) {
            buffer.upload();
        }

        glUseProgram(VertexComputeShader.getShader().program());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, vertexBuffer);
        transformBuffer.bind(1);
        normalBuffer.bind(2);
        transformIndexBuffer.bind(3);
        extraDataBuffer.bind(4);

        glDispatchCompute(vertices, 1, 1);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
}
