package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.Eyelib;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibCapabilities {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Eyelib.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AnimatableCapability<Object>>> ANIMATABLE =
            ATTACHMENT_TYPES.register("animatable",
                    () -> AttachmentType.builder(() -> new AnimatableCapability<>()).build());
}
