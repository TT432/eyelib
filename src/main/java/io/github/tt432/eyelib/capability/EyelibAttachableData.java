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
public class EyelibAttachableData {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Eyelib.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RenderData<Object>>> RENDER_DATA =
            ATTACHMENT_TYPES.register("render_data",
                    () -> AttachmentType.builder(() -> new RenderData<>())
                            .serialize(RenderData.CODEC)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EntityStatistics>> ENTITY_STATISTICS =
            ATTACHMENT_TYPES.register("entity_statistics",
                    () -> AttachmentType.builder(EntityStatistics::new)
                            .serialize(EntityStatistics.CODEC)
                            .build());
}
