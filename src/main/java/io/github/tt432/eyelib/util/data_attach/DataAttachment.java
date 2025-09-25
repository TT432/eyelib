package io.github.tt432.eyelib.util.data_attach;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Data attachments.
 * @param capability  Cap instance (We can get it from CapabilityManager anytime!)
 * @param id          Cap ID
 * @param supplier    Cap factory method
 * @param codec       (De)Serializer (Null for not persist)
 * @param streamCodec Codec for network syncing (Null for not sync)
 * @param <C>         Cap Type
 */
public record DataAttachment<C>(Capability<C> capability,
                                ResourceLocation id,
                                Supplier<C> supplier,
                                @Nullable Codec<C> codec,
                                @Nullable StreamCodec<C> streamCodec) {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataAttachment.class);

    public DataAttachment(ResourceLocation id, Supplier<C> supplier, @Nullable Codec<C> codec, @Nullable StreamCodec<C> streamCodec) {
        this(CapabilityManager.get(new CapabilityToken<>() {
        }), id, supplier, codec, streamCodec);
    }

    public Provider<C> createDataAttachmentProvider() {
        return new Provider<>(this);
    }

    public static class Provider<C> implements ICapabilitySerializable<Tag>, ICapabilityProvider {

        private final DataAttachment<C> dataAttachment;

        private C data;

        private final LazyOptional<C> optional = LazyOptional.of(() -> data);

        public Provider(DataAttachment<C> dataAttachment) {
            this.dataAttachment = dataAttachment;
            data = dataAttachment.supplier().get();
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return dataAttachment.capability.orEmpty(cap, optional);
        }

        private void invalidate() {
            optional.invalidate();
        }

        @Override
        public Tag serializeNBT() {
            if (dataAttachment.codec != null) {
                var result = dataAttachment.codec.encodeStart(NbtOps.INSTANCE, data);
                // XXX: Recoverable fail.
                return result.getOrThrow(false, LOGGER::warn);
            }

            // XXX: could we return a null?
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            if (dataAttachment.codec != null) {
                var result = dataAttachment.codec.decode(NbtOps.INSTANCE, nbt);
                var cTagPair = result.getOrThrow(false, LOGGER::warn);
                // XXX: Recoverable fail.
                data = cTagPair.getFirst();
                invalidate();
            }
        }
    }
}
