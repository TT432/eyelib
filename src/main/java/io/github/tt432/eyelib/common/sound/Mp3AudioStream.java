package io.github.tt432.eyelib.common.sound;

import net.minecraft.client.sounds.AudioStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author DustW
 */
public class Mp3AudioStream implements AudioStream {
    AudioInputStream stream;
    byte[] array;
    int offset;

    public Mp3AudioStream(AudioInputStream stream) throws IOException {
        this.stream = stream;
        this.array = IOUtils.toByteArray(stream);
        this.offset = 0;
    }

    @Override
    public @NotNull AudioFormat getFormat() {
        return stream.getFormat();
    }

    @Override
    public @NotNull ByteBuffer read(int pSize) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(pSize);
        if (array.length >= offset + pSize) {
            byteBuffer.put(array, offset, pSize);
        } else {
            byteBuffer.put(new byte[pSize]);
        }
        offset += pSize;
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}

