package io.github.tt432.eyelib.client.model.locator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.tree.ModelTree;

import java.util.Map;

/**
 * @author TT432
 */
public record ModelLocator(
        Map<String, GroupLocator> groupLocatorMap
) implements ModelTree<GroupLocator, LocatorEntry> {
    public static final Codec<ModelLocator> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.unboundedMap(Codec.STRING, GroupLocator.CODEC).fieldOf("groupLocatorMap").forGetter(ModelLocator::groupLocatorMap)
    ).apply(ins, ModelLocator::new));

    @Override
    public GroupLocator getGroup(String groupName) {
        return groupLocatorMap.get(groupName);
    }
}
