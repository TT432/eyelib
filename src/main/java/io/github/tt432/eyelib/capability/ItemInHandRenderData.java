package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.type.MolangString;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * @author TT432
 */
public record ItemInHandRenderData(
        RenderData<ItemStack> leftHandData,
        RenderData<ItemStack> rightHandData
) {
    public static final Codec<ItemInHandRenderData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            RenderData.<ItemStack>codec().fieldOf("leftHandData").forGetter(o -> o.leftHandData),
            RenderData.<ItemStack>codec().fieldOf("rightHandData").forGetter(o -> o.rightHandData)
    ).apply(ins, ItemInHandRenderData::new));

    public static ItemInHandRenderData empty() {
        return new ItemInHandRenderData(new RenderData<>(), new RenderData<>());
    }

    public void init(LivingEntity entity, RenderData<?> parents) {
        if (entity.getMainHandItem() != leftHandData.getOwner()) {
            leftHandData.init(entity.getMainHandItem());
            leftHandData.getScope().setParent(parents.getScope());
            leftHandData.getScope().set("context.item_slot", MolangString.valueOf("off_hand"));
        }
        if (entity.getOffhandItem() != rightHandData.getOwner()) {
            rightHandData.init(entity.getOffhandItem());
            rightHandData.getScope().setParent(parents.getScope());
            leftHandData.getScope().set("context.item_slot", MolangString.valueOf("main_hand"));
        }
    }
}
