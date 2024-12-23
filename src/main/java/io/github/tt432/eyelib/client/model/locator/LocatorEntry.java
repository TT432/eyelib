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
        Vector3f rotation
) implements ModelCubeNode {
    public static final Codec<LocatorEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(LocatorEntry::name),
            ExtraCodecs.VECTOR3F.fieldOf("offset").forGetter(LocatorEntry::offset),
            ExtraCodecs.VECTOR3F.fieldOf("rotation").forGetter(LocatorEntry::rotation)
    ).apply(ins, LocatorEntry::new));
}
