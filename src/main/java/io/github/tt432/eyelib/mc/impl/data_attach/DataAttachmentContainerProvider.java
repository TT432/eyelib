package io.github.tt432.eyelib.mc.impl.data_attach;

import io.github.tt432.eyelib.util.data_attach.IDataAttachmentContainer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataAttachmentContainerProvider implements ICapabilitySerializable<CompoundTag>, ICapabilityProvider {
    private final McDataAttachmentContainer container;

    public DataAttachmentContainerProvider() {
        container = new McDataAttachmentContainer();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return DataAttachmentContainerCapability.INSTANCE.orEmpty(cap, LazyOptional.of(() -> (IDataAttachmentContainer) container));
    }

    @Override
    public CompoundTag serializeNBT() {
        return container.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        container.deserializeNBT(nbt);
    }
}
