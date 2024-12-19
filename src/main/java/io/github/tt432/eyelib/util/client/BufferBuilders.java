package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import lombok.experimental.UtilityClass;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * @author TT432
 */
@UtilityClass
public class BufferBuilders {
    public void putAll(BufferBuilder a, BufferBuilder b) {
        putAll(a, b.buffer, b.vertices);
    }

    public void putAll(BufferBuilder a, ByteBufferBuilder b, int vertices) {
        ByteBufferBuilder aBuffer = a.buffer;
        long reserve = aBuffer.reserve(b.writeOffset);
        a.vertexPointer = reserve;
        MemoryUtil.memCopy(b.pointer, reserve, b.writeOffset);

        a.vertices += vertices;
    }

    public void putAll(BufferBuilder a, ByteBuffer result, int vertices) {
        ByteBufferBuilder aBuffer = a.buffer;
        long reserve = aBuffer.reserve(result.capacity());
        a.vertexPointer = reserve;
        MemoryUtil.memCopy(MemoryUtil.memAddress(result), reserve, result.capacity());

        a.vertices += vertices;
    }
}
