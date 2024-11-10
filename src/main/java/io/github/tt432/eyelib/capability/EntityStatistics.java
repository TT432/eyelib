package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.*;

/**
 * @author TT432
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class EntityStatistics {
    public static final Codec<EntityStatistics> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.fieldOf("distanceWalked").forGetter(o -> o.distanceWalked)
    ).apply(ins, EntityStatistics::new));

    @Setter
    @Getter
    private float distanceWalked;
}
