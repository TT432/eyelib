package io.github.tt432.eyelib.client.model.locator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.tree.ModelCubeNode;
import net.minecraft.util.ExtraCodecs;
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
            ExtraCodecs.VECTOR3F.fieldOf("offset").forGetter(LocatorEntry::offset),
            ExtraCodecs.VECTOR3F.fieldOf("rotation").forGetter(LocatorEntry::rotation),
            Codec.BOOL.optionalFieldOf("ignoreInheritedScale", false).forGetter(LocatorEntry::ignoreInheritedScale),
            Codec.BOOL.optionalFieldOf("isNullObject", false).forGetter(LocatorEntry::isNullObject)
    ).apply(ins, LocatorEntry::new));
}
