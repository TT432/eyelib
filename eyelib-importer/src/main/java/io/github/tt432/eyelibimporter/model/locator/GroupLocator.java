package io.github.tt432.eyelibimporter.model.locator;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.*;
import io.github.tt432.eyelibimporter.model.*;
import io.github.tt432.eyelibimporter.model.tree.*;
import it.unimi.dsi.fastutil.ints.*;
import org.jspecify.annotations.*;

import java.util.*;

public record GroupLocator(
        Int2ObjectMap<GroupLocator> children,
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
    @Nullable
    public LocatorEntry getCubeNode(int index) {
        if (index < 0 || index >= cubes.size()) return null;
        return cubes.get(index);
    }
}

