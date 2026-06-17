package io.github.tt432.eyelibattachment.dataattach.mc;

import io.github.tt432.eyelibattachment.dataattach.IDataAttachmentContainer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jspecify.annotations.Nullable;

/**
 * 数据附属容器的能力提供者。
 *
 * @author TT432
 */
public class DataAttachmentContainerProvider implements ICapabilitySerializable<CompoundTag>, ICapabilityProvider {
    private final McDataAttachmentContainer container;

    public DataAttachmentContainerProvider() {
        container = new McDataAttachmentContainer();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
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