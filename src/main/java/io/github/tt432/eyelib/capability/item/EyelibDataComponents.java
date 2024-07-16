package io.github.tt432.eyelib.capability.item;

import io.github.tt432.eyelib.Eyelib;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Eyelib.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemTooltipReplaceData>> ITEM_TOOLTIP_REPLACE_DATA =
            COMPONENTS.register("item_tooltip_replace_data",
                    () -> DataComponentType.<ItemTooltipReplaceData>builder()
                            .persistent(ItemTooltipReplaceData.CODEC)
                            .networkSynchronized(ItemTooltipReplaceData.STREAM_CODEC).build());
}
