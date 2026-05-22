package io.github.tt432.eyelibmodel.locator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmodel.ImporterCodecs;
import io.github.tt432.eyelibmodel.tree.ModelCubeNode;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record LocatorEntry(
        String name,
        Vector3f offset,
        Vector3f rotation,
        boolean ignoreInheritedScale,
        boolean isNullObject
) implements ModelCubeNode {
    public LocatorEntry(String name, Vector3f offset, Vector3f rotation) {
        this(name, offset, rotation, false, false);
    }

    public static final Codec<LocatorEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(LocatorEntry::name),
            ImporterCodecs.VECTOR3F.fieldOf("offset").forGetter(LocatorEntry::offset),
            ImporterCodecs.VECTOR3F.fieldOf("rotation").forGetter(LocatorEntry::rotation),
            Codec.BOOL.optionalFieldOf("ignoreInheritedScale", false).forGetter(LocatorEntry::ignoreInheritedScale),
            Codec.BOOL.optionalFieldOf("isNullObject", false).forGetter(LocatorEntry::isNullObject)
    ).apply(ins, LocatorEntry::new));
}