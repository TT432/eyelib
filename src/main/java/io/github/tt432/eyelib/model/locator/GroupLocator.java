package io.github.tt432.eyelib.model.locator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.tree.ModelGroupNode;
import io.github.tt432.eyelib.util.codec.CodecOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author TT432
 */
public record GroupLocator(
        Int2ObjectMap<GroupLocator> children,
        List<LocatorEntry> cubes
) implements ModelGroupNode<LocatorEntry> {
    // 自引用 codec 必须延迟初始化：直接引用 GroupLocator.CODEC 会在类初始化期间读到 null，
    // 使 children map codec 的 elementCodec 为 null（编码非空 locator 树时 NPE）。
    public static final Codec<GroupLocator> CODEC = CodecOps.lazyCodec(() -> RecordCodecBuilder.create(ins -> ins.group(
            GlobalBoneIdHandler.map(GroupLocator.CODEC).fieldOf("children").forGetter(GroupLocator::children),
            LocatorEntry.CODEC.listOf().fieldOf("cubes").forGetter(GroupLocator::cubes)
    ).apply(ins, GroupLocator::new)));

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