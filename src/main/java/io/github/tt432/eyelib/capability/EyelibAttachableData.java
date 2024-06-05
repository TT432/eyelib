package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibAttachableData {
    public static final Capability<RenderData<?>> ANIMATABLE = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class RegistryEvents {
        @SubscribeEvent
        public static void registerCaps(RegisterCapabilitiesEvent event) {
            event.register(RenderData.class);
        }
    }

    @Mod.EventBusSubscriber
    public static final class AttachCapabilitiesEvents {
        @Slf4j
        static class RenderDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
            LazyOptional<RenderData<?>> capa;

            public RenderDataProvider(AttachCapabilitiesEvent event) {
                capa = LazyOptional.of(() -> {
                    RenderData<Object> result = new RenderData<>();
                    result.init(event.getObject());
                    return result;
                });
            }

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction direction) {
                if (cap == ANIMATABLE) {
                    return capa.cast();
                }
                return LazyOptional.empty();
            }

            @Override
            @SuppressWarnings("unchecked")
            public CompoundTag serializeNBT() {
                return capa.map(rd -> (CompoundTag) RenderData.CODEC.encodeStart(NbtOps.INSTANCE, (RenderData<Object>) rd)
                        .getOrThrow(true, log::error)).orElse(new CompoundTag());
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                RenderData<Object> orThrow = RenderData.CODEC.parse(NbtOps.INSTANCE, nbt)
                        .getOrThrow(true, log::error);
                capa.ifPresent(rd -> {
                    rd.getModelComponent().setInfo(orThrow.getModelComponent().getSerializableInfo());
                    AnimationComponent.SerializableInfo serializableInfo = orThrow.getAnimationComponent().getSerializableInfo();

                    if (serializableInfo != null) {
                        rd.getAnimationComponent().setup(serializableInfo.animationControllers(), serializableInfo.targetAnimations());
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onEvent(AttachCapabilitiesEvent event) {
            event.addCapability(new ResourceLocation(Eyelib.MOD_ID, "render_data"), new RenderDataProvider(event));
        }
    }
}
