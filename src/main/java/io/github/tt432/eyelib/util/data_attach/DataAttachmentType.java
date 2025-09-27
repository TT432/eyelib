package io.github.tt432.eyelib.util.data_attach;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Data attachments.
 *
 * @param id          Attachment ID
 * @param factory     Factory method
 * @param codec       (De)Serializer (Null for not persist)
 * @param streamCodec Codec for network syncing (Null for not sync)
 * @param <C>         Attachment Type
 */
public record DataAttachmentType<C>(ResourceLocation id,
                                    Supplier<C> factory,
                                    @Nullable Codec<C> codec,
                                    @Nullable StreamCodec<C> streamCodec) {

    public @NotNull StreamCodec<C> getStreamCodec() {
        if (streamCodec == null) {
            throw new IllegalStateException(id + " has no StreamCodec");
        }
        return streamCodec;
    }
}
