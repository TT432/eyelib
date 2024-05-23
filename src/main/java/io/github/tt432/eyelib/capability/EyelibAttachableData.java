package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.Eyelib;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
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
    public static final Capability<AnimatableComponent<?>> ANIMATABLE = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class RegistryEvents {
        @SubscribeEvent
        public static void registerCaps(RegisterCapabilitiesEvent event) {
            event.register(AnimatableComponent.class);
        }
    }

    @Mod.EventBusSubscriber
    public static final class AttachCapabilitiesEvents {
        @SubscribeEvent
        public static void onEvent(AttachCapabilitiesEvent event) {
            ICapabilityProvider provider = new ICapabilityProvider() {
                LazyOptional<AnimatableComponent<?>> capa = LazyOptional.of(() -> {
                    AnimatableComponent<Object> result = new AnimatableComponent<>();
                    result.init(event.getObject());
                    return result;
                });

                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction direction) {
                    if (cap == ANIMATABLE) {
                        return capa.cast();
                    }
                    return LazyOptional.empty();
                }
            };
            event.addCapability(new ResourceLocation(Eyelib.MOD_ID, "animatable"), provider);
        }
    }
}
