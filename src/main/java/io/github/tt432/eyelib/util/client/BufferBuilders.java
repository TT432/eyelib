package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import lombok.experimental.UtilityClass;
import org.lwjgl.system.MemoryUtil;

/**
 * @author TT432
 */
@UtilityClass
public class BufferBuilders {
    public void putAll(BufferBuilder a, BufferBuilder b) {
        ByteBufferBuilder aBuffer = a.buffer;
        var bBuffer = b.buffer;
        long reserve = aBuffer.reserve(bBuffer.writeOffset);
        a.vertexPointer = reserve;
        MemoryUtil.memCopy(bBuffer.pointer, reserve, bBuffer.writeOffset);

        a.vertices += b.vertices;
    }
}
