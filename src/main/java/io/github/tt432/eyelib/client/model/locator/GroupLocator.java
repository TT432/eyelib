package io.github.tt432.eyelib.client.model.locator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.tree.ModelGroupNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.List;

/**
 * @author TT432
 */
public record GroupLocator(
        Int2ObjectMap< GroupLocator> children,
        List<LocatorEntry> cubes
) implements ModelGroupNode<LocatorEntry> {
    public static final Codec<GroupLocator> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            GlobalBoneIdHandler.map(GroupLocator.CODEC).fieldOf("children").forGetter(GroupLocator::children),
            LocatorEntry.CODEC.listOf().fieldOf("cubes").forGetter(GroupLocator::cubes)
    ).apply(ins, GroupLocator::new));

    @Override
    public GroupLocator getChild(int groupName) {
        return children.get(groupName);
    }

    @Override
    public LocatorEntry getCubeNode(int index) {
        if (index < 0 || index >= cubes.size()) return null;
        return cubes.get(index);
    }
}
