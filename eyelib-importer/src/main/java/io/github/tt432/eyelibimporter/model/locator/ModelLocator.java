package io.github.tt432.eyelibimporter.model.locator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibimporter.model.tree.ModelTree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public record ModelLocator(
        Int2ObjectMap<GroupLocator> groupLocatorMap
) implements ModelTree<GroupLocator, LocatorEntry> {
    public static final Codec<ModelLocator> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            GlobalBoneIdHandler.map(GroupLocator.CODEC).fieldOf("groupLocatorMap").forGetter(ModelLocator::groupLocatorMap)
    ).apply(ins, ModelLocator::new));

    @Override
    public GroupLocator getGroup(int groupName) {
        return groupLocatorMap.get(groupName);
    }
}
