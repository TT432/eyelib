package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.nio.ByteBuffer;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BedrockBinaryAsset(
        String extension,
        byte[] bytes
) {
    public static final Codec<BedrockBinaryAsset> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("extension").forGetter(BedrockBinaryAsset::extension),
            Codec.BYTE_BUFFER.xmap(ByteBuffer::array, ByteBuffer::wrap)
                    .fieldOf("bytes")
                    .forGetter(BedrockBinaryAsset::bytes)
    ).apply(ins, BedrockBinaryAsset::new));

    public BedrockBinaryAsset {
        bytes = bytes.clone();
    }
}
